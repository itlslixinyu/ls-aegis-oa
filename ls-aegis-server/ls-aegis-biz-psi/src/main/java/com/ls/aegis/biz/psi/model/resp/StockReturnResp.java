package com.ls.aegis.biz.psi.model.resp;

import com.ls.aegis.biz.psi.enums.DocStatusEnum;
import com.ls.aegis.biz.psi.enums.ReturnTypeEnum;
import com.ls.aegis.common.base.model.resp.BaseDetailResp;
import lombok.Data;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class StockReturnResp extends BaseDetailResp {
    @Serial
    private static final long serialVersionUID = 1L;
    private String docNo;
    private DocStatusEnum docStatus;
    private ReturnTypeEnum returnType;
    private LocalDateTime returnDate;
    private Long warehouseId;
    private String warehouseName;
    private Long unitId;
    private String unitName;
    private Long originStockOutId;
    private Long originStockInId;
    private BigDecimal taxInclusiveAmount;
    private BigDecimal untaxAmount;
    private BigDecimal taxAmount;
    private BigDecimal costAmount;
    private String remark;
    private List<StockReturnItemResp> items;

    @Data
    public static class StockReturnItemResp {
        private Long id;
        private Long originItemId;
        private Long goodsId;
        private BigDecimal quantity;
        private BigDecimal taxInclusivePrice;
        private BigDecimal taxRate;
        private BigDecimal untaxAmount;
        private BigDecimal taxAmount;
        private BigDecimal taxInclusiveAmount;
        private BigDecimal costAmount;
    }
}
