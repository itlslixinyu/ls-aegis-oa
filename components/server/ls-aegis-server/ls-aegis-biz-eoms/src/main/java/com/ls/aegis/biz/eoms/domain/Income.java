package com.ls.aegis.biz.eoms.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ls.aegis.mybatis.base.model.entity.XmlMapperDO;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class Income extends XmlMapperDO
{
    private static final long serialVersionUID = 1L;

    private String incomeNo;

    private Long repairOrderId;

    /** 关联工单编号（非持久化，JOIN eoms_repair_order 查询用于显示） */

    private String orderNo;

    private Long customerId;

    /** 关联客户编号（非持久化，JOIN psi_unit 查询用于显示） */

    private String customerNo;

    /** 关联客户名称（非持久化，JOIN psi_unit 查询用于显示） */

    private String customerName;

    private BigDecimal amount;

    private String incomeType;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")

    private Date incomeDate;

    private String delFlag;

    public void setIncomeNo(String incomeNo) { this.incomeNo = incomeNo; }
    public String getIncomeNo() { return incomeNo; }

    public void setRepairOrderId(Long repairOrderId) { this.repairOrderId = repairOrderId; }
    public Long getRepairOrderId() { return repairOrderId; }

    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public String getOrderNo() { return orderNo; }

    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public Long getCustomerId() { return customerId; }

    public void setCustomerNo(String customerNo) { this.customerNo = customerNo; }
    public String getCustomerNo() { return customerNo; }

    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getCustomerName() { return customerName; }

    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getAmount() { return amount; }

    public void setIncomeType(String incomeType) { this.incomeType = incomeType; }
    public String getIncomeType() { return incomeType; }

    public void setIncomeDate(Date incomeDate) { this.incomeDate = incomeDate; }
    public Date getIncomeDate() { return incomeDate; }

    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }
    public String getDelFlag() { return delFlag; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("incomeNo", getIncomeNo())
            .append("repairOrderId", getRepairOrderId())
            .append("orderNo", getOrderNo())
            .append("customerId", getCustomerId())
            .append("customerNo", getCustomerNo())
            .append("customerName", getCustomerName())
            .append("amount", getAmount())
            .append("incomeType", getIncomeType())
            .append("incomeDate", getIncomeDate())
            .append("delFlag", getDelFlag())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
    private Long paymentId;
    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long value) { this.paymentId = value; }
    private Long reversalOfId;
    public Long getReversalOfId() { return reversalOfId; }
    public void setReversalOfId(Long value) { this.reversalOfId = value; }

}
