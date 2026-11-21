package com.ls.aegis.biz.sign.config;

import com.ls.aegis.compat.common.utils.PasswordEncryptUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 签章系统启动初始化配置
 * <p>
 * 【安全要点】Fail-Fast 启动自检（与 DB_PWD / REDIS_PWD 同级：必填、无空兜底）
 * 1. TOKEN_SECRET 必须通过环境变量注入（严禁代码硬编码或自动生成兜底）
 * 2. 密钥缺失或强度不足直接阻止应用启动，避免"默认密钥生效"的静默漏洞
 * 3. 启动成功后日志中仅打印密钥长度与摘要指纹，绝不打印真实密钥
 */
@Component
public class SignApplicationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SignApplicationRunner.class);

    /**
     * 签章敏感字段加密密钥：与库口令相同，来自环境变量 TOKEN_SECRET（见 application.yml itls.sign.token-secret）。
     */
    @Value("${itls.sign.token-secret}")
    private String tokenSecret;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (tokenSecret == null || tokenSecret.isBlank()) {
            throw new IllegalStateException(
                    "【安全自检失败】未配置 TOKEN_SECRET，请在 docker/.env 注入（≥32 字符，与 DB_PWD 相同必填）。"
                            + "生成建议：openssl rand -base64 32");
        }
        // 1. 设置加密密钥（内部会校验长度≥32字符，长度不足时抛异常）
        PasswordEncryptUtils.setSecretKey(tokenSecret);

        // 2. 启动自检：做一次加密-解密往返，确认密钥可用
        String testPlain = "sign-system-self-check";
        String testCipher = PasswordEncryptUtils.encrypt(testPlain);
        String testDecrypt = PasswordEncryptUtils.decrypt(testCipher);
        if (!testPlain.equals(testDecrypt)) {
            throw new IllegalStateException("【安全自检失败】加密密钥往返校验异常，请检查 TOKEN_SECRET 配置");
        }

        // 3. 打印密钥指纹（仅用于运维确认密钥已加载，不泄露真实值）
        int len = tokenSecret.length();
        String fingerprint = sha256Fingerprint(tokenSecret);
        log.info("========================================");
        log.info("[签章系统启动就绪] 加密密钥自检通过");
        log.info("  - 密钥字符长度: {}", len);
        log.info("  - 密钥SHA-256指纹: {}", fingerprint.substring(0, 16) + "…");
        log.info("  - 加密模式: AES-GCM-256 (认证加密)");
        log.info("========================================");
    }

    /**
     * 计算密钥指纹（仅用于日志展示，非安全用途）
     */
    private static String sha256Fingerprint(String input) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
