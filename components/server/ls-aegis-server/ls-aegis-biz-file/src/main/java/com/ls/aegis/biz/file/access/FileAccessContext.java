package com.ls.aegis.biz.file.access;

import com.ls.aegis.common.context.UserContextHolder;
import lombok.Getter;

/**
 * 文件访问鉴权上下文。
 */
@Getter
public class FileAccessContext {

    private final Long userId;
    private Long h5AccountId;
    private boolean h5CompanyAdmin;
    public Long getH5AccountId() { return h5AccountId; }
    public boolean isH5CompanyAdmin() { return h5CompanyAdmin; }
    public static FileAccessContext forH5Member(Long tenantId, Long customerId, String customerNo, Long accountId, boolean companyAdmin) {
        var context = forH5Customer(tenantId, customerId, customerNo);
        context.h5AccountId = accountId; context.h5CompanyAdmin = companyAdmin; return context;
    }
    private final String h5CustomerNo;
    private final Long h5TenantId;
    private final Long h5CustomerId;
    private final boolean platformLogin;

    private FileAccessContext(Long userId, String h5CustomerNo, boolean platformLogin) {
        this(userId, h5CustomerNo, platformLogin, null, null);
    }

    private FileAccessContext(Long userId, String h5CustomerNo, boolean platformLogin, Long h5TenantId, Long h5CustomerId) {
        this.userId = userId;
        this.h5CustomerNo = h5CustomerNo;
        this.platformLogin = platformLogin;
        this.h5TenantId = h5TenantId;
        this.h5CustomerId = h5CustomerId;
    }

    public static FileAccessContext forPlatformUser() {
        Long userId = null;
        try {
            if (UserContextHolder.getContext() != null) {
                userId = UserContextHolder.getUserId();
            }
        } catch (Exception ignored) {
            // 未登录
        }
        return new FileAccessContext(userId, null, userId != null);
    }

    public static FileAccessContext forH5Customer(String customerNo) {
        return new FileAccessContext(null, customerNo, false);
    }

    public static FileAccessContext forH5Customer(Long tenantId, Long customerId, String customerNo) {
        return new FileAccessContext(null, customerNo, false, tenantId, customerId);
    }

    public boolean isSuperAdmin() {
        try {
            return UserContextHolder.isSuperAdmin();
        } catch (Exception ignored) {
            return false;
        }
    }
}
