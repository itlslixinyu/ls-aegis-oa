package com.ls.aegis.biz.eoms.domain;

import com.ls.aegis.biz.eoms.utils.EomsDataScopeHelper;
import com.ls.aegis.mybatis.base.model.entity.XmlMapperDO;
import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * eoms_repair_settlement 实体类
 * 
 * @author EOMS Generator
 */
public class RepairSettlement extends XmlMapperDO implements EomsDataScopeHelper.DeptAware
{
    private static final long serialVersionUID = 1L;

    /** 租户ID（DataScope） */
    private Long tenantId;

    /** 创建人部门（DataScope） */
    private Long deptId;

    /** 结算单号（JS前缀） */

    private String settlementNo;

    private Long repairOrderId;

    /** 关联工单编号（非持久化，JOIN eoms_repair_order 查询用于显示） */

    private String orderNo;

    private BigDecimal serviceFee;

    private BigDecimal materialAmount;

    private BigDecimal laborAmount;

    private BigDecimal otherAmount;

    private BigDecimal totalIncome;

    private BigDecimal materialCost;

    private BigDecimal laborCost;

    private BigDecimal otherCost;

    private BigDecimal totalCost;

    private BigDecimal profitAmount;

    private String status;

    private String delFlag;

    /** 客户编号（非持久化，JOIN psi_unit 查询用于显示） */

    private String customerNo;

    /** 客户名称（非持久化，JOIN psi_unit 查询用于显示） */

    private String customerName;

    /** 客户单位名称（结算单用，非持久化，JOIN psi_unit 查询用于显示） */

    private String customerNameForBill;

    /** 客户联系电话（非持久化，JOIN psi_unit 查询用于显示） */

    private String phone;

    /** 客户地址（非持久化，JOIN psi_unit 查询用于显示） */

    private String address;

    /** 关联报修编号（非持久化，JOIN eoms_repair_apply 查询用于显示） */

    private String applyNo;

    /** 报修联系人（非持久化，JOIN eoms_repair_apply 查询用于显示） */

    private String contactName;

    /** 报修联系电话（非持久化，JOIN eoms_repair_apply 查询用于显示） */

    private String contactPhone;

    /** 故障描述（非持久化，JOIN eoms_repair_apply 查询用于显示） */

    private String faultDesc;

    /** 故障类型（非持久化，JOIN eoms_repair_apply 查询用于显示） */

    private String faultType;

    /** 报修时间（非持久化，JOIN eoms_repair_apply 查询用于显示） */

    private java.util.Date applyTime;

    /** 维修工程师姓名（非持久化，JOIN sys_user 查询用于显示） */

    private String engineerName;

    /** 维修工程师ID（非持久化，JOIN eoms_repair_order 查询用于显示） */

    private Long engineerId;

    /** 维修地址（非持久化，JOIN eoms_repair_order 查询用于显示） */

    private String serviceAddress;

    /** 完工结算时间（非持久化，JOIN eoms_repair_order.finish_time 查询用于显示） */

    private java.util.Date finishTime;

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

public String getSettlementNo()    {
        return settlementNo;
    }

    public void setSettlementNo(String settlementNo)
    {
        this.settlementNo = settlementNo;
    }

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

    public BigDecimal getServiceFee()
    {
        return serviceFee;
    }

    public void setServiceFee(BigDecimal serviceFee)
    {
        this.serviceFee = serviceFee;
    }

    public BigDecimal getMaterialAmount()
    {
        return materialAmount;
    }

    public void setMaterialAmount(BigDecimal materialAmount)
    {
        this.materialAmount = materialAmount;
    }

    public BigDecimal getLaborAmount()
    {
        return laborAmount;
    }

    public void setLaborAmount(BigDecimal laborAmount)
    {
        this.laborAmount = laborAmount;
    }

    public BigDecimal getOtherAmount()
    {
        return otherAmount;
    }

    public void setOtherAmount(BigDecimal otherAmount)
    {
        this.otherAmount = otherAmount;
    }

    public BigDecimal getTotalIncome()
    {
        return totalIncome;
    }

    public void setTotalIncome(BigDecimal totalIncome)
    {
        this.totalIncome = totalIncome;
    }

    public BigDecimal getMaterialCost()
    {
        return materialCost;
    }

    public void setMaterialCost(BigDecimal materialCost)
    {
        this.materialCost = materialCost;
    }

    public BigDecimal getLaborCost()
    {
        return laborCost;
    }

    public void setLaborCost(BigDecimal laborCost)
    {
        this.laborCost = laborCost;
    }

    public BigDecimal getOtherCost()
    {
        return otherCost;
    }

    public void setOtherCost(BigDecimal otherCost)
    {
        this.otherCost = otherCost;
    }

    public BigDecimal getTotalCost()
    {
        return totalCost;
    }

    public void setTotalCost(BigDecimal totalCost)
    {
        this.totalCost = totalCost;
    }

    public BigDecimal getProfitAmount()
    {
        return profitAmount;
    }

    public void setProfitAmount(BigDecimal profitAmount)
    {
        this.profitAmount = profitAmount;
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

    public String getCustomerNameForBill()
    {
        return customerNameForBill;
    }

    public void setCustomerNameForBill(String customerNameForBill)
    {
        this.customerNameForBill = customerNameForBill;
    }

    public String getPhone()
    {
        return phone;
    }

    public void setPhone(String phone)
    {
        this.phone = phone;
    }

    public String getAddress()
    {
        return address;
    }

    public void setAddress(String address)
    {
        this.address = address;
    }

    public String getApplyNo()
    {
        return applyNo;
    }

    public void setApplyNo(String applyNo)
    {
        this.applyNo = applyNo;
    }

    public String getContactName()
    {
        return contactName;
    }

    public void setContactName(String contactName)
    {
        this.contactName = contactName;
    }

    public String getContactPhone()
    {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone)
    {
        this.contactPhone = contactPhone;
    }

    public String getFaultDesc()
    {
        return faultDesc;
    }

    public void setFaultDesc(String faultDesc)
    {
        this.faultDesc = faultDesc;
    }

    public String getFaultType()
    {
        return faultType;
    }

    public void setFaultType(String faultType)
    {
        this.faultType = faultType;
    }

    public java.util.Date getApplyTime()
    {
        return applyTime;
    }

    public void setApplyTime(java.util.Date applyTime)
    {
        this.applyTime = applyTime;
    }

    public String getEngineerName()
    {
        return engineerName;
    }

    public void setEngineerName(String engineerName)
    {
        this.engineerName = engineerName;
    }

    public Long getEngineerId()
    {
        return engineerId;
    }

    public void setEngineerId(Long engineerId)
    {
        this.engineerId = engineerId;
    }

    public String getServiceAddress()
    {
        return serviceAddress;
    }

    public void setServiceAddress(String serviceAddress)
    {
        this.serviceAddress = serviceAddress;
    }

    public java.util.Date getFinishTime()
    {
        return finishTime;
    }

    public void setFinishTime(java.util.Date finishTime)
    {
        this.finishTime = finishTime;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("repairOrderId", getRepairOrderId())
            .append("orderNo", getOrderNo())
            .append("serviceFee", getServiceFee())
            .append("materialAmount", getMaterialAmount())
            .append("laborAmount", getLaborAmount())
            .append("otherAmount", getOtherAmount())
            .append("totalIncome", getTotalIncome())
            .append("materialCost", getMaterialCost())
            .append("laborCost", getLaborCost())
            .append("otherCost", getOtherCost())
            .append("totalCost", getTotalCost())
            .append("profitAmount", getProfitAmount())
            .append("status", getStatus())
            .append("delFlag", getDelFlag())
            .append("customerNo", getCustomerNo())
            .append("customerName", getCustomerName())
            .append("customerNameForBill", getCustomerNameForBill())
            .append("phone", getPhone())
            .append("address", getAddress())
            .append("applyNo", getApplyNo())
            .append("contactName", getContactName())
            .append("contactPhone", getContactPhone())
            .append("faultDesc", getFaultDesc())
            .append("faultType", getFaultType())
            .append("applyTime", getApplyTime())
            .append("engineerName", getEngineerName())
            .append("engineerId", getEngineerId())
            .append("serviceAddress", getServiceAddress())
            .append("finishTime", getFinishTime())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
