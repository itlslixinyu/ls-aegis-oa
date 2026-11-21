package com.ls.aegis.biz.psi.model.resp;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import com.ls.aegis.biz.psi.enums.UnitNatureEnum;
import com.ls.aegis.biz.psi.enums.UnitTypeEnum;
import com.ls.aegis.common.base.model.resp.BaseDetailResp;
import com.ls.aegis.common.enums.DisEnableStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import top.continew.starter.excel.converter.ExcelBaseEnumConverter;

import java.io.Serial;

/**
 * 往来单位详情响应
 */
@Data
@ExcelIgnoreUnannotated
@Schema(description = "往来单位详情响应")
public class UnitDetailResp extends BaseDetailResp {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "单位编号")
    @ExcelProperty(value = "单位编号", order = 2)
    private String unitCode;

    @Schema(description = "单位全称")
    @ExcelProperty(value = "单位全称", order = 3)
    private String unitName;

    @Schema(description = "单位类型")
    @ExcelProperty(value = "单位类型", converter = ExcelBaseEnumConverter.class, order = 4)
    private UnitTypeEnum unitType;

    @Schema(description = "单位性质")
    @ExcelProperty(value = "单位性质", converter = ExcelBaseEnumConverter.class, order = 5)
    private UnitNatureEnum unitNature;

    @Schema(description = "统一社会信用代码")
    @ExcelProperty(value = "信用代码", order = 6)
    private String creditCode;

    @Schema(description = "地址")
    @ExcelProperty(value = "地址", order = 7)
    private String address;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "等级编码")
    private String levelCode;

    @Schema(description = "状态")
    @ExcelProperty(value = "状态", converter = ExcelBaseEnumConverter.class, order = 8)
    private DisEnableStatusEnum status;

    @Schema(description = "备注")
    @ExcelProperty(value = "备注", order = 9)
    private String remark;
}
