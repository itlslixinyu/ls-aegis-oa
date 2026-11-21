package com.ls.aegis.biz.sign.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.ls.aegis.biz.sign.config.ArrayToStringDeserializer;
import com.ls.aegis.mybatis.base.model.entity.XmlMapperDO;

import java.util.Date;
import java.util.List;

/**
 * 签章项目表 sign_project
 */
public class SignProject extends XmlMapperDO implements com.ls.aegis.biz.sign.utils.SignDataScopeHelper.DeptAware {

    private static final long serialVersionUID = 1L;

    public static final String STATUS_PENDING_REVIEW = "0";
    public static final String STATUS_NOT_STARTED = "1";
    public static final String STATUS_IN_PROGRESS = "2";
    public static final String STATUS_COMPLETED = "3";
    public static final String STATUS_PAUSED = "4";
    public static final String STATUS_CANCELLED = "5";

    public static final String CONTRACT_SALE = "SALE";
    public static final String CONTRACT_PURCHASE = "PURCHASE";
    public static final String CONTRACT_REPAIR = "REPAIR";

    /** 项目ID */
    private Long projectId;

    /** 项目名称 */
    private String projectName;

    /** 项目编号 */
    private String projectCode;

    /** 负责人用户ID(多个用逗号分隔) */
    @JsonDeserialize(using = ArrayToStringDeserializer.class)
    private String managerIds;

    /** 负责人姓名(多个用逗号分隔) */
    @JsonDeserialize(using = ArrayToStringDeserializer.class)
    private String managerNames;

    /** 所属部门ID */
    private Long deptId;

    /** 租户ID（DataScope） */
    private Long tenantId;

    /** 所属部门名称 */
    private String deptName;

    /** 开始日期 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date startDate;

    /** 结束日期 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date endDate;

    /**
     * 状态(0待审核 1未开始 2进行中 3已完成 4已暂停 5已取消)
     * <p>流转：新建→待审核；审核通过→未开始（若已到开始时间则进行中，已到结束时间则已完成）；
     * 到达开始时间自动→进行中；到达结束时间自动→已完成。</p>
     */
    private String status;

    /** 项目描述 */
    private String description;

    /** 兼容旧筛/汇总：合同类型 */
    private String contractType;

    /** 兼容旧筛/汇总：合同ID */
    private Long contractId;

    /** 合同编号汇总快照 */
    private String contractNo;

    /** 合同名称汇总快照 */
    private String contractName;

    /** 往来单位汇总快照 */
    private String partnerName;

    /** 销售合同ID */
    private Long saleContractId;
    private String saleContractNo;
    private String saleContractName;
    private String salePartnerName;

    /** 采购合同ID */
    private Long purchaseContractId;
    private String purchaseContractNo;
    private String purchaseContractName;
    private String purchasePartnerName;

    /** 维修合同ID */
    private Long repairContractId;
    private String repairContractNo;
    private String repairContractName;
    private String repairPartnerName;

    /** 其他文件（平台 sys_file.id，逗号分隔；可与合同二选一或并存） */
    private String otherFileIds;

    /** 删除标志(0存在 2删除) */
    private String delFlag;

    // --- 扩展字段 ---
    /** 关联印章ID列表 */
    private List<Long> sealIds;

    /** 关联用户ID列表 */
    private Long[] userIds;

    /** 关联印章名称列表 */
    private String[] sealNames;

    /** 关联用户名列表 */
    private String[] userNames;

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getProjectCode() {
        return projectCode;
    }

    public void setProjectCode(String projectCode) {
        this.projectCode = projectCode;
    }

    public String getManagerIds() {
        return managerIds;
    }

    public void setManagerIds(String managerIds) {
        this.managerIds = managerIds;
    }

    public String getManagerNames() {
        return managerNames;
    }

    public void setManagerNames(String managerNames) {
        this.managerNames = managerNames;
    }

    @Override
    public Long getDeptId() {
        return deptId;
    }

    @Override
    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }

    @Override
    public Long getTenantId() {
        return tenantId;
    }

    @Override
    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getContractType() {
        return contractType;
    }

    public void setContractType(String contractType) {
        this.contractType = contractType;
    }

    public Long getContractId() {
        return contractId;
    }

    public void setContractId(Long contractId) {
        this.contractId = contractId;
    }

    public String getContractNo() {
        return contractNo;
    }

    public void setContractNo(String contractNo) {
        this.contractNo = contractNo;
    }

    public String getContractName() {
        return contractName;
    }

    public void setContractName(String contractName) {
        this.contractName = contractName;
    }

    public String getPartnerName() {
        return partnerName;
    }

    public void setPartnerName(String partnerName) {
        this.partnerName = partnerName;
    }

    public Long getSaleContractId() {
        return saleContractId;
    }

    public void setSaleContractId(Long saleContractId) {
        this.saleContractId = saleContractId;
    }

    public String getSaleContractNo() {
        return saleContractNo;
    }

    public void setSaleContractNo(String saleContractNo) {
        this.saleContractNo = saleContractNo;
    }

    public String getSaleContractName() {
        return saleContractName;
    }

    public void setSaleContractName(String saleContractName) {
        this.saleContractName = saleContractName;
    }

    public String getSalePartnerName() {
        return salePartnerName;
    }

    public void setSalePartnerName(String salePartnerName) {
        this.salePartnerName = salePartnerName;
    }

    public Long getPurchaseContractId() {
        return purchaseContractId;
    }

    public void setPurchaseContractId(Long purchaseContractId) {
        this.purchaseContractId = purchaseContractId;
    }

    public String getPurchaseContractNo() {
        return purchaseContractNo;
    }

    public void setPurchaseContractNo(String purchaseContractNo) {
        this.purchaseContractNo = purchaseContractNo;
    }

    public String getPurchaseContractName() {
        return purchaseContractName;
    }

    public void setPurchaseContractName(String purchaseContractName) {
        this.purchaseContractName = purchaseContractName;
    }

    public String getPurchasePartnerName() {
        return purchasePartnerName;
    }

    public void setPurchasePartnerName(String purchasePartnerName) {
        this.purchasePartnerName = purchasePartnerName;
    }

    public Long getRepairContractId() {
        return repairContractId;
    }

    public void setRepairContractId(Long repairContractId) {
        this.repairContractId = repairContractId;
    }

    public String getRepairContractNo() {
        return repairContractNo;
    }

    public void setRepairContractNo(String repairContractNo) {
        this.repairContractNo = repairContractNo;
    }

    public String getRepairContractName() {
        return repairContractName;
    }

    public void setRepairContractName(String repairContractName) {
        this.repairContractName = repairContractName;
    }

    public String getRepairPartnerName() {
        return repairPartnerName;
    }

    public void setRepairPartnerName(String repairPartnerName) {
        this.repairPartnerName = repairPartnerName;
    }

    public String getOtherFileIds() {
        return otherFileIds;
    }

    public void setOtherFileIds(String otherFileIds) {
        this.otherFileIds = otherFileIds;
    }

    public String getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(String delFlag) {
        this.delFlag = delFlag;
    }

    public List<Long> getSealIds() {
        return sealIds;
    }

    public void setSealIds(List<Long> sealIds) {
        this.sealIds = sealIds;
    }

    public Long[] getUserIds() {
        return userIds;
    }

    public void setUserIds(Long[] userIds) {
        this.userIds = userIds;
    }

    public String[] getSealNames() {
        return sealNames;
    }

    public void setSealNames(String[] sealNames) {
        this.sealNames = sealNames;
    }

    public String[] getUserNames() {
        return userNames;
    }

    public void setUserNames(String[] userNames) {
        this.userNames = userNames;
    }
}
