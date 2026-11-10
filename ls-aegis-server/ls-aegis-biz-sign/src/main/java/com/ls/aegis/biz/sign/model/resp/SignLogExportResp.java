package com.ls.aegis.biz.sign.model.resp;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

@Data
@ExcelIgnoreUnannotated
@Schema(description = "签章日志导出")
public class SignLogExportResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "日志ID", order = 1)
    private Long logId;
    @ExcelProperty(value = "项目名称", order = 2)
    private String projectName;
    @ExcelProperty(value = "印章名称", order = 3)
    private String sealName;
    @ExcelProperty(value = "操作人", order = 4)
    private String userName;
    @ExcelProperty(value = "操作昵称", order = 5)
    private String nickName;
    @ExcelProperty(value = "操作类型", order = 6)
    private String actionType;
    @ExcelProperty(value = "结果", order = 7)
    private String resultCode;
    @ExcelProperty(value = "客户端IP", order = 8)
    private String clientIp;
    @ExcelProperty(value = "IP归属地", order = 9)
    private String ipLocation;
    @ExcelProperty(value = "签章时间", order = 10)
    private Date signTime;
    @ExcelProperty(value = "创建时间", order = 11)
    private LocalDateTime createTime;
}
