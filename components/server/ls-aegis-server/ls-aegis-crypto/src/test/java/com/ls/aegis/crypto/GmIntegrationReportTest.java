package com.ls.aegis.crypto;

import com.ls.aegis.crypto.api.IGmCrypto;
import com.ls.aegis.crypto.config.GmCryptoProperties;
import com.ls.aegis.crypto.impl.SoftwareGmCrypto;
import com.ls.aegis.crypto.password.Sm3PasswordEncoder;
import com.ls.aegis.crypto.util.Sm4EcbUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guomi end-to-end self-check: assert fixed vectors and print GM_REPORT_OK on success.
 */
class GmIntegrationReportTest {

    private static final String ADMIN_SM3 = "dc1fd00e3eeeb940ff46f457bf97d66ba7fcc36e0b20802383de142860e76ae6";
    private static final String ADMIN_SHA256_OF_SM3 = "318c70107cb60b3641fc7f720dc0b5ee3a29e06642f7c2799a287e8c42ce84b1";
    private static final String ADMIN123_HASH =
        "v1$ef4114fbfe4881f4710b626429c97c04$bd43c9a531b4cc2705aaff6d1a2f9563906c254cc3ffe05fbb21c2456bd86d99";
    private static final String TEST123_HASH =
        "v1$cfa6968c5df393377b04ff6e33f6f90c$025649846502c230dbfcff724d5724a2aa0af1c2a97defc84da59c70b9df24c8";
    private static final String SM4_ECB_VECTOR = "0ICPh9h5Oi27T6YvqeTbsCA3xNEO6Ke1hBhMSKZqVPw=";

    private static String publicKeyBase64;
    private static String privateKeyBase64;
    private static final String SM4_KEY = Base64.getEncoder()
        .encodeToString("0123456789abcdeF".getBytes(StandardCharsets.UTF_8));
    private static final String SM4_IV = Base64.getEncoder()
        .encodeToString("1234567890abcdef".getBytes(StandardCharsets.UTF_8));

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

    @Test
    void gmIntegrationReport() throws Exception {
        GmCryptoProperties properties = new GmCryptoProperties();
        properties.setEnable(true);
        properties.getSm2().setPublicKey(publicKeyBase64);
        properties.getSm2().setPrivateKey(privateKeyBase64);
        properties.getSm4().setKey(SM4_KEY);
        properties.getSm4().setIv(SM4_IV);
        IGmCrypto crypto = new SoftwareGmCrypto(properties);

        String sm3Admin = crypto.sm3Hex("admin");
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        String sha256OfSm3 = Hex.toHexString(sha256.digest(sm3Admin.getBytes(StandardCharsets.UTF_8)));
        assertEquals(ADMIN_SM3, sm3Admin);
        assertEquals(ADMIN_SHA256_OF_SM3, sha256OfSm3);
        System.out.println("SM3_ADMIN=" + sm3Admin);
        System.out.println("SHA256_OF_SM3_ADMIN=" + sha256OfSm3);

        Sm3PasswordEncoder encoder = new Sm3PasswordEncoder();
        assertTrue(encoder.matches("admin123", ADMIN123_HASH));
        System.out.println("SEED_ADMIN123_MATCH=true");

        assertTrue(encoder.matches("test123", TEST123_HASH));
        System.out.println("SEED_TEST123_MATCH=true");

        String key = "1234567890123456";
        String plain = "{\"x\":120.5,\"y\":5.0}";
        String cipher = Sm4EcbUtils.encryptToBase64(plain, key);
        assertEquals(SM4_ECB_VECTOR, cipher);
        assertEquals(plain, Sm4EcbUtils.decryptFromBase64(cipher, key));
        System.out.println("SM4_ECB_B64=" + cipher);

        String sm2Plain = "gm-sm2-roundtrip";
        String sm2Cipher = crypto.sm2EncryptToHex(sm2Plain);
        assertEquals(sm2Plain, crypto.sm2DecryptFromHex(sm2Cipher));
        System.out.println("SM2_ENCRYPT_DECRYPT_OK=true");

        byte[] signData = "gm-sm2-sign".getBytes(StandardCharsets.UTF_8);
        byte[] signature = crypto.sm2Sign(signData);
        assertTrue(crypto.sm2Verify(signData, signature));
        System.out.println("SM2_SIGN_VERIFY_OK=true");

        byte[] sm4Plain = "gm-sm4-cbc".getBytes(StandardCharsets.UTF_8);
        String sm4Cipher = crypto.sm4EncryptToBase64(sm4Plain);
        assertArrayEquals(sm4Plain, crypto.sm4DecryptFromBase64(sm4Cipher));
        System.out.println("SM4_CBC_ROUNDTRIP_OK=true");

        System.out.println("GM_REPORT_OK");
    }
}