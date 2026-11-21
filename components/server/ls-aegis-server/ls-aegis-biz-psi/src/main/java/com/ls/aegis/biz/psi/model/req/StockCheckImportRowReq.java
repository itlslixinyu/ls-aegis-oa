package com.ls.aegis.biz.psi.model.req;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 盘点实盘明细 Excel 导入行
 */
@Data
public class StockCheckImportRowReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty("商品编号")
    private String goodsCode;

    @ExcelProperty("实盘数量")
    private String checkQty;

    @ExcelProperty("未税单价")
    private String untaxPrice;
}
