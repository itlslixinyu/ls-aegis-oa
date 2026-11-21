package com.ls.aegis.biz.eoms.util;

import com.ls.aegis.redis.constant.CacheConstants;
import top.continew.starter.cache.redisson.util.RedisUtils;
import top.continew.starter.core.util.validation.ValidationUtils;

/**
 * H5 / 登录图形验证码校验。
 */
public final class ImageCaptchaValidator {

    private static final String CAPTCHA_EXPIRED = "验证码已失效，请重新获取";

    private ImageCaptchaValidator() {
    }

    public static void validate(String uuid, String captcha) {
        ValidationUtils.throwIfBlank(uuid, "验证码标识不能为空");
        ValidationUtils.throwIfBlank(captcha, "验证码不能为空");
        String captchaKey = CacheConstants.CAPTCHA_KEY_PREFIX + uuid;
        String cached = RedisUtils.get(captchaKey);
        ValidationUtils.throwIfBlank(cached, CAPTCHA_EXPIRED);
        RedisUtils.delete(captchaKey);
        ValidationUtils.throwIfNotEqualIgnoreCase(captcha, cached, "验证码不正确");
    }
}
