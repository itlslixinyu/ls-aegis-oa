package com.ls.aegis.common.util;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.ReUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.extra.spring.SpringUtil;
import com.ls.aegis.common.config.RsaProperties;
import com.ls.aegis.common.constant.RegexConstants;
import com.ls.aegis.crypto.api.IGmCrypto;
import com.ls.aegis.crypto.config.GmCryptoProperties;
import top.continew.starter.core.util.ExceptionUtils;
import top.continew.starter.core.util.validation.ValidationUtils;

/**
 * 加解密工具：整体国密下传输密文优先 SM2；仅 {@code gm.legacy-rsa-fallback=true} 或国密关闭时回退 RSA。
 */
public class SecureUtils {

    private SecureUtils() {
    }

    public static String encryptByRsaPublicKey(String data) {
        String publicKey = RsaProperties.PUBLIC_KEY;
        ValidationUtils.throwIfBlank(publicKey, "请配置 RSA 公钥");
        return encryptByRsaPublicKey(data, publicKey);
    }

    public static String decryptByRsaPrivateKey(String data) {
        String privateKey = RsaProperties.PRIVATE_KEY;
        ValidationUtils.throwIfBlank(privateKey, "请配置 RSA 私钥");
        return decryptByRsaPrivateKey(data, privateKey);
    }

    public static String encryptByRsaPublicKey(String data, String publicKey) {
        return new String(SecureUtil.rsa(null, publicKey).encrypt(data, KeyType.PublicKey));
    }

    public static String decryptByRsaPrivateKey(String data, String privateKey) {
        return new String(SecureUtil.rsa(privateKey, null).decrypt(Base64.decode(data), KeyType.PrivateKey));
    }

    /**
     * 解密前端传输密文（口令/密钥等）：国密开启时使用 SM2；失败是否回退 RSA 由 {@code gm.legacy-rsa-fallback} 控制。
     */
    public static String decryptTransport(String encrypted) {
        if (encrypted == null || encrypted.isBlank()) {
            return null;
        }
        IGmCrypto gmCrypto = ExceptionUtils.exToNull(() -> SpringUtil.getBean(IGmCrypto.class));
        if (gmCrypto != null && gmCrypto.isEnabled()) {
            String sm2Plain = ExceptionUtils.exToNull(() -> gmCrypto.sm2DecryptFromHex(encrypted.trim()));
            if (sm2Plain != null && !sm2Plain.isBlank()) {
                return sm2Plain;
            }
            if (!isLegacyRsaFallback()) {
                return null;
            }
        }
        return ExceptionUtils.exToNull(() -> decryptByRsaPrivateKey(encrypted));
    }

    /**
     * 解密前端传输口令（兼容旧方法名）。
     */
    public static String decryptPasswordByRsaPrivateKey(String encryptedPassword, String errorMsg) {
        return decryptPasswordByRsaPrivateKey(encryptedPassword, errorMsg, false);
    }

    /**
     * 解密前端传输口令：优先 SM2，可选 RSA 回退。
     */
    public static String decryptPasswordByRsaPrivateKey(String encryptedPassword,
                                                        String errorMsg,
                                                        boolean isVerifyPattern) {
        String rawPassword = ExceptionUtils.exToNull(() -> decryptTransport(encryptedPassword));
        ValidationUtils.throwIfBlank(rawPassword, errorMsg);
        if (isVerifyPattern) {
            ValidationUtils.throwIf(!ReUtil
                .isMatch(RegexConstants.PASSWORD, rawPassword), "密码长度为 8-32 个字符，支持大小写字母、数字、特殊字符，至少包含字母和数字");
        }
        return rawPassword;
    }

    private static boolean isLegacyRsaFallback() {
        GmCryptoProperties properties = ExceptionUtils.exToNull(() -> SpringUtil.getBean(GmCryptoProperties.class));
        return properties != null && properties.isLegacyRsaFallback();
    }
}
