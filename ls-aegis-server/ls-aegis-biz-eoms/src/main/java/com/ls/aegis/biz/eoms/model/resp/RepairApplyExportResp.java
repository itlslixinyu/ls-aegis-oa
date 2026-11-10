package com.ls.aegis.biz.eoms.model.resp;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@ExcelIgnoreUnannotated
@Schema(description = "报修申请导出")
public class RepairApplyExportResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "主键", order = 1)
    private Long id;
    @ExcelProperty(value = "报修编号，唯一", order = 2)
    private String repairNo;
    @ExcelProperty(value = "客户ID", order = 3)
    private Long customerId;
    @ExcelProperty(value = "客户编号", order = 4)
    private String customerNo;
    @ExcelProperty(value = "客户名称", order = 5)
    private String customerName;
    @ExcelProperty(value = "联系人姓名", order = 6)
    private String contactName;
    @ExcelProperty(value = "联系电话", order = 7)
    private String contactPhone;
    @ExcelProperty(value = "故障类型", order = 8)
    private String faultType;
    @ExcelProperty(value = "故障描述", order = 9)
    private String faultDesc;
    @ExcelProperty(value = "现场照片URL（多个逗号分隔，带水印）", order = 10)
    private String photos;
    @ExcelProperty(value = "紧急程度", order = 11)
    private String urgentLevel;
    @ExcelProperty(value = "关联合同ID", order = 12)
    private Long contractId;
    @ExcelProperty(value = "合同编号", order = 13)
    private String contractNo;
    @ExcelProperty(value = "状态：WAIT-待处理、CONFIRM-已确认、ORDER-已转工单、CANCEL-已取消", order = 14)
    private String status;
    @ExcelProperty(value = "报修时间", order = 15)
    private Date applyTime;
    @ExcelProperty(value = "报修来源：H5-客户H5、ADMIN-后台登记", order = 16)
    private String source;
}
