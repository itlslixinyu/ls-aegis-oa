package com.ls.aegis.biz.psi.model.req;

import com.ls.aegis.biz.psi.enums.InvoiceTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "发票请求")
public class InvoiceReq implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    @NotBlank
    @Length(max = 64)
    private String invoiceNo;
    @NotNull
    private InvoiceTypeEnum invoiceType;
    @NotNull
    private LocalDateTime invoiceDate;
    @NotNull
    private Long unitId;
    @Length(max = 64)
    private String buyerTaxNo;
    @Length(max = 64)
    private String sellerTaxNo;
    @NotNull
    private BigDecimal taxInclusiveAmount;
    @NotNull
    private BigDecimal untaxAmount;
    @NotNull
    private BigDecimal taxAmount;
    private Long fileId;
    @Length(max = 500)
    private String remark;
    @Valid
    private List<Item> items;
    /** 草稿允许空或未分摊完；确认时须关联合计≈含税金额 */
    @Valid
    private List<Link> links;

    @Data
    public static class Item implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private String goodsName;
        private BigDecimal quantity;
        @NotNull
        private BigDecimal taxInclusiveAmount;
        @NotNull
        private BigDecimal untaxAmount;
        @NotNull
        private BigDecimal taxAmount;
        private BigDecimal taxRate;
    }

    @Data
    public static class Link implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        /** 销项关联出库 / 进项关联入库 */
        @NotNull
        private Long stockDocId;
        @NotNull
        private BigDecimal amount;
    }
}
