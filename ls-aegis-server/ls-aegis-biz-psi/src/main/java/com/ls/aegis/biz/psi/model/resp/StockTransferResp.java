package com.ls.aegis.biz.psi.model.resp;

import com.ls.aegis.biz.psi.enums.DocStatusEnum;
import com.ls.aegis.common.base.model.resp.BaseDetailResp;
import lombok.Data;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class StockTransferResp extends BaseDetailResp {
    @Serial
    private static final long serialVersionUID = 1L;
    private String docNo;
    private DocStatusEnum docStatus;
    private LocalDateTime transferDate;
    private Long fromWarehouseId;
    private String fromWarehouseName;
    private Long toWarehouseId;
    private String toWarehouseName;
    private String remark;
    private List<Item> items;

    @Data
    public static class Item {
        private Long id;
        private Long goodsId;
        private BigDecimal quantity;
        private BigDecimal costAmount;
    }
}
