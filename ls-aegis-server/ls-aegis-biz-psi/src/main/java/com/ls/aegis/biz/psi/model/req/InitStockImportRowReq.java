package com.ls.aegis.biz.psi.model.req;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 库存期初 Excel 导入行
 */
@Data
public class InitStockImportRowReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty("仓库编码")
    private String warehouseCode;

    @ExcelProperty("商品编号")
    private String goodsCode;

    @ExcelProperty("数量")
    private String quantity;

    @ExcelProperty("未税单价")
    private String untaxPrice;

    @ExcelProperty("期初日期")
    private String initDate;

    @ExcelProperty("备注")
    private String remark;
}
