package com.ls.aegis.biz.psi.model.resp;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import com.ls.aegis.biz.psi.enums.DocStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import top.continew.starter.excel.converter.ExcelBaseEnumConverter;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@ExcelIgnoreUnannotated
@Schema(description = "调拨单导出")
public class StockTransferExportResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "单号", order = 1)
    private String docNo;
    @ExcelProperty(value = "状态", converter = ExcelBaseEnumConverter.class, order = 2)
    private DocStatusEnum docStatus;
    @ExcelProperty(value = "调拨日期", order = 3)
    private LocalDateTime transferDate;
    @ExcelProperty(value = "调出仓", order = 4)
    private String fromWarehouseName;
    @ExcelProperty(value = "调入仓", order = 5)
    private String toWarehouseName;
    @ExcelProperty(value = "备注", order = 6)
    private String remark;
}
