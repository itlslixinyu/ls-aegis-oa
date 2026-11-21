package com.ls.aegis.biz.eoms.model.resp;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.math.BigDecimal;

@Data
@ExcelIgnoreUnannotated
@Schema(description = "利润统计管理导出")
public class IncomeExportResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "利润单号", order = 1)
    private String incomeNo;
    @ExcelProperty(value = "工单编号", order = 2)
    private String orderNo;
    @ExcelProperty(value = "客户编号", order = 3)
    private String customerNo;
    @ExcelProperty(value = "客户名称", order = 4)
    private String customerName;
    @ExcelProperty(value = "利润金额", order = 5)
    private BigDecimal amount;
    @ExcelProperty(value = "利润类型", order = 6)
    private String incomeType;
    @ExcelProperty(value = "发生日期", order = 7)
    private Date incomeDate;
}
