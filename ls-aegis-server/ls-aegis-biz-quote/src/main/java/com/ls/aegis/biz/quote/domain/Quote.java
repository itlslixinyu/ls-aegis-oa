package com.ls.aegis.biz.quote.domain;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ls.aegis.mybatis.base.model.entity.XmlMapperDO;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * eoms_quote 实体（C4：继承 XmlMapperDO，主键用 BaseIdDO.id）
 */
public class Quote extends XmlMapperDO {

    private static final long serialVersionUID = 1L;

    /** Nullable only for legacy rows awaiting independently verified ownership assignment. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long tenantId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long deptId;

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }

    private String quoteNo;

    private Long customerId;

    /** 仓库 ID（psi_warehouse.id） */
    private Long warehouseId;

    /** 关联仓库名称（非持久化） */
    private String warehouseName;

    /** 项目名称 */
    private String projectName;

    /** 报价单位 */
    private String quoteUnit;

    /** 报价时间 */
    private Date quoteDate;

    /** 报价有效期 */
    private Date validUntil;

    /** 增值税综合税率(%) */
    private BigDecimal taxRate;

    /** 材料小计 */
    private BigDecimal materialSubtotal;

    /** 技术服务费小计 */
    private BigDecimal serviceSubtotal;

    /** 备注/条款 */
    private String terms;

    /** 关联客户编号（非持久化） */
    private String customerNo;

    /** 关联客户名称（非持久化） */
    private String customerName;

    private Long repairOrderId;

    /** 关联工单编号（非持久化） */
    private String orderNo;

    /** 报价总金额；请求体兼容前端历史字段 amount */
    @JsonAlias("amount")
    private BigDecimal totalAmount;

    private String status;

    /**
     * 列表场景：ACTIVE=进行中(草稿/已提交/已取消)，HISTORY=历史(仅已确认)。非表字段。
     * 与 status 同时传时以 status 精确匹配为准。
     */
    private String statusScope;

    private String delFlag;

    /** 明细行（非表字段；详情返回，新增/修改时一并保存） */
    private List<QuoteDetail> details;

    public String getQuoteNo() {
        return quoteNo;
    }

    public void setQuoteNo(String quoteNo) {
        this.quoteNo = quoteNo;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    public void setWarehouseName(String warehouseName) {
        this.warehouseName = warehouseName;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getQuoteUnit() {
        return quoteUnit;
    }

    public void setQuoteUnit(String quoteUnit) {
        this.quoteUnit = quoteUnit;
    }

    public Date getQuoteDate() {
        return quoteDate;
    }

    public void setQuoteDate(Date quoteDate) {
        this.quoteDate = quoteDate;
    }

    public Date getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(Date validUntil) {
        this.validUntil = validUntil;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    public BigDecimal getMaterialSubtotal() {
        return materialSubtotal;
    }

    public void setMaterialSubtotal(BigDecimal materialSubtotal) {
        this.materialSubtotal = materialSubtotal;
    }

    public BigDecimal getServiceSubtotal() {
        return serviceSubtotal;
    }

    public void setServiceSubtotal(BigDecimal serviceSubtotal) {
        this.serviceSubtotal = serviceSubtotal;
    }

    public String getTerms() {
        return terms;
    }

    public void setTerms(String terms) {
        this.terms = terms;
    }

    public String getCustomerNo() {
        return customerNo;
    }

    public void setCustomerNo(String customerNo) {
        this.customerNo = customerNo;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public Long getRepairOrderId() {
        return repairOrderId;
    }

    public void setRepairOrderId(Long repairOrderId) {
        this.repairOrderId = repairOrderId;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    /**
     * 兼容前端历史字段 amount，序列化时一并输出
     */
    @JsonProperty("amount")
    public BigDecimal getAmount() {
        return totalAmount;
    }

    @JsonProperty("amount")
    public void setAmount(BigDecimal amount) {
        this.totalAmount = amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusScope() {
        return statusScope;
    }

    public void setStatusScope(String statusScope) {
        this.statusScope = statusScope;
    }

    public String getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(String delFlag) {
        this.delFlag = delFlag;
    }

    public List<QuoteDetail> getDetails() {
        return details;
    }

    public void setDetails(List<QuoteDetail> details) {
        this.details = details;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("quoteNo", getQuoteNo())
            .append("customerId", getCustomerId())
            .append("warehouseId", getWarehouseId())
            .append("warehouseName", getWarehouseName())
            .append("projectName", getProjectName())
            .append("quoteUnit", getQuoteUnit())
            .append("quoteDate", getQuoteDate())
            .append("validUntil", getValidUntil())
            .append("taxRate", getTaxRate())
            .append("materialSubtotal", getMaterialSubtotal())
            .append("serviceSubtotal", getServiceSubtotal())
            .append("customerNo", getCustomerNo())
            .append("customerName", getCustomerName())
            .append("repairOrderId", getRepairOrderId())
            .append("orderNo", getOrderNo())
            .append("totalAmount", getTotalAmount())
            .append("status", getStatus())
            .append("delFlag", getDelFlag())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .append("terms", getTerms())
            .toString();
    }
}
