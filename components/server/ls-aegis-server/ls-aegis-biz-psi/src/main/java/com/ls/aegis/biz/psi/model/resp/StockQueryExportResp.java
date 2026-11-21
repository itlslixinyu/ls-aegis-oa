package com.ls.aegis.biz.psi.model.resp;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 库存查询导出
 */
@Data
@ExcelIgnoreUnannotated
@Schema(description = "库存查询导出")
public class StockQueryExportResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "仓库", order = 1)
    private String warehouseName;

    @ExcelProperty(value = "商品编码", order = 2)
    private String goodsCode;

    @ExcelProperty(value = "商品名称", order = 3)
    private String goodsName;

    @ExcelProperty(value = "规格", order = 4)
    private String spec;

    @ExcelProperty(value = "单位", order = 5)
    private String unit;

    @ExcelProperty(value = "剩余数量", order = 6)
    private BigDecimal remainQuantity;

    @ExcelProperty(value = "剩余金额", order = 7)
    private BigDecimal remainAmount;
}
