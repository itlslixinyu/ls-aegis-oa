package com.ls.aegis.biz.psi.model.resp;

import lombok.Data;
import java.math.BigDecimal;

/** 原批次盘盈纠正的只读候选，缺口并不代表可免审核增加库存。 */
@Data
public class StockRestoreBatchResp {
    private Long id;
    private String batchNo;
    private String sourceNo;
    private BigDecimal missingQuantity;
    private BigDecimal untaxPrice;
}
