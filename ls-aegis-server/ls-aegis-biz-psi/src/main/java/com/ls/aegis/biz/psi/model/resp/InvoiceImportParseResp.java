package com.ls.aegis.biz.psi.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 税务发票文件解析结果（填入草稿表单；关联出入库仍需手工选；字段可手改）
 */
@Data
@Schema(description = "税务发票文件解析结果")
public class InvoiceImportParseResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Integer totalRows;
    private Integer validRows;
    private String invoiceNo;
    private String invoiceType;
    private String invoiceDate;
    private Long unitId;
    private String unitCode;
    private String unitName;
    private String buyerTaxNo;
    private String sellerTaxNo;
    private BigDecimal taxInclusiveAmount;
    private BigDecimal untaxAmount;
    private BigDecimal taxAmount;
    private String remark;
    private List<Item> items;

    @Data
    public static class Item implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private String goodsName;
        private BigDecimal quantity;
        private BigDecimal taxInclusiveAmount;
        private BigDecimal untaxAmount;
        private BigDecimal taxAmount;
        private BigDecimal taxRate;
    }
}
