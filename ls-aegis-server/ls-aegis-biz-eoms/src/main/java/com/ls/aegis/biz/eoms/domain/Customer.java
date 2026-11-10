package com.ls.aegis.biz.eoms.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import com.ls.aegis.mybatis.base.model.entity.XmlMapperDO;
import com.ls.aegis.compat.common.annotation.Sensitive;
import com.ls.aegis.compat.common.enums.DesensitizedType;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * H5 客户投影（读 {@code psi_unit}，不合进 sys_user）
 */
public class Customer extends XmlMapperDO
{
    private static final long serialVersionUID = 1L;

    /** 客户编号：自编，6-20位字母或数字（防 H5 登录枚举攻击） */
    @NotBlank(message = "客户编号不能为空")
    @Pattern(regexp = "^[A-Za-z0-9]{6,20}$",
            message = "客户编号必须为6-20位字母或数字")

    private String customerNo;

    /** 客户名称 NOT NULL 无 DEFAULT，必填 */
    @NotBlank(message = "客户名称不能为空")

    private String customerName;

    private String customerType;

    private String address;

    @Sensitive(desensitizedType = DesensitizedType.PHONE)
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请输入正确的手机号码")
    @Size(min = 11, max = 11, message = "手机号必须为11位")
    private String phone;

    private String status;

    private String delFlag;

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

    public String getCustomerType()
    {
        return customerType;
    }

    public void setCustomerType(String customerType)
    {
        this.customerType = customerType;
    }

    public String getAddress()
    {
        return address;
    }

    public void setAddress(String address)
    {
        this.address = address;
    }

    public String getPhone()
    {
        return phone;
    }

    public void setPhone(String phone)
    {
        this.phone = phone;
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

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("customerNo", getCustomerNo())
            .append("customerName", getCustomerName())
            .append("customerType", getCustomerType())
            .append("address", getAddress())
            .append("phone", getPhone())
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
