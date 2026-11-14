package com.ls.aegis.biz.eoms.domain;

/**
 * H5 客户身份信息（令牌解析后的客户上下文）
 *
 * @author EOMS
 */
public class H5CustomerInfo
{
    private Long accountId;
    private String loginName;
    private String displayName;
    private String accountRole;
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long value) { accountId = value; }
    public String getLoginName() { return loginName; }
    public void setLoginName(String value) { loginName = value; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String value) { displayName = value; }
    public String getAccountRole() { return accountRole; }
    public void setAccountRole(String value) { accountRole = value; }
    public boolean isCompanyAdmin() { return "ADMIN".equals(accountRole); }

    private Long tenantId;

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    /** 客户ID */
    private Long customerId;

    /** 客户编号 */
    private String customerNo;

    /** 客户名称 */
    private String customerName;

    /** 访问令牌（仅生成令牌时返回） */
    private String token;

    public H5CustomerInfo()
    {
    }

    public H5CustomerInfo(Long customerId, String customerNo, String customerName)
    {
        this.customerId = customerId;
        this.customerNo = customerNo;
        this.customerName = customerName;
    }

    public Long getCustomerId()
    {
        return customerId;
    }

    public void setCustomerId(Long customerId)
    {
        this.customerId = customerId;
    }

    public String getCustomerNo()
    {
        return customerNo;
    }

    public void setCustomerNo(String customerNo)
    {
        this.customerNo = customerNo;
    }

    public String getCustomerName()
    {
        return customerName;
    }

    public void setCustomerName(String customerName)
    {
        this.customerName = customerName;
    }

    public String getToken()
    {
        return token;
    }

    public void setToken(String token)
    {
        this.token = token;
    }
}
