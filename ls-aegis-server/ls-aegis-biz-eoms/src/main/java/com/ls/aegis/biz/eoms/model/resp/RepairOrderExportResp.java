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
@Schema(description = "维修工单管理导出")
public class RepairOrderExportResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "主键，工单ID", order = 1)
    private Long id;
    @ExcelProperty(value = "维修单号，唯一", order = 2)
    private String orderNo;
    @ExcelProperty(value = "关联报修申请ID", order = 3)
    private Long applyId;
    @ExcelProperty(value = "关联报价单ID", order = 4)
    private Long quoteId;
    @ExcelProperty(value = "报修编号", order = 5)
    private String applyNo;
    @ExcelProperty(value = "报价单号", order = 6)
    private String quoteNo;
    @ExcelProperty(value = "客户ID", order = 7)
    private Long customerId;
    @ExcelProperty(value = "工程师ID，关联sys_user.user_id", order = 8)
    private Long engineerId;
    @ExcelProperty(value = "客户名称", order = 9)
    private String customerName;
    @ExcelProperty(value = "工程师姓名", order = 10)
    private String engineerName;
    @ExcelProperty(value = "状态：WAIT_ACCEPT-待接单、ACCEPTED-已接单、PROCESSING-维修中、WAIT_SETTLEMENT-待结算、FINISHED-已完成、CLOSED-已关闭", order = 11)
    private String status;
    @ExcelProperty(value = "接单时间", order = 12)
    private Date acceptTime;
    @ExcelProperty(value = "开始维修时间", order = 13)
    private Date startTime;
    @ExcelProperty(value = "完成时间", order = 14)
    private Date finishTime;
    @ExcelProperty(value = "维修地址", order = 15)
    private String address;
    @ExcelProperty(value = "现场照片URL（多个逗号分隔，带水印）", order = 16)
    private String photos;
    @ExcelProperty(value = "签到经度", order = 17)
    private String signInLongitude;
    @ExcelProperty(value = "签到纬度", order = 18)
    private String signInLatitude;
    @ExcelProperty(value = "签到地址", order = 19)
    private String signInAddress;
    @ExcelProperty(value = "签退经度", order = 20)
    private String signOutLongitude;
    @ExcelProperty(value = "签退纬度", order = 21)
    private String signOutLatitude;
    @ExcelProperty(value = "签退地址", order = 22)
    private String signOutAddress;
}
