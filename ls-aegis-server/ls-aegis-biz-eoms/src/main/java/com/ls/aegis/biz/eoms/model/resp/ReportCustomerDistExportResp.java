package com.ls.aegis.biz.eoms.model.resp;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@ExcelIgnoreUnannotated
@Schema(description = "客户分布导出")
public class ReportCustomerDistExportResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "客户类型", order = 1)
    private String type;
    @ExcelProperty(value = "客户数", order = 2)
    private String count;
}
