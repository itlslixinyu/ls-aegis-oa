package com.ls.aegis.biz.eoms.h5;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ls.aegis.biz.eoms.config.WechatNoticeProperties;
import com.ls.aegis.biz.eoms.domain.RepairOrder;
import com.ls.aegis.biz.eoms.mapper.h5.H5CustomerCredentialMapper;
import com.ls.aegis.biz.eoms.mapper.repair.RepairOrderMapper;
import com.ls.aegis.biz.eoms.service.h5.WechatNoticeService;
import com.ls.aegis.compat.common.exception.ServiceException;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class WechatNoticeServiceTest {
    private final WechatNoticeProperties config = new WechatNoticeProperties();
    private final RedissonClient redis = mock(RedissonClient.class);
    private final RepairOrderMapper orders = mock(RepairOrderMapper.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final ThreadPoolTaskExecutor executor = mock(ThreadPoolTaskExecutor.class);
    private final WechatNoticeService service = new WechatNoticeService(config, redis,
        mock(H5CustomerCredentialMapper.class), orders, events, executor, new ObjectMapper());

    @Test
    void disabledIntegrationDoesNotPublishOrReadRedis() {
        assertThrows(ServiceException.class, () -> service.issueEngineer(0L, 21L));
        service.assigned(new RepairOrder(), 21L);
        verifyNoInteractions(redis, events);
    }

    @Test
    void invalidTicketDoesNotCallWechat() {
        var builder = RestClient.builder().baseUrl("https://api.weixin.qq.com");
        var http = MockRestServiceServer.bindTo(builder).build();
        ReflectionTestUtils.setField(service, "wechat", builder.build());
        when(redis.<String>getBucket("eoms:wechat:notice:ticket:expired", StringCodec.INSTANCE)).thenReturn(mockBucket());
        assertThrows(ServiceException.class, () -> service.redeem("expired", "example-code"));
        http.verify();
    }

    @Test
    void acceptsWechatLoginJsonWithTextContentTypeAndSavesGrant() {
        configureEngineerTemplate();
        RBucket<String> ticket = mockBucket();
        when(ticket.getAndDelete()).thenReturn("engineer|0:21|21|test-template", (String) null);
        when(redis.<String>getBucket("eoms:wechat:notice:ticket:ticket", StringCodec.INSTANCE)).thenReturn(ticket);
        RBucket<String> grant = mockBucket();
        when(redis.<String>getBucket("eoms:wechat:notice:grant:mock-app:test-template:engineer:0:21",
            StringCodec.INSTANCE)).thenReturn(grant);

        var builder = RestClient.builder().baseUrl("https://api.weixin.qq.com");
        var http = MockRestServiceServer.bindTo(builder).build();
        ReflectionTestUtils.setField(service, "wechat", builder.build());
        http.expect(requestTo("https://api.weixin.qq.com/sns/jscode2session?appid=mock-app&secret=mock-secret&js_code=example-code&grant_type=authorization_code"))
            .andRespond(withSuccess("{\"openid\":\"synthetic-openid\",\"session_key\":\"example\"}", MediaType.TEXT_PLAIN));

        service.redeem("ticket", "example-code");
        verify(grant).set(startsWith("21|synthetic-openid|"));
        assertThrows(ServiceException.class, () -> service.redeem("ticket", "example-code"));
        http.verify();
    }

    private void configureEngineerTemplate() {
        config.setAppId("mock-app");
        config.setAppSecret("mock-secret");
        config.getEngineer().setId("test-template");
        config.getEngineer().setNumberField("character_string21");
        config.getEngineer().setStatusField("thing20");
        config.getEngineer().setTimeField("time23");
        config.getEngineer().setTitleField("thing4");
    }

    @Test
    void sendsToCurrentlyAssignedEngineerAndConsumesOnlyOneGrant() {
        configureEngineerTemplate();
        var order = new RepairOrder();
        order.setTenantId(0L);
        order.setEngineerId(21L);
        order.setStatus("WAIT_ACCEPT");
        when(orders.selectRepairOrderById(33L)).thenReturn(order);
        when(orders.countEnabledEngineer(0L, 21L)).thenReturn(1);

        RBucket<String> grant = mockBucket();
        String authorized = "21|synthetic-openid|nonce";
        when(redis.<String>getBucket("eoms:wechat:notice:grant:mock-app:test-template:engineer:0:21",
            StringCodec.INSTANCE)).thenReturn(grant);
        when(grant.get()).thenReturn(authorized, (String) null);
        when(grant.compareAndSet(authorized, null)).thenReturn(true);
        doAnswer(invocation -> {
            invocation.<Runnable>getArgument(0).run();
            return null;
        }).when(executor).execute(any(Runnable.class));

        var builder = RestClient.builder().baseUrl("https://api.weixin.qq.com");
        var http = MockRestServiceServer.bindTo(builder).build();
        ReflectionTestUtils.setField(service, "wechat", builder.build());
        http.expect(requestTo("https://api.weixin.qq.com/cgi-bin/stable_token"))
            .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
            .andExpect(request -> assertTrue(request.getHeaders().getContentLength() > 0))
            .andRespond(withSuccess("{\"access_token\":\"test-token\",\"expires_in\":7200}", MediaType.TEXT_PLAIN));
        http.expect(requestTo("https://api.weixin.qq.com/cgi-bin/message/subscribe/send?access_token=test-token"))
            .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
            .andExpect(request -> assertTrue(request.getHeaders().getContentLength() > 0))
            .andExpect(jsonPath("$.touser").value("synthetic-openid"))
            .andExpect(jsonPath("$.page").value("pages/service/index?role=engineer&id=33"))
            .andExpect(jsonPath("$.data.length()").value(4))
            .andExpect(jsonPath("$.data.character_string21.value").value("TEST-33"))
            .andExpect(jsonPath("$.data.thing4.value").value("设备维修工单"))
            .andExpect(jsonPath("$.data.time23.value").value("1970-01-01 08:00"))
            .andExpect(jsonPath("$.data.thing20.value").value("待接单"))
            .andRespond(withSuccess("{\"errcode\":0}", MediaType.APPLICATION_OCTET_STREAM));

        var event = new WechatNoticeService.Notice(true, 0L, 21L, null, 33L, "TEST-33", Instant.EPOCH);
        service.afterCommit(event);
        service.afterCommit(event);
        verify(grant, times(1)).compareAndSet(authorized, null);
        http.verify();
    }

    @SuppressWarnings("unchecked")
    private RBucket<String> mockBucket() {
        return mock(RBucket.class);
    }
}
