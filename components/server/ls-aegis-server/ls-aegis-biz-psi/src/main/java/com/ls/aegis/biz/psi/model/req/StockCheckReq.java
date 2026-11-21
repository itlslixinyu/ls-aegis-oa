package com.ls.aegis.biz.psi.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "库存盘点请求")
public class StockCheckReq implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    @NotNull(message = "盘点日期不能为空")
    private LocalDateTime checkDate;
    @NotNull(message = "仓库不能为空")
    private Long warehouseId;
    @Length(max = 500)
    private String remark;
    @NotEmpty(message = "盘点明细不能为空")
    @Valid
    private List<Item> items;

    @Data
    public static class Item implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        @NotNull(message = "商品不能为空")
        private Long goodsId;
        @NotNull(message = "账面数量不能为空")
        private BigDecimal bookQty;
        @NotNull(message = "实盘数量不能为空")
        private BigDecimal checkQty;
        @Schema(description = "盘盈时必填未税单价")
        private BigDecimal untaxPrice;
    }
}
