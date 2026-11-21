package com.ls.aegis.biz.psi.model.query;

import com.ls.aegis.common.enums.DisEnableStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import top.continew.starter.data.annotation.Query;
import top.continew.starter.data.enums.QueryType;

import java.io.Serial;
import java.io.Serializable;

/**
 * 商品查询
 */
@Data
@Schema(description = "商品查询")
public class GoodsQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "关键词（编号/名称）")
    @Query(columns = {"goods_code", "goods_name"}, type = QueryType.LIKE)
    private String keyword;

    @Schema(description = "分类ID")
    @Query(type = QueryType.EQ)
    private Long categoryId;

    @Schema(description = "状态")
    @Query(type = QueryType.EQ)
    private DisEnableStatusEnum status;
}
