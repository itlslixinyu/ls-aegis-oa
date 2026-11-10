package com.ls.aegis.biz.psi.model.resp;

import com.ls.aegis.common.base.model.resp.BaseDetailResp;
import com.ls.aegis.common.enums.DisEnableStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import top.continew.starter.extension.crud.annotation.TreeField;

import java.io.Serial;

/**
 * 商品分类响应
 */
@Data
@TreeField(value = "id", nameKey = "name")
@Schema(description = "商品分类响应")
public class GoodsCategoryResp extends BaseDetailResp {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "上级分类 ID")
    private Long parentId;

    @Schema(description = "分类编码")
    private String code;

    @Schema(description = "分类名称")
    private String name;

    @Schema(description = "级别：1=一级，2=二级", example = "1")
    private Integer level;

    @Schema(description = "级别名称", example = "一级")
    private String levelName;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "状态")
    private DisEnableStatusEnum status;

    @Schema(description = "下级分类")
    private java.util.List<GoodsCategoryResp> children;
}
