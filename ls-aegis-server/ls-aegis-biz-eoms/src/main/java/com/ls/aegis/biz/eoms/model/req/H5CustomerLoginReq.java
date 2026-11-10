package com.ls.aegis.biz.eoms.model.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class H5CustomerLoginReq {
    @NotBlank(message = "用户名不能为空")
    @Size(max = 32, message = "用户名不正确")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(max = 2048, message = "加密密码格式不正确")
    private String password;

    @NotBlank(message = "验证码不能为空")
    private String captcha;

    @NotBlank(message = "验证码标识不能为空")
    private String uuid;
}
