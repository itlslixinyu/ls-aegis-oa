package com.ls.aegis.biz.psi.model.req;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 商品导入行（分类编码须为二级）
 */
@Data
public class GoodsImportRowReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty("商品编号")
    private String goodsCode;

    @ExcelProperty("商品名称")
    private String goodsName;

    @ExcelProperty("分类编码")
    private String categoryCode;

    @ExcelProperty("规格型号")
    private String spec;

    @ExcelProperty("计量单位")
    private String unit;

    @ExcelProperty("默认税率")
    private String defaultTaxRate;

    @ExcelProperty("条码")
    private String barcode;

    @ExcelProperty("采购参考价")
    private String purchasePrice;

    @ExcelProperty("销售参考价")
    private String salePrice;

    @ExcelProperty("库存下限")
    private String stockLower;

    @ExcelProperty("库存上限")
    private String stockUpper;

    @ExcelProperty("状态")
    private String status;

    @ExcelProperty("备注")
    private String remark;
}
