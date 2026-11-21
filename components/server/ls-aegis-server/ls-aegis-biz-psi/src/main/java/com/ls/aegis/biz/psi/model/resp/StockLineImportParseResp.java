package com.ls.aegis.biz.psi.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 出入库明细 Excel 解析结果（填入草稿明细）
 */
@Data
@Schema(description = "出入库明细 Excel 解析结果")
public class StockLineImportParseResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Integer totalRows;
    private Integer validRows;
    private List<Item> items;

    @Data
    public static class Item implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Long goodsId;
        private String goodsCode;
        private String goodsName;
        private BigDecimal quantity;
        private BigDecimal taxInclusivePrice;
        private BigDecimal taxRate;
    }
}
