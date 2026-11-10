package com.ls.aegis.crypto.util;

import com.ls.aegis.crypto.exception.GmCryptoException;
import org.bouncycastle.crypto.engines.SM4Engine;
import org.bouncycastle.crypto.paddings.PKCS7Padding;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.Base64;

/**
 * SM4/ECB/PKCS7（与 AJ-Captcha 原 AES/ECB 协议位宽一致：16 字节密钥）。
 * <p>供行为验证码坐标加密；密钥为会话下发的 secretKey（UTF-8 字节）。</p>
 */
public final class Sm4EcbUtils {

    private static final int SM4_BLOCK = 16;

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private Sm4EcbUtils() {
    }

    public static String encryptToBase64(String plainUtf8, String keyUtf8) {
        if (plainUtf8 == null) {
            return null;
        }
        if (keyUtf8 == null || keyUtf8.isBlank()) {
            return plainUtf8;
        }
        byte[] cipher = process(true, plainUtf8.getBytes(StandardCharsets.UTF_8), keyBytes(keyUtf8));
        return Base64.getEncoder().encodeToString(cipher);
    }

    public static String decryptFromBase64(String cipherBase64, String keyUtf8) {
        if (cipherBase64 == null) {
            return null;
        }
        if (keyUtf8 == null || keyUtf8.isBlank()) {
            return cipherBase64;
        }
        try {
            byte[] plain = process(false, Base64.getDecoder().decode(cipherBase64.trim()), keyBytes(keyUtf8));
            return new String(plain, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new GmCryptoException("SM4-ECB 密文 Base64 非法", e);
        }
    }

    private static byte[] keyBytes(String keyUtf8) {
        byte[] key = keyUtf8.getBytes(StandardCharsets.UTF_8);
        if (key.length != SM4_BLOCK) {
            throw new GmCryptoException("SM4-ECB 密钥须为 16 字节（与验证码 secretKey 一致）");
        }
        return key;
    }

    private static byte[] process(boolean encrypt, byte[] input, byte[] key) {
        try {
            PaddedBufferedBlockCipher cipher = new PaddedBufferedBlockCipher(new SM4Engine(), new PKCS7Padding());
            cipher.init(encrypt, new KeyParameter(key));
            byte[] out = new byte[cipher.getOutputSize(input.length)];
            int len = cipher.processBytes(input, 0, input.length, out, 0);
            len += cipher.doFinal(out, len);
            if (len == out.length) {
                return out;
            }
            byte[] trimmed = new byte[len];
            System.arraycopy(out, 0, trimmed, 0, len);
            return trimmed;
        } catch (GmCryptoException e) {
            throw e;
        } catch (Exception e) {
            throw new GmCryptoException(encrypt ? "SM4-ECB 加密失败" : "SM4-ECB 解密失败", e);
        }
    }
}
