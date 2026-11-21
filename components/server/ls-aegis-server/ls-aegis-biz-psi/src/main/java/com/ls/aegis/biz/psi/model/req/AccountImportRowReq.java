package com.ls.aegis.biz.psi.model.req;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 公司账户导入行
 */
@Data
public class AccountImportRowReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty("户名")
    private String accountName;

    @ExcelProperty("开户行")
    private String bankName;

    @ExcelProperty("账号")
    private String accountNo;

    @ExcelProperty("状态")
    private String status;

    @ExcelProperty("备注")
    private String remark;
}
