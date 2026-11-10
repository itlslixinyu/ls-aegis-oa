package com.ls.aegis.biz.psi.model.resp;

import java.math.BigDecimal;
import lombok.Data;

/** 工作台内部聚合投影，不加载明细实体。 */
@Data
public class WorkbenchAggregate {
    private long count;
    private BigDecimal amount = BigDecimal.ZERO;
    private long monthCount;
    private BigDecimal monthAmount = BigDecimal.ZERO;
    private long spanCount;
    private BigDecimal spanAmount = BigDecimal.ZERO;
    private BigDecimal monthQty = BigDecimal.ZERO;
    private BigDecimal spanQty = BigDecimal.ZERO;
}
