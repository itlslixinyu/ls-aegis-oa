package com.ls.aegis.biz.quote.domain;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.ls.aegis.mybatis.base.model.entity.XmlMapperDO;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * eoms_quote_detail 实体（C4：继承 XmlMapperDO，主键用 BaseIdDO.id）
 */
public class QuoteDetail extends XmlMapperDO {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "报价单ID不能为空")
    private Long quoteId;

    /** MATERIAL 材料 / SERVICE 服务 */
    private String lineType;

    /** 材料行关联 psi_goods.id */
    private Long goodsId;

    @NotBlank(message = "项目名称不能为空")
    private String itemName;

    /** 品牌/型号（材料行） */
    private String brandModel;

    /** 单位：台/项等 */
    private String unit;

    /** 服务说明（服务行） */
    private String description;

    /** 行序号 */
    private Integer sortNo;

    @NotNull(message = "数量不能为空")
    private BigDecimal quantity;

    @NotNull(message = "单价不能为空")
    private BigDecimal price;

    @NotNull(message = "金额不能为空")
    private BigDecimal amount;

    private String delFlag;

    public Long getQuoteId() {
        return quoteId;
    }

    public void setQuoteId(Long quoteId) {
        this.quoteId = quoteId;
    }

    public String getLineType() {
        return lineType;
    }

    public void setLineType(String lineType) {
        this.lineType = lineType;
    }

    public Long getGoodsId() {
        return goodsId;
    }

    public void setGoodsId(Long goodsId) {
        this.goodsId = goodsId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getBrandModel() {
        return brandModel;
    }

    public void setBrandModel(String brandModel) {
        this.brandModel = brandModel;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getSortNo() {
        return sortNo;
    }

    public void setSortNo(Integer sortNo) {
        this.sortNo = sortNo;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(String delFlag) {
        this.delFlag = delFlag;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("quoteId", getQuoteId())
            .append("lineType", getLineType())
            .append("goodsId", getGoodsId())
            .append("itemName", getItemName())
            .append("brandModel", getBrandModel())
            .append("unit", getUnit())
            .append("description", getDescription())
            .append("sortNo", getSortNo())
            .append("quantity", getQuantity())
            .append("price", getPrice())
            .append("amount", getAmount())
            .append("delFlag", getDelFlag())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
