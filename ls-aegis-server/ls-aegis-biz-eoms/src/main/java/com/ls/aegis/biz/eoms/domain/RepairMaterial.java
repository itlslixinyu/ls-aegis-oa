package com.ls.aegis.biz.eoms.domain;

import com.ls.aegis.mybatis.base.model.entity.XmlMapperDO;
import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * eoms_repair_material 实体类
 * 
 * @author EOMS Generator
 */
public class RepairMaterial extends XmlMapperDO
{
    private static final long serialVersionUID = 1L;


    private Long repairOrderId;

    /** 关联工单编号（非持久化，JOIN eoms_repair_order 查询用于显示） */

    private String orderNo;

    private Long materialId;

    /** 出库仓库（关联 psi_warehouse.id） */
    private Long warehouseId;

    private String materialName;

    private BigDecimal quantity;

    private BigDecimal salePrice;

    private BigDecimal costPrice;

    private BigDecimal saleAmount;

    private BigDecimal costAmount;

    private String delFlag;

    /** 物料编号（非持久化，JOIN psi_goods 查询用于显示） */

    private String materialNo;

    /** 物料规格型号（非持久化，JOIN psi_goods.spec 查询用于显示） */

    private String specification;

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

    public Long getMaterialId()
    {
        return materialId;
    }

    public void setMaterialId(Long materialId)
    {
        this.materialId = materialId;
    }

    public Long getWarehouseId()
    {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId)
    {
        this.warehouseId = warehouseId;
    }

    public String getMaterialName()
    {
        return materialName;
    }

    public void setMaterialName(String materialName)
    {
        this.materialName = materialName;
    }

    public BigDecimal getQuantity()
    {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity)
    {
        this.quantity = quantity;
    }

    public BigDecimal getSalePrice()
    {
        return salePrice;
    }

    public void setSalePrice(BigDecimal salePrice)
    {
        this.salePrice = salePrice;
    }

    public BigDecimal getCostPrice()
    {
        return costPrice;
    }

    public void setCostPrice(BigDecimal costPrice)
    {
        this.costPrice = costPrice;
    }

    public BigDecimal getSaleAmount()
    {
        return saleAmount;
    }

    public void setSaleAmount(BigDecimal saleAmount)
    {
        this.saleAmount = saleAmount;
    }

    public BigDecimal getCostAmount()
    {
        return costAmount;
    }

    public void setCostAmount(BigDecimal costAmount)
    {
        this.costAmount = costAmount;
    }

    public String getDelFlag()
    {
        return delFlag;
    }

    public void setDelFlag(String delFlag)
    {
        this.delFlag = delFlag;
    }

    public String getMaterialNo()
    {
        return materialNo;
    }

    public void setMaterialNo(String materialNo)
    {
        this.materialNo = materialNo;
    }

    public String getSpecification()
    {
        return specification;
    }

    public void setSpecification(String specification)
    {
        this.specification = specification;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("repairOrderId", getRepairOrderId())
            .append("orderNo", getOrderNo())
            .append("materialId", getMaterialId())
            .append("materialName", getMaterialName())
            .append("quantity", getQuantity())
            .append("salePrice", getSalePrice())
            .append("costPrice", getCostPrice())
            .append("saleAmount", getSaleAmount())
            .append("costAmount", getCostAmount())
            .append("delFlag", getDelFlag())
            .append("materialNo", getMaterialNo())
            .append("specification", getSpecification())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
