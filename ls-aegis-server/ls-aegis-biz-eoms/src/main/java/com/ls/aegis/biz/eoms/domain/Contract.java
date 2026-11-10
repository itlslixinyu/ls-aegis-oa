package com.ls.aegis.biz.eoms.domain;

import java.math.BigDecimal;
import java.util.Date;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ls.aegis.mybatis.base.model.entity.XmlMapperDO;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * eoms_contract 实体类
 *
 * @author EOMS Generator
 */
public class Contract extends XmlMapperDO implements com.ls.aegis.biz.eoms.utils.EomsDataScopeHelper.DeptAware
{
    private static final long serialVersionUID = 1L;

    /** 租户ID（DataScope） */
    private Long tenantId;

    /** 创建人部门（DataScope） */
    private Long deptId;

    private String contractNo;

    /** 客户ID NOT NULL 无 DEFAULT，必填 */
    @NotNull(message = "客户ID不能为空")

    private Long customerId;

    /** 所属项目（sign_project.project_id，非强制） */
    private Long projectId;

    /** 仅查未绑定项目；非持久化 */
    private Boolean unboundOnly;

    /** 关联客户编号（非持久化，JOIN psi_unit 查询用于显示） */

    private String customerNo;

    /** 关联客户名称（非持久化，JOIN psi_unit 查询用于显示） */

    private String customerName;

    /** 合同名称 NOT NULL 无 DEFAULT，必填 */
    @NotBlank(message = "合同名称不能为空")

    private String contractName;

    private String contractType;

    @DecimalMin(value = "0", message = "合同金额不能为负数")
    @Digits(integer = 10, fraction = 2, message = "合同金额最多10位整数、2位小数")
    private BigDecimal amount;

    @NotNull(message = "签订日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date signDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")

    private Date startDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")

    private Date endDate;

    private String status;

    private String delFlag;

    /** 平台 sys_file.id，多个以英文逗号分隔 */
    private String fileId;

    /** 执行进度 0~1（非持久化，列表/详情展示用） */
    private BigDecimal progress;

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

    public String getContractNo()
    {
        return contractNo;
    }

    public void setContractNo(String contractNo)
    {
        this.contractNo = contractNo;
    }

    public Long getCustomerId()
    {
        return customerId;
    }

    public void setCustomerId(Long customerId)
    {
        this.customerId = customerId;
    }

    public Long getProjectId()
    {
        return projectId;
    }

    public void setProjectId(Long projectId)
    {
        this.projectId = projectId;
    }

    public Boolean getUnboundOnly()
    {
        return unboundOnly;
    }

    public void setUnboundOnly(Boolean unboundOnly)
    {
        this.unboundOnly = unboundOnly;
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

    public String getContractName()
    {
        return contractName;
    }

    public void setContractName(String contractName)
    {
        this.contractName = contractName;
    }

    public String getContractType()
    {
        return contractType;
    }

    public void setContractType(String contractType)
    {
        this.contractType = contractType;
    }

    public BigDecimal getAmount()
    {
        return amount;
    }

    public void setAmount(BigDecimal amount)
    {
        this.amount = amount;
    }

    public Date getSignDate()
    {
        return signDate;
    }

    public void setSignDate(Date signDate)
    {
        this.signDate = signDate;
    }

    public Date getStartDate()
    {
        return startDate;
    }

    public void setStartDate(Date startDate)
    {
        this.startDate = startDate;
    }

    public Date getEndDate()
    {
        return endDate;
    }

    public void setEndDate(Date endDate)
    {
        this.endDate = endDate;
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

    public String getFileId()
    {
        return fileId;
    }

    public void setFileId(String fileId)
    {
        this.fileId = fileId;
    }

    public BigDecimal getProgress()
    {
        return progress;
    }

    public void setProgress(BigDecimal progress)
    {
        this.progress = progress;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("contractNo", getContractNo())
            .append("customerId", getCustomerId())
            .append("projectId", getProjectId())
            .append("customerNo", getCustomerNo())
            .append("customerName", getCustomerName())
            .append("contractName", getContractName())
            .append("contractType", getContractType())
            .append("amount", getAmount())
            .append("startDate", getStartDate())
            .append("endDate", getEndDate())
            .append("status", getStatus())
            .append("delFlag", getDelFlag())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
