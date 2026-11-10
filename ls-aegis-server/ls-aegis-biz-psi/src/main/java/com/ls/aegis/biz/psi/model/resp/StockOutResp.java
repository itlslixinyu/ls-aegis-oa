package com.ls.aegis.biz.psi.model.resp;

import com.ls.aegis.biz.psi.enums.DocStatusEnum;
import com.ls.aegis.biz.psi.enums.InvoiceStatusEnum;
import com.ls.aegis.biz.psi.enums.StockOutTypeEnum;
import com.ls.aegis.common.base.model.resp.BaseDetailResp;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "出库单响应")
public class StockOutResp extends BaseDetailResp {

    @Serial
    private static final long serialVersionUID = 1L;

    private String docNo;
    private DocStatusEnum docStatus;
    private StockOutTypeEnum outType;
    private LocalDateTime outDate;
    private Long warehouseId;
    private String warehouseName;
    private Long unitId;
    private String unitName;
    private Long contractId;
    @Schema(description = "销售合同名称")
    private String contractName;
    private BigDecimal taxInclusiveAmount;
    private BigDecimal untaxAmount;
    private BigDecimal taxAmount;
    private BigDecimal taxDiffAmount;
    private BigDecimal costAmount;
    private InvoiceStatusEnum invoiceStatus;
    private String remark;
    private Long confirmUser;
    private LocalDateTime confirmTime;
    private List<StockLineResp> items;
}
