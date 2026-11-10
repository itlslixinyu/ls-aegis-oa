package com.ls.aegis.biz.psi.model.query;

import com.ls.aegis.biz.psi.enums.BizSourceTypeEnum;
import com.ls.aegis.biz.psi.enums.StockFlowTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import top.continew.starter.data.annotation.Query;
import top.continew.starter.data.enums.QueryType;

import java.io.Serial;
import java.io.Serializable;

@Data
@Schema(description = "库存流水查询")
public class StockFlowQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Query(type = QueryType.EQ)
    private Long warehouseId;

    @Query(type = QueryType.EQ)
    private Long goodsId;

    @Query(type = QueryType.EQ)
    private StockFlowTypeEnum flowType;

    @Query(type = QueryType.EQ)
    private BizSourceTypeEnum sourceType;

    @Query(type = QueryType.EQ)
    private String sourceNo;
}
