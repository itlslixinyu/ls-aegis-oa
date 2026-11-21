package com.ls.aegis.biz.psi.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ls.aegis.biz.psi.enums.BizSourceTypeEnum;
import com.ls.aegis.mybatis.base.model.entity.TenantBaseDO;
import lombok.Data;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 库存批次
 */
@Data
@TableName("psi_stock_batch")
public class StockBatchDO extends TenantBaseDO {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long warehouseId;
    private Long goodsId;
    private String batchNo;
    private BizSourceTypeEnum sourceType;
    private Long sourceId;
    private String sourceNo;
    private LocalDateTime inDate;
    private LocalDateTime originInDate;
    private BigDecimal totalQuantity;
    private BigDecimal remainQuantity;
    private BigDecimal untaxPrice;
    private BigDecimal totalAmount;
    private BigDecimal remainAmount;
}
