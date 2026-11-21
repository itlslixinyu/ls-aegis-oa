package com.ls.aegis.biz.psi.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ls.aegis.biz.psi.enums.DocStatusEnum;
import com.ls.aegis.biz.psi.enums.InvoiceTypeEnum;
import com.ls.aegis.mybatis.base.model.entity.TenantBaseDO;
import lombok.Data;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("psi_invoice_out")
public class InvoiceOutDO extends TenantBaseDO {
    @Serial
    private static final long serialVersionUID = 1L;
    /** 创建人部门（数据权限） */
    private Long deptId;
    private String docNo;
    private DocStatusEnum docStatus;
    private String invoiceNo;
    private InvoiceTypeEnum invoiceType;
    private LocalDateTime invoiceDate;
    private Long unitId;
    private String buyerTaxNo;
    private String sellerTaxNo;
    private BigDecimal taxInclusiveAmount;
    private BigDecimal untaxAmount;
    private BigDecimal taxAmount;
    private Long redOfId;
    private String idempotentKey;
    private Long fileId;
    private String remark;
    private Long confirmUser;
    private LocalDateTime confirmTime;
}
