package com.ls.aegis.biz.psi.model.req;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 仓库导入行
 */
@Data
public class WarehouseImportRowReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty("仓库编码")
    private String warehouseCode;

    @ExcelProperty("仓库名称")
    private String warehouseName;

    @ExcelProperty("负责人")
    private String manager;

    @ExcelProperty("地址")
    private String address;

    @ExcelProperty("是否默认仓")
    private String isDefault;

    @ExcelProperty("状态")
    private String status;

    @ExcelProperty("备注")
    private String remark;
}
