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
    com.ls.aegis.biz.eoms.mapper.h5.H5CustomerCredentialMapper credentials;
    org.springframework.security.crypto.password.PasswordEncoder passwords;
    com.ls.aegis.biz.eoms.domain.H5CustomerCredential member;
    com.ls.aegis.biz.eoms.domain.H5CustomerCredential member() {
        var m = new com.ls.aegis.biz.eoms.domain.H5CustomerCredential();m.setId(501L);m.setTenantId(7L);m.setCustomerId(99L);
        m.setLoginName("alice");m.setDisplayName("Alice");m.setAccountRole("ADMIN");m.setStatus(1);m.setAuthVersion(1L);m.setPasswordHash("hashed");return m;
    }
    final H5CustomerAccessApi.Customer customer = new H5CustomerAccessApi.Customer(99L, 7L, "WLDW0001", "synthetic");
    final String code = "12345678";
    @BeforeEach @SuppressWarnings("unchecked") void setup() {
        service = new H5TokenService(); cache = mock(RedisCache.class); redis = mock(RedissonClient.class);
        access = mock(H5CustomerAccessApi.class); bucket = mock(RBucket.class);
        keys = mock(RKeys.class); when(redis.getKeys()).thenReturn(keys);
        when(redis.<String>getBucket(anyString(), any(org.redisson.client.codec.StringCodec.class))).thenReturn(bucket);
        when(bucket.trySet(anyString(), eq(600L), eq(TimeUnit.SECONDS))).thenReturn(true);
        when(access.requireManagedCustomer(99L)).thenReturn(customer);
        when(access.requireAvailableCustomer(7L, 99L)).thenReturn(customer);
        credentials = mock(com.ls.aegis.biz.eoms.mapper.h5.H5CustomerCredentialMapper.class); passwords = mock(org.springframework.security.crypto.password.PasswordEncoder.class);
        member = member();when(credentials.selectById(501L)).thenReturn(member);when(credentials.list(7L,99L)).thenReturn(List.of());
        when(passwords.encode(anyString())).thenReturn("hashed");when(passwords.matches(anyString(),eq("hashed"))).thenReturn(true);
        when(credentials.insert(any())).thenAnswer(call -> {com.ls.aegis.biz.eoms.domain.H5CustomerCredential m=call.getArgument(0);m.setId(501L);return 1;});
        ReflectionTestUtils.setField(service,"credentialMapper",credentials);ReflectionTestUtils.setField(service,"passwordEncoder",passwords);
        ReflectionTestUtils.setField(service, "redisCache", cache);
        ReflectionTestUtils.setField(service, "redissonClient", redis);
        ReflectionTestUtils.setField(service, "customerAccessApi", access);
        ReflectionTestUtils.setField(service, "sysOptionApi", mock(SysOptionApi.class));
        ReflectionTestUtils.setField(service, "gmCryptoProvider", mock(ObjectProvider.class));
    }
    Map<String,Object> session() { return new HashMap<>(Map.of("customerId","99","tenantId","7","authVersion","3","accountId","501","accountVersion","1")); }
    @Test void codeOnlySessionExchangeAlwaysRejected() {assertThrows(RuntimeException.class,()->service.exchangeInvitation(code));verifyNoInteractions(cache);}
    @Test void resetOfOneMemberNeverUpdatesAnother() {
        when(bucket.get()).thenReturn("7:99:RESET:501:ADMIN:1");when(bucket.getAndDelete()).thenReturn("7:99:RESET:501:ADMIN:1");
        assertThrows(RuntimeException.class,()->service.activateInvitation(code,"other","SamplePass123","Other"));verify(credentials,never()).updatePassword(any());
        when(credentials.updatePassword(any())).thenReturn(1);
        service.activateInvitation(code,"alice","SamplePass123",null);verify(credentials).updatePassword(member);
        assertEquals(501L,member.getId());
    }
    @Test void disabledOrChangedVersionSessionRejected() {
        when(cache.getCacheMap(anyString())).thenReturn(session());member.setStatus(0);assertNull(service.validateToken("bearer"));
        member.setStatus(1);member.setAuthVersion(2L);assertNull(service.validateToken("bearer"));
    }
    @Test void secondMemberCanBeCreatedWithoutResettingFirst() {
        when(credentials.list(7L,99L)).thenReturn(List.of(member));var code=service.createInvitation(99L);
        verify(bucket).trySet("7:99:CREATE:0:MEMBER:0",600L,TimeUnit.SECONDS);assertNotNull(code);
        when(bucket.get()).thenReturn("7:99:CREATE:0:MEMBER:0");when(bucket.getAndDelete()).thenReturn("7:99:CREATE:0:MEMBER:0");
        doAnswer(call->{com.ls.aegis.biz.eoms.domain.H5CustomerCredential created=call.getArgument(0);created.setId(502L);when(credentials.selectById(502L)).thenReturn(created);return 1;}).when(credentials).insert(any());
        var second=service.activateInvitation(code.invitationCode(),"bobby","SamplePass123","Bobby");assertEquals(502L,second.getAccountId());assertEquals("bobby",second.getLoginName());assertEquals("alice",member.getLoginName());verify(credentials).insert(argThat(m->m.getLoginName().equals("bobby")&&m.getCustomerId()==99L));verify(credentials,never()).updatePassword(any());
    }
    @Test void lastEnabledCompanyAdminCannotBeDisabledOrDemoted() {
        when(credentials.lockMembers(7L,99L)).thenReturn(List.of(member));
        assertThrows(RuntimeException.class,()->service.updateMember(99L,501L,"Alice","MEMBER",1));
        assertThrows(RuntimeException.class,()->service.updateMember(99L,501L,"Alice","ADMIN",0));verify(credentials,never()).updateMember(any());
    }
    @Test void listDoesNotSerializePasswords() throws Exception {
        var mapper=new com.fasterxml.jackson.databind.ObjectMapper();assertFalse(mapper.writeValueAsString(member).contains("hashed"));
    }
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
        verify(bucket).trySet("7:99:CREATE:0:ADMIN:0",600L,TimeUnit.SECONDS);
    }
    @Test void deniedStaffCannotCreateInvitation() {
        when(access.requireManagedCustomer(99L)).thenThrow(new IllegalStateException("denied"));
        assertThrows(RuntimeException.class, () -> service.createInvitation(99L));
        verifyNoInteractions(redis,cache);
    }
    @Test void atomicallyConsumedInviteCreatesVersionedTenantSession() {
        when(bucket.get()).thenReturn("7:99:CREATE:0:ADMIN:0",null);
        when(bucket.getAndDelete()).thenReturn("7:99:CREATE:0:ADMIN:0",null);
        H5CustomerInfo result = service.activateInvitation(code,"alice","SamplePass123","Alice");
        assertEquals(7L,result.getTenantId()); assertEquals(99L,result.getCustomerId()); assertNotNull(result.getToken());
        verify(cache).setCacheMapValue(anyString(),eq("authVersion"),eq("3"));
        verify(cache).setCacheMapValue(anyString(),eq("tenantId"),eq("7"));
        assertThrows(RuntimeException.class, () -> service.activateInvitation(code,"alice","SamplePass123","Alice"));
        verify(access,times(1)).requireAvailableCustomer(7L,99L);
    }
    @Test @Timeout(45) void concurrentExchangeHasExactlyOneWinner() throws Exception {
        AtomicReference<String> binding = new AtomicReference<>("7:99:CREATE:0:ADMIN:0");
        when(bucket.get()).thenReturn("7:99:CREATE:0:ADMIN:0");
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
                try { service.activateInvitation(code,"alice","SamplePass123","Alice"); return true; }
                catch (RuntimeException denied) { return false; }
            };
            Future<Boolean> a = pool.submit(task), b = pool.submit(task);
            assertTrue(ready.await(10, TimeUnit.SECONDS), "Both workers must be scheduled before starting");
            start.countDown();
            // Correctness test, not a 3-second SLA including cold JVM/mock initialization.
            assertNotEquals(a.get(10, TimeUnit.SECONDS), b.get(10, TimeUnit.SECONDS));
            verify(bucket, times(2)).getAndDelete();
            verify(cache, times(1)).setCacheMapValue(anyString(), eq("authVersion"), eq("3"));
        } finally {
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        }
    }
    @Test void expiredMalformedOrDisabledInviteNeverCreatesSession() {
        assertThrows(RuntimeException.class,()->service.exchangeInvitation(null));
        assertThrows(RuntimeException.class,()->service.exchangeInvitation("WLDW0001"));
        when(bucket.get()).thenReturn(null,"7:99:CREATE:0:ADMIN:0","bad");
        assertThrows(RuntimeException.class,()->service.activateInvitation(code,"alice","SamplePass123","Alice"));
        when(access.requireAvailableCustomer(7L,99L)).thenThrow(new IllegalStateException("disabled"));
        assertThrows(RuntimeException.class,()->service.activateInvitation(code,"alice","SamplePass123","Alice"));
        assertThrows(RuntimeException.class,()->service.activateInvitation(code,"alice","SamplePass123","Alice"));
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
        String handleKey="eoms:h5:session-handle:v3:"+cn.hutool.crypto.SecureUtil.sha256("bearer");
        when(keys.getKeysByPattern("eoms:h5:session-handle:v3:*",1000)).thenReturn(List.of(handleKey));
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
