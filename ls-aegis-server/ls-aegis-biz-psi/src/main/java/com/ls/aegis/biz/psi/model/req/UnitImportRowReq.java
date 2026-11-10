package com.ls.aegis.biz.psi.model.req;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 往来单位导入行
 */
@Data
public class UnitImportRowReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty("单位编号")
    private String unitCode;

    @ExcelProperty("单位全称")
    private String unitName;

    @ExcelProperty("单位类型")
    private String unitType;

    @ExcelProperty("单位性质")
    private String unitNature;

    @ExcelProperty("信用代码")
    private String creditCode;

    @ExcelProperty("地址")
    private String address;

    @ExcelProperty("状态")
    private String status;

    @ExcelProperty("备注")
    private String remark;
}
