package com.ls.aegis.biz.psi.model.resp;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import com.ls.aegis.biz.psi.enums.DocStatusEnum;
import com.ls.aegis.biz.psi.enums.PayStatusEnum;
import com.ls.aegis.biz.psi.enums.StockInTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import top.continew.starter.excel.converter.ExcelBaseEnumConverter;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@ExcelIgnoreUnannotated
@Schema(description = "入库单导出")
public class StockInExportResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "单号", order = 1)
    private String docNo;
    @ExcelProperty(value = "状态", converter = ExcelBaseEnumConverter.class, order = 2)
    private DocStatusEnum docStatus;
    @ExcelProperty(value = "入库类型", converter = ExcelBaseEnumConverter.class, order = 3)
    private StockInTypeEnum inType;
    @ExcelProperty(value = "入库日期", order = 4)
    private LocalDateTime inDate;
    @ExcelProperty(value = "仓库", order = 5)
    private String warehouseName;
    @ExcelProperty(value = "往来单位", order = 6)
    private String unitName;
    @ExcelProperty(value = "含税金额", order = 7)
    private BigDecimal taxInclusiveAmount;
    @ExcelProperty(value = "未税金额", order = 8)
    private BigDecimal untaxAmount;
    @ExcelProperty(value = "税额", order = 9)
    private BigDecimal taxAmount;
    @ExcelProperty(value = "付款状态", converter = ExcelBaseEnumConverter.class, order = 10)
    private PayStatusEnum payStatus;
    @ExcelProperty(value = "备注", order = 11)
    private String remark;
    @ExcelProperty(value = "确认时间", order = 12)
    private LocalDateTime confirmTime;
}
