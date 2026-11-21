package com.ls.aegis.crypto.impl;

import com.ls.aegis.crypto.api.IGmCrypto;
import com.ls.aegis.crypto.config.GmCryptoProperties;
import com.ls.aegis.crypto.exception.GmCryptoException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SoftwareGmCrypto 单元测试（密钥仅测试内动态生成，不入库）。
 */
class SoftwareGmCryptoTest {

    private static String publicKeyBase64;
    private static String privateKeyBase64;
    private static final String SM4_KEY = Base64.getEncoder().encodeToString("0123456789abcdeF".getBytes(StandardCharsets.UTF_8));
    private static final String SM4_IV = Base64.getEncoder().encodeToString("1234567890abcdef".getBytes(StandardCharsets.UTF_8));

    private IGmCrypto crypto;

    @BeforeAll
    static void initKeys() throws Exception {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME);
        generator.initialize(new ECGenParameterSpec("sm2p256v1"), new SecureRandom());
        KeyPair keyPair = generator.generateKeyPair();
        publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        privateKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
    }

    @BeforeEach
    void setUp() {
        GmCryptoProperties properties = new GmCryptoProperties();
        properties.setEnable(true);
        properties.getSm2().setPublicKey(publicKeyBase64);
        properties.getSm2().setPrivateKey(privateKeyBase64);
        properties.getSm4().setKey(SM4_KEY);
        properties.getSm4().setIv(SM4_IV);
        crypto = new SoftwareGmCrypto(properties);
    }

    @Test
    void sm2SignAndVerify() {
        byte[] data = "雷铄御警-国密签名".getBytes(StandardCharsets.UTF_8);
        byte[] signature = crypto.sm2Sign(data);
        assertTrue(crypto.sm2Verify(data, signature));
        assertFalse(crypto.sm2Verify("篡改".getBytes(StandardCharsets.UTF_8), signature));
    }

    @Test
    void sm2EncryptDecryptRoundTrip() {
        String plain = "传输口令-SM2";
        String cipherHex = crypto.sm2EncryptToHex(plain);
        assertEquals(plain, crypto.sm2DecryptFromHex(cipherHex));
        assertTrue(crypto.getSm2PublicKeyHex().startsWith("04"));
    }

    @Test
    void sm2DecryptAcceptsCipherWithout04Prefix() {
        String plain = "admin123";
        String cipherHex = crypto.sm2EncryptToHex(plain);
        assertTrue(cipherHex.startsWith("04"));
        // 模拟前端 sm-crypto：去掉未压缩点前缀
        String withoutPrefix = cipherHex.substring(2);
        assertEquals(plain, crypto.sm2DecryptFromHex(withoutPrefix));
    }

    @Test
    void sm3DigestStable() {
        String hex1 = crypto.sm3Hex("abc");
        String hex2 = crypto.sm3Hex("abc".getBytes(StandardCharsets.UTF_8));
        assertEquals(hex1, hex2);
        assertEquals(64, hex1.length());
    }

    @Test
    void sm4EncryptDecryptRoundTrip() {
        byte[] plain = "敏感字段-SM4存储".getBytes(StandardCharsets.UTF_8);
        String cipher = crypto.sm4EncryptToBase64(plain);
        assertArrayEquals(plain, crypto.sm4DecryptFromBase64(cipher));
    }

    @Test
    void missingSm4KeyThrows() {
        GmCryptoProperties properties = new GmCryptoProperties();
        properties.setEnable(true);
        properties.getSm2().setPublicKey(publicKeyBase64);
        properties.getSm2().setPrivateKey(privateKeyBase64);
        IGmCrypto broken = new SoftwareGmCrypto(properties);
        assertThrows(GmCryptoException.class, () -> broken.sm4Encrypt("x".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void isEnabledReflectsConfig() {
        assertTrue(crypto.isEnabled());
    }
}
