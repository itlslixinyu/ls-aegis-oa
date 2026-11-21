package com.ls.aegis.biz.psi.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ls.aegis.mybatis.base.model.entity.TenantBaseDO;
import lombok.Data;

import java.io.Serial;
import java.math.BigDecimal;

/**
 * 采购入库明细
 */
@Data
@TableName("psi_stock_in_item")
public class StockInItemDO extends TenantBaseDO {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long stockInId;
    private Long goodsId;
    private BigDecimal quantity;
    private BigDecimal taxInclusivePrice;
    private BigDecimal taxRate;
    private BigDecimal untaxPrice;
    private BigDecimal untaxAmount;
    private BigDecimal taxAmount;
    private BigDecimal taxInclusiveAmount;
    private Integer sort;
}
