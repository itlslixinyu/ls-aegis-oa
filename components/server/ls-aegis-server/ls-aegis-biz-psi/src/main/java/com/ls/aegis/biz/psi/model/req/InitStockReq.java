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
@Schema(description = "库存期初请求")
public class InitStockReq implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    @NotNull(message = "期初日期不能为空")
    private LocalDateTime initDate;
    @NotNull(message = "仓库不能为空")
    private Long warehouseId;
    @Length(max = 500)
    private String remark;
    @NotEmpty
    @Valid
    private List<Item> items;

    @Data
    public static class Item implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        @NotNull
        private Long goodsId;
        @NotNull
        private BigDecimal quantity;
        @NotNull
        private BigDecimal untaxPrice;
    }
}
