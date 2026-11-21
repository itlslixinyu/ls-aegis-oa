package com.ls.aegis.biz.psi.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ls.aegis.biz.psi.enums.DocStatusEnum;
import com.ls.aegis.biz.psi.enums.InvoiceStatusEnum;
import com.ls.aegis.biz.psi.enums.PayStatusEnum;
import com.ls.aegis.biz.psi.enums.StockInTypeEnum;
import com.ls.aegis.mybatis.base.model.entity.TenantBaseDO;
import lombok.Data;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 采购入库单
 */
@Data
@TableName("psi_stock_in")
public class StockInDO extends TenantBaseDO {

    @Serial
    private static final long serialVersionUID = 1L;

    private String docNo;
    private DocStatusEnum docStatus;
    private StockInTypeEnum inType;
    private LocalDateTime inDate;
    private Long warehouseId;
    private Long unitId;
    private Long contractId;
    /** 创建人部门（数据权限） */
    private Long deptId;
    private BigDecimal taxInclusiveAmount;
    private BigDecimal untaxAmount;
    private BigDecimal taxAmount;
    private BigDecimal taxDiffAmount;
    private PayStatusEnum payStatus;
    private InvoiceStatusEnum invoiceStatus;
    private String idempotentKey;
    private String remark;
    private Long confirmUser;
    private LocalDateTime confirmTime;
}
