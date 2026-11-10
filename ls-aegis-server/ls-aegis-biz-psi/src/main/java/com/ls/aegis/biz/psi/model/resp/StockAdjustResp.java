package com.ls.aegis.biz.psi.model.resp;

import com.ls.aegis.biz.psi.enums.AdjustTypeEnum;
import com.ls.aegis.biz.psi.enums.DocStatusEnum;
import com.ls.aegis.common.base.model.resp.BaseDetailResp;
import lombok.Data;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class StockAdjustResp extends BaseDetailResp {
    @Serial
    private static final long serialVersionUID = 1L;
    private String docNo;
    private DocStatusEnum docStatus;
    private LocalDateTime adjustDate;
    private Long warehouseId;
    private String warehouseName;
    private String remark;
    private List<Item> items;

    @Data
    public static class Item {
        private Long id;
        private Long goodsId;
        private Long originBatchId;
        private AdjustTypeEnum adjustType;
        private BigDecimal quantity;
        private BigDecimal untaxPrice;
        private BigDecimal costAmount;
    }
}
