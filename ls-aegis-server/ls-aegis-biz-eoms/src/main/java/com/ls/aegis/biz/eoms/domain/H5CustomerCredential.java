package com.ls.aegis.biz.eoms.domain;

/** Customer portal credential; scoped to one PSI customer and tenant. */
public class H5CustomerCredential {
    private Long tenantId;
    private Long customerId;
    private String loginName;
    private String passwordHash;

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public String getLoginName() { return loginName; }
    public void setLoginName(String loginName) { this.loginName = loginName; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
}
