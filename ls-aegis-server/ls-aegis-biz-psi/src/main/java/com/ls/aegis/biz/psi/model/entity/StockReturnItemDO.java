package com.ls.aegis.biz.psi.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ls.aegis.mybatis.base.model.entity.TenantBaseDO;
import lombok.Data;

import java.io.Serial;
import java.math.BigDecimal;

@Data
@TableName("psi_stock_return_item")
public class StockReturnItemDO extends TenantBaseDO {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long returnId;
    private Long originItemId;
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
