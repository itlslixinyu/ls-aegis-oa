package com.ls.aegis.biz.quote.service;

import cn.dev33.satoken.context.SaTokenContextForThreadLocalStaff;
import cn.dev33.satoken.servlet.model.*;
import cn.dev33.satoken.stp.StpUtil;
import com.ls.aegis.common.context.*;
import com.ls.aegis.common.enums.DataScopeEnum;
import java.util.Set;
import org.springframework.mock.web.*;
import cn.hutool.extra.spring.SpringUtil;
import com.ls.aegis.common.config.TenantExtensionProperties;
import org.springframework.context.support.GenericApplicationContext;

public final class QuoteTestActor {
    private QuoteTestActor() { }
    private static synchronized void ensureSpringContext() {
        try { SpringUtil.getBean(TenantExtensionProperties.class); }
        catch (RuntimeException unavailable) {
            var context = new GenericApplicationContext(); context.registerBean(TenantExtensionProperties.class); context.refresh();
            new SpringUtil().setApplicationContext(context);
        }
    }
    public static void open() {
        ensureSpringContext();
        var request = new MockHttpServletRequest();
        SaTokenContextForThreadLocalStaff.setModelBox(new SaRequestForServlet(request),
            new SaResponseForServlet(new MockHttpServletResponse()), new SaStorageForServlet(request));
        StpUtil.login(100L);
        actor(1, 100, 10, DataScopeEnum.ALL);
    }
    public static void actor(long tenant, long user, long dept, DataScopeEnum scope) {
        StpUtil.login(user);
        var context = new UserContext(); context.setId(user); context.setTenantId(tenant); context.setDeptId(dept);
        context.setUsername("reused-display-name");
        context.setRoles(Set.of(new RoleContext(9L, "quote_test", scope)));
        UserContextHolder.setContext(context, false);
    }
    public static void close() {
        UserContextHolder.clearContext(); SaTokenContextForThreadLocalStaff.clearModelBox();
    }
}
