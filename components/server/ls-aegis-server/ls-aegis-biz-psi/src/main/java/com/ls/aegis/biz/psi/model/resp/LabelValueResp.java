package com.ls.aegis.biz.psi.model.resp;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 下拉选项
 */
@Data
@NoArgsConstructor
public class LabelValueResp implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private String label;
    private Long value;
    /** 可选：库存数量（跨仓/单仓汇总） */
    private BigDecimal stockQty;
    /** 可选：库存加权未税单价（剩余金额 / 剩余数量） */
    private BigDecimal untaxPrice;

    public LabelValueResp(String label, Long value) {
        this.label = label;
        this.value = value;
    }

    public LabelValueResp(String label, Long value, BigDecimal stockQty) {
        this.label = label;
        this.value = value;
        this.stockQty = stockQty;
    }

    public LabelValueResp(String label, Long value, BigDecimal stockQty, BigDecimal untaxPrice) {
        this.label = label;
        this.value = value;
        this.stockQty = stockQty;
        this.untaxPrice = untaxPrice;
    }
}
