package com.ls.aegis.security.config.satoken;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.ls.aegis.common.context.UserContext;
import com.ls.aegis.common.context.UserContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

class SaExtensionInterceptorTest {

    @AfterEach
    void clearThreadContext() {
        UserContextHolder.clearContext();
    }

    @Test
    void rejectedAuthenticationCannotLeakContextToTheNextRequest() throws Exception {
        UserContext previous = user(1L);
        UserContext authenticated = user(2L);
        UserContext next = user(3L);
        SaSession session = new SaSession("request-isolation-test");
        session.set(SaSession.USER, authenticated);
        IllegalStateException rejection = new IllegalStateException("模拟权限拒绝");
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getSession).thenReturn(session);
            UserContextHolder.setContext(previous, false);
            SaExtensionInterceptor interceptor = new SaExtensionInterceptor(ignored -> {
                assertSame(authenticated, UserContextHolder.getContext(), "鉴权不能读取上一次请求的身份");
                throw rejection;
            });
            assertSame(rejection, assertThrows(IllegalStateException.class,
                () -> interceptor.preHandle(new MockHttpServletRequest(), new MockHttpServletResponse(), handler())));
            // Spring 在 preHandle 抛异常后不会回调本拦截器的 afterCompletion。
            session.set(SaSession.USER, next);
            assertSame(next, UserContextHolder.getContext(), "拒绝后必须重新从下一次会话读取身份");
        }
    }

    @Test
    void acceptedRequestKeepsItsContextUntilCompletionThenClearsIt() throws Exception {
        UserContext current = user(4L);
        UserContext next = user(5L);
        SaSession session = new SaSession("successful-request-test");
        session.set(SaSession.USER, next);
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getSession).thenReturn(session);
            // 模拟已被排除登录检查的公开接口，避免本用例依赖 Spring 容器和真实 JWT。
            stp.when(StpUtil::isLogin).thenReturn(false);
            SaExtensionInterceptor interceptor = new SaExtensionInterceptor(ignored -> UserContextHolder.setContext(current, false));
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();
            Object handler = handler();
            assertTrue(interceptor.preHandle(request, response, handler));
            assertSame(current, UserContextHolder.getContext());
            interceptor.afterCompletion(request, response, handler, null);
            assertSame(next, UserContextHolder.getContext(), "请求结束后不能缓存上一次身份");
        }
    }

    private static UserContext user(Long id) {
        UserContext context = new UserContext();
        context.setId(id);
        return context;
    }

    private static HandlerMethod handler() throws NoSuchMethodException {
        return new HandlerMethod(new TestController(), TestController.class.getMethod("endpoint"));
    }

    public static class TestController {
        public void endpoint() {
        }
    }
}
