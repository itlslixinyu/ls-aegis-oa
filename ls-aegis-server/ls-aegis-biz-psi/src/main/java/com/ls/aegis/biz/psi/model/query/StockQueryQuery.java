package com.ls.aegis.biz.psi.model.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 库存查询（按仓+商品汇总剩余）
 */
@Data
@Schema(description = "库存查询条件")
public class StockQueryQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "仓库ID")
    private Long warehouseId;

    @Schema(description = "商品ID")
    private Long goodsId;

    @Schema(description = "是否仅显示有库存（默认 true）")
    private Boolean onlyPositive;
}
