package com.ls.aegis.biz.eoms.model.resp;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@ExcelIgnoreUnannotated
@Schema(description = "Dashboard KPI 导出行")
public class ReportKpiExportResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "指标", order = 1)
    private String label;
    @ExcelProperty(value = "数值", order = 2)
    private String value;
}
