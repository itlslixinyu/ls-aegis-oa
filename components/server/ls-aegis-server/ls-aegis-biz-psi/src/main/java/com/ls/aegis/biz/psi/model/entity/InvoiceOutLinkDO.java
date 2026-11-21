package com.ls.aegis.biz.psi.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ls.aegis.mybatis.base.model.entity.TenantBaseDO;
import lombok.Data;

import java.io.Serial;
import java.math.BigDecimal;

@Data
@TableName("psi_invoice_out_link")
public class InvoiceOutLinkDO extends TenantBaseDO {
    @Serial
    private static final long serialVersionUID = 1L;
    private Long invoiceId;
    private Long stockOutId;
    private BigDecimal amount;
}
