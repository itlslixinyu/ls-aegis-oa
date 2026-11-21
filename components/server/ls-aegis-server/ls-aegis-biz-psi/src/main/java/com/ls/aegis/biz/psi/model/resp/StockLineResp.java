package com.ls.aegis.biz.psi.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Schema(description = "出入库明细响应")
public class StockLineResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long goodsId;
    @Schema(description = "商品名称")
    private String goodsName;
    private BigDecimal quantity;
    private BigDecimal taxInclusivePrice;
    private BigDecimal taxRate;
    private BigDecimal untaxPrice;
    private BigDecimal untaxAmount;
    private BigDecimal taxAmount;
    private BigDecimal taxInclusiveAmount;
    private BigDecimal costAmount;
    private Integer sort;
}
