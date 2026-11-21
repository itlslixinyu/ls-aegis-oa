package com.ls.aegis.biz.eoms.model.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * H5 客户获取令牌请求。
 */
@Data
public class H5TokenReq {

    @Deprecated
    private String customerNo;

    @NotBlank(message = "请提供管理员签发的一次性邀请码")
    @jakarta.validation.constraints.Pattern(regexp = "(?:\\d{8}|[A-Za-z0-9_-]{43})", message = "邀请码格式不正确")
    private String invitationCode;

    @NotBlank(message = "验证码不能为空")
    private String captcha;

    @NotBlank(message = "验证码标识不能为空")
    private String uuid;
}
