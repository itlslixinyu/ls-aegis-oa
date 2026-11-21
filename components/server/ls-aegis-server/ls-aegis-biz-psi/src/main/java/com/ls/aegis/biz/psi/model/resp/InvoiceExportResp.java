package com.ls.aegis.biz.psi.model.resp;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import com.ls.aegis.biz.psi.enums.DocStatusEnum;
import com.ls.aegis.biz.psi.enums.InvoiceTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import top.continew.starter.excel.converter.ExcelBaseEnumConverter;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@ExcelIgnoreUnannotated
@Schema(description = "发票导出")
public class InvoiceExportResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "单据号", order = 1)
    private String docNo;
    @ExcelProperty(value = "状态", converter = ExcelBaseEnumConverter.class, order = 2)
    private DocStatusEnum docStatus;
    @ExcelProperty(value = "发票号码", order = 3)
    private String invoiceNo;
    @ExcelProperty(value = "发票类型", converter = ExcelBaseEnumConverter.class, order = 4)
    private InvoiceTypeEnum invoiceType;
    @ExcelProperty(value = "开票日期", order = 5)
    private LocalDateTime invoiceDate;
    @ExcelProperty(value = "含税金额", order = 6)
    private BigDecimal taxInclusiveAmount;
    @ExcelProperty(value = "未税金额", order = 7)
    private BigDecimal untaxAmount;
    @ExcelProperty(value = "税额", order = 8)
    private BigDecimal taxAmount;
    @ExcelProperty(value = "备注", order = 9)
    private String remark;
}
