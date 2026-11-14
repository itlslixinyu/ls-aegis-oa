package com.ls.aegis.biz.eoms.controller.h5;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.hutool.extra.servlet.JakartaServletUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.IdUtil;
import com.wf.captcha.base.Captcha;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ls.aegis.compat.common.annotation.RateLimiter;
import com.ls.aegis.compat.common.enums.LimitType;
import com.ls.aegis.biz.eoms.domain.H5CustomerInfo;
import com.ls.aegis.biz.eoms.model.req.H5CustomerActivateReq;
import com.ls.aegis.biz.eoms.model.req.H5CustomerLoginReq;
import com.ls.aegis.biz.eoms.service.h5.H5TokenService;
import com.ls.aegis.biz.eoms.util.ImageCaptchaValidator;
import com.ls.aegis.common.util.SecureUtils;
import com.ls.aegis.common.config.CaptchaProperties;
import com.ls.aegis.redis.constant.CacheConstants;
import top.continew.starter.cache.redisson.util.RedisUtils;
import top.continew.starter.captcha.graphic.core.GraphicCaptchaService;
import top.continew.starter.web.model.R;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * H5 客户授权 Controller
 * 管理员邀请建立客户门户凭据，客户用户名密码登录后签发 h5-token。
 * 须 @SaIgnore：客户无 Sa-Token JWT。
 */
@top.continew.starter.log.annotation.Log(ignore = true)
@SaIgnore
@RestController
@RequestMapping("/eoms/h5/auth")
public class H5AuthController
{
    @Autowired
    private H5TokenService h5TokenService;

    @Autowired
    private GraphicCaptchaService graphicCaptchaService;

    @Autowired
    private CaptchaProperties captchaProperties;

    /**
     * H5 专用图形验证码（不受 PC 登录验证码开关影响）。
     */
    @GetMapping("/captcha")
    public R<Map<String, Object>> captcha()
    {
        String uuid = IdUtil.fastUUID();
        String captchaKey = CacheConstants.CAPTCHA_KEY_PREFIX + uuid;
        Captcha captcha = graphicCaptchaService.getCaptcha();
        long expireTime = LocalDateTimeUtil.toEpochMilli(LocalDateTime.now()
            .plusMinutes(captchaProperties.getExpirationInMinutes()));
        RedisUtils.set(captchaKey, captcha.text(), Duration.ofMinutes(captchaProperties.getExpirationInMinutes()));
        Map<String, Object> data = new HashMap<>(4);
        data.put("uuid", uuid);
        data.put("img", captcha.toBase64());
        data.put("expireTime", expireTime);
        data.put("isEnabled", true);
        return R.ok(data);
    }

    /**
     * 首次激活或管理员协助重置客户账号。密码使用平台传输加密协议。
     */
    @RateLimiter(key = "h5_auth_activate", time = 60, count = 5, limitType = LimitType.IP)
    @PostMapping("/activate")
    public R<H5CustomerInfo> activate(@Valid @RequestBody H5CustomerActivateReq req)
    {
        ImageCaptchaValidator.validate(req.getUuid(), req.getCaptcha());
        String password = SecureUtils.decryptPasswordByRsaPrivateKey(req.getPassword(), "密码解密失败", true);
        H5CustomerInfo info = h5TokenService.activateInvitation(req.getInvitationCode(), req.getUsername(), password, req.getDisplayName());
        return R.ok(info);
    }

    /** 客户用户名密码登录；验证码与 IP/账号失败次数共同限制口令猜测。 */
    @RateLimiter(key = "h5_auth_password", time = 60, count = 10, limitType = LimitType.IP)
    @PostMapping("/password")
    public R<H5CustomerInfo> passwordLogin(@Valid @RequestBody H5CustomerLoginReq req, jakarta.servlet.http.HttpServletRequest request)
    {
        ImageCaptchaValidator.validate(req.getUuid(), req.getCaptcha());
        String password = SecureUtils.decryptPasswordByRsaPrivateKey(req.getPassword(), "密码解密失败");
        H5CustomerInfo info = h5TokenService.loginWithPassword(req.getUsername(), password, JakartaServletUtil.getClientIP(request));
        return R.ok(info);
    }

    /**
     * 退出登录（主动失效令牌）
     */
    @PostMapping("/logout")
    public R<Void> logout(@org.springframework.web.bind.annotation.RequestHeader(name = "h5-token", required = false) String token)
    {
        if (token != null && !token.isEmpty())
        {
            h5TokenService.removeToken(token);
        }
        return R.ok();
    }
}
