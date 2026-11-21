package com.ls.aegis.biz.psi.model.req;

import com.ls.aegis.biz.psi.enums.ReturnTypeEnum;
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
@Schema(description = "退货单请求")
public class StockReturnReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "退货类型不能为空")
    private ReturnTypeEnum returnType;

    @NotNull(message = "退货日期不能为空")
    private LocalDateTime returnDate;

    @NotNull(message = "仓库不能为空")
    private Long warehouseId;

    private Long unitId;
    private Long originStockOutId;
    private Long originStockInId;

    @Length(max = 500)
    private String remark;

    @NotEmpty(message = "明细不能为空")
    @Valid
    private List<StockReturnItemReq> items;

    @Data
    public static class StockReturnItemReq implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        @NotNull(message = "原明细ID不能为空")
        private Long originItemId;
        @NotNull(message = "商品不能为空")
        private Long goodsId;
        @NotNull(message = "数量不能为空")
        private BigDecimal quantity;
        @NotNull(message = "含税单价不能为空")
        private BigDecimal taxInclusivePrice;
        @NotNull(message = "税率不能为空")
        private BigDecimal taxRate;
    }
}
