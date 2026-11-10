package com.ls.aegis.biz.eoms.domain;

import com.ls.aegis.mybatis.base.model.entity.XmlMapperDO;
import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * eoms_cost 实体类
 * 
 * @author EOMS Generator
 */
public class Cost extends XmlMapperDO
{
    private static final long serialVersionUID = 1L;

    private Long repairOrderId;

    /** 关联工单编号（非持久化，JOIN eoms_repair_order 查询用于显示） */

    private String orderNo;

    private String costType;

    private BigDecimal amount;

    private String description;

    private String delFlag;

    public Long getRepairOrderId()
    {
        return repairOrderId;
    }

    public void setRepairOrderId(Long repairOrderId)
    {
        this.repairOrderId = repairOrderId;
    }

    public String getOrderNo()
    {
        return orderNo;
    }

    public void setOrderNo(String orderNo)
    {
        this.orderNo = orderNo;
    }

    public String getCostType()
    {
        return costType;
    }

    public void setCostType(String costType)
    {
        this.costType = costType;
    }

    public BigDecimal getAmount()
    {
        return amount;
    }

    public void setAmount(BigDecimal amount)
    {
        this.amount = amount;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
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
            .append("repairOrderId", getRepairOrderId())
            .append("orderNo", getOrderNo())
            .append("costType", getCostType())
            .append("amount", getAmount())
            .append("description", getDescription())
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
    private Long reversalOfId;
    public Long getReversalOfId() { return reversalOfId; }
    public void setReversalOfId(Long value) { this.reversalOfId = value; }

}
