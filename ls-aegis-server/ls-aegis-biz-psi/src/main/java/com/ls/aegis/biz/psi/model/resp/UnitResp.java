package com.ls.aegis.biz.psi.model.resp;

import com.ls.aegis.biz.psi.enums.UnitNatureEnum;
import com.ls.aegis.biz.psi.enums.UnitTypeEnum;
import com.ls.aegis.common.base.model.resp.BaseDetailResp;
import com.ls.aegis.common.enums.DisEnableStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;

/**
 * 往来单位响应
 */
@Data
@Schema(description = "往来单位响应")
public class UnitResp extends BaseDetailResp {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "单位编号")
    private String unitCode;

    @Schema(description = "单位全称")
    private String unitName;

    @Schema(description = "单位类型")
    private UnitTypeEnum unitType;

    @Schema(description = "单位性质")
    private UnitNatureEnum unitNature;

    @Schema(description = "统一社会信用代码")
    private String creditCode;

    @Schema(description = "地址")
    private String address;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "等级编码")
    private String levelCode;

    @Schema(description = "状态")
    private DisEnableStatusEnum status;

    @Schema(description = "备注")
    private String remark;
}
