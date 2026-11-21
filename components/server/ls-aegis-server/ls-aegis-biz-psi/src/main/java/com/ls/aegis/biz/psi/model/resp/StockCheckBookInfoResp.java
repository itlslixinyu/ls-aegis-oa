package com.ls.aegis.biz.psi.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 盘点录入：按仓库+商品带出账面数量与参考未税单价
 */
@Data
@Schema(description = "盘点账面带出")
public class StockCheckBookInfoResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "账面数量（当前仓实时库存）")
    private BigDecimal bookQty;

    @Schema(description = "参考未税单价（库存加权均价 → 批次单价 → 商品采购价）")
    private BigDecimal untaxPrice;
}
