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
@Schema(description = "合同管理导出")
public class EomsContractExportResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "主键", order = 1)
    private Long id;
    @ExcelProperty(value = "合同编号，唯一", order = 2)
    private String contractNo;
    @ExcelProperty(value = "客户ID", order = 3)
    private Long customerId;
    @ExcelProperty(value = "客户编号", order = 4)
    private String customerNo;
    @ExcelProperty(value = "客户名称", order = 5)
    private String customerName;
    @ExcelProperty(value = "合同名称", order = 6)
    private String contractName;
    @ExcelProperty(value = "合同类型", order = 7)
    private String contractType;
    @ExcelProperty(value = "合同金额", order = 8)
    private BigDecimal amount;
    @ExcelProperty(value = "开始日期", order = 9)
    private Date startDate;
    @ExcelProperty(value = "结束日期", order = 10)
    private Date endDate;
    @ExcelProperty(value = "状态：EFFECTIVE-生效中、EXPIRED-已到期、TERMINATED-已终止", order = 11)
    private String status;
}
