package com.ls.aegis.biz.psi.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 库存查询行（仓库 × 商品实时剩余）
 */
@Data
@Schema(description = "库存查询")
public class StockQueryResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "行键（warehouseId_goodsId）")
    private String id;

    @Schema(description = "仓库ID")
    private Long warehouseId;

    @Schema(description = "仓库名称")
    private String warehouseName;

    @Schema(description = "商品ID")
    private Long goodsId;

    @Schema(description = "商品编码")
    private String goodsCode;

    @Schema(description = "商品名称")
    private String goodsName;

    @Schema(description = "规格")
    private String spec;

    @Schema(description = "单位")
    private String unit;

    @Schema(description = "剩余数量")
    private BigDecimal remainQuantity;

    @Schema(description = "剩余金额")
    private BigDecimal remainAmount;
}
