package com.ls.aegis.biz.sign.model.resp;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@ExcelIgnoreUnannotated
@Schema(description = "签章项目导出")
public class SignProjectExportResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "项目编号", order = 1)
    private String projectCode;
    @ExcelProperty(value = "项目名称", order = 2)
    private String projectName;
    @ExcelProperty(value = "所属部门", order = 3)
    private String deptName;
    @ExcelProperty(value = "负责人", order = 4)
    private String managerNames;
    @ExcelProperty(value = "状态", order = 5)
    private String status;
    @ExcelProperty(value = "开始日期", order = 6)
    private Date startDate;
    @ExcelProperty(value = "结束日期", order = 7)
    private Date endDate;
    @ExcelProperty(value = "合同编号", order = 8)
    private String contractNo;
    @ExcelProperty(value = "合同名称", order = 9)
    private String contractName;
    @ExcelProperty(value = "往来单位", order = 10)
    private String partnerName;
    @ExcelProperty(value = "备注", order = 11)
    private String remark;
}
