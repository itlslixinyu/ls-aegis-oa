package com.ls.aegis.biz.psi.model.resp;

import com.ls.aegis.common.base.model.resp.BaseResp;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;

/**
 * 往来单位收货地址响应
 */
@Data
@Schema(description = "往来单位收货地址响应")
public class UnitAddressResp extends BaseResp {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "往来单位ID")
    private Long unitId;

    @Schema(description = "收货人")
    private String contactName;

    @Schema(description = "手机号")
    private String mobile;

    @Schema(description = "省市区")
    private String region;

    @Schema(description = "详细地址")
    private String address;

    @Schema(description = "是否默认：0否 1是")
    private Integer isDefault;

    @Schema(description = "备注")
    private String remark;
}
