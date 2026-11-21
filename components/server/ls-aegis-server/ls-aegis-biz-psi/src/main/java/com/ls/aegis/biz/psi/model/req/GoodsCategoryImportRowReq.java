package com.ls.aegis.biz.psi.model.req;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 商品分类导入行（编码自动生成；上级编码空=一级）
 */
@Data
public class GoodsCategoryImportRowReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty("上级分类编码")
    private String parentCode;

    @ExcelProperty("分类名称")
    private String name;

    @ExcelProperty("排序")
    private String sort;

    @ExcelProperty("状态")
    private String status;
}
