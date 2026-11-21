package com.ls.aegis.biz.eoms.model.resp;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@ExcelIgnoreUnannotated
@Schema(description = "应收账款管理导出")
public class ReceivableExportResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "主键", order = 1)
    private Long id;
    @ExcelProperty(value = "客户ID", order = 2)
    private Long customerId;
    @ExcelProperty(value = "客户编号", order = 3)
    private String customerNo;
    @ExcelProperty(value = "客户名称", order = 4)
    private String customerName;
    @ExcelProperty(value = "关联维修工单ID", order = 5)
    private Long repairOrderId;
    @ExcelProperty(value = "应收单号", order = 6)
    private String receivableNo;
    @ExcelProperty(value = "工单编号", order = 7)
    private String orderNo;
    @ExcelProperty(value = "应收金额", order = 8)
    private BigDecimal amount;
    @ExcelProperty(value = "已收金额", order = 9)
    private BigDecimal receivedAmount;
    @ExcelProperty(value = "欠款金额 = 应收 - 已收", order = 10)
    private BigDecimal arrearsAmount;
    @ExcelProperty(value = "状态：UNPAID-未结清、PARTIAL-部分收款、PAID-已结清、VOID-已作废", order = 11)
    private String status;
}
