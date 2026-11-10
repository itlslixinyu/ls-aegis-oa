package com.ls.aegis.biz.psi.model.req;

import com.ls.aegis.biz.psi.enums.StockInTypeEnum;
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

/**
 * 入库单保存请求
 */
@Data
@Schema(description = "入库单保存请求")
public class StockInReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "入库类型")
    @NotNull(message = "入库类型不能为空")
    private StockInTypeEnum inType;

    @Schema(description = "入库日期")
    @NotNull(message = "入库日期不能为空")
    private LocalDateTime inDate;

    @Schema(description = "仓库ID")
    @NotNull(message = "仓库不能为空")
    private Long warehouseId;

    @Schema(description = "供应商ID（采购入库必填）")
    private Long unitId;

    @Schema(description = "采购合同ID")
    private Long contractId;

    @Schema(description = "手工表头含税金额（可选，用于尾差）")
    private BigDecimal taxInclusiveAmount;

    @Schema(description = "备注")
    @Length(max = 500, message = "备注长度不能超过 {max} 个字符")
    private String remark;

    @Schema(description = "明细")
    @NotEmpty(message = "明细不能为空")
    @Valid
    private List<StockLineReq> items;
}
