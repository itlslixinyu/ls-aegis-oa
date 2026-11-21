package com.ls.aegis.biz.eoms.model.resp;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@ExcelIgnoreUnannotated
@Schema(description = "物料消耗 Top 导出")
public class ReportMaterialTopExportResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "物料ID", order = 1)
    private String materialId;
    @ExcelProperty(value = "物料编号", order = 2)
    private String materialNo;
    @ExcelProperty(value = "物料名称", order = 3)
    private String name;
    @ExcelProperty(value = "消耗数量", order = 4)
    private String usage;
}
