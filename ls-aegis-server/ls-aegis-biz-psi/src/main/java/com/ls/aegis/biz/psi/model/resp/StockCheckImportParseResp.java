package com.ls.aegis.biz.psi.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 盘点实盘 Excel 解析结果（填入草稿明细）
 */
@Data
@Schema(description = "盘点实盘 Excel 解析结果")
public class StockCheckImportParseResp implements Serializable {

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
        private BigDecimal bookQty;
        private BigDecimal checkQty;
        private BigDecimal untaxPrice;
    }
}
