package com.ls.aegis.biz.quote.model.resp;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@ExcelIgnoreUnannotated
@Schema(description = "报价导出")
public class QuoteExportResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "主键", order = 1)
    private Long id;
    @ExcelProperty(value = "报价编号，唯一", order = 2)
    private String quoteNo;
    @ExcelProperty(value = "客户ID", order = 3)
    private Long customerId;
    @ExcelProperty(value = "客户编号", order = 4)
    private String customerNo;
    @ExcelProperty(value = "客户名称", order = 5)
    private String customerName;
    @ExcelProperty(value = "关联维修工单ID", order = 6)
    private Long repairOrderId;
    @ExcelProperty(value = "工单编号", order = 7)
    private String orderNo;
    @ExcelProperty(value = "报价总金额", order = 8)
    private BigDecimal totalAmount;
    @ExcelProperty(value = "状态：DRAFT-草稿、SUBMIT-已提交、CONFIRM-已确认、CANCEL-已取消", order = 9)
    private String status;
}
