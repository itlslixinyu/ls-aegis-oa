package com.ls.aegis.biz.psi.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ls.aegis.mybatis.base.model.entity.TenantBaseDO;
import lombok.Data;

import java.io.Serial;
import java.math.BigDecimal;

/**
 * 销售出库明细
 */
@Data
@TableName("psi_stock_out_item")
public class StockOutItemDO extends TenantBaseDO {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long stockOutId;
    private Long goodsId;
    private BigDecimal quantity;
    private BigDecimal taxInclusivePrice;
    private BigDecimal taxRate;
    private BigDecimal untaxPrice;
    private BigDecimal untaxAmount;
    private BigDecimal taxAmount;
    private BigDecimal taxInclusiveAmount;
    private BigDecimal costAmount;
    private Integer sort;
}
