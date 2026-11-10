package com.ls.aegis.biz.psi.model.query;

import com.ls.aegis.common.enums.DisEnableStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import top.continew.starter.data.annotation.Query;
import top.continew.starter.data.enums.QueryType;

import java.io.Serial;
import java.io.Serializable;

/**
 * 仓库查询
 */
@Data
@Schema(description = "仓库查询")
public class WarehouseQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "关键词（编码/名称）")
    @Query(columns = {"warehouse_code", "warehouse_name"}, type = QueryType.LIKE)
    private String keyword;

    @Schema(description = "状态")
    @Query(type = QueryType.EQ)
    private DisEnableStatusEnum status;
}
