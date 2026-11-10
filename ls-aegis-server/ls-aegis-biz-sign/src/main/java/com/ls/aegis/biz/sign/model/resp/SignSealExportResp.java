package com.ls.aegis.biz.sign.model.resp;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@ExcelIgnoreUnannotated
@Schema(description = "印章导出")
public class SignSealExportResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "印章编号", order = 1)
    private String sealCode;
    @ExcelProperty(value = "印章名称", order = 2)
    private String sealName;
    @ExcelProperty(value = "印章分类", order = 3)
    private String categoryLabel;
    @ExcelProperty(value = "印章类型", order = 4)
    private String sealType;
    @ExcelProperty(value = "启用日期", order = 5)
    private String enableDate;
    @ExcelProperty(value = "所属部门", order = 6)
    private String deptName;
    @ExcelProperty(value = "状态", order = 7)
    private String status;
    @ExcelProperty(value = "备注", order = 8)
    private String remark;
}
