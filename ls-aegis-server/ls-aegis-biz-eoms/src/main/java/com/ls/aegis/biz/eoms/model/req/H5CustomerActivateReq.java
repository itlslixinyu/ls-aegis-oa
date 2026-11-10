package com.ls.aegis.biz.eoms.model.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class H5CustomerActivateReq {
    @NotBlank(message = "请提供管理员签发的一次性邀请码")
    @Pattern(regexp = "(?:\\d{8}|[A-Za-z0-9_-]{43})", message = "邀请码格式不正确")
    private String invitationCode;

    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "[A-Za-z][A-Za-z0-9._-]{3,31}", message = "用户名需为4-32位字母开头的字母、数字或 . _ -")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(max = 2048, message = "加密密码格式不正确")
    private String password;

    @NotBlank(message = "验证码不能为空")
    private String captcha;

    @NotBlank(message = "验证码标识不能为空")
    private String uuid;
}
