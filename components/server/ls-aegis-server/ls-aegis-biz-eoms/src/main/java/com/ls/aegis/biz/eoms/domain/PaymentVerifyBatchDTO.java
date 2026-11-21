package com.ls.aegis.biz.eoms.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * 批量收款核销请求 DTO
 * <p>
 * 业务场景：同一客户一次付款需核销多笔未结清应收单。
 * 统一付款方式、付款时间、备注；每笔应收单独立核销金额。
 * </p>
 *
 * @author EOMS Generator
 */
public class PaymentVerifyBatchDTO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 统一付款方式（现金、转账、微信、支付宝） */
    @NotNull(message = "收款方式不能为空")
    private String paymentMethod;

    /** 统一付款时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date paymentTime;

    /** 统一备注 */
    private String remark;

    /** 幂等令牌（防并发重复批量收款，整个批量操作共享一个 token） */
    private String idempotentToken;

    /** 核销明细列表 */
    @NotEmpty(message = "核销明细不能为空")
    @Valid
    private List<VerifyItem> items;

    /**
     * 单笔核销明细
     */
    public static class VerifyItem implements Serializable
    {
        private static final long serialVersionUID = 1L;

        /** 应收单ID */
        @NotNull(message = "应收单ID不能为空")
        private Long receivableId;

        /** 本次核销金额（必须大于0） */
        @NotNull(message = "核销金额不能为空")
        private BigDecimal amount;

        public Long getReceivableId()
        {
            return receivableId;
        }

        public void setReceivableId(Long receivableId)
        {
            this.receivableId = receivableId;
        }

        public BigDecimal getAmount()
        {
            return amount;
        }

        public void setAmount(BigDecimal amount)
        {
            this.amount = amount;
        }
    }

    public String getPaymentMethod()
    {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod)
    {
        this.paymentMethod = paymentMethod;
    }

    public Date getPaymentTime()
    {
        return paymentTime;
    }

    public void setPaymentTime(Date paymentTime)
    {
        this.paymentTime = paymentTime;
    }

    public String getRemark()
    {
        return remark;
    }

    public void setRemark(String remark)
    {
        this.remark = remark;
    }

    public String getIdempotentToken()
    {
        return idempotentToken;
    }

    public void setIdempotentToken(String idempotentToken)
    {
        this.idempotentToken = idempotentToken;
    }

    public List<VerifyItem> getItems()
    {
        return items;
    }

    public void setItems(List<VerifyItem> items)
    {
        this.items = items;
    }
}
