package com.ls.aegis.biz.eoms.model.resp;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@ExcelIgnoreUnannotated
@Schema(description = "维修记录管理导出")
public class RepairRecordExportResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "主键", order = 1)
    private Long id;
    @ExcelProperty(value = "维修工单ID", order = 2)
    private Long repairOrderId;
    @ExcelProperty(value = "工单编号", order = 3)
    private String orderNo;
    @ExcelProperty(value = "工程师姓名", order = 4)
    private String engineerName;
    @ExcelProperty(value = "故障原因", order = 5)
    private String faultReason;
    @ExcelProperty(value = "检测过程", order = 6)
    private String checkProcess;
    @ExcelProperty(value = "解决方案", order = 7)
    private String solution;
    @ExcelProperty(value = "维修结果", order = 8)
    private String result;
    @ExcelProperty(value = "维修中照片", order = 9)
    private String processingPhotos;
    @ExcelProperty(value = "维修完成照片", order = 10)
    private String completedPhotos;
}
