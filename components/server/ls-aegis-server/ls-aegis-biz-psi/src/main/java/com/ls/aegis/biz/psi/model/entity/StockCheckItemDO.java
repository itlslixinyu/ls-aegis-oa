package com.ls.aegis.biz.psi.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ls.aegis.mybatis.base.model.entity.TenantBaseDO;
import lombok.Data;

import java.io.Serial;
import java.math.BigDecimal;

@Data
@TableName("psi_stock_check_item")
public class StockCheckItemDO extends TenantBaseDO {
    @Serial
    private static final long serialVersionUID = 1L;
    private Long checkId;
    private Long goodsId;
    private BigDecimal bookQty;
    private BigDecimal checkQty;
    private BigDecimal diffQty;
    private BigDecimal untaxPrice;
    private BigDecimal costAmount;
    private Integer sort;
}
