package com.ls.aegis.biz.eoms.utils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import com.ls.aegis.common.context.RoleContext;
import com.ls.aegis.common.context.UserContext;
import com.ls.aegis.common.context.UserContextHolder;
import com.ls.aegis.common.enums.DataScopeEnum;
import com.ls.aegis.mybatis.base.model.entity.XmlMapperDO;

/**
 * EOMS XML Mapper 数据权限注入（路径 A）。
 * <p>
 * 与 ContiNew {@code @DataPermission} 语义对齐：超管/租户管理员不过滤；
 * 多角色按 OR 合并；SELF 对工单可额外匹配 {@code engineer_id}。
 * <p>
 * H5 客户仅有 h5-token、无平台 Sa-Token 登录态：不注入 PC 数据权限（由业务侧用
 * customerId 等自行限定）。切勿在无登录时调用 {@link UserContextHolder#getContext()}，
 * 其内部 {@code StpUtil.getSession()} 会抛 NotLoginException，前端误报「登录已过期」。
 */
public final class EomsDataScopeHelper {

    public static final String P_FILTER = "dataScopeFilter";
    public static final String P_USER_ID = "dsUserId";
    public static final String P_DEPT_ID = "dsDeptId";
    /** DEPT / DEPT_AND_CHILD / 空 */
    public static final String P_DEPT_SCOPE = "dsDeptScope";
    public static final String P_SELF = "dsSelf";
    public static final String P_INCLUDE_ENGINEER = "dsIncludeEngineer";
    public static final String P_CUSTOM_ROLE_IDS = "dsCustomRoleIds";
    /** SQL 表别名（白名单） */
    public static final String P_ALIAS = "dsAlias";

    private static final Set<String> ALIAS_WHITELIST = Set.of("o", "ct", "r", "p", "a", "s");

    private EomsDataScopeHelper() {
    }

    /**
     * 安全读取平台用户上下文：未登录（含 H5 客户）返回 null，不抛 NotLoginException。
     */
    private static UserContext safeContext() {
        try {
            if (!StpUtil.isLogin()) {
                return null;
            }
            return UserContextHolder.getContext();
        } catch (Exception e) {
            return null;
        }
    }

    /** 是否需要行级过滤（非超管、非租户管理员） */
    public static boolean shouldFilter() {
        UserContext ctx = safeContext();
        if (ctx == null) {
            // 无平台登录：PC 侧不应走到此处；H5 侧由 apply 直接跳过注入
            return true;
        }
        return !ctx.isSuperAdmin() && !ctx.isTenantAdmin();
    }

    /**
     * 向查询实体注入 DataScope（默认表别名 {@code o}，兼容工单）。
     */
    public static void apply(XmlMapperDO query, boolean includeEngineer) {
        apply(query, includeEngineer, "o");
    }

    /**
     * 向查询实体 {@code params} 注入 DataScope 条件标记，供 XML 拼装。
     *
     * @param query           列表查询实体（须为 XmlMapperDO）
     * @param includeEngineer SELF 时是否同时匹配 engineer_id（仅工单）
     * @param tableAlias      主表别名（须在白名单内）
     */
    public static void apply(XmlMapperDO query, boolean includeEngineer, String tableAlias) {
        if (query == null) {
            return;
        }
        // Every application rebuilds server-owned scope, including ALL/H5 early returns.
        // Never merge client-supplied flags or a previous user's predicates into the current identity.
        Map<String, Object> scopeParams = query.getParams();
        for (String key : Set.of(P_FILTER, P_USER_ID, P_DEPT_ID, P_DEPT_SCOPE, P_SELF,
                P_INCLUDE_ENGINEER, P_CUSTOM_ROLE_IDS, P_ALIAS)) {
            scopeParams.remove(key);
        }
        if (!shouldFilter()) {
            return;
        }
        String alias = sanitizeAlias(tableAlias);
        UserContext ctx = safeContext();
        // H5 客户等无平台会话：不注入 create_user/部门条件，避免误拦或 NotLogin
        if (ctx == null) {
            return;
        }
        if (CollUtil.isEmpty(ctx.getRoles())) {
            Map<String, Object> params = query.getParams();
            params.put(P_FILTER, true);
            params.put(P_ALIAS, alias);
            params.put(P_SELF, true);
            params.put(P_USER_ID, ctx.getId());
            if (includeEngineer) {
                params.put(P_INCLUDE_ENGINEER, true);
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
            if (includeEngineer) {
                params.put(P_INCLUDE_ENGINEER, true);
            }
        }

        if (CollUtil.isNotEmpty(customRoleIds)) {
            params.put(P_CUSTOM_ROLE_IDS, customRoleIds);
        }

        if (!hasDept && !hasDeptAndChild && !hasSelf && customRoleIds.isEmpty()) {
            params.put(P_SELF, true);
            if (includeEngineer) {
                params.put(P_INCLUDE_ENGINEER, true);
            }
        }
    }

    private static String sanitizeAlias(String tableAlias) {
        if (tableAlias != null && ALIAS_WHITELIST.contains(tableAlias)) {
            return tableAlias;
        }
        return "o";
    }

    /**
     * 写入时填充 create_user / dept_id / tenant_id（保留 create_by 兼容）。
     */
    public static void fillOnInsert(XmlMapperDO entity) {
        if (entity == null) {
            return;
        }
        UserContext ctx = safeContext();
        Long userId = ctx != null ? ctx.getId() : null;
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
     * 具备部门/租户字段的实体标记。
     */
    public interface DeptAware {
        Long getDeptId();

        void setDeptId(Long deptId);

        Long getTenantId();

        void setTenantId(Long tenantId);
    }
}
