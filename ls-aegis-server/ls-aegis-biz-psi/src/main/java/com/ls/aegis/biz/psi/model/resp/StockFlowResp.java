package com.ls.aegis.biz.psi.model.resp;

import com.ls.aegis.biz.psi.enums.BizSourceTypeEnum;
import com.ls.aegis.biz.psi.enums.StockFlowTypeEnum;
import com.ls.aegis.common.base.model.resp.BaseDetailResp;
import lombok.Data;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class StockFlowResp extends BaseDetailResp {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long warehouseId;
    private String warehouseName;
    private Long goodsId;
    private String goodsName;
    private StockFlowTypeEnum flowType;
    private BigDecimal beforeQty;
    private BigDecimal changeQty;
    private BigDecimal afterQty;
    private BigDecimal beforeAmount;
    private BigDecimal changeAmount;
    private BigDecimal afterAmount;
    private BizSourceTypeEnum sourceType;
    private Long sourceId;
    private String sourceNo;
    private Long confirmUser;
    private LocalDateTime confirmTime;
}
