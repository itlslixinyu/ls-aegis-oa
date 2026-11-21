package com.ls.aegis.biz.psi.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ls.aegis.biz.psi.enums.DocStatusEnum;
import com.ls.aegis.biz.psi.enums.InvoiceStatusEnum;
import com.ls.aegis.biz.psi.enums.PayStatusEnum;
import com.ls.aegis.biz.psi.enums.StockOutTypeEnum;
import com.ls.aegis.mybatis.base.model.entity.TenantBaseDO;
import lombok.Data;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 销售出库单
 */
@Data
@TableName("psi_stock_out")
public class StockOutDO extends TenantBaseDO {

    @Serial
    private static final long serialVersionUID = 1L;

    private String docNo;
    private DocStatusEnum docStatus;
    private StockOutTypeEnum outType;
    private LocalDateTime outDate;
    private Long warehouseId;
    private Long unitId;
    private Long contractId;
    /** 仅内部维修结算API写入，不能由HTTP创建请求伪造关联。 */
    private Long repairOrderId;
    private Long repairSettlementId;
    /** 创建人部门（数据权限） */
    private Long deptId;
    private BigDecimal taxInclusiveAmount;
    private BigDecimal untaxAmount;
    private BigDecimal taxAmount;
    private BigDecimal taxDiffAmount;
    private BigDecimal costAmount;
    private InvoiceStatusEnum invoiceStatus;
    private PayStatusEnum payStatus;
    private String idempotentKey;
    private String remark;
    private Long confirmUser;
    private LocalDateTime confirmTime;
}
