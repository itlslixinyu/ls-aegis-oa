package com.ls.aegis.biz.quote.support;

import cn.dev33.satoken.stp.StpUtil;
import com.ls.aegis.common.context.UserContextHolder;
import top.continew.starter.core.exception.BusinessException;

/** Authenticated scope only; request parameters and mutable usernames never grant ownership. */
public final class QuoteAccessContext {
    private QuoteAccessContext() { }

    public record Scope(Long tenantId, Long userId, Long deptId, boolean legacyAdministrator) { }

    public static Scope current() {
        StpUtil.checkLogin();
        var actor = UserContextHolder.getContext();
        if (actor == null || actor.getId() == null || !String.valueOf(actor.getId()).equals(StpUtil.getLoginIdAsString())
            || (!actor.isSuperAdmin() && !actor.isTenantAdmin()
            && (actor.getDeptId() == null || actor.getRoles() == null || actor.getRoles().isEmpty()
            || actor.getRoles().stream().anyMatch(role -> role == null || role.getId() == null || role.getDataScope() == null)))) {
            throw new BusinessException("报价数据权限上下文无效，请重新登录");
        }
        Long tenant = actor.getTenantId() == null ? 0L : actor.getTenantId();
        return new Scope(tenant, actor.getId(), actor.getDeptId(), actor.isSuperAdmin() && tenant == 0L);
    }
}
