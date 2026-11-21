package com.ls.aegis.biz.psi.model.resp;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import com.ls.aegis.common.base.model.resp.BaseDetailResp;
import com.ls.aegis.common.enums.DisEnableStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import top.continew.starter.excel.converter.ExcelBaseEnumConverter;

import java.io.Serial;
import java.math.BigDecimal;

@Data
@ExcelIgnoreUnannotated
@Schema(description = "商品详情响应")
public class GoodsDetailResp extends BaseDetailResp {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "商品编号", order = 2)
    private String goodsCode;
    @ExcelProperty(value = "商品名称", order = 3)
    private String goodsName;
    @ExcelProperty(value = "条码", order = 4)
    private String barcode;
    @ExcelProperty(value = "规格型号", order = 5)
    private String spec;
    @ExcelProperty(value = "计量单位", order = 6)
    private String unit;
    @ExcelProperty(value = "默认税率", order = 7)
    private BigDecimal defaultTaxRate;
    private Long categoryId;
    @ExcelProperty(value = "分类名称", order = 8)
    private String categoryName;
    private Long brandId;
    @ExcelProperty(value = "采购参考价", order = 9)
    private BigDecimal purchasePrice;
    @ExcelProperty(value = "销售参考价", order = 10)
    private BigDecimal salePrice;
    private BigDecimal minSalePrice;
    @ExcelProperty(value = "库存上限", order = 11)
    private BigDecimal stockUpper;
    @ExcelProperty(value = "库存下限", order = 12)
    private BigDecimal stockLower;
    @ExcelProperty(value = "状态", converter = ExcelBaseEnumConverter.class, order = 13)
    private DisEnableStatusEnum status;
    @ExcelProperty(value = "备注", order = 14)
    private String remark;
}
