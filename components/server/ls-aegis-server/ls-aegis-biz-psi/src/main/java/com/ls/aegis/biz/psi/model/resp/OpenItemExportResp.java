package com.ls.aegis.biz.psi.model.resp;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import com.ls.aegis.biz.psi.enums.BizSourceTypeEnum;
import com.ls.aegis.biz.psi.enums.OpenItemStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import top.continew.starter.excel.converter.ExcelBaseEnumConverter;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@ExcelIgnoreUnannotated
@Schema(description = "应收应付开口导出")
public class OpenItemExportResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "往来单位", order = 1)
    private String unitName;
    @ExcelProperty(value = "来源类型", converter = ExcelBaseEnumConverter.class, order = 2)
    private BizSourceTypeEnum sourceType;
    @ExcelProperty(value = "来源单号", order = 3)
    private String sourceNo;
    @ExcelProperty(value = "含税金额", order = 4)
    private BigDecimal taxInclusiveAmount;
    @ExcelProperty(value = "已核销", order = 5)
    private BigDecimal writtenOffAmount;
    @ExcelProperty(value = "未核销", order = 6)
    private BigDecimal unwrittenAmount;
    @ExcelProperty(value = "状态", converter = ExcelBaseEnumConverter.class, order = 7)
    private OpenItemStatusEnum docStatus;
}
