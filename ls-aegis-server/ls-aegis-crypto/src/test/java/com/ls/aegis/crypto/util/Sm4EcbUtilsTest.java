package com.ls.aegis.crypto.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Sm4EcbUtilsTest {

    @Test
    void roundTrip() {
        String key = "1234567890123456";
        String plain = "{\"x\":120.5,\"y\":5.0}";
        String cipher = Sm4EcbUtils.encryptToBase64(plain, key);
        assertEquals(plain, Sm4EcbUtils.decryptFromBase64(cipher, key));
    }

    @Test
    void blankKeyPassthrough() {
        String plain = "hello";
        assertEquals(plain, Sm4EcbUtils.encryptToBase64(plain, ""));
        assertEquals(plain, Sm4EcbUtils.decryptFromBase64(plain, " "));
    }

    /** 与前端 sm-crypto SM4/ECB 对齐的固定向量 */
    @Test
    void matchesFrontendSmCryptoVector() {
        String key = "1234567890123456";
        String plain = "{\"x\":120.5,\"y\":5.0}";
        assertEquals("0ICPh9h5Oi27T6YvqeTbsCA3xNEO6Ke1hBhMSKZqVPw=", Sm4EcbUtils.encryptToBase64(plain, key));
    }
}
