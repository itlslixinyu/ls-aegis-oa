package com.ls.aegis.biz.psi.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ls.aegis.mybatis.base.model.entity.TenantBaseDO;
import lombok.Data;

import java.io.Serial;
import java.math.BigDecimal;

@Data
@TableName("psi_invoice_out_item")
public class InvoiceOutItemDO extends TenantBaseDO {
    @Serial
    private static final long serialVersionUID = 1L;
    private Long invoiceId;
    private String goodsName;
    private BigDecimal quantity;
    private BigDecimal taxInclusiveAmount;
    private BigDecimal untaxAmount;
    private BigDecimal taxAmount;
    private BigDecimal taxRate;
    private Integer sort;
}
