package com.ls.aegis.biz.psi.model.req;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 银行流水 Excel 导入行
 */
@Data
public class BankImportRowReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty("流水日期")
    private String stmtDate;

    @ExcelProperty("方向")
    private String direction;

    @ExcelProperty("金额")
    private String amount;

    @ExcelProperty("对方")
    private String counterparty;

    @ExcelProperty("摘要")
    private String summary;

    @ExcelProperty("银行流水号")
    private String bankSeqNo;
}
