package com.ls.aegis.biz.psi.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ls.aegis.mybatis.base.model.entity.TenantBaseDO;
import lombok.Data;

import java.io.Serial;
import java.math.BigDecimal;

@Data
@TableName("psi_invoice_in_link")
public class InvoiceInLinkDO extends TenantBaseDO {
    @Serial
    private static final long serialVersionUID = 1L;
    private Long invoiceId;
    private Long stockInId;
    private BigDecimal amount;
}
