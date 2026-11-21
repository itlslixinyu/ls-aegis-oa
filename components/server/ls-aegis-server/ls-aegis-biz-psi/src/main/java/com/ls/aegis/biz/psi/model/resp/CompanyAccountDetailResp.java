package com.ls.aegis.biz.psi.model.resp;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import com.ls.aegis.common.base.model.resp.BaseDetailResp;
import com.ls.aegis.common.enums.DisEnableStatusEnum;
import lombok.Data;
import top.continew.starter.excel.converter.ExcelBaseEnumConverter;

import java.io.Serial;

@Data
@ExcelIgnoreUnannotated
public class CompanyAccountDetailResp extends BaseDetailResp {
    @Serial
    private static final long serialVersionUID = 1L;
    @ExcelProperty(value = "户名", order = 1)
    private String accountName;
    @ExcelProperty(value = "开户行", order = 2)
    private String bankName;
    @ExcelProperty(value = "账号", order = 3)
    private String accountNo;
    @ExcelProperty(value = "状态", converter = ExcelBaseEnumConverter.class, order = 4)
    private DisEnableStatusEnum status;
    @ExcelProperty(value = "备注", order = 5)
    private String remark;
}
