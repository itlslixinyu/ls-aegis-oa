package com.ls.aegis.biz.eoms.service.h5;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.ls.aegis.biz.eoms.config.WechatNoticeProperties;
import com.ls.aegis.biz.eoms.constant.EomsConstants;
import com.ls.aegis.biz.eoms.domain.RepairOrder;
import com.ls.aegis.biz.eoms.mapper.h5.H5CustomerCredentialMapper;
import com.ls.aegis.biz.eoms.mapper.repair.RepairOrderMapper;
import com.ls.aegis.compat.common.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
public class WechatNoticeService {
    private static final String PREFIX = "eoms:wechat:notice:";
    private static final DateTimeFormatter CHINA_TIME =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.of("Asia/Shanghai"));

    private final WechatNoticeProperties properties;
    private final RedissonClient redis;
    private final H5CustomerCredentialMapper members;
    private final RepairOrderMapper orders;
    private final ApplicationEventPublisher events;
    private final ThreadPoolTaskExecutor executor;
    private final ObjectMapper json;
    private final RestClient wechat;
    private String accessToken;
    private Instant refreshAt = Instant.EPOCH;

    public WechatNoticeService(WechatNoticeProperties properties, RedissonClient redis,
                              H5CustomerCredentialMapper members, RepairOrderMapper orders,
                              ApplicationEventPublisher events, ThreadPoolTaskExecutor executor, ObjectMapper json) {
        this.properties = properties;
        this.redis = redis;
        this.members = members;
        this.orders = orders;
        this.events = events;
        this.executor = executor;
        this.json = json;
        var http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(4)).build();
        var factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofSeconds(6));
        this.wechat = RestClient.builder().baseUrl("https://api.weixin.qq.com").requestFactory(factory).build();
    }

    public record Ticket(String ticket, String templateId) {}

    public boolean enabled(boolean engineer) {
        return !properties.getAppId().isBlank() && !properties.getAppSecret().isBlank()
            && template(engineer).configured();
    }

    public Ticket issueCustomer(Long tenant, Long customer, Long account, Long apply) {
        return issue(false, tenant + ":" + customer + ":" + apply, account);
    }

    public Ticket issueEngineer(Long tenant, Long engineer) {
        return issue(true, tenant + ":" + engineer, engineer);
    }

    private Ticket issue(boolean engineer, String scope, Long account) {
        if (!enabled(engineer)) throw new ServiceException("微信订阅消息暂未开通");
        var template = template(engineer);
        String ticket = UUID.randomUUID().toString();
        bucket("ticket:" + ticket).set((engineer ? "engineer" : "customer") + "|" + scope + "|" + account
            + "|" + template.getId(), Duration.ofMinutes(3));
        return new Ticket(ticket, template.getId());
    }

    public void redeem(String ticket, String code) {
        String grant = bucket("ticket:" + ticket).getAndDelete();
        if (grant == null) throw new ServiceException("订阅请求已过期，请返回业务页面重试");
        String[] parts = grant.split("\\|", -1);
        boolean engineer = "engineer".equals(parts[0]);
        if (!enabled(engineer) || !template(engineer).getId().equals(parts[3])) {
            throw new ServiceException("订阅模板已变更，请返回业务页面重试");
        }

        JsonNode result;
        try {
            result = readJson(wechat.get().uri(uri -> uri.path("/sns/jscode2session")
                .queryParam("appid", properties.getAppId()).queryParam("secret", properties.getAppSecret())
                .queryParam("js_code", code).queryParam("grant_type", "authorization_code").build())
                .retrieve());
        } catch (RuntimeException ex) {
            // HTTP client errors can contain the request URL including the AppSecret.
            log.warn("微信登录身份交换失败，异常类型 {}", ex.getClass().getSimpleName());
            throw new ServiceException("暂时无法确认微信身份，请稍后重试");
        }
        String openId = result == null ? "" : result.path("openid").asText("");
        if (openId.isBlank()) {
            log.warn("微信登录身份交换被拒绝，微信错误码 {}", result == null ? -1 : result.path("errcode").asInt(-1));
            throw new ServiceException("微信身份无效，请返回业务页面重试");
        }
        bucket(grantKey(engineer, parts[1])).set(parts[2] + "|" + openId + "|" + UUID.randomUUID());
    }

    public void assigned(RepairOrder order, Long engineer) {
        if (enabled(true) && order.getTenantId() != null) {
            events.publishEvent(new Notice(true, order.getTenantId(), engineer, null, order.getId(),
                order.getOrderNo(), Instant.now()));
        }
    }

    public void repaired(RepairOrder order, Instant when) {
        if (enabled(false) && order.getTenantId() != null && order.getCustomerId() != null && order.getApplyId() != null) {
            events.publishEvent(new Notice(false, order.getTenantId(), order.getCustomerId(), order.getApplyId(), order.getId(),
                order.getOrderNo(), when));
        }
    }

    public record Notice(boolean engineer, Long tenant, Long recipient, Long apply, Long order, String number, Instant when) {}

    @TransactionalEventListener
    public void afterCommit(Notice notice) {
        try {
            executor.execute(() -> deliver(notice));
        } catch (RuntimeException ex) {
            log.warn("工单 {} 的微信通知无法加入发送队列，异常类型 {}", notice.order(), ex.getClass().getSimpleName());
        }
    }

    private void deliver(Notice notice) {
        try {
            if (!enabled(notice.engineer())) return;
            String scope = notice.tenant() + ":" + notice.recipient();
            if (!notice.engineer()) scope += ":" + notice.apply();
            RBucket<String> grant = bucket(grantKey(notice.engineer(), scope));
            String pending = grant.get();
            if (pending == null || !recipientEnabled(notice, pending)) return;

            Map<String, Object> data = new HashMap<>();
            var template = template(notice.engineer());
            data.put(template.getNumberField(), Map.of("value", notice.number()));
            data.put(template.getStatusField(), Map.of("value", notice.engineer() ? "待接单" : "维修完成"));
            if (!template.getTimeField().isBlank()) {
                data.put(template.getTimeField(), Map.of("value", CHINA_TIME.format(notice.when())));
            }
            if (!template.getTitleField().isBlank()) {
                data.put(template.getTitleField(), Map.of("value", "设备维修工单"));
            }
            String page = "pages/service/index?role=" + (notice.engineer() ? "engineer" : "customer") + "&id="
                + (notice.engineer() ? notice.order() : notice.apply());
            var payload = Map.of("touser", pending.split("\\|", 3)[1], "template_id", template.getId(),
                "page", page, "miniprogram_state", properties.getMiniprogramState(), "data", data);
            String token = token();
            // Only one sender can consume this authorization, including concurrent deliveries.
            if (!grant.compareAndSet(pending, null)) return;
            JsonNode result = postJson(wechat.post().uri(uri -> uri.path("/cgi-bin/message/subscribe/send")
                .queryParam("access_token", token).build()), payload);
            int code = result == null ? -1 : result.path("errcode").asInt(-1);
            if (code == 0) {
                log.info("工单 {} 的微信订阅消息已被微信接收", notice.order());
            } else {
                log.warn("工单 {} 的微信订阅消息发送失败，微信错误码 {}", notice.order(), code);
            }
        } catch (RuntimeException ex) {
            // Never log the access token, recipient, or API response.
            log.warn("工单 {} 的微信通知发送失败，异常类型 {}", notice.order(), ex.getClass().getSimpleName());
        }
    }

    private boolean recipientEnabled(Notice notice, String grant) {
        var currentOrder = orders.selectRepairOrderById(notice.order());
        if (currentOrder == null || !Objects.equals(notice.tenant(), currentOrder.getTenantId())) return false;
        if (notice.engineer()) {
            return Objects.equals(notice.recipient(), currentOrder.getEngineerId())
                && EomsConstants.ORDER_STATUS_WAIT_ACCEPT.equals(currentOrder.getStatus())
                && orders.countEnabledEngineer(notice.tenant(), notice.recipient()) == 1;
        }
        var credential = members.selectById(Long.valueOf(grant.substring(0, grant.indexOf('|'))));
        return credential != null && Integer.valueOf(1).equals(credential.getStatus())
            && Objects.equals(notice.apply(), currentOrder.getApplyId())
            && Objects.equals(notice.recipient(), currentOrder.getCustomerId())
            && Objects.equals(notice.tenant(), credential.getTenantId())
            && Objects.equals(notice.recipient(), credential.getCustomerId());
    }

    private synchronized String token() {
        if (Instant.now().isBefore(refreshAt)) return accessToken;
        JsonNode result = postJson(wechat.post().uri("/cgi-bin/stable_token"),
            Map.of("grant_type", "client_credential", "appid", properties.getAppId(),
                "secret", properties.getAppSecret(), "force_refresh", false));
        String fetched = result == null ? "" : result.path("access_token").asText("");
        if (fetched.isBlank()) throw new IllegalStateException("微信访问凭证不可用");
        accessToken = fetched;
        refreshAt = Instant.now().plusSeconds(Math.max(0L, result.path("expires_in").asLong(0L) - 120L));
        return fetched;
    }

    private JsonNode postJson(RestClient.RequestBodySpec request, Map<String, ?> payload) {
        byte[] body;
        try {
            body = json.writeValueAsBytes(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("无法序列化微信请求");
        }
        // WeChat returns HTTP 412 for requests sent with chunked transfer encoding.
        return readJson(request.contentType(MediaType.APPLICATION_JSON).contentLength(body.length).body(body).retrieve());
    }

    private JsonNode readJson(RestClient.ResponseSpec response) {
        // WeChat may return JSON bytes with a non-JSON Content-Type.
        byte[] body = response.body(byte[].class);
        if (body == null || body.length == 0) return null;
        try {
            return json.readTree(body);
        } catch (IOException ex) {
            // The payload and parsing error may contain WeChat credentials.
            throw new IllegalStateException("微信接口返回无效 JSON");
        }
    }

    private WechatNoticeProperties.Template template(boolean engineer) {
        return engineer ? properties.getEngineer() : properties.getCustomer();
    }

    private String grantKey(boolean engineer, String scope) {
        return "grant:" + properties.getAppId() + ":" + template(engineer).getId() + ":"
            + (engineer ? "engineer:" : "customer:") + scope;
    }

    private RBucket<String> bucket(String key) {
        return redis.getBucket(PREFIX + key, StringCodec.INSTANCE);
    }
}
