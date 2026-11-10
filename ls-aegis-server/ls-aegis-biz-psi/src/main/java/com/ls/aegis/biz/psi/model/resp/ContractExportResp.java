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
@Schema(description = "合同导出")
public class ContractExportResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "合同号", order = 1)
    private String contractNo;
    @ExcelProperty(value = "合同名称", order = 2)
    private String name;
    @ExcelProperty(value = "状态", converter = ExcelBaseEnumConverter.class, order = 3)
    private DocStatusEnum docStatus;
    @ExcelProperty(value = "签订日期", order = 4)
    private LocalDateTime signDate;
    @ExcelProperty(value = "含税金额", order = 5)
    private BigDecimal taxInclusiveAmount;
    @ExcelProperty(value = "开始日期", order = 6)
    private LocalDateTime startDate;
    @ExcelProperty(value = "结束日期", order = 7)
    private LocalDateTime endDate;
    @ExcelProperty(value = "备注", order = 8)
    private String remark;
}
