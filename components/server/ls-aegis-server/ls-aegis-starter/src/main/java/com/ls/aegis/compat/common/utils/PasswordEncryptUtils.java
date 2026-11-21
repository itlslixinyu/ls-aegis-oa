package com.ls.aegis.compat.common.utils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 密码加密工具类（AES-GCM-256 带认证）
 * <p>
 * 【安全升级要点】
 * 1. 移除默认密钥兜底：密钥缺失时启动失败（Fail-Fast），禁止弱密钥
 * 2. 加密模式升级：AES/ECB → AES/GCM/NoPadding，带 128-bit 认证标签，防篡改+防重放
 * 3. 每次加密随机生成 12 字节 IV（GCM推荐），前置存储在密文头部
 * 4. 兼容旧版 ECB 密文：GCM解密失败时自动尝试 ECB 兼容模式（过渡期使用）
 * <p>
 * 密文格式（Base64编码后）：
 * [12字节 IV][可变长 密文+16字节GCM Tag]
 */
public class PasswordEncryptUtils {

    private static final String ALGORITHM = "AES";

    /** 新版：GCM模式（带认证，防篡改） */
    private static final String TRANSFORMATION_GCM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    /** 旧版：ECB模式（仅用于兼容历史已加密数据，解密失败时回退尝试） */
    @Deprecated
    private static final String TRANSFORMATION_ECB = "AES/ECB/PKCS5Padding";

    /** 最小密钥长度（SHA-256后总是32字节=256位，但原始种子要求≥32字符增加熵） */
    private static final int MIN_SECRET_LENGTH = 32;

    /** 密钥（必须启动时注入，禁止代码内硬编码兜底） */
    private static volatile String secretKey;

    /** 加解密锁：防止多线程下密钥未就绪并发读写 */
    private static final Object LOCK = new Object();

    /**
     * 设置加密密钥（启动时调用一次即可）
     * <p>
     * 【Fail-Fast】密钥为空或强度不足时直接抛出异常，阻止应用继续启动
     *
     * @param secret 密钥字符串，建议使用 openssl rand -base64 32 生成
     */
    public static void setSecretKey(String secret) {
        synchronized (LOCK) {
            if (secret == null || secret.trim().isEmpty()) {
                throw new IllegalStateException(
                        "【安全错误】TOKEN_SECRET 不能为空，请通过环境变量或配置文件注入强密钥（≥32字符）。"
                                + "禁止在代码中硬编码默认密钥。生成建议：openssl rand -base64 32");
            }
            if (secret.length() < MIN_SECRET_LENGTH) {
                throw new IllegalStateException(
                        "【安全错误】TOKEN_SECRET 长度不足 (" + secret.length()
                                + "字符)，要求至少 " + MIN_SECRET_LENGTH + " 字符。"
                                + "请更换为强密钥。生成建议：openssl rand -base64 32");
            }
            secretKey = secret;
        }
    }

    /**
     * 检查密钥是否已就绪（主要用于单元测试/启动自检）
     */
    public static boolean isKeyReady() {
        synchronized (LOCK) {
            return secretKey != null && !secretKey.isEmpty();
        }
    }

    /**
     * 基于密钥种子派生 256-bit AES 密钥（SHA-256 哈希）
     */
    private static SecretKeySpec deriveKey() throws Exception {
        String key;
        synchronized (LOCK) {
            if (secretKey == null || secretKey.isEmpty()) {
                throw new IllegalStateException(
                        "【安全错误】加密密钥未初始化，请在应用启动时调用 PasswordEncryptUtils.setSecretKey()"
                                + "注入 TOKEN_SECRET 环境变量。禁止使用未授权的默认密钥。");
            }
            key = secretKey;
        }
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = sha.digest(key.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(keyBytes, ALGORITHM);
    }

    /**
     * 加密密码
     *
     * @param password 明文密码
     * @return AES-GCM 加密后的密文（Base64编码：[12B IV][密文+Tag]）
     */
    public static String encrypt(String password) {
        if (password == null || password.isEmpty()) {
            return null;
        }
        try {
            SecretKeySpec keySpec = deriveKey();

            // 1. 随机生成 12 字节 IV（GCM必须每次唯一）
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            // 2. GCM参数
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION_GCM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

            // 3. 加密
            byte[] plainBytes = password.getBytes(StandardCharsets.UTF_8);
            byte[] cipherWithTag = cipher.doFinal(plainBytes);

            // 4. 拼接：IV (12B) + Ciphertext+Tag
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + cipherWithTag.length);
            buffer.put(iv);
            buffer.put(cipherWithTag);

            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            throw new IllegalStateException("敏感信息加密失败，请检查密钥配置", e);
        }
    }

    /**
     * 解密密码
     *
     * @param encryptedPassword Base64编码的密文（支持新版GCM和旧版ECB）
     * @return 明文密码
     */
    public static String decrypt(String encryptedPassword) {
        if (encryptedPassword == null || encryptedPassword.isEmpty()) {
            return null;
        }
        try {
            SecretKeySpec keySpec = deriveKey();
            byte[] allBytes = Base64.getDecoder().decode(encryptedPassword);

            // ==== 尝试 1：新版 GCM 格式（最小长度 12B IV + 16B Tag ≥ 28B） ====
            if (allBytes.length >= GCM_IV_LENGTH + 16) {
                try {
                    ByteBuffer buffer = ByteBuffer.wrap(allBytes);
                    byte[] iv = new byte[GCM_IV_LENGTH];
                    buffer.get(iv);
                    byte[] cipherWithTag = new byte[buffer.remaining()];
                    buffer.get(cipherWithTag);

                    GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
                    Cipher cipher = Cipher.getInstance(TRANSFORMATION_GCM);
                    cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);
                    byte[] plain = cipher.doFinal(cipherWithTag);
                    return new String(plain, StandardCharsets.UTF_8);
                } catch (Exception gcmFailed) {
                    // GCM 认证失败，可能是旧版 ECB 数据 → 进入回退分支
                }
            }

            // ==== 尝试 2：旧版 ECB 兼容模式（仅用于历史数据迁移过渡） ====
            try {
                Cipher cipher = Cipher.getInstance(TRANSFORMATION_ECB);
                cipher.init(Cipher.DECRYPT_MODE, keySpec);
                byte[] decrypted = cipher.doFinal(allBytes);
                return new String(decrypted, StandardCharsets.UTF_8);
            } catch (Exception ecbFailed) {
                // 两种模式都失败 → 抛出最终异常
                throw new IllegalStateException("敏感信息解密失败：密文格式不合法或密钥不匹配");
            }
        } catch (IllegalStateException se) {
            throw se;
        } catch (Exception e) {
            throw new IllegalStateException("敏感信息解密失败，请检查密文或密钥配置", e);
        }
    }

    /**
     * 检查密码是否已加密（Base64格式启发式判断）
     */
    public static boolean isEncrypted(String password) {
        if (password == null || password.isEmpty()) {
            return false;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(password);
            return decoded.length > 0;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
