package com.ls.aegis.biz.psi.model.resp;

import com.ls.aegis.biz.psi.enums.DocStatusEnum;
import com.ls.aegis.biz.psi.enums.InitTypeEnum;
import com.ls.aegis.common.base.model.resp.BaseDetailResp;
import lombok.Data;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class InitDocResp extends BaseDetailResp {
    @Serial
    private static final long serialVersionUID = 1L;
    private InitTypeEnum type;
    private String docNo;
    private DocStatusEnum docStatus;
    private Long warehouseId;
    private LocalDateTime initDate;
    private String remark;
    private List<Item> items;

    @Data
    public static class Item {
        private Long id;
        private Long goodsId;
        private BigDecimal qty;
        private BigDecimal untaxPrice;
        private Long unitId;
        private BigDecimal taxInclusiveAmount;
    }
}
