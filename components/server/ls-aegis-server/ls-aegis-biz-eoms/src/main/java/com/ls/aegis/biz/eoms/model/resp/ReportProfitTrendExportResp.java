package com.ls.aegis.biz.eoms.model.resp;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@ExcelIgnoreUnannotated
@Schema(description = "利润趋势导出")
public class ReportProfitTrendExportResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "月份", order = 1)
    private String month;
    @ExcelProperty(value = "收入(元)", order = 2)
    private String income;
    @ExcelProperty(value = "成本(元)", order = 3)
    private String cost;
    @ExcelProperty(value = "利润(元)", order = 4)
    private String profit;
}
