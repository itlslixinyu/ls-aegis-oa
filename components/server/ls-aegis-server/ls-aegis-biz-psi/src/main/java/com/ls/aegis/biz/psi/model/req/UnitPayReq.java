package com.ls.aegis.biz.psi.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.io.Serial;
import java.io.Serializable;

/**
 * 往来单位付款账户请求
 */
@Data
@Schema(description = "往来单位付款账户请求")
public class UnitPayReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "账户名称", example = "示例科技有限公司")
    @NotBlank(message = "账户名称不能为空")
    @Length(max = 128, message = "账户名称长度不能超过 {max} 个字符")
    private String accountName;

    @Schema(description = "开户银行")
    @Length(max = 128, message = "开户银行长度不能超过 {max} 个字符")
    private String bankName;

    @Schema(description = "银行账号")
    @NotBlank(message = "银行账号不能为空")
    @Length(max = 64, message = "银行账号长度不能超过 {max} 个字符")
    private String accountNo;

    @Schema(description = "是否默认：0否 1是", example = "0")
    private Integer isDefault;

    @Schema(description = "备注")
    @Length(max = 500, message = "备注长度不能超过 {max} 个字符")
    private String remark;
}
