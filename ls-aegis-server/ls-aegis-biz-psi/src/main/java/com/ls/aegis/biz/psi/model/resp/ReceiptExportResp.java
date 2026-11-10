package com.ls.aegis.biz.psi.model.resp;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import com.ls.aegis.biz.psi.enums.DocStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import top.continew.starter.excel.converter.ExcelBaseEnumConverter;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@ExcelIgnoreUnannotated
@Schema(description = "收款单导出")
public class ReceiptExportResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "单号", order = 1)
    private String docNo;
    @ExcelProperty(value = "状态", converter = ExcelBaseEnumConverter.class, order = 2)
    private DocStatusEnum docStatus;
    @ExcelProperty(value = "收款日期", order = 3)
    private LocalDateTime receiptDate;
    @ExcelProperty(value = "往来单位", order = 4)
    private String unitName;
    @ExcelProperty(value = "账户", order = 5)
    private String accountName;
    @ExcelProperty(value = "收款方式", order = 6)
    private String payMethod;
    @ExcelProperty(value = "金额", order = 7)
    private BigDecimal amount;
    @ExcelProperty(value = "未核销金额", order = 8)
    private BigDecimal unallocatedAmount;
    @ExcelProperty(value = "备注", order = 9)
    private String remark;
}
