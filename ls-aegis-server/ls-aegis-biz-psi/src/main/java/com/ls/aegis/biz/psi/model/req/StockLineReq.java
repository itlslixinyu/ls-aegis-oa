package com.ls.aegis.biz.psi.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 出入库明细行（含税单价口径）
 */
@Data
@Schema(description = "出入库明细行")
public class StockLineReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "商品ID")
    @NotNull(message = "商品不能为空")
    private Long goodsId;

    @Schema(description = "数量")
    @NotNull(message = "数量不能为空")
    private BigDecimal quantity;

    @Schema(description = "含税单价")
    @NotNull(message = "含税单价不能为空")
    private BigDecimal taxInclusivePrice;

    @Schema(description = "税率%")
    @NotNull(message = "税率不能为空")
    private BigDecimal taxRate;
}
