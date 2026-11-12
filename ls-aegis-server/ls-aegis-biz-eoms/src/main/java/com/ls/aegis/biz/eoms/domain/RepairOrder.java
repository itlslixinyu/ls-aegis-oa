package com.ls.aegis.biz.eoms.domain;

import com.ls.aegis.biz.eoms.utils.EomsDataScopeHelper;
import com.ls.aegis.mybatis.base.model.entity.XmlMapperDO;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * eoms_repair_order 实体类
 * 
 * @author EOMS Generator
 */
public class RepairOrder extends XmlMapperDO implements EomsDataScopeHelper.DeptAware
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

    private String orderNo;

    private Long applyId;

    private Long quoteId;

    /** 关联报修编号（非持久化，JOIN eoms_repair_apply 查询用于显示） */

    private String applyNo;

    /** 关联报价单号（非持久化，JOIN eoms_quote 查询用于显示） */

    private String quoteNo;

    private Long customerId;

    private Long engineerId;

    /** 客户名称（非持久化，JOIN psi_unit 查询用于显示） */

    private String customerName;

    /** 工程师姓名（非持久化，JOIN sys_user 查询用于显示） */

    private String engineerName;

    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")

    private Date acceptTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")

    private Date startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")

    private Date finishTime;

    private String address;

    private String photos;

    /** 预览 URL，逗号分隔（非持久化；photos 存 sys_file.id 或存量路径） */
    private String photoUrls;

    /** 签到经度（开始维修时定位） */

    private String signInLongitude;

    /** 签到纬度（开始维修时定位） */

    private String signInLatitude;

    /** 签到地址（逆地理编码中文地址） */

    private String signInAddress;

    /** 签退经度（完成维修时定位） */

    private String signOutLongitude;

    /** 签退纬度（完成维修时定位） */

    private String signOutLatitude;

    /** 签退地址（逆地理编码中文地址） */

    private String signOutAddress;

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

    public String getOrderNo()
    {
        return orderNo;
    }

    public void setOrderNo(String orderNo)
    {
        this.orderNo = orderNo;
    }

    public Long getApplyId()
    {
        return applyId;
    }

    public void setApplyId(Long applyId)
    {
        this.applyId = applyId;
    }

    public Long getQuoteId()
    {
        return quoteId;
    }

    public void setQuoteId(Long quoteId)
    {
        this.quoteId = quoteId;
    }

    public String getApplyNo()
    {
        return applyNo;
    }

    public void setApplyNo(String applyNo)
    {
        this.applyNo = applyNo;
    }

    public String getQuoteNo()
    {
        return quoteNo;
    }

    public void setQuoteNo(String quoteNo)
    {
        this.quoteNo = quoteNo;
    }

    public Long getCustomerId()
    {
        return customerId;
    }

    public void setCustomerId(Long customerId)
    {
        this.customerId = customerId;
    }

    public Long getEngineerId()
    {
        return engineerId;
    }

    public void setEngineerId(Long engineerId)
    {
        this.engineerId = engineerId;
    }

    public String getCustomerName()
    {
        return customerName;
    }

    public void setCustomerName(String customerName)
    {
        this.customerName = customerName;
    }

    public String getEngineerName()
    {
        return engineerName;
    }

    public void setEngineerName(String engineerName)
    {
        this.engineerName = engineerName;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
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

    public Date getAcceptTime()
    {
        return acceptTime;
    }

    public void setAcceptTime(Date acceptTime)
    {
        this.acceptTime = acceptTime;
    }

    public Date getStartTime()
    {
        return startTime;
    }

    public void setStartTime(Date startTime)
    {
        this.startTime = startTime;
    }

    public Date getFinishTime()
    {
        return finishTime;
    }

    public void setFinishTime(Date finishTime)
    {
        this.finishTime = finishTime;
    }

    public String getAddress()
    {
        return address;
    }

    public void setAddress(String address)
    {
        this.address = address;
    }

    public String getDelFlag()
    {
        return delFlag;
    }

    public void setDelFlag(String delFlag)
    {
        this.delFlag = delFlag;
    }

    public String getSignInLongitude()
    {
        return signInLongitude;
    }

    public void setSignInLongitude(String signInLongitude)
    {
        this.signInLongitude = signInLongitude;
    }

    public String getSignInLatitude()
    {
        return signInLatitude;
    }

    public void setSignInLatitude(String signInLatitude)
    {
        this.signInLatitude = signInLatitude;
    }

    public String getSignInAddress()
    {
        return signInAddress;
    }

    public void setSignInAddress(String signInAddress)
    {
        this.signInAddress = signInAddress;
    }

    public String getSignOutLongitude()
    {
        return signOutLongitude;
    }

    public void setSignOutLongitude(String signOutLongitude)
    {
        this.signOutLongitude = signOutLongitude;
    }

    public String getSignOutLatitude()
    {
        return signOutLatitude;
    }

    public void setSignOutLatitude(String signOutLatitude)
    {
        this.signOutLatitude = signOutLatitude;
    }

    public String getSignOutAddress()
    {
        return signOutAddress;
    }

    public void setSignOutAddress(String signOutAddress)
    {
        this.signOutAddress = signOutAddress;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("orderNo", getOrderNo())
            .append("applyId", getApplyId())
            .append("quoteId", getQuoteId())
            .append("customerId", getCustomerId())
            .append("engineerId", getEngineerId())
            .append("status", getStatus())
            .append("acceptTime", getAcceptTime())
            .append("startTime", getStartTime())
            .append("finishTime", getFinishTime())
            .append("address", getAddress())
            .append("signInLongitude", getSignInLongitude())
            .append("signInLatitude", getSignInLatitude())
            .append("signInAddress", getSignInAddress())
            .append("signOutLongitude", getSignOutLongitude())
            .append("signOutLatitude", getSignOutLatitude())
            .append("signOutAddress", getSignOutAddress())
            .append("delFlag", getDelFlag())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
