package com.ls.aegis.biz.psi.model.query;

import com.ls.aegis.biz.psi.enums.DocStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import top.continew.starter.data.annotation.Query;
import top.continew.starter.data.enums.QueryType;

import java.io.Serial;
import java.io.Serializable;

@Data
@Schema(description = "库存期初查询")
public class InitStockQuery implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    @Query(type = QueryType.LIKE)
    private String docNo;
    @Query(type = QueryType.EQ)
    private DocStatusEnum docStatus;
    @Query(type = QueryType.EQ)
    private Long warehouseId;
}
