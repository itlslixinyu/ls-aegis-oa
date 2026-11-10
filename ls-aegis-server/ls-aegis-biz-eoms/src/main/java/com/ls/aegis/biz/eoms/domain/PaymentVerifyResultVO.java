package com.ls.aegis.biz.eoms.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 批量收款核销结果 VO
 * <p>
 * 返回每笔核销明细及汇总信息，供前端展示核销结果。
 * </p>
 *
 * @author EOMS Generator
 */
public class PaymentVerifyResultVO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 核销明细列表 */
    private List<ItemResult> items;

    /** 核销总笔数 */
    private Integer totalCount;

    /** 核销总金额 */
    private BigDecimal totalAmount;

    /** 批量核销共享的收款单号（单笔核销时为该笔单号） */
    private String paymentNo;

    /** 批量核销批次ID（单笔核销时为 null） */
    private String batchId;

    /**
     * 单笔核销结果
     */
    public static class ItemResult implements Serializable
    {
        private static final long serialVersionUID = 1L;

        /** 应收单ID */
        private Long receivableId;

        /** 应收单号（AR+id） */
        private String receivableNo;

        /** 客户ID */
        private Long customerId;

        /** 客户名称 */
        private String customerName;

        /** 关联工单号 */
        private String orderNo;

        /** 本次核销金额 */
        private BigDecimal verifyAmount;

        /** 核销后剩余欠款 */
        private BigDecimal arrearsAmount;

        /** 核销后应收单状态（PAID 或 PARTIAL） */
        private String newStatus;

        /** 生成的收款记录ID */
        private Long paymentId;

        public Long getReceivableId()
        {
            return receivableId;
        }

        public void setReceivableId(Long receivableId)
        {
            this.receivableId = receivableId;
        }

        public String getReceivableNo()
        {
            return receivableNo;
        }

        public void setReceivableNo(String receivableNo)
        {
            this.receivableNo = receivableNo;
        }

        public Long getCustomerId()
        {
            return customerId;
        }

        public void setCustomerId(Long customerId)
        {
            this.customerId = customerId;
        }

        public String getCustomerName()
        {
            return customerName;
        }

        public void setCustomerName(String customerName)
        {
            this.customerName = customerName;
        }

        public String getOrderNo()
        {
            return orderNo;
        }

        public void setOrderNo(String orderNo)
        {
            this.orderNo = orderNo;
        }

        public BigDecimal getVerifyAmount()
        {
            return verifyAmount;
        }

        public void setVerifyAmount(BigDecimal verifyAmount)
        {
            this.verifyAmount = verifyAmount;
        }

        public BigDecimal getArrearsAmount()
        {
            return arrearsAmount;
        }

        public void setArrearsAmount(BigDecimal arrearsAmount)
        {
            this.arrearsAmount = arrearsAmount;
        }

        public String getNewStatus()
        {
            return newStatus;
        }

        public void setNewStatus(String newStatus)
        {
            this.newStatus = newStatus;
        }

        public Long getPaymentId()
        {
            return paymentId;
        }

        public void setPaymentId(Long paymentId)
        {
            this.paymentId = paymentId;
        }
    }

    public List<ItemResult> getItems()
    {
        return items;
    }

    public void setItems(List<ItemResult> items)
    {
        this.items = items;
    }

    public Integer getTotalCount()
    {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount)
    {
        this.totalCount = totalCount;
    }

    public BigDecimal getTotalAmount()
    {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount)
    {
        this.totalAmount = totalAmount;
    }

    public String getPaymentNo()
    {
        return paymentNo;
    }

    public void setPaymentNo(String paymentNo)
    {
        this.paymentNo = paymentNo;
    }

    public String getBatchId()
    {
        return batchId;
    }

    public void setBatchId(String batchId)
    {
        this.batchId = batchId;
    }
}
