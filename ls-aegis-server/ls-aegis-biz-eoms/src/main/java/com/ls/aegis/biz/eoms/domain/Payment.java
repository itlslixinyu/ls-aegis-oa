package com.ls.aegis.biz.eoms.domain;

import com.ls.aegis.mybatis.base.model.entity.XmlMapperDO;
import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * eoms_payment 实体类
 * 
 * @author EOMS Generator
 */
public class Payment extends XmlMapperDO implements com.ls.aegis.biz.eoms.utils.EomsDataScopeHelper.DeptAware
{
    private static final long serialVersionUID = 1L;

    /** 租户ID（DataScope） */
    private Long tenantId;

    /** 创建人部门（DataScope） */
    private Long deptId;

    private Long customerId;

    /** 关联客户编号（非持久化，JOIN psi_unit 查询用于显示） */

    private String customerNo;

    /** 关联客户名称（非持久化，JOIN psi_unit 查询用于显示） */

    private String customerName;

    private Long receivableId;

    /** 收款单号（持久化，SK 前缀 + 日期 + 流水号） */

    private String paymentNo;

    /**
     * 批量收款批次ID（同一批次共享1个SK单号）
     * 单笔核销时为 NULL；批量核销时所有明细共享同一 batch_id（UUID）
     * 唯一索引为 (payment_no, batch_id)，允许批量场景下多条记录共享 payment_no
     */
    private String batchId;

    /** 关联应收单号（非持久化，JOIN eoms_receivable.receivable_no 用于显示） */

    private String receivableNo;

    private BigDecimal amount;

    private String paymentMethod;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")

    private Date paymentTime;

    private String delFlag;

    /**
     * P1-8 幂等令牌（防并发重复收款）
     * 前端生成的唯一令牌（如 UUID），不持久化到数据库。
     * 后端使用 Redis setIfAbsent 校验，相同 token 的请求在有效期内只能成功一次。
     */
    private transient String idempotentToken;

    @Override
    public Long getTenantId()
    {
        return tenantId;
    }

    @Override
    public void setTenantId(Long tenantId)
    {
        this.tenantId = tenantId;
    }

    @Override
    public Long getDeptId()
    {
        return deptId;
    }

    @Override
    public void setDeptId(Long deptId)
    {
        this.deptId = deptId;
    }

    public String getIdempotentToken()
    {
        return idempotentToken;
    }

    public void setIdempotentToken(String idempotentToken)
    {
        this.idempotentToken = idempotentToken;
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

    public Long getReceivableId()
    {
        return receivableId;
    }

    public void setReceivableId(Long receivableId)
    {
        this.receivableId = receivableId;
    }

    public String getPaymentNo()
    {
        return paymentNo;
    }

    public void setPaymentNo(String paymentNo)
    {
        this.paymentNo = paymentNo;
    }

    public String getBatchId()
    {
        return batchId;
    }

    public void setBatchId(String batchId)
    {
        this.batchId = batchId;
    }

    public String getReceivableNo()
    {
        return receivableNo;
    }

    public void setReceivableNo(String receivableNo)
    {
        this.receivableNo = receivableNo;
    }

    public BigDecimal getAmount()
    {
        return amount;
    }

    public void setAmount(BigDecimal amount)
    {
        this.amount = amount;
    }

    public String getPaymentMethod()
    {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod)
    {
        this.paymentMethod = paymentMethod;
    }

    public Date getPaymentTime()
    {
        return paymentTime;
    }

    public void setPaymentTime(Date paymentTime)
    {
        this.paymentTime = paymentTime;
    }

    public String getDelFlag()
    {
        return delFlag;
    }

    public void setDelFlag(String delFlag)
    {
        this.delFlag = delFlag;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("customerId", getCustomerId())
            .append("customerNo", getCustomerNo())
            .append("customerName", getCustomerName())
            .append("receivableId", getReceivableId())
            .append("paymentNo", getPaymentNo())
            .append("batchId", getBatchId())
            .append("receivableNo", getReceivableNo())
            .append("amount", getAmount())
            .append("paymentMethod", getPaymentMethod())
            .append("paymentTime", getPaymentTime())
            .append("delFlag", getDelFlag())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
    private String status;
    public String getStatus() { return status; }
    public void setStatus(String value) { this.status = value; }
    private Date reversedTime;
    public Date getReversedTime() { return reversedTime; }
    public void setReversedTime(Date value) { this.reversedTime = value; }
    private String reversedBy;
    public String getReversedBy() { return reversedBy; }
    public void setReversedBy(String value) { this.reversedBy = value; }

}
