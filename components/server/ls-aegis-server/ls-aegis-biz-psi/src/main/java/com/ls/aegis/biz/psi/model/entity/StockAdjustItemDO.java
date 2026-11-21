package com.ls.aegis.biz.psi.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ls.aegis.biz.psi.enums.AdjustTypeEnum;
import com.ls.aegis.mybatis.base.model.entity.TenantBaseDO;
import lombok.Data;

import java.io.Serial;
import java.math.BigDecimal;

@Data
@TableName("psi_stock_adjust_item")
public class StockAdjustItemDO extends TenantBaseDO {
    @Serial
    private static final long serialVersionUID = 1L;
    private Long adjustId;
    private Long goodsId;
    private Long originBatchId;
    private AdjustTypeEnum adjustType;
    private BigDecimal quantity;
    private BigDecimal untaxPrice;
    private BigDecimal costAmount;
    private Integer sort;
}
