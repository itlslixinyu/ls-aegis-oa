package com.ls.aegis.biz.psi.model.req;

import com.ls.aegis.biz.psi.enums.StockOutTypeEnum;
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
 * 出库单保存请求
 */
@Data
@Schema(description = "出库单保存请求")
public class StockOutReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "出库类型")
    @NotNull(message = "出库类型不能为空")
    private StockOutTypeEnum outType;

    @Schema(description = "出库日期")
    @NotNull(message = "出库日期不能为空")
    private LocalDateTime outDate;

    @Schema(description = "仓库ID")
    @NotNull(message = "仓库不能为空")
    private Long warehouseId;

    @Schema(description = "客户ID（销售出库必填）")
    private Long unitId;

    @Schema(description = "销售合同ID（销售出库必填）")
    private Long contractId;

    @Schema(description = "手工表头含税金额（可选）")
    private BigDecimal taxInclusiveAmount;

    @Schema(description = "备注")
    @Length(max = 500, message = "备注长度不能超过 {max} 个字符")
    private String remark;

    @Schema(description = "明细")
    @NotEmpty(message = "明细不能为空")
    @Valid
    private List<StockLineReq> items;
}
