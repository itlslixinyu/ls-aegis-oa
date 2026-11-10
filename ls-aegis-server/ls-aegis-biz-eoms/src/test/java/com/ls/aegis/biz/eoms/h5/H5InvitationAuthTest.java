package com.ls.aegis.biz.eoms.h5;

import com.ls.aegis.biz.eoms.service.h5.H5TokenService;
import com.ls.aegis.biz.eoms.controller.admin.H5AuthAdminController;
import com.ls.aegis.biz.eoms.domain.H5CustomerInfo;
import com.ls.aegis.biz.eoms.constant.EomsConstants;
import com.ls.aegis.common.api.psi.H5CustomerAccessApi;
import com.ls.aegis.common.api.system.SysOptionApi;
import com.ls.aegis.compat.common.core.redis.RedisCache;
import com.ls.aegis.crypto.api.IGmCrypto;
import cn.dev33.satoken.annotation.SaCheckPermission;
import org.junit.jupiter.api.*;
import org.redisson.api.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class H5InvitationAuthTest {
    H5TokenService service;
    RedisCache cache;
    RedissonClient redis;
    RBucket<String> bucket;
    H5CustomerAccessApi access;
    RKeys keys;
    final H5CustomerAccessApi.Customer customer = new H5CustomerAccessApi.Customer(99L, 7L, "WLDW0001", "synthetic");
    final String code = "a".repeat(43);
    @BeforeEach @SuppressWarnings("unchecked") void setup() {
        service = new H5TokenService(); cache = mock(RedisCache.class); redis = mock(RedissonClient.class);
        access = mock(H5CustomerAccessApi.class); bucket = mock(RBucket.class);
        keys = mock(RKeys.class); when(redis.getKeys()).thenReturn(keys);
        when(redis.<String>getBucket(anyString(), any(org.redisson.client.codec.StringCodec.class))).thenReturn(bucket);
        when(bucket.trySet(anyString(), eq(600L), eq(TimeUnit.SECONDS))).thenReturn(true);
        when(access.requireManagedCustomer(99L)).thenReturn(customer);
        when(access.requireAvailableCustomer(7L, 99L)).thenReturn(customer);
        ReflectionTestUtils.setField(service, "redisCache", cache);
        ReflectionTestUtils.setField(service, "redissonClient", redis);
        ReflectionTestUtils.setField(service, "customerAccessApi", access);
        ReflectionTestUtils.setField(service, "sysOptionApi", mock(SysOptionApi.class));
        ReflectionTestUtils.setField(service, "gmCryptoProvider", mock(ObjectProvider.class));
    }
    Map<String,Object> session() { return new HashMap<>(Map.of("customerId","99","tenantId","7","authVersion","2")); }
    @Test void customerNumberAloneAlwaysRejected() {
        assertThrows(RuntimeException.class, () -> service.generateToken("WLDW0001"));
        verifyNoInteractions(cache,access,redis);
    }
    @Test void invitationIsEightDigitsShortLivedAndOnlyDigestStored() {
        var invitation = service.createInvitation(99L);
        assertTrue(invitation.invitationCode().matches("\\d{8}"));
        assertEquals(600, invitation.expiresInSeconds());
        assertEquals("99", invitation.customerId());
        var keys = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(redis).getBucket(keys.capture(),any(org.redisson.client.codec.StringCodec.class));
        assertFalse(keys.getValue().contains(invitation.invitationCode()));
        assertTrue(keys.getValue().endsWith(cn.hutool.crypto.SecureUtil.sha256(invitation.invitationCode())));
        verify(bucket).trySet("7:99",600L,TimeUnit.SECONDS);
    }
    @Test void deniedStaffCannotCreateInvitation() {
        when(access.requireManagedCustomer(99L)).thenThrow(new IllegalStateException("denied"));
        assertThrows(RuntimeException.class, () -> service.createInvitation(99L));
        verifyNoInteractions(redis,cache);
    }
    @Test void atomicallyConsumedInviteCreatesVersionedTenantSession() {
        when(bucket.getAndDelete()).thenReturn("7:99",null);
        H5CustomerInfo result = service.exchangeInvitation(code);
        assertEquals(7L,result.getTenantId()); assertEquals(99L,result.getCustomerId()); assertNotNull(result.getToken());
        verify(cache).setCacheMapValue(anyString(),eq("authVersion"),eq("2"));
        verify(cache).setCacheMapValue(anyString(),eq("tenantId"),eq("7"));
        assertThrows(RuntimeException.class, () -> service.exchangeInvitation(code));
        verify(access,times(1)).requireAvailableCustomer(7L,99L);
    }
    @Test @Timeout(45) void concurrentExchangeHasExactlyOneWinner() throws Exception {
        AtomicReference<String> binding = new AtomicReference<>("7:99");
        CountDownLatch consuming = new CountDownLatch(2);
        when(bucket.getAndDelete()).thenAnswer(call -> {
            consuming.countDown();
            assertTrue(consuming.await(10, TimeUnit.SECONDS), "Both attempts must reach atomic consume");
            return binding.getAndSet(null);
        });
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch ready = new CountDownLatch(2), start = new CountDownLatch(1);
            Callable<Boolean> task = () -> {
                ready.countDown();
                assertTrue(start.await(10, TimeUnit.SECONDS));
                try { service.exchangeInvitation(code); return true; }
                catch (RuntimeException denied) { return false; }
            };
            Future<Boolean> a = pool.submit(task), b = pool.submit(task);
            assertTrue(ready.await(10, TimeUnit.SECONDS), "Both workers must be scheduled before starting");
            start.countDown();
            // Correctness test, not a 3-second SLA including cold JVM/mock initialization.
            assertNotEquals(a.get(10, TimeUnit.SECONDS), b.get(10, TimeUnit.SECONDS));
            verify(bucket, times(2)).getAndDelete();
            verify(access,times(1)).requireAvailableCustomer(7L,99L);
            verify(cache, times(1)).setCacheMapValue(anyString(), eq("authVersion"), eq("2"));
        } finally {
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        }
    }
    @Test void expiredMalformedOrDisabledInviteNeverCreatesSession() {
        assertThrows(RuntimeException.class,()->service.exchangeInvitation(null));
        assertThrows(RuntimeException.class,()->service.exchangeInvitation("WLDW0001"));
        when(bucket.getAndDelete()).thenReturn(null,"7:99","bad");
        assertThrows(RuntimeException.class,()->service.exchangeInvitation(code));
        when(access.requireAvailableCustomer(7L,99L)).thenThrow(new IllegalStateException("disabled"));
        assertThrows(RuntimeException.class,()->service.exchangeInvitation(code));
        assertThrows(RuntimeException.class,()->service.exchangeInvitation(code));
        verifyNoInteractions(cache);
    }
    @Test void legacySessionNeverValidatedOrRefreshed() {
        when(cache.getCacheMap(anyString())).thenReturn(Map.of("customerId","99"));
        assertNull(service.validateToken("legacy")); assertFalse(service.refreshToken("legacy"));
        verifyNoInteractions(access); verify(cache,never()).expire(anyString(),anyLong(),any());
    }
    @Test void validatedSessionUsesExactCurrentTenantCustomerAndRejectsDisabled() {
        when(cache.getCacheMap(anyString())).thenReturn(session());
        assertEquals(7L,service.validateToken("synthetic").getTenantId());
        when(access.requireAvailableCustomer(7L,99L)).thenThrow(new IllegalStateException("disabled"));
        assertNull(service.validateToken("synthetic"));
    }
    @Test void missingOrInvalidTenantNeverFallsBackToZero() {
        var values=session(); values.remove("tenantId"); when(cache.getCacheMap(anyString())).thenReturn(values);
        assertNull(service.validateToken("synthetic"));
        values.put("tenantId","-1");assertNull(service.validateToken("synthetic"));verifyNoInteractions(access);
    }
    @Test void staffEndpointReusesExistingUnitUpdatePermission() throws Exception {
        var method=H5AuthAdminController.class.getMethod("invitation",H5AuthAdminController.InvitationReq.class);
        assertArrayEquals(new String[]{"psi:unit:update"},method.getAnnotation(SaCheckPermission.class).value());
    }
    @Test void onlineNeverExposesBearerAndFiltersHiddenCustomer() throws Exception {
        String handleKey="eoms:h5:session-handle:v2:"+cn.hutool.crypto.SecureUtil.sha256("bearer");
        when(keys.getKeysByPattern("eoms:h5:session-handle:v2:*",1000)).thenReturn(List.of(handleKey));
        when(bucket.get()).thenReturn("bearer");
        when(cache.getExpire(anyString())).thenReturn(600L);
        when(cache.getCacheMap(anyString())).thenReturn(session());
        var rows=service.listOnline(null); assertEquals(1,rows.size());
        assertEquals(cn.hutool.crypto.SecureUtil.sha256("bearer"),rows.get(0).getSessionId());
        assertThrows(NoSuchMethodException.class,()->rows.get(0).getClass().getMethod("getToken"));
        when(access.requireManagedCustomer(99L)).thenThrow(new IllegalStateException("hidden"));
        assertTrue(service.listOnline(null).isEmpty());
        verify(cache,never()).scanKeys(anyString());
    }
    @Test void administrativeHandleRequiresScopeAndCannotBeUsedAsSession() {
        when(bucket.get()).thenReturn("bearer");when(cache.getCacheMap(EomsConstants.H5_TOKEN_KEY+"bearer")).thenReturn(session());
        String handle=cn.hutool.crypto.SecureUtil.sha256("bearer");
        assertNull(service.validateToken(handle));
        when(access.requireManagedCustomer(99L)).thenThrow(new IllegalStateException("hidden"));
        assertThrows(RuntimeException.class,()->service.kickoutSession(handle));
        verify(cache,never()).deleteObject(anyString());
        doReturn(customer).when(access).requireManagedCustomer(99L);
        service.kickoutSession(handle);verify(cache).deleteObject(EomsConstants.H5_TOKEN_KEY+"bearer");
    }
    @Test void realPlatformLogHandlerExcludesSecretEndpoints() throws Exception {
        var handler = new top.continew.starter.log.handler.InterceptorLogHandler();
        Class<?> publicAuth=com.ls.aegis.biz.eoms.controller.h5.H5AuthController.class;
        assertFalse(handler.isRecord(publicAuth.getMethod("activate",com.ls.aegis.biz.eoms.model.req.H5CustomerActivateReq.class),publicAuth));
        assertFalse(handler.isRecord(publicAuth.getMethod("passwordLogin",com.ls.aegis.biz.eoms.model.req.H5CustomerLoginReq.class,jakarta.servlet.http.HttpServletRequest.class),publicAuth));
        assertFalse(handler.isRecord(publicAuth.getMethod("logout",String.class),publicAuth));
        assertFalse(handler.isRecord(H5AuthAdminController.class.getMethod("invitation",H5AuthAdminController.InvitationReq.class),H5AuthAdminController.class));
    }

}
