package com.ls.aegis.biz.eoms.service.h5;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.dao.DuplicateKeyException;
import com.ls.aegis.compat.common.core.redis.RedisCache;
import com.ls.aegis.compat.common.exception.ServiceException;
import com.ls.aegis.compat.common.utils.StringUtils;
import com.ls.aegis.biz.eoms.constant.EomsConstants;
import com.ls.aegis.biz.eoms.domain.H5CustomerInfo;
import com.ls.aegis.biz.eoms.domain.H5CustomerCredential;
import com.ls.aegis.biz.eoms.mapper.h5.H5CustomerCredentialMapper;
import com.ls.aegis.biz.eoms.model.query.H5OnlineTokenQuery;
import com.ls.aegis.biz.eoms.model.resp.H5OnlineTokenResp;
import com.ls.aegis.common.api.system.SysOptionApi;
import com.ls.aegis.crypto.api.IGmCrypto;
import cn.hutool.crypto.SecureUtil;
import top.continew.starter.extension.crud.model.query.PageQuery;
import top.continew.starter.extension.crud.model.resp.PageResp;

/**
 * H5 客户身份令牌服务
 * 基于 Redis Hash 存储客户令牌，支持生成、校验、刷新、失效
 * 国密开启时令牌由 SM3 摘要生成；管理端展示 SM3 摘要前缀，不记录完整令牌日志
 */
@Component
public class H5TokenService
{
    private static final Logger log = LoggerFactory.getLogger(H5TokenService.class);

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private com.ls.aegis.common.api.psi.H5CustomerAccessApi customerAccessApi;

    @Autowired
    private org.redisson.api.RedissonClient redissonClient;

    @Autowired
    private H5CustomerCredentialMapper credentialMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final java.security.SecureRandom INVITATION_RANDOM = new java.security.SecureRandom();
    private static final String INVITATION_PREFIX = "eoms:h5:invitation:v3:";
    private static final String SESSION_HANDLE_PREFIX = "eoms:h5:session-handle:v3:";
    private static final String AUTH_VERSION = "3";
    private static final int INVITATION_SECONDS = 600;

    public record Invitation(String invitationCode, int expiresInSeconds, String customerId) {}

    /** Legacy staff call bootstraps an admin only for a company without any accounts. */
    public Invitation createInvitation(Long customerId) {
        var customer = customerAccessApi.requireManagedCustomer(customerId);
        String role = credentialMapper.list(customer.tenantId(), customer.customerId()).isEmpty() ? "ADMIN" : "MEMBER";
        return createMemberInvitation(customerId, role, null);
    }

    public Invitation createMemberInvitation(Long customerId, String role, Long resetAccountId) {
        var customer = customerAccessApi.requireManagedCustomer(customerId);
        if (!Set.of("ADMIN", "MEMBER").contains(role)) throw new ServiceException("成员角色无效");
        long resetVersion = 0L;
        if (resetAccountId != null) {
            var member = requireManagedMember(customerId, resetAccountId);
            if (!Integer.valueOf(1).equals(member.getStatus())) throw new ServiceException("请先启用该成员再重置密码");
            role = member.getAccountRole(); resetVersion = member.getAuthVersion();
        }
        String binding = customer.tenantId() + ":" + customer.customerId() + ":" +
            (resetAccountId == null ? "CREATE:0:" : "RESET:" + resetAccountId + ":") + role + ":" + resetVersion;
        for (int attempt = 0; attempt < 5; attempt++) {
            String code = String.format(java.util.Locale.ROOT, "%08d", INVITATION_RANDOM.nextInt(100_000_000));
            org.redisson.api.RBucket<String> bucket = redissonClient.getBucket(invitationKey(code), org.redisson.client.codec.StringCodec.INSTANCE);
            if (bucket.trySet(binding, INVITATION_SECONDS, TimeUnit.SECONDS))
                return new Invitation(code, INVITATION_SECONDS, customer.customerId().toString());
        }
        throw new ServiceException("访问码暂时无法生成，请稍后重试");
    }

    public List<H5CustomerCredential> listMembers(Long customerId) {
        var customer = customerAccessApi.requireManagedCustomer(customerId);
        return credentialMapper.list(customer.tenantId(), customer.customerId());
    }

    private H5CustomerCredential requireManagedMember(Long customerId, Long accountId) {
        var customer = customerAccessApi.requireManagedCustomer(customerId);
        var member = credentialMapper.selectById(accountId);
        if (member == null || !customer.tenantId().equals(member.getTenantId()) || !customer.customerId().equals(member.getCustomerId()))
            throw new ServiceException("成员不存在或无权操作");
        return member;
    }

    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public void updateMember(Long customerId, Long accountId, String name, String role, Integer status) {
        var member = requireManagedMember(customerId, accountId);
        if (!Set.of("ADMIN", "MEMBER").contains(role) || status == null || (status != 0 && status != 1))
            throw new ServiceException("成员角色或状态无效");
        var locked = credentialMapper.lockMembers(member.getTenantId(), customerId);
        member = locked.stream().filter(m -> m.getId().equals(accountId)).findFirst().orElseThrow(() -> new ServiceException("成员已变更"));
        if ("ADMIN".equals(member.getAccountRole()) && Integer.valueOf(1).equals(member.getStatus())
            && (!"ADMIN".equals(role) || status == 0)
            && locked.stream().filter(m -> "ADMIN".equals(m.getAccountRole()) && Integer.valueOf(1).equals(m.getStatus())).count() <= 1)
            throw new ServiceException("请保留至少一位启用的公司管理员");
        member.setDisplayName(memberName(name, member.getLoginName())); member.setAccountRole(role); member.setStatus(status);
        if (credentialMapper.updateMember(member) != 1) throw new ServiceException("成员已变更，请刷新后重试");
    }

    private String invitationKey(String code) { return INVITATION_PREFIX + SecureUtil.sha256(code); }

    /** No code-only login, including internal callers: every session needs a named account. */
    @Deprecated
    public H5CustomerInfo exchangeInvitation(String code) { throw new ServiceException("请使用邀请码激活个人账号后登录"); }

    public H5CustomerInfo activateInvitation(String code, String loginName, String rawPassword) {
        return activateInvitation(code, loginName, rawPassword, null);
    }

    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public H5CustomerInfo activateInvitation(String code, String loginName, String rawPassword, String displayName) {
        String normalizedName = normalizeLoginName(loginName); validateCustomerPassword(rawPassword);
        Invite invite = readInvitation(code, false);
        var customer = customerAccessApi.requireAvailableCustomer(invite.tenantId(), invite.customerId());
        H5CustomerCredential credential;
        if ("RESET".equals(invite.purpose())) {
            credential = credentialMapper.selectById(invite.accountId());
            if (credential == null || !invite.tenantId().equals(credential.getTenantId()) || !invite.customerId().equals(credential.getCustomerId())
                || !Integer.valueOf(1).equals(credential.getStatus()) || !normalizedName.equals(credential.getLoginName()) || !invite.accountVersion().equals(credential.getAuthVersion()))
                throw new ServiceException("请输入该重置码指定成员的原用户名");
        } else {
            if (credentialMapper.selectByLoginName(normalizedName) != null) throw new ServiceException("用户名已被占用，请更换；重置密码须使用该成员的重置码");
            credential = new H5CustomerCredential(); credential.setTenantId(invite.tenantId()); credential.setCustomerId(invite.customerId());
            credential.setLoginName(normalizedName); credential.setDisplayName(memberName(displayName, normalizedName));
            credential.setAccountRole(invite.role()); credential.setStatus(1); credential.setAuthVersion(1L);
        }
        if (!invite.equals(readInvitation(code, true))) throw new ServiceException("邀请码已变更，请重试");
        credential.setPasswordHash(passwordEncoder.encode(rawPassword));
        try {
            int changed = "RESET".equals(invite.purpose()) ? credentialMapper.updatePassword(credential) : credentialMapper.insert(credential);
            if (changed != 1) throw new ServiceException("账号已变更，请重新获取邀请码后重试");
        } catch (DuplicateKeyException ex) { throw new ServiceException("用户名已被占用，请重新获取邀请码后重试"); }
        return generateVerifiedToken(customer, credentialMapper.selectById(credential.getId()));
    }

    private static String memberName(String name, String fallback) {
        String value = name == null || name.isBlank() ? fallback : name.trim();
        if (value.length() > 50 || value.chars().anyMatch(Character::isISOControl)) throw new ServiceException("姓名最多50字且不可含控制字符");
        return value;
    }

    /** Password login shares the existing customer-token boundary and rechecks customer status. */
    public H5CustomerInfo loginWithPassword(String loginName, String rawPassword, String clientIp) {
        String normalizedName = normalizeLoginName(loginName);
        String attemptKey = "eoms:h5:password-fail:" + SecureUtil.sha256(normalizedName + ":" + clientIp);
        org.redisson.api.RAtomicLong failures = redissonClient.getAtomicLong(attemptKey);
        if (failures.get() >= 5) {
            throw new ServiceException("用户名或密码不正确，或账号暂时无法登录");
        }
        H5CustomerCredential credential = credentialMapper.selectByLoginName(normalizedName);
        boolean valid = credential != null && Integer.valueOf(1).equals(credential.getStatus()) && passwordEncoder.matches(rawPassword, credential.getPasswordHash());
        if (!valid) {
            failures.incrementAndGet();
            failures.expire(java.time.Duration.ofMinutes(15));
            throw new ServiceException("用户名或密码不正确，或账号暂时无法登录");
        }
        failures.delete();
        var customer = customerAccessApi.requireAvailableCustomer(credential.getTenantId(), credential.getCustomerId());
        return generateVerifiedToken(customer, credential);
    }

    private static String normalizeLoginName(String loginName) {
        if (loginName == null || !loginName.matches("[A-Za-z][A-Za-z0-9._-]{3,31}")) {
            throw new ServiceException("用户名需为4-32位字母开头的字母、数字或 . _ -");
        }
        return loginName.toLowerCase(java.util.Locale.ROOT);
    }

    private static void validateCustomerPassword(String password) {
        if (password == null || !password.matches("(?=.*[A-Za-z])(?=.*\\d)[\\x21-\\x7E]{8,32}")) {
            throw new ServiceException("密码需为8-32位，至少包含字母和数字");
        }
    }

    private record Invite(Long tenantId, Long customerId, String purpose, Long accountId, String role, Long accountVersion) {}
    private Invite readInvitation(String code, boolean consume) {
        if (code == null || !code.matches("\\d{8}")) throw new ServiceException("请使用管理员签发的8位一次性邀请码");
        org.redisson.api.RBucket<String> bucket = redissonClient.getBucket(invitationKey(code), org.redisson.client.codec.StringCodec.INSTANCE);
        String binding = consume ? bucket.getAndDelete() : bucket.get();
        if (binding == null) throw new ServiceException("邀请码无效、已使用或已过期");
        try {
            String[] parts = binding.split(":", -1);
            if (parts.length != 6) throw new IllegalArgumentException();
            var invite = new Invite(Long.valueOf(parts[0]),Long.valueOf(parts[1]),parts[2],Long.valueOf(parts[3]),parts[4],Long.valueOf(parts[5]));
            if (invite.tenantId() < 0 || invite.customerId() <= 0 || !Set.of("ADMIN","MEMBER").contains(invite.role())
                || !("CREATE".equals(invite.purpose()) && invite.accountId() == 0 && invite.accountVersion() == 0 || "RESET".equals(invite.purpose()) && invite.accountId() > 0 && invite.accountVersion() > 0)) throw new IllegalArgumentException();
            return invite;
        } catch (IllegalArgumentException ex) { throw new ServiceException("邀请码无效，请重新获取"); }
    }

    @Autowired
    private SysOptionApi sysOptionApi;

    @Autowired
    private ObjectProvider<IGmCrypto> gmCryptoProvider;

    /** Redis Hash 字段：客户ID */
    private static final String FIELD_CUSTOMER_ID = "customerId";
    /** Redis Hash 字段：客户编号 */
    private static final String FIELD_CUSTOMER_NO = "customerNo";
    /** Redis Hash 字段：客户名称 */
    private static final String FIELD_CUSTOMER_NAME = "customerName";
    /** Redis Hash 字段：签发时间戳 */
    private static final String FIELD_LOGIN_TIME = "loginTime";

    /**
     * 根据客户编号生成临时访问令牌
     *
     * @param customerNo 客户编号
     * @return H5 客户身份信息（含 token）
     */
    @Deprecated
    public H5CustomerInfo generateToken(String customerNo) {
        // Intentionally no legacy flag: a public business number never proves customer identity.
        throw new ServiceException("客户编号不能用于登录，请使用一次性邀请码", EomsConstants.CODE_H5_TOKEN_INVALID);
    }

    private H5CustomerInfo generateVerifiedToken(com.ls.aegis.common.api.psi.H5CustomerAccessApi.Customer customer, H5CustomerCredential credential)
    {
        if (credential == null || credential.getId() == null || !Integer.valueOf(1).equals(credential.getStatus())) throw new ServiceException("成员账号不可用");
        String token = createTokenValue();
        String key = EomsConstants.H5_TOKEN_KEY + token;
        int expireMinutes = resolveExpireMinutes();

        redisCache.setCacheMapValue(key, "authVersion", AUTH_VERSION);
        redisCache.setCacheMapValue(key, "accountId", credential.getId().toString());
        redisCache.setCacheMapValue(key, "accountVersion", credential.getAuthVersion().toString());
        redisCache.setCacheMapValue(key, "loginName", credential.getLoginName());
        redisCache.setCacheMapValue(key, "displayName", credential.getDisplayName());
        redisCache.setCacheMapValue(key, "tenantId", String.valueOf(customer.tenantId()));
        redisCache.setCacheMapValue(key, FIELD_CUSTOMER_ID, String.valueOf(customer.customerId()));
        redisCache.setCacheMapValue(key, FIELD_CUSTOMER_NO, customer.customerNo());
        redisCache.setCacheMapValue(key, FIELD_CUSTOMER_NAME,
                customer.customerName() == null ? "" : customer.customerName());
        redisCache.setCacheMapValue(key, FIELD_LOGIN_TIME, String.valueOf(System.currentTimeMillis()));
        redisCache.expire(key, expireMinutes, TimeUnit.MINUTES);
        sessionHandleBucket(token).set(token, java.time.Duration.ofMinutes(expireMinutes));

        log.info("H5 客户令牌已签发，客户编号={}，摘要={}", customer.customerNo(), digestToken(token));

        H5CustomerInfo info = new H5CustomerInfo(customer.customerId(), customer.customerNo(), customer.customerName());
        info.setTenantId(customer.tenantId());
        applyMember(info, credential);
        info.setToken(token);
        return info;
    }

    /**
     * 校验令牌有效性，返回客户身份信息
     */
    public H5CustomerInfo validateToken(String token)
    {
        if (StringUtils.isEmpty(token))
        {
            return null;
        }
        Map<String, Object> entries = redisCache.getCacheMap(EomsConstants.H5_TOKEN_KEY + token);
        if (entries == null || !AUTH_VERSION.equals(entries.get("authVersion")))
        {
            return null;
        }
        Object idObj = entries.get(FIELD_CUSTOMER_ID);
        Object noObj = entries.get(FIELD_CUSTOMER_NO);
        Object nameObj = entries.get(FIELD_CUSTOMER_NAME);
        if (idObj == null)
        {
            return null;
        }
        try
        {
            Long customerId = Long.parseLong(String.valueOf(idObj));
            Long tenantId = Long.valueOf(String.valueOf(entries.get("tenantId")));
            if (tenantId < 0 || customerId <= 0) return null;
            var member = credentialMapper.selectById(Long.valueOf(String.valueOf(entries.get("accountId"))));
            if (member == null || member.getAuthVersion() == null || member.getAuthVersion() <= 0 || !Integer.valueOf(1).equals(member.getStatus()) || !tenantId.equals(member.getTenantId())
                || !customerId.equals(member.getCustomerId()) || !String.valueOf(member.getAuthVersion()).equals(String.valueOf(entries.get("accountVersion")))
                || !Set.of("ADMIN", "MEMBER").contains(member.getAccountRole())) return null;
            // Current unit status is checked on every request; disable/deletion revokes old sessions too.
            var customer = customerAccessApi.requireAvailableCustomer(tenantId, customerId);
            H5CustomerInfo info = new H5CustomerInfo(customerId, customer.customerNo(), customer.customerName());
            info.setTenantId(tenantId);
            applyMember(info, member);
            return info;
        }
        catch (RuntimeException e)
        {
            return null;
        }
    }

    private void applyMember(H5CustomerInfo info, H5CustomerCredential member) {
        info.setAccountId(member.getId()); info.setLoginName(member.getLoginName());
        info.setDisplayName(member.getDisplayName()); info.setAccountRole(member.getAccountRole());
    }

    /**
     * 刷新令牌有效期（按配置决定是否续期）
     */
    public boolean refreshToken(String token)
    {
        if (StringUtils.isEmpty(token) || validateToken(token) == null || !resolveAutoRefresh())
        {
            return false;
        }
        String key = EomsConstants.H5_TOKEN_KEY + token;
        long ttl = redisCache.getExpire(key);
        if (ttl <= 0)
        {
            return false;
        }
        int minutes = resolveExpireMinutes();
        redisCache.expire(key, minutes, TimeUnit.MINUTES);
        sessionHandleBucket(token).expire(java.time.Duration.ofMinutes(minutes));
        return true;
    }

    /**
     * 主动失效令牌（登出 / 强退）
     */
    public void removeToken(String token)
    {
        if (StringUtils.isNotEmpty(token))
        {
            redisCache.deleteObject(EomsConstants.H5_TOKEN_KEY + token);
            sessionHandleBucket(token).delete();
            log.info("H5 客户令牌已失效，摘要={}", digestToken(token));
        }
    }

    private org.redisson.api.RBucket<String> sessionHandleBucket(String token) {
        return redissonClient.getBucket(SESSION_HANDLE_PREFIX + SecureUtil.sha256(token), org.redisson.client.codec.StringCodec.INSTANCE);
    }

    /** Administrative handle is never accepted by H5 authentication. Staff scope is rechecked. */
    public void kickoutSession(String sessionId) {
        if (sessionId == null || !sessionId.matches("[a-f0-9]{64}")) throw new ServiceException("会话不存在或无权操作");
        org.redisson.api.RBucket<String> handle = redissonClient.getBucket(SESSION_HANDLE_PREFIX + sessionId, org.redisson.client.codec.StringCodec.INSTANCE);
        String token = handle.get();
        H5CustomerInfo info = validateToken(token);
        if (info == null) throw new ServiceException("会话不存在或无权操作");
        var managed = customerAccessApi.requireManagedCustomer(info.getCustomerId());
        if (!info.getTenantId().equals(managed.tenantId())) throw new ServiceException("会话不存在或无权操作");
        removeToken(token);
    }

    /**
     * 分页查询在线 H5 客户令牌
     */
    public PageResp<H5OnlineTokenResp> pageOnline(H5OnlineTokenQuery query, PageQuery pageQuery)
    {
        List<H5OnlineTokenResp> all = listOnline(query);
        int page = pageQuery.getPage() == null ? 1 : pageQuery.getPage();
        int size = pageQuery.getSize() == null ? 10 : pageQuery.getSize();
        return PageResp.build(page, size, all);
    }

    /**
     * 查询全部在线 H5 客户令牌（按登录时间倒序）
     */
    public List<H5OnlineTokenResp> listOnline(H5OnlineTokenQuery query)
    {
        // Session hashes use RedisTemplate's configured key serializer (JDK by default),
        // so raw UTF-8 SCAN patterns cannot enumerate them. Scan the existing opaque
        // handle index through Redisson, which also maps/unmaps the configured namespace.
        Iterable<String> handles = redissonClient.getKeys().getKeysByPattern(SESSION_HANDLE_PREFIX + "*", 1000);
        List<H5OnlineTokenResp> list = new ArrayList<>();
        String noFilter = query == null ? null : query.getCustomerNo();
        String nameFilter = query == null ? null : query.getCustomerName();

        for (String handleKey : handles)
        {
            org.redisson.api.RBucket<String> handle = redissonClient.getBucket(handleKey, org.redisson.client.codec.StringCodec.INSTANCE);
            String token = handle.get();
            if (StringUtils.isEmpty(token) || !handleKey.equals(SESSION_HANDLE_PREFIX + SecureUtil.sha256(token))) continue;
            String key = EomsConstants.H5_TOKEN_KEY + token;
            long ttl = redisCache.getExpire(key);
            if (ttl <= 0)
            {
                continue;
            }
            Map<String, Object> entries = redisCache.getCacheMap(key);
            if (entries == null || entries.isEmpty())
            {
                continue;
            }
            if (!AUTH_VERSION.equals(entries.get("authVersion")) || validateToken(token) == null) continue;
            try {
                Long tenant = Long.valueOf(String.valueOf(entries.get("tenantId")));
                Long customerId = Long.valueOf(String.valueOf(entries.get(FIELD_CUSTOMER_ID)));
                var managed = customerAccessApi.requireManagedCustomer(customerId);
                if (!tenant.equals(managed.tenantId())) continue;
            } catch (RuntimeException denied) { continue; }
            H5OnlineTokenResp resp = toOnlineResp(token, entries, ttl);
            if (resp == null)
            {
                continue;
            }
            if (StringUtils.isNotEmpty(noFilter)
                    && (resp.getCustomerNo() == null || !resp.getCustomerNo().contains(noFilter.trim())))
            {
                continue;
            }
            if (StringUtils.isNotEmpty(nameFilter)
                    && (resp.getCustomerName() == null || !resp.getCustomerName().contains(nameFilter.trim())))
            {
                continue;
            }
            list.add(resp);
        }
        list.sort(Comparator.comparing(H5OnlineTokenResp::getLoginTime,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return list;
    }

    /** 读取配置的令牌有效期（分钟） */
    public int resolveExpireMinutes()
    {
        try
        {
            String raw = sysOptionApi.getValue(EomsConstants.OPTION_CATEGORY,
                    EomsConstants.OPTION_CODE_H5_TOKEN_EXPIRE_MINUTES);
            if (StringUtils.isNotEmpty(raw))
            {
                int parsed = Integer.parseInt(raw.trim());
                if (parsed >= EomsConstants.H5_TOKEN_EXPIRE_MIN && parsed <= EomsConstants.H5_TOKEN_EXPIRE_MAX)
                {
                    return parsed;
                }
            }
        }
        catch (Exception e)
        {
            log.warn("读取 H5 令牌有效期配置失败，使用默认值", e);
        }
        return EomsConstants.H5_TOKEN_EXPIRE_MINUTES;
    }

    /** 是否开启活跃续期 */
    public boolean resolveAutoRefresh()
    {
        String raw = sysOptionApi.getValue(EomsConstants.OPTION_CATEGORY,
                EomsConstants.OPTION_CODE_H5_TOKEN_AUTO_REFRESH, "1");
        return !"0".equals(StringUtils.trim(raw));
    }

    /** 令牌 SM3/MD5 摘要（管理端展示与审计） */
    public String digestToken(String token)
    {
        if (StringUtils.isEmpty(token))
        {
            return "";
        }
        IGmCrypto gmCrypto = gmCryptoProvider.getIfAvailable();
        if (gmCrypto != null && gmCrypto.isEnabled())
        {
            return gmCrypto.sm3Hex(token.getBytes(StandardCharsets.UTF_8));
        }
        return SecureUtil.md5(token);
    }

    private H5OnlineTokenResp toOnlineResp(String token, Map<String, Object> entries, long ttlSeconds)
    {
        Object idObj = entries.get(FIELD_CUSTOMER_ID);
        if (idObj == null)
        {
            return null;
        }
        H5OnlineTokenResp resp = new H5OnlineTokenResp();
        try
        {
            resp.setCustomerId(Long.parseLong(String.valueOf(idObj)));
        }
        catch (NumberFormatException e)
        {
            return null;
        }
        Object noObj = entries.get(FIELD_CUSTOMER_NO);
        Object nameObj = entries.get(FIELD_CUSTOMER_NAME);
        Object loginObj = entries.get(FIELD_LOGIN_TIME);
        resp.setCustomerNo(noObj == null ? null : String.valueOf(noObj));
        resp.setCustomerName(nameObj == null ? null : String.valueOf(nameObj));
        resp.setLoginName(entries.get("loginName") == null ? null : String.valueOf(entries.get("loginName")));
        resp.setDisplayName(entries.get("displayName") == null ? null : String.valueOf(entries.get("displayName")));
        if (loginObj != null)
        {
            try
            {
                long millis = Long.parseLong(String.valueOf(loginObj));
                resp.setLoginTime(LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault()));
            }
            catch (NumberFormatException ignored)
            {
                // 忽略非法时间戳
            }
        }
        resp.setExpireSeconds(ttlSeconds);
        String digest = digestToken(token);
        resp.setTokenDigest(digest.length() > 16 ? digest.substring(0, 16) + "…" : digest);
        resp.setSessionId(SecureUtil.sha256(token));
        return resp;
    }

    /**
     * 生成令牌：国密开启时用 SM3，关闭时用 UUID（兼容 gm.enable=false 环境）
     */
    private String createTokenValue()
    {
        String raw = UUID.randomUUID().toString().replace("-", "") + System.nanoTime();
        IGmCrypto gmCrypto = gmCryptoProvider.getIfAvailable();
        if (gmCrypto != null && gmCrypto.isEnabled())
        {
            return gmCrypto.sm3Hex(raw.getBytes(StandardCharsets.UTF_8));
        }
        return UUID.randomUUID().toString().replace("-", "");
    }
}
