package com.ls.aegis.biz.psi.model.resp;

import com.ls.aegis.biz.psi.enums.DocStatusEnum;
import com.ls.aegis.biz.psi.enums.PayStatusEnum;
import com.ls.aegis.biz.psi.enums.StockInTypeEnum;
import com.ls.aegis.common.base.model.resp.BaseDetailResp;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "入库单响应")
public class StockInResp extends BaseDetailResp {

    @Serial
    private static final long serialVersionUID = 1L;

    private String docNo;
    private DocStatusEnum docStatus;
    private StockInTypeEnum inType;
    private LocalDateTime inDate;
    private Long warehouseId;
    @Schema(description = "仓库名称")
    private String warehouseName;
    private Long unitId;
    @Schema(description = "供应商名称")
    private String unitName;
    private Long contractId;
    private BigDecimal taxInclusiveAmount;
    private BigDecimal untaxAmount;
    private BigDecimal taxAmount;
    private BigDecimal taxDiffAmount;
    private PayStatusEnum payStatus;
    private String remark;
    private Long confirmUser;
    private LocalDateTime confirmTime;
    private List<StockLineResp> items;
}
