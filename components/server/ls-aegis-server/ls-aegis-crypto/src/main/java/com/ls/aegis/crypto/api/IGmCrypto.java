package com.ls.aegis.crypto.api;

/**
 * 国密统一接口（GM/T 软件实现）。
 * <p>由 {@code gm.enable=true} 时注册 {@link com.ls.aegis.crypto.impl.SoftwareGmCrypto}；私钥仅服务端使用，禁止下发前端。</p>
 */
public interface IGmCrypto {

    boolean isEnabled();

    byte[] sm2Sign(byte[] data);

    boolean sm2Verify(byte[] data, byte[] signature);

    byte[] sm2Sign(byte[] data, byte[] privateKeyBytes);

    boolean sm2Verify(byte[] data, byte[] signature, byte[] publicKeyBytes);

    /**
     * SM2 加密（C1C3C2），密文为十六进制（与前端 sm-crypto 对齐）。
     */
    String sm2EncryptToHex(String plainUtf8);

    /**
     * SM2 解密（C1C3C2），密文为十六进制。
     */
    String sm2DecryptFromHex(String cipherHex);

    /**
     * SM2 公钥 X.509 Base64（可对外下发）。
     */
    String getSm2PublicKeyBase64();

    /**
     * SM2 未压缩公钥点十六进制（含 04 前缀，供前端 sm-crypto）。
     */
    String getSm2PublicKeyHex();

    byte[] sm3(byte[] data);

    String sm3Hex(byte[] data);

    String sm3Hex(String utf8);

    byte[] sm4Encrypt(byte[] plain);

    byte[] sm4Decrypt(byte[] cipher);

    String sm4EncryptToBase64(byte[] plain);

    byte[] sm4DecryptFromBase64(String cipherBase64);
}
