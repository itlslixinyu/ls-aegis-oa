package com.ls.aegis.biz.psi.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 银行流水 Excel 解析结果（填入前端导入表格）
 */
@Data
@Schema(description = "银行流水 Excel 解析结果")
public class BankImportParseResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Integer totalRows;
    private Integer validRows;
    private List<Item> items;

    @Data
    public static class Item implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        /** ISO 日期时间，供前端 date-picker */
        private String stmtDate;
        /** IN / OUT */
        private String direction;
        private BigDecimal amount;
        private String counterparty;
        private String summary;
        private String bankSeqNo;
    }
}
