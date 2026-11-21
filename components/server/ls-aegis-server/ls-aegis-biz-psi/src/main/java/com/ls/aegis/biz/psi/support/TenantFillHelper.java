package com.ls.aegis.biz.psi.support;

import cn.hutool.core.util.ObjectUtil;
import com.ls.aegis.common.context.UserContext;
import com.ls.aegis.common.context.UserContextHolder;
import com.ls.aegis.mybatis.base.model.entity.TenantBaseDO;

/**
 * 租户 / 部门字段填充（现网租户关闭时默认 0）
 */
public final class TenantFillHelper {

    private TenantFillHelper() {
    }

    public static Long currentTenantId() {
        return ObjectUtil.defaultIfNull(UserContextHolder.getTenantId(), 0L);
    }

    /**
     * 当前登录用户部门；无上下文时 0（仍可走「仅本人」数据权限）。
     */
    public static Long currentDeptId() {
        try {
            UserContext ctx = UserContextHolder.getContext();
            if (ctx == null) {
                return 0L;
            }
            return ObjectUtil.defaultIfNull(ctx.getDeptId(), 0L);
        } catch (Exception ignored) {
            return 0L;
        }
    }

    public static void fill(TenantBaseDO entity) {
        if (entity != null && entity.getTenantId() == null) {
            entity.setTenantId(currentTenantId());
        }
    }
}
