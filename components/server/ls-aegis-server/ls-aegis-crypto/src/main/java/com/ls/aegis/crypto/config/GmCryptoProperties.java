package com.ls.aegis.crypto.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 国密配置。密钥优先环境变量/配置中心；未配置时由 {@link GmKeyMaterialBootstrap} 自动生成并落盘。
 */
@ConfigurationProperties(prefix = "gm")
public class GmCryptoProperties {

    /**
     * 全局开关：true 注册 SoftwareGmCrypto。
     */
    private boolean enable = true;

    /**
     * 密钥为空时是否自动生成并写入 key-store-path。
     */
    private boolean autoGenerate = true;

    /**
     * 本地密钥库路径（禁止提交仓库）。
     */
    private String keyStorePath = "./data/gm/keys.properties";

    /**
     * 字段解密是否允许回退 AES（仅迁移存量 AES 密文时临时开启；整体国密默认 false）。
     */
    private boolean legacyAesFallback = false;

    /**
     * 传输解密是否允许回退 RSA（仅兼容旧客户端时临时开启；整体国密默认 false）。
     */
    private boolean legacyRsaFallback = false;

    private final Sm2 sm2 = new Sm2();

    private final Sm4 sm4 = new Sm4();

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public boolean isAutoGenerate() {
        return autoGenerate;
    }

    public void setAutoGenerate(boolean autoGenerate) {
        this.autoGenerate = autoGenerate;
    }

    public String getKeyStorePath() {
        return keyStorePath;
    }

    public void setKeyStorePath(String keyStorePath) {
        this.keyStorePath = keyStorePath;
    }

    public boolean isLegacyAesFallback() {
        return legacyAesFallback;
    }

    public void setLegacyAesFallback(boolean legacyAesFallback) {
        this.legacyAesFallback = legacyAesFallback;
    }

    public boolean isLegacyRsaFallback() {
        return legacyRsaFallback;
    }

    public void setLegacyRsaFallback(boolean legacyRsaFallback) {
        this.legacyRsaFallback = legacyRsaFallback;
    }

    public Sm2 getSm2() {
        return sm2;
    }

    public Sm4 getSm4() {
        return sm4;
    }

    public static class Sm2 {
        /**
         * SM2 公钥 Base64。空则自动生成。
         */
        private String publicKey = "";

        /**
         * SM2 私钥 Base64。仅服务端，禁止下发前端。
         */
        private String privateKey = "";

        public String getPublicKey() {
            return publicKey;
        }

        public void setPublicKey(String publicKey) {
            this.publicKey = publicKey;
        }

        public String getPrivateKey() {
            return privateKey;
        }

        public void setPrivateKey(String privateKey) {
            this.privateKey = privateKey;
        }
    }

    public static class Sm4 {
        /**
         * SM4 密钥 Base64（16 字节）。空则自动生成。
         */
        private String key = "";

        /**
         * SM4-CBC IV Base64（16 字节）。空则自动生成。
         */
        private String iv = "";

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getIv() {
            return iv;
        }

        public void setIv(String iv) {
            this.iv = iv;
        }
    }
}
