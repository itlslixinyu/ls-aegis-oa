package com.ls.aegis.biz.psi.model.query;

import com.ls.aegis.common.enums.DisEnableStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import top.continew.starter.data.annotation.Query;
import top.continew.starter.data.enums.QueryType;

import java.io.Serial;
import java.io.Serializable;

/**
 * 商品分类查询
 */
@Data
@Schema(description = "商品分类查询")
public class GoodsCategoryQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "名称")
    @Query(type = QueryType.LIKE)
    private String name;

    @Schema(description = "状态")
    @Query(type = QueryType.EQ)
    private DisEnableStatusEnum status;
}
