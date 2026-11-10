package com.ls.aegis.biz.psi.model.query;

import com.ls.aegis.biz.psi.enums.UnitTypeEnum;
import com.ls.aegis.common.enums.DisEnableStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import top.continew.starter.data.annotation.Query;
import top.continew.starter.data.enums.QueryType;

import java.io.Serial;
import java.io.Serializable;

/**
 * 往来单位查询
 */
@Data
@Schema(description = "往来单位查询")
public class UnitQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "关键词（编号/名称）")
    @Query(columns = {"unit_code", "unit_name"}, type = QueryType.LIKE)
    private String keyword;

    @Schema(description = "单位类型")
    @Query(type = QueryType.EQ)
    private UnitTypeEnum unitType;

    @Schema(description = "状态")
    @Query(type = QueryType.EQ)
    private DisEnableStatusEnum status;
}
