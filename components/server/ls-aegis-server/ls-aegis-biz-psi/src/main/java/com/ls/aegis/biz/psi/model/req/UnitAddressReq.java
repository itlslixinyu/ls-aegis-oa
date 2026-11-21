package com.ls.aegis.biz.psi.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.io.Serial;
import java.io.Serializable;

/**
 * 往来单位收货地址请求
 */
@Data
@Schema(description = "往来单位收货地址请求")
public class UnitAddressReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "收货人", example = "李四")
    @Length(max = 64, message = "收货人长度不能超过 {max} 个字符")
    private String contactName;

    @Schema(description = "手机号")
    @Length(max = 32, message = "手机号长度不能超过 {max} 个字符")
    private String mobile;

    @Schema(description = "省市区")
    @Length(max = 128, message = "省市区长度不能超过 {max} 个字符")
    private String region;

    @Schema(description = "详细地址", example = "某某路 1 号")
    @NotBlank(message = "详细地址不能为空")
    @Length(max = 256, message = "详细地址长度不能超过 {max} 个字符")
    private String address;

    @Schema(description = "是否默认：0否 1是", example = "0")
    private Integer isDefault;

    @Schema(description = "备注")
    @Length(max = 500, message = "备注长度不能超过 {max} 个字符")
    private String remark;
}
