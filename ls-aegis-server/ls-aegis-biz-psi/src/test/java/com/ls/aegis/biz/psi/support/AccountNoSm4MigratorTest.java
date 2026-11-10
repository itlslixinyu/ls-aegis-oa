package com.ls.aegis.biz.psi.support;

import com.ls.aegis.crypto.api.IGmCrypto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

class AccountNoSm4MigratorTest {

    @Test
    void plaintextNeedsEncrypt() {
        IGmCrypto gm = new FakeGm();
        Assertions.assertTrue(AccountNoSm4Migrator.needsEncrypt("6222021234563456", gm));
    }

    @Test
    void alreadyCipherSkips() {
        IGmCrypto gm = new FakeGm();
        String plain = "DEMO-ACC-001";
        String cipher = gm.sm4EncryptToBase64(plain.getBytes(StandardCharsets.UTF_8));
        Assertions.assertFalse(AccountNoSm4Migrator.needsEncrypt(cipher, gm));
    }

    @Test
    void blankSkips() {
        IGmCrypto gm = new FakeGm();
        Assertions.assertFalse(AccountNoSm4Migrator.needsEncrypt("  ", gm));
        Assertions.assertFalse(AccountNoSm4Migrator.needsEncrypt(null, gm));
    }

    /** 确定性假实现：明文加前缀模拟密文 */
    private static final class FakeGm implements IGmCrypto {
        private static final String PREFIX = "SM4:";

        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        public byte[] sm2Sign(byte[] data) {
            return data;
        }

        @Override
        public boolean sm2Verify(byte[] data, byte[] signature) {
            return true;
        }

        @Override
        public byte[] sm2Sign(byte[] data, byte[] privateKeyBytes) {
            return data;
        }

        @Override
        public boolean sm2Verify(byte[] data, byte[] signature, byte[] publicKeyBytes) {
            return true;
        }

        @Override
        public String sm2EncryptToHex(String plainUtf8) {
            return plainUtf8;
        }

        @Override
        public String sm2DecryptFromHex(String cipherHex) {
            return cipherHex;
        }

        @Override
        public String getSm2PublicKeyBase64() {
            return "";
        }

        @Override
        public String getSm2PublicKeyHex() {
            return "";
        }

        @Override
        public byte[] sm3(byte[] data) {
            return data;
        }

        @Override
        public String sm3Hex(byte[] data) {
            return "";
        }

        @Override
        public String sm3Hex(String utf8) {
            return "";
        }

        @Override
        public byte[] sm4Encrypt(byte[] plain) {
            return plain;
        }

        @Override
        public byte[] sm4Decrypt(byte[] cipher) {
            return cipher;
        }

        @Override
        public String sm4EncryptToBase64(byte[] plaintext) {
            return PREFIX + new String(plaintext, StandardCharsets.UTF_8);
        }

        @Override
        public byte[] sm4DecryptFromBase64(String ciphertext) {
            if (ciphertext == null || !ciphertext.startsWith(PREFIX)) {
                throw new IllegalArgumentException("not cipher");
            }
            return ciphertext.substring(PREFIX.length()).getBytes(StandardCharsets.UTF_8);
        }
    }
}
