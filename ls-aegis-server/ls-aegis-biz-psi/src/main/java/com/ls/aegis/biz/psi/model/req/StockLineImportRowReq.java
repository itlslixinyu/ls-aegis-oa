package com.ls.aegis.biz.psi.model.req;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 出入库明细 Excel 导入行（草稿填表）
 */
@Data
public class StockLineImportRowReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty("商品编号")
    private String goodsCode;

    @ExcelProperty("数量")
    private String quantity;

    @ExcelProperty("含税单价")
    private String taxInclusivePrice;

    @ExcelProperty("税率")
    private String taxRate;
}
