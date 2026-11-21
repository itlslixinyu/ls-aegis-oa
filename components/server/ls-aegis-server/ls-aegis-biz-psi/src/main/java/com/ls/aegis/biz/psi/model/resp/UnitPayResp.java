package com.ls.aegis.biz.psi.model.resp;

import com.ls.aegis.common.base.model.resp.BaseResp;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;

/**
 * 往来单位付款账户响应
 */
@Data
@Schema(description = "往来单位付款账户响应")
public class UnitPayResp extends BaseResp {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "往来单位ID")
    private Long unitId;

    @Schema(description = "账户名称")
    private String accountName;

    @Schema(description = "开户银行")
    private String bankName;

    @Schema(description = "银行账号")
    private String accountNo;

    @Schema(description = "是否默认：0否 1是")
    private Integer isDefault;

    @Schema(description = "备注")
    private String remark;
}
