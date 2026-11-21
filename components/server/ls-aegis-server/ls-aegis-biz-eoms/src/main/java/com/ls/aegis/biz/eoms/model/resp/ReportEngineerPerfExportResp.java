package com.ls.aegis.biz.eoms.model.resp;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@ExcelIgnoreUnannotated
@Schema(description = "工程师绩效导出")
public class ReportEngineerPerfExportResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "工程师ID", order = 1)
    private String engineerId;
    @ExcelProperty(value = "工程师姓名", order = 2)
    private String engineerName;
    @ExcelProperty(value = "工单总数", order = 3)
    private String orderCount;
    @ExcelProperty(value = "已完成数", order = 4)
    private String completedCount;
}
