package com.ls.aegis.crypto.password;

import org.bouncycastle.crypto.digests.SM3Digest;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * 国密口令哈希：SM3 + 随机盐。
 * <p>编码结果形如 {@code v1$&lt;saltHex&gt;$&lt;sm3Hex&gt;}，由 DelegatingPasswordEncoder 加 {@code {sm3}} 前缀。</p>
 */
public class Sm3PasswordEncoder implements PasswordEncoder {

    private static final String VERSION = "v1";
    private static final int SALT_LEN = 16;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String encode(CharSequence rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("口令不能为空");
        }
        byte[] salt = new byte[SALT_LEN];
        secureRandom.nextBytes(salt);
        String hash = sm3Hex(concat(salt, rawPassword.toString().getBytes(StandardCharsets.UTF_8)));
        return VERSION + "$" + Hex.toHexString(salt) + "$" + hash;
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null || encodedPassword.isBlank()) {
            return false;
        }
        String[] parts = encodedPassword.split("\\$");
        if (parts.length != 3 || !VERSION.equals(parts[0])) {
            return false;
        }
        byte[] salt = Hex.decode(parts[1]);
        String expected = parts[2];
        String actual = sm3Hex(concat(salt, rawPassword.toString().getBytes(StandardCharsets.UTF_8)));
        return constantTimeEquals(expected, actual);
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] out = Arrays.copyOf(a, a.length + b.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    private static String sm3Hex(byte[] data) {
        SM3Digest digest = new SM3Digest();
        digest.update(data, 0, data.length);
        byte[] out = new byte[digest.getDigestSize()];
        digest.doFinal(out, 0);
        return Hex.toHexString(out);
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) {
            return false;
        }
        int r = 0;
        for (int i = 0; i < a.length(); i++) {
            r |= a.charAt(i) ^ b.charAt(i);
        }
        return r == 0;
    }
}
