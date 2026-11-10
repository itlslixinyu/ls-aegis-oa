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
@Schema(description = "签章记录导出")
public class SignRecordExportResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "记录ID", order = 1)
    private String recordId;
    @ExcelProperty(value = "项目名称", order = 2)
    private String projectName;
    @ExcelProperty(value = "印章名称", order = 3)
    private String sealName;
    @ExcelProperty(value = "操作人", order = 4)
    private String userName;
    @ExcelProperty(value = "签章模式", order = 5)
    private String signMode;
    @ExcelProperty(value = "签章时间", order = 6)
    private Date signTime;
    @ExcelProperty(value = "客户端IP", order = 7)
    private String clientIp;
    @ExcelProperty(value = "IP归属地", order = 8)
    private String ipLocation;
    @ExcelProperty(value = "原文件名称", order = 9)
    private String fileName;
    @ExcelProperty(value = "签章后文件", order = 10)
    private String signedFileName;
    @ExcelProperty(value = "状态", order = 11)
    private String status;
}
