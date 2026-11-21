package com.ls.aegis.biz.psi.model.req;

import com.ls.aegis.biz.psi.enums.AdjustTypeEnum;
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
@Schema(description = "库存调整请求")
public class StockAdjustReq implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    @NotNull(message = "调整日期不能为空")
    private LocalDateTime adjustDate;
    @NotNull(message = "仓库不能为空")
    private Long warehouseId;
    @Length(max = 500)
    private String remark;
    @NotEmpty(message = "调整明细不能为空")
    @Valid
    private List<Item> items;

    @Data
    public static class Item implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        @NotNull(message = "商品不能为空")
        private Long goodsId;
        @Schema(description = "可选：按原价恢复原批次缺口，仅盘盈支持；保留本调整单审计")
        private Long originBatchId;
        @NotNull(message = "调整类型不能为空")
        private AdjustTypeEnum adjustType;
        @NotNull(message = "数量不能为空")
        private BigDecimal quantity;
        @Schema(description = "盘盈时必填未税单价")
        private BigDecimal untaxPrice;
    }
}
