package com.ls.aegis.biz.psi.model.resp;

import com.ls.aegis.common.base.model.resp.BaseResp;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;

/**
 * 往来单位联系人响应
 */
@Data
@Schema(description = "往来单位联系人响应")
public class UnitContactResp extends BaseResp {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "往来单位ID")
    private Long unitId;

    @Schema(description = "联系人姓名")
    private String contactName;

    @Schema(description = "手机号")
    private String mobile;

    @Schema(description = "固定电话")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "职位")
    private String position;

    @Schema(description = "是否默认：0否 1是")
    private Integer isDefault;

    @Schema(description = "备注")
    private String remark;
}
