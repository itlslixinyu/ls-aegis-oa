package com.ls.aegis.biz.eoms.domain;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotNull;
import com.ls.aegis.mybatis.base.model.entity.XmlMapperDO;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * eoms_receivable 实体类
 *
 * @author EOMS Generator
 */
public class Receivable extends XmlMapperDO implements com.ls.aegis.biz.eoms.utils.EomsDataScopeHelper.DeptAware
{
    private static final long serialVersionUID = 1L;

    /** 租户ID（DataScope） */
    private Long tenantId;

    /** 创建人部门（DataScope） */
    private Long deptId;

    /** 客户ID NOT NULL 无 DEFAULT，必填 */
    @NotNull(message = "客户ID不能为空")

    private Long customerId;

    /** 关联客户编号（非持久化，JOIN psi_unit 查询用于显示） */

    private String customerNo;

    /** 关联客户名称（非持久化，JOIN psi_unit 查询用于显示） */

    private String customerName;

    /** 关联维修工单ID NOT NULL 无 DEFAULT，必填 */
    @NotNull(message = "维修工单ID不能为空")

    private Long repairOrderId;

    /** 应收单号（持久化，YS 前缀 + 日期 + 流水号） */

    private String receivableNo;

    /** 关联工单编号（非持久化，JOIN eoms_repair_order 查询用于显示） */

    private String orderNo;

    private BigDecimal amount;

    private BigDecimal receivedAmount;

    private BigDecimal arrearsAmount;

    private String status;

    private String delFlag;

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

    public Long getRepairOrderId()
    {
        return repairOrderId;
    }

    public void setRepairOrderId(Long repairOrderId)
    {
        this.repairOrderId = repairOrderId;
    }

    public String getReceivableNo()
    {
        return receivableNo;
    }

    public void setReceivableNo(String receivableNo)
    {
        this.receivableNo = receivableNo;
    }

    public String getOrderNo()
    {
        return orderNo;
    }

    public void setOrderNo(String orderNo)
    {
        this.orderNo = orderNo;
    }

    public BigDecimal getAmount()
    {
        return amount;
    }

    public void setAmount(BigDecimal amount)
    {
        this.amount = amount;
    }

    public BigDecimal getReceivedAmount()
    {
        return receivedAmount;
    }

    public void setReceivedAmount(BigDecimal receivedAmount)
    {
        this.receivedAmount = receivedAmount;
    }

    public BigDecimal getArrearsAmount()
    {
        return arrearsAmount;
    }

    public void setArrearsAmount(BigDecimal arrearsAmount)
    {
        this.arrearsAmount = arrearsAmount;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
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
            .append("repairOrderId", getRepairOrderId())
            .append("receivableNo", getReceivableNo())
            .append("orderNo", getOrderNo())
            .append("amount", getAmount())
            .append("receivedAmount", getReceivedAmount())
            .append("arrearsAmount", getArrearsAmount())
            .append("status", getStatus())
            .append("delFlag", getDelFlag())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
    private Long settlementId;
    public Long getSettlementId() { return settlementId; }
    public void setSettlementId(Long value) { this.settlementId = value; }

}
