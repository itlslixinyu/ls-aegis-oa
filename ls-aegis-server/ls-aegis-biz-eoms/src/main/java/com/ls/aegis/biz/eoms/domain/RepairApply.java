package com.ls.aegis.biz.eoms.domain;

import com.ls.aegis.biz.eoms.utils.EomsDataScopeHelper;
import com.ls.aegis.mybatis.base.model.entity.XmlMapperDO;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ls.aegis.compat.common.annotation.Sensitive;
import com.ls.aegis.compat.common.enums.DesensitizedType;
import jakarta.validation.constraints.NotNull;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * eoms_repair_apply 实体类
 * 
 * @author EOMS Generator
 */
public class RepairApply extends XmlMapperDO implements EomsDataScopeHelper.DeptAware
{
    private static final long serialVersionUID = 1L;

    /** Customer-submitted location, separate from service sign-in/out and legacy order address. */
    private String reportAddress;
    public String getReportAddress() { return reportAddress; }
    public void setReportAddress(String value) { reportAddress = value; }


    /** 租户ID（DataScope） */
    private Long tenantId;

    /** 创建人部门（DataScope） */
    private Long deptId;

    private String repairNo;

    @NotNull(message = "客户不能为空")
    private Long reporterAccountId;
    private String reporterName;
    public Long getReporterAccountId() { return reporterAccountId; }
    public void setReporterAccountId(Long value) { reporterAccountId = value; }
    public String getReporterName() { return reporterName; }
    public void setReporterName(String value) { reporterName = value; }

    private Long customerId;

    /** 关联客户编号（非持久化，JOIN psi_unit 查询用于显示） */

    private String customerNo;

    /** 关联客户名称（非持久化，JOIN psi_unit 查询用于显示） */

    private String customerName;

    private String contactName;

    @Sensitive(desensitizedType = DesensitizedType.PHONE)
    private String contactPhone;

    private String faultType;

    private String faultDesc;

    private String photos;

    /** 预览 URL，逗号分隔（非持久化；photos 存 sys_file.id 或存量路径） */
    private String photoUrls;

    private String urgentLevel;

    private Long contractId;

    /** 关联合同编号（非持久化，JOIN eoms_contract 查询用于显示） */

    private String contractNo;

    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")

    private Date applyTime;

    private String source;

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

public String getRepairNo()
    {
        return repairNo;
    }

    public void setRepairNo(String repairNo)
    {
        this.repairNo = repairNo;
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

    public String getFaultType()
    {
        return faultType;
    }

    public void setFaultType(String faultType)
    {
        this.faultType = faultType;
    }

    public String getFaultDesc()
    {
        return faultDesc;
    }

    public void setFaultDesc(String faultDesc)
    {
        this.faultDesc = faultDesc;
    }

    public String getPhotos()
    {
        return photos;
    }

    public void setPhotos(String photos)
    {
        this.photos = photos;
    }

    public String getPhotoUrls()
    {
        return photoUrls;
    }

    public void setPhotoUrls(String photoUrls)
    {
        this.photoUrls = photoUrls;
    }

    public String getUrgentLevel()
    {
        return urgentLevel;
    }

    public void setUrgentLevel(String urgentLevel)
    {
        this.urgentLevel = urgentLevel;
    }

    public Long getContractId()
    {
        return contractId;
    }

    public void setContractId(Long contractId)
    {
        this.contractId = contractId;
    }

    public String getContractNo()
    {
        return contractNo;
    }

    public void setContractNo(String contractNo)
    {
        this.contractNo = contractNo;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public Date getApplyTime()
    {
        return applyTime;
    }

    public void setApplyTime(Date applyTime)
    {
        this.applyTime = applyTime;
    }

    public String getSource()
    {
        return source;
    }

    public void setSource(String source)
    {
        this.source = source;
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
            .append("repairNo", getRepairNo())
            .append("customerId", getCustomerId())
            .append("customerNo", getCustomerNo())
            .append("customerName", getCustomerName())
            .append("contactName", getContactName())
            .append("contactPhone", getContactPhone())
            .append("faultType", getFaultType())
            .append("faultDesc", getFaultDesc())
            .append("urgentLevel", getUrgentLevel())
            .append("contractId", getContractId())
            .append("contractNo", getContractNo())
            .append("status", getStatus())
            .append("applyTime", getApplyTime())
            .append("source", getSource())
            .append("delFlag", getDelFlag())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
