package com.ls.aegis.crypto.encryptor;

import cn.hutool.extra.spring.SpringUtil;
import com.ls.aegis.crypto.api.IGmCrypto;
import com.ls.aegis.crypto.config.GmCryptoProperties;
import com.ls.aegis.crypto.exception.GmCryptoException;
import top.continew.starter.encrypt.context.CryptoContext;
import top.continew.starter.encrypt.encryptor.AbstractEncryptor;
import top.continew.starter.encrypt.encryptor.AesEncryptor;

import java.nio.charset.StandardCharsets;

/**
 * ContiNew 字段加密 SM4 实现（国密存储）。
 * <p>加密始终 SM4；解密默认仅 SM4。存量 AES 密文须开启 {@code gm.legacy-aes-fallback=true}。</p>
 */
public class Sm4Encryptor extends AbstractEncryptor {

    private final CryptoContext context;

    public Sm4Encryptor(CryptoContext context) {
        super(context);
        this.context = context;
    }

    @Override
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) {
            return plaintext;
        }
        IGmCrypto gmCrypto = SpringUtil.getBean(IGmCrypto.class);
        return gmCrypto.sm4EncryptToBase64(plaintext.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String decrypt(String ciphertext) {
        if (ciphertext == null || ciphertext.isBlank()) {
            return ciphertext;
        }
        try {
            IGmCrypto gmCrypto = SpringUtil.getBean(IGmCrypto.class);
            return new String(gmCrypto.sm4DecryptFromBase64(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception sm4Failed) {
            GmCryptoProperties properties = SpringUtil.getBean(GmCryptoProperties.class);
            if (properties != null && properties.isLegacyAesFallback()) {
                return new AesEncryptor(context).decrypt(ciphertext);
            }
            throw new GmCryptoException("字段 SM4 解密失败（如为存量 AES 密文，请临时开启 gm.legacy-aes-fallback）", sm4Failed);
        }
    }
}
