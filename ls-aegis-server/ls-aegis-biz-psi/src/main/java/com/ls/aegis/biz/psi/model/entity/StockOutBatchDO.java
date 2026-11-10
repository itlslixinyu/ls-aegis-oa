package com.ls.aegis.biz.psi.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ls.aegis.mybatis.base.model.entity.TenantBaseDO;
import lombok.Data;

import java.io.Serial;
import java.math.BigDecimal;

/**
 * 出库批次消耗明细
 */
@Data
@TableName("psi_stock_out_batch")
public class StockOutBatchDO extends TenantBaseDO {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long stockOutItemId;
    private Long batchId;
    private BigDecimal quantity;
    private BigDecimal untaxPrice;
    private BigDecimal costAmount;
}
