package com.ls.aegis.biz.psi.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ls.aegis.mybatis.base.model.entity.TenantBaseDO;
import lombok.Data;

import java.io.Serial;
import java.math.BigDecimal;

@Data
@TableName("psi_init_stock_item")
public class InitStockItemDO extends TenantBaseDO {
    @Serial
    private static final long serialVersionUID = 1L;
    private Long initId;
    private Long goodsId;
    private BigDecimal quantity;
    private BigDecimal untaxPrice;
    private BigDecimal untaxAmount;
    private Integer sort;
}
