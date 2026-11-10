package com.ls.aegis.biz.eoms.model.resp;

import java.time.LocalDateTime;

/**
 * H5 客户在线令牌（管理端列表）
 */
public class H5OnlineTokenResp
{
    /** 客户 ID */
    private Long customerId;

    /** 客户编号 */
    private String customerNo;

    /** 客户名称 */
    private String customerName;

    /** 登录时间 */
    private LocalDateTime loginTime;

    /** 剩余有效秒数 */
    private Long expireSeconds;

    /**
     * 令牌 SM3 摘要（国密审计展示，不暴露完整令牌）
     */
    private String tokenDigest;

    /**
     * 不可用于登录的管理会话句柄
     */
    private String sessionId;

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

    public LocalDateTime getLoginTime()
    {
        return loginTime;
    }

    public void setLoginTime(LocalDateTime loginTime)
    {
        this.loginTime = loginTime;
    }

    public Long getExpireSeconds()
    {
        return expireSeconds;
    }

    public void setExpireSeconds(Long expireSeconds)
    {
        this.expireSeconds = expireSeconds;
    }

    public String getTokenDigest()
    {
        return tokenDigest;
    }

    public void setTokenDigest(String tokenDigest)
    {
        this.tokenDigest = tokenDigest;
    }

    public String getSessionId()
    {
        return sessionId;
    }

    public void setSessionId(String sessionId)
    {
        this.sessionId = sessionId;
    }
}
