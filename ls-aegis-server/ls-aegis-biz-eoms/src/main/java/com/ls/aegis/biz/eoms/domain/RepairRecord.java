package com.ls.aegis.biz.eoms.domain;

import com.ls.aegis.biz.eoms.utils.EomsDataScopeHelper;
import com.ls.aegis.mybatis.base.model.entity.XmlMapperDO;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * eoms_repair_record 实体类
 * 
 * @author EOMS Generator
 */
public class RepairRecord extends XmlMapperDO implements EomsDataScopeHelper.DeptAware
{
    private static final long serialVersionUID = 1L;

    /** 租户ID（DataScope） */
    private Long tenantId;

    /** 创建人部门（DataScope） */
    private Long deptId;

    private Long repairOrderId;

    /** 关联工单编号（非持久化，JOIN eoms_repair_order 查询用于显示） */

    private String orderNo;

    /** 工程师姓名（非持久化，JOIN eoms_repair_order -> sys_user 查询用于显示） */

    private String engineerName;

    private String faultReason;

    private String checkProcess;

    private String solution;

    private String result;

    /** 维修中照片URL（逗号分隔，带水印） */

    private String processingPhotos;

    /** 维修完成照片URL（逗号分隔，带水印） */

    private String completedPhotos;

    /** 维修中照片预览 URL（非持久化） */
    private String processingPhotoUrls;

    /** 维修完成照片预览 URL（非持久化） */
    private String completedPhotoUrls;

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

    public String getEngineerName()
    {
        return engineerName;
    }

    public void setEngineerName(String engineerName)
    {
        this.engineerName = engineerName;
    }

    public String getFaultReason()
    {
        return faultReason;
    }

    public void setFaultReason(String faultReason)
    {
        this.faultReason = faultReason;
    }

    public String getCheckProcess()
    {
        return checkProcess;
    }

    public void setCheckProcess(String checkProcess)
    {
        this.checkProcess = checkProcess;
    }

    public String getSolution()
    {
        return solution;
    }

    public void setSolution(String solution)
    {
        this.solution = solution;
    }

    public String getResult()
    {
        return result;
    }

    public void setResult(String result)
    {
        this.result = result;
    }

    public String getProcessingPhotos()
    {
        return processingPhotos;
    }

    public void setProcessingPhotos(String processingPhotos)
    {
        this.processingPhotos = processingPhotos;
    }

    public String getCompletedPhotos()
    {
        return completedPhotos;
    }

    public void setCompletedPhotos(String completedPhotos)
    {
        this.completedPhotos = completedPhotos;
    }

    public String getProcessingPhotoUrls()
    {
        return processingPhotoUrls;
    }

    public void setProcessingPhotoUrls(String processingPhotoUrls)
    {
        this.processingPhotoUrls = processingPhotoUrls;
    }

    public String getCompletedPhotoUrls()
    {
        return completedPhotoUrls;
    }

    public void setCompletedPhotoUrls(String completedPhotoUrls)
    {
        this.completedPhotoUrls = completedPhotoUrls;
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
            .append("engineerName", getEngineerName())
            .append("faultReason", getFaultReason())
            .append("checkProcess", getCheckProcess())
            .append("solution", getSolution())
            .append("result", getResult())
            .append("processingPhotos", getProcessingPhotos())
            .append("completedPhotos", getCompletedPhotos())
            .append("delFlag", getDelFlag())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
