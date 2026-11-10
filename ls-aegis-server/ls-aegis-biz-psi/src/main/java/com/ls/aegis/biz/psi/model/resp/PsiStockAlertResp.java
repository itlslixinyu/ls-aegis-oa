package com.ls.aegis.biz.psi.model.resp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 库存上下限预警行
 */
@Data
public class PsiStockAlertResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long goodsId;
    private String goodsCode;
    private String goodsName;
    /** 当前结存（全仓合计） */
    private BigDecimal stockQty;
    private BigDecimal stockLower;
    private BigDecimal stockUpper;
    /** LOW / HIGH */
    private String alertType;
}
