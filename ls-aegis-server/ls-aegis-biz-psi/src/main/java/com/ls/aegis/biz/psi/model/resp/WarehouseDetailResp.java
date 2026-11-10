package com.ls.aegis.biz.psi.model.resp;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import com.ls.aegis.common.base.model.resp.BaseDetailResp;
import com.ls.aegis.common.enums.DisEnableStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import top.continew.starter.excel.converter.ExcelBaseEnumConverter;

import java.io.Serial;

@Data
@ExcelIgnoreUnannotated
@Schema(description = "仓库详情响应")
public class WarehouseDetailResp extends BaseDetailResp {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "仓库编码", order = 2)
    private String warehouseCode;
    @ExcelProperty(value = "仓库名称", order = 3)
    private String warehouseName;
    @Schema(description = "负责人用户ID")
    private Long managerUserId;
    @ExcelProperty(value = "负责人", order = 4)
    private String manager;
    @ExcelProperty(value = "地址", order = 5)
    private String address;
    @ExcelProperty(value = "是否默认仓", order = 6)
    private Boolean isDefault;
    @ExcelProperty(value = "状态", converter = ExcelBaseEnumConverter.class, order = 7)
    private DisEnableStatusEnum status;
    @ExcelProperty(value = "备注", order = 8)
    private String remark;
}
