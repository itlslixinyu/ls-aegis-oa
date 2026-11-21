package com.ls.aegis.biz.psi.model.resp;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import com.ls.aegis.biz.psi.enums.BizSourceTypeEnum;
import com.ls.aegis.biz.psi.enums.StockFlowTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import top.continew.starter.excel.converter.ExcelBaseEnumConverter;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 库存流水导出
 */
@Data
@ExcelIgnoreUnannotated
@Schema(description = "库存流水导出")
public class StockFlowExportResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "仓库", order = 1)
    private String warehouseName;
    @ExcelProperty(value = "商品", order = 2)
    private String goodsName;
    @ExcelProperty(value = "流水类型", converter = ExcelBaseEnumConverter.class, order = 3)
    private StockFlowTypeEnum flowType;
    @ExcelProperty(value = "变动前数量", order = 4)
    private BigDecimal beforeQty;
    @ExcelProperty(value = "变动数量", order = 5)
    private BigDecimal changeQty;
    @ExcelProperty(value = "变动后数量", order = 6)
    private BigDecimal afterQty;
    @ExcelProperty(value = "变动前金额", order = 7)
    private BigDecimal beforeAmount;
    @ExcelProperty(value = "变动金额", order = 8)
    private BigDecimal changeAmount;
    @ExcelProperty(value = "变动后金额", order = 9)
    private BigDecimal afterAmount;
    @ExcelProperty(value = "来源类型", converter = ExcelBaseEnumConverter.class, order = 10)
    private BizSourceTypeEnum sourceType;
    @ExcelProperty(value = "来源单号", order = 11)
    private String sourceNo;
    @ExcelProperty(value = "确认时间", order = 12)
    private LocalDateTime confirmTime;
}
