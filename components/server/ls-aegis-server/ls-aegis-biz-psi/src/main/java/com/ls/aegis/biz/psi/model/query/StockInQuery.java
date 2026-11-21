package com.ls.aegis.biz.psi.model.query;

import com.ls.aegis.biz.psi.enums.DocStatusEnum;
import com.ls.aegis.biz.psi.enums.StockInTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import top.continew.starter.data.annotation.Query;
import top.continew.starter.data.enums.QueryType;

import java.io.Serial;
import java.io.Serializable;

@Data
@Schema(description = "入库单查询")
public class StockInQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "单号")
    @Query(type = QueryType.LIKE)
    private String docNo;

    @Schema(description = "单据状态")
    @Query(type = QueryType.EQ)
    private DocStatusEnum docStatus;

    @Schema(description = "入库类型")
    @Query(type = QueryType.EQ)
    private StockInTypeEnum inType;

    @Schema(description = "仓库ID")
    @Query(type = QueryType.EQ)
    private Long warehouseId;
}
