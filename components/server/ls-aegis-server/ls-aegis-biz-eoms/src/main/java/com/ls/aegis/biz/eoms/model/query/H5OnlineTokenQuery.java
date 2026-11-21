package com.ls.aegis.biz.eoms.model.query;

/**
 * H5 客户在线令牌查询条件
 */
public class H5OnlineTokenQuery
{
    /** 客户编号（模糊） */
    private String customerNo;

    /** 客户名称（模糊） */
    private String customerName;

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
}
