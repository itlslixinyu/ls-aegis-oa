package com.ls.aegis.biz.psi.model.resp;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import com.ls.aegis.biz.psi.enums.DocStatusEnum;
import com.ls.aegis.biz.psi.enums.InvoiceStatusEnum;
import com.ls.aegis.biz.psi.enums.StockOutTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import top.continew.starter.excel.converter.ExcelBaseEnumConverter;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@ExcelIgnoreUnannotated
@Schema(description = "出库单导出")
public class StockOutExportResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "单号", order = 1)
    private String docNo;
    @ExcelProperty(value = "状态", converter = ExcelBaseEnumConverter.class, order = 2)
    private DocStatusEnum docStatus;
    @ExcelProperty(value = "出库类型", converter = ExcelBaseEnumConverter.class, order = 3)
    private StockOutTypeEnum outType;
    @ExcelProperty(value = "出库日期", order = 4)
    private LocalDateTime outDate;
    @ExcelProperty(value = "含税金额", order = 5)
    private BigDecimal taxInclusiveAmount;
    @ExcelProperty(value = "未税金额", order = 6)
    private BigDecimal untaxAmount;
    @ExcelProperty(value = "税额", order = 7)
    private BigDecimal taxAmount;
    @ExcelProperty(value = "成本金额", order = 8)
    private BigDecimal costAmount;
    @ExcelProperty(value = "开票状态", converter = ExcelBaseEnumConverter.class, order = 9)
    private InvoiceStatusEnum invoiceStatus;
    @ExcelProperty(value = "备注", order = 10)
    private String remark;
    @ExcelProperty(value = "确认时间", order = 11)
    private LocalDateTime confirmTime;
}
