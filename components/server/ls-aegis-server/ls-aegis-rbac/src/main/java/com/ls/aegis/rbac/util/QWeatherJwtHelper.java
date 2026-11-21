package com.ls.aegis.rbac.util;

import cn.hutool.core.util.StrUtil;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Base64;

/**
 * 和风天气 JWT（EdDSA / Ed25519）签发，对齐官方文档 Java 15+ 示例。
 */
public final class QWeatherJwtHelper {

    /** Token 有效期（秒），服务端场景 15 分钟 */
    private static final long TOKEN_TTL_SECONDS = 900;

    private QWeatherJwtHelper() {
    }

    /**
     * @param kid           凭据 ID（Header.kid）
     * @param projectId     项目 ID（Payload.sub）
     * @param privateKeyPem Ed25519 私钥 PEM
     */
    public static String createToken(String kid, String projectId, String privateKeyPem) {
        if (StrUtil.hasBlank(kid, projectId, privateKeyPem)) {
            throw new IllegalArgumentException("和风 JWT 参数不完整");
        }
        try {
            PrivateKey privateKey = parsePrivateKey(privateKeyPem);
            long iat = ZonedDateTime.now(ZoneOffset.UTC).toEpochSecond() - 30;
            long exp = iat + TOKEN_TTL_SECONDS;
            String headerJson = "{\"alg\":\"EdDSA\",\"kid\":\"" + escapeJson(kid) + "\"}";
            String payloadJson = "{\"sub\":\"" + escapeJson(projectId) + "\",\"iat\":" + iat + ",\"exp\":" + exp + "}";
            String headerEncoded = base64Url(headerJson.getBytes(StandardCharsets.UTF_8));
            String payloadEncoded = base64Url(payloadJson.getBytes(StandardCharsets.UTF_8));
            String data = headerEncoded + "." + payloadEncoded;

            Signature signer = Signature.getInstance("EdDSA");
            signer.initSign(privateKey);
            signer.update(data.getBytes(StandardCharsets.UTF_8));
            return data + "." + base64Url(signer.sign());
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("和风 JWT 签发失败：" + e.getMessage(), e);
        }
    }

    public static long tokenTtlSeconds() {
        return TOKEN_TTL_SECONDS;
    }

    private static PrivateKey parsePrivateKey(String privateKeyPem) throws Exception {
        String normalized = privateKeyPem
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replaceAll("\\s+", "");
        byte[] der = Base64.getDecoder().decode(normalized);
        return KeyFactory.getInstance("EdDSA").generatePrivate(new PKCS8EncodedKeySpec(der));
    }

    private static String base64Url(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    private static String escapeJson(String raw) {
        return StrUtil.replace(StrUtil.replace(raw, "\\", "\\\\"), "\"", "\\\"");
    }
}
