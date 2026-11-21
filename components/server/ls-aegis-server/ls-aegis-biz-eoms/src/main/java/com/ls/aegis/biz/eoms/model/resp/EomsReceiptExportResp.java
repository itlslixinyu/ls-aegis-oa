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
@Schema(description = "维修收款导出")
public class EomsReceiptExportResp implements Serializable {

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
    @ExcelProperty(value = "关联应收单ID", order = 5)
    private Long receivableId;
    @ExcelProperty(value = "收款单号", order = 6)
    private String paymentNo;
    @ExcelProperty(value = "应收单号", order = 7)
    private String receivableNo;
    @ExcelProperty(value = "收款金额", order = 8)
    private BigDecimal amount;
    @ExcelProperty(value = "收款方式：现金、转账、微信、支付宝", order = 9)
    private String paymentMethod;
    @ExcelProperty(value = "收款时间", order = 10)
    private Date paymentTime;
}
