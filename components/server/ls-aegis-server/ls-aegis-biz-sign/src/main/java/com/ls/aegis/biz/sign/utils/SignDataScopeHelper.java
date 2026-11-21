package com.ls.aegis.biz.sign.utils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cn.hutool.core.collection.CollUtil;
import com.ls.aegis.common.context.RoleContext;
import com.ls.aegis.common.context.UserContext;
import com.ls.aegis.common.context.UserContextHolder;
import com.ls.aegis.common.enums.DataScopeEnum;
import com.ls.aegis.mybatis.base.model.entity.XmlMapperDO;

/**
 * Sign XML Mapper 数据权限注入（路径 A）。
 * <p>
 * 与 ContiNew / EOMS 语义对齐；印章 ACL（{@code sign_seal_permission}）不走本 Helper。
 * SELF 可选：项目匹配负责人 {@code manager_ids}；日志/记录匹配 {@code user_id}。
 */
public final class SignDataScopeHelper {

    public static final String P_FILTER = "dataScopeFilter";
    public static final String P_USER_ID = "dsUserId";
    public static final String P_DEPT_ID = "dsDeptId";
    public static final String P_DEPT_SCOPE = "dsDeptScope";
    public static final String P_SELF = "dsSelf";
    public static final String P_INCLUDE_MANAGER = "dsIncludeManager";
    public static final String P_INCLUDE_ACTOR = "dsIncludeActor";
    public static final String P_CUSTOM_ROLE_IDS = "dsCustomRoleIds";
    public static final String P_ALIAS = "dsAlias";

    private static final Set<String> ALIAS_WHITELIST = Set.of("p", "l", "r");

    private SignDataScopeHelper() {
    }

    public static boolean shouldFilter() {
        try {
            UserContext ctx = UserContextHolder.getContext();
            if (ctx == null) {
                return true;
            }
            return !ctx.isSuperAdmin() && !ctx.isTenantAdmin();
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * @param includeManager SELF 时是否匹配项目负责人 manager_ids
     * @param includeActor   SELF 时是否匹配 user_id（日志/记录操作人）
     * @param tableAlias     主表别名白名单：p / l / r
     */
    public static void apply(XmlMapperDO query, boolean includeManager, boolean includeActor, String tableAlias) {
        if (query == null || !shouldFilter()) {
            return;
        }
        String alias = sanitizeAlias(tableAlias);
        UserContext ctx = UserContextHolder.getContext();
        if (ctx == null || CollUtil.isEmpty(ctx.getRoles())) {
            Map<String, Object> params = query.getParams();
            params.put(P_FILTER, true);
            params.put(P_ALIAS, alias);
            params.put(P_SELF, true);
            params.put(P_USER_ID, UserContextHolder.getUserId());
            if (includeManager) {
                params.put(P_INCLUDE_MANAGER, true);
            }
            if (includeActor) {
                params.put(P_INCLUDE_ACTOR, true);
            }
            return;
        }

        boolean hasAll = false;
        boolean hasDept = false;
        boolean hasDeptAndChild = false;
        boolean hasSelf = false;
        Set<Long> customRoleIds = new HashSet<>();

        for (RoleContext role : ctx.getRoles()) {
            if (role == null || role.getDataScope() == null) {
                continue;
            }
            DataScopeEnum scope = role.getDataScope();
            if (scope == DataScopeEnum.ALL) {
                hasAll = true;
                break;
            }
            switch (scope) {
                case DEPT -> hasDept = true;
                case DEPT_AND_CHILD -> hasDeptAndChild = true;
                case SELF -> hasSelf = true;
                case CUSTOM -> {
                    if (role.getId() != null) {
                        customRoleIds.add(role.getId());
                    }
                }
                default -> {
                }
            }
        }

        if (hasAll) {
            return;
        }

        Map<String, Object> params = query.getParams();
        params.put(P_FILTER, true);
        params.put(P_ALIAS, alias);
        params.put(P_USER_ID, ctx.getId());
        params.put(P_DEPT_ID, ctx.getDeptId());

        if (hasDeptAndChild) {
            params.put(P_DEPT_SCOPE, "DEPT_AND_CHILD");
        } else if (hasDept) {
            params.put(P_DEPT_SCOPE, "DEPT");
        }

        if (hasSelf) {
            params.put(P_SELF, true);
            if (includeManager) {
                params.put(P_INCLUDE_MANAGER, true);
            }
            if (includeActor) {
                params.put(P_INCLUDE_ACTOR, true);
            }
        }

        if (CollUtil.isNotEmpty(customRoleIds)) {
            params.put(P_CUSTOM_ROLE_IDS, customRoleIds);
        }

        if (!hasDept && !hasDeptAndChild && !hasSelf && customRoleIds.isEmpty()) {
            params.put(P_SELF, true);
            if (includeManager) {
                params.put(P_INCLUDE_MANAGER, true);
            }
            if (includeActor) {
                params.put(P_INCLUDE_ACTOR, true);
            }
        }
    }

    private static String sanitizeAlias(String tableAlias) {
        if (tableAlias != null && ALIAS_WHITELIST.contains(tableAlias)) {
            return tableAlias;
        }
        return "p";
    }

    public static void fillOnInsert(XmlMapperDO entity) {
        if (entity == null) {
            return;
        }
        Long userId = UserContextHolder.getUserId();
        UserContext ctx = UserContextHolder.getContext();
        if (entity.getCreateUser() == null || entity.getCreateUser() == 0L) {
            entity.setCreateUser(userId != null ? userId : 0L);
        }
        if (entity instanceof DeptAware aware) {
            if (aware.getDeptId() == null || aware.getDeptId() == 0L) {
                Long deptId = ctx != null ? ctx.getDeptId() : null;
                aware.setDeptId(deptId != null ? deptId : 0L);
            }
            if (aware.getTenantId() == null) {
                Long tenantId = ctx != null ? ctx.getTenantId() : null;
                aware.setTenantId(tenantId != null ? tenantId : 0L);
            }
        }
    }

    /**
     * 日志/记录写入：优先继承项目部门，create_user 回落到操作人。
     */
    public static void fillAuditRow(XmlMapperDO entity, Long actorUserId, DeptAware project) {
        if (entity instanceof DeptAware aware && project != null) {
            if (project.getDeptId() != null && project.getDeptId() > 0) {
                aware.setDeptId(project.getDeptId());
            }
            if (project.getTenantId() != null) {
                aware.setTenantId(project.getTenantId());
            }
        }
        fillOnInsert(entity);
        if ((entity.getCreateUser() == null || entity.getCreateUser() == 0L) && actorUserId != null) {
            entity.setCreateUser(actorUserId);
        }
    }

    public interface DeptAware {
        Long getDeptId();

        void setDeptId(Long deptId);

        Long getTenantId();

        void setTenantId(Long tenantId);
    }
}
