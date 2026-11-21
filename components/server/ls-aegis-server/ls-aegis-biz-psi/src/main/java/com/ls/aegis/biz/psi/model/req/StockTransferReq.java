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
@Schema(description = "调拨单请求")
public class StockTransferReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "调拨日期不能为空")
    private LocalDateTime transferDate;

    @NotNull(message = "调出仓不能为空")
    private Long fromWarehouseId;

    @NotNull(message = "调入仓不能为空")
    private Long toWarehouseId;

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
    }
}
