package com.ls.aegis.biz.psi.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.io.Serial;
import java.io.Serializable;

/**
 * 往来单位联系人请求
 */
@Data
@Schema(description = "往来单位联系人请求")
public class UnitContactReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "联系人姓名", example = "张三")
    @NotBlank(message = "联系人姓名不能为空")
    @Length(max = 64, message = "联系人姓名长度不能超过 {max} 个字符")
    private String contactName;

    @Schema(description = "手机号")
    @Length(max = 32, message = "手机号长度不能超过 {max} 个字符")
    private String mobile;

    @Schema(description = "固定电话")
    @Length(max = 32, message = "固定电话长度不能超过 {max} 个字符")
    private String phone;

    @Schema(description = "邮箱")
    @Length(max = 128, message = "邮箱长度不能超过 {max} 个字符")
    private String email;

    @Schema(description = "职位")
    @Length(max = 64, message = "职位长度不能超过 {max} 个字符")
    private String position;

    @Schema(description = "是否默认：0否 1是", example = "0")
    private Integer isDefault;

    @Schema(description = "备注")
    @Length(max = 500, message = "备注长度不能超过 {max} 个字符")
    private String remark;
}
