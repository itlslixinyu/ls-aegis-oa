package com.ls.aegis.biz.psi.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ls.aegis.biz.psi.enums.DocStatusEnum;
import com.ls.aegis.biz.psi.enums.ReturnTypeEnum;
import com.ls.aegis.mybatis.base.model.entity.TenantBaseDO;
import lombok.Data;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("psi_stock_return")
public class StockReturnDO extends TenantBaseDO {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 创建人部门（数据权限） */
    private Long deptId;
    private String docNo;
    private DocStatusEnum docStatus;
    private ReturnTypeEnum returnType;
    private LocalDateTime returnDate;
    private Long warehouseId;
    private Long unitId;
    private Long originStockOutId;
    private Long originStockInId;
    private BigDecimal taxInclusiveAmount;
    private BigDecimal untaxAmount;
    private BigDecimal taxAmount;
    private BigDecimal costAmount;
    private String idempotentKey;
    private String remark;
    private Long confirmUser;
    private LocalDateTime confirmTime;
}
