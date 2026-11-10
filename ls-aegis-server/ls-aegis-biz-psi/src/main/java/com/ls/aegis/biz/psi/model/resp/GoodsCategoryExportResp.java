package com.ls.aegis.biz.psi.model.resp;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import com.ls.aegis.common.enums.DisEnableStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import top.continew.starter.excel.converter.ExcelBaseEnumConverter;

import java.io.Serial;
import java.io.Serializable;

@Data
@ExcelIgnoreUnannotated
@Schema(description = "商品分类导出")
public class GoodsCategoryExportResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "分类编码", order = 1)
    private String code;
    @ExcelProperty(value = "分类名称", order = 2)
    private String name;
    @ExcelProperty(value = "上级分类编码", order = 3)
    private String parentCode;
    @ExcelProperty(value = "级别", order = 4)
    private String levelName;
    @ExcelProperty(value = "排序", order = 5)
    private Integer sort;
    @ExcelProperty(value = "状态", converter = ExcelBaseEnumConverter.class, order = 6)
    private DisEnableStatusEnum status;
}
