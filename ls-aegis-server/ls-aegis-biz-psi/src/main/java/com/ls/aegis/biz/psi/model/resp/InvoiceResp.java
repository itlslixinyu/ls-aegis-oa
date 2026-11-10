package com.ls.aegis.biz.psi.model.resp;

import com.ls.aegis.biz.psi.enums.DocStatusEnum;
import com.ls.aegis.biz.psi.enums.InvoiceTypeEnum;
import com.ls.aegis.common.base.model.resp.BaseDetailResp;
import lombok.Data;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class InvoiceResp extends BaseDetailResp {
    @Serial
    private static final long serialVersionUID = 1L;
    private String docNo;
    private DocStatusEnum docStatus;
    private String invoiceNo;
    private InvoiceTypeEnum invoiceType;
    private LocalDateTime invoiceDate;
    private Long unitId;
    private String buyerTaxNo;
    private String sellerTaxNo;
    private BigDecimal taxInclusiveAmount;
    private BigDecimal untaxAmount;
    private BigDecimal taxAmount;
    /** 已关联出入库金额合计 */
    private BigDecimal linkedAmount;
    /** 未分摊金额 = 含税 − 已关联（≥0） */
    private BigDecimal unallocatedAmount;
    private Long redOfId;
    private Long fileId;
    private String remark;
    private List<Item> items;
    private List<Link> links;

    @Data
    public static class Item {
        private Long id;
        private String goodsName;
        private BigDecimal quantity;
        private BigDecimal taxInclusiveAmount;
        private BigDecimal untaxAmount;
        private BigDecimal taxAmount;
        private BigDecimal taxRate;
    }

    @Data
    public static class Link {
        private Long id;
        private Long stockDocId;
        private BigDecimal amount;
    }
}
