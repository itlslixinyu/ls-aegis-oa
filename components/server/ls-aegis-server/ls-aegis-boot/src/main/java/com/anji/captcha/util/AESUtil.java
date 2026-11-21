package com.anji.captcha.util;

import cn.hutool.extra.spring.SpringUtil;
import com.ls.aegis.crypto.api.IGmCrypto;
import com.ls.aegis.crypto.util.Sm4EcbUtils;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Random;

/**
 * 覆盖 anji-captcha 内置 AESUtil（类名不可改，供库内静态调用）。
 * <ul>
 *   <li>整体国密（gm.enable=true）：坐标加密为 SM4/ECB</li>
 *   <li>gm.enable=false：回退 AES/ECB/PKCS5Padding</li>
 * </ul>
 */
public class AESUtil {

    private static final String ALGORITHMSTR = "AES/ECB/PKCS5Padding";
    private static final Random RANDOM = new SecureRandom();

    /** 与 captcha 1.4.0 RandomUtils.getRandomString(16) 一致：a-zA-Z0-9，恰 16 字节 UTF-8 */
    public static String getKey() {
        String alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder uid = new StringBuilder(16);
        for (int i = 0; i < 16; i++) {
            uid.append(alphabet.charAt(RANDOM.nextInt(alphabet.length())));
        }
        return uid.toString();
    }

    public static byte[] aesEncryptToBytes(String content, String encryptKey) throws Exception {
        if (content == null) {
            return null;
        }
        if (encryptKey == null || encryptKey.isBlank()) {
            return content.getBytes(StandardCharsets.UTF_8);
        }
        if (gmEnabled()) {
            return Base64.getDecoder().decode(Sm4EcbUtils.encryptToBase64(content, encryptKey));
        }
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(128);
        Cipher cipher = Cipher.getInstance(ALGORITHMSTR);
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(encryptKey.getBytes(), "AES"));
        return cipher.doFinal(content.getBytes(StandardCharsets.UTF_8));
    }

    public static String aesEncrypt(String content, String encryptKey) throws Exception {
        if (content == null) {
            return null;
        }
        if (encryptKey == null || encryptKey.isBlank()) {
            return content;
        }
        if (gmEnabled()) {
            return Sm4EcbUtils.encryptToBase64(content, encryptKey);
        }
        return base64Encode(aesEncryptToBytes(content, encryptKey));
    }

    public static String aesDecryptByBytes(byte[] encryptBytes, String decryptKey) throws Exception {
        if (encryptBytes == null) {
            return null;
        }
        if (decryptKey == null || decryptKey.isBlank()) {
            return new String(encryptBytes, StandardCharsets.UTF_8);
        }
        if (gmEnabled()) {
            return Sm4EcbUtils.decryptFromBase64(Base64.getEncoder().encodeToString(encryptBytes), decryptKey);
        }
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(128);
        Cipher cipher = Cipher.getInstance(ALGORITHMSTR);
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(decryptKey.getBytes(), "AES"));
        byte[] decryptBytes = cipher.doFinal(encryptBytes);
        return new String(decryptBytes, StandardCharsets.UTF_8);
    }

    public static String aesDecrypt(String encryptStr, String decryptKey) throws Exception {
        if (encryptStr == null || encryptStr.isBlank()) {
            return null;
        }
        if (decryptKey == null || decryptKey.isBlank()) {
            return encryptStr;
        }
        if (gmEnabled()) {
            return Sm4EcbUtils.decryptFromBase64(encryptStr, decryptKey);
        }
        return aesDecryptByBytes(base64Decode(encryptStr), decryptKey);
    }

    public static String base64Encode(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static byte[] base64Decode(String base64Code) {
        return base64Code == null || base64Code.isBlank() ? null : Base64.getDecoder().decode(base64Code);
    }

    public static String binary(byte[] bytes, int radix) {
        return new BigInteger(1, bytes).toString(radix);
    }

    private static boolean gmEnabled() {
        try {
            IGmCrypto crypto = SpringUtil.getBean(IGmCrypto.class);
            return crypto != null && crypto.isEnabled();
        } catch (Exception ignored) {
            return false;
        }
    }
}
