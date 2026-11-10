package com.ls.aegis.biz.psi.model.resp;

import com.ls.aegis.common.base.model.resp.BaseResp;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;

/**
 * 往来单位开票信息响应
 */
@Data
@Schema(description = "往来单位开票信息响应")
public class UnitInvoiceResp extends BaseResp {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "往来单位ID")
    private Long unitId;

    @Schema(description = "发票抬头")
    private String title;

    @Schema(description = "纳税人识别号")
    private String taxNo;

    @Schema(description = "开户银行")
    private String bankName;

    @Schema(description = "银行账号")
    private String accountNo;

    @Schema(description = "地址")
    private String address;

    @Schema(description = "电话")
    private String phone;

    @Schema(description = "是否默认：0否 1是")
    private Integer isDefault;

    @Schema(description = "备注")
    private String remark;
}
