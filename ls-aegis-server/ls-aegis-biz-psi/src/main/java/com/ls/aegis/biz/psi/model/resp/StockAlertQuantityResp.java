package com.ls.aegis.biz.psi.model.resp;

import lombok.Data;
import java.math.BigDecimal;

/** Current positive stock summed for one watched good across authorized warehouses. */
@Data
public class StockAlertQuantityResp {
    private Long goodsId;
    private BigDecimal remainQuantity;
}
