package com.ls.aegis.crypto.config;

import com.ls.aegis.crypto.exception.GmCryptoException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import java.util.Properties;

/**
 * 国密密钥自动生成与本地落盘（不入库、不提交仓库；环境变量优先）。
 */
public final class GmKeyMaterialBootstrap {

    private static final Logger log = LoggerFactory.getLogger(GmKeyMaterialBootstrap.class);

    private GmKeyMaterialBootstrap() {
    }

    /**
     * 若配置/环境变量未提供密钥：先读本地密钥文件，仍缺失则自动生成并持久化。
     */
    public static void ensureKeys(GmCryptoProperties properties) {
        if (!properties.isEnable()) {
            return;
        }
        if (!properties.isAutoGenerate()) {
            return;
        }
        loadFromStoreIfBlank(properties);
        if (hasCompleteKeys(properties)) {
            return;
        }
        generateMissing(properties);
        saveToStore(properties);
        log.info("国密密钥已自动生成并写入本地密钥库（私钥不下发前端）: {}", resolveStorePath(properties));
    }

    private static boolean hasCompleteKeys(GmCryptoProperties properties) {
        return isPresent(properties.getSm2().getPublicKey())
            && isPresent(properties.getSm2().getPrivateKey())
            && isPresent(properties.getSm4().getKey())
            && isPresent(properties.getSm4().getIv());
    }

    private static void loadFromStoreIfBlank(GmCryptoProperties properties) {
        Path path = resolveStorePath(properties);
        if (!Files.isRegularFile(path)) {
            return;
        }
        Properties fileProps = new Properties();
        try (InputStream in = Files.newInputStream(path)) {
            fileProps.load(in);
        } catch (IOException e) {
            throw new GmCryptoException("读取国密密钥库失败: " + path, e);
        }
        if (!isPresent(properties.getSm2().getPublicKey())) {
            properties.getSm2().setPublicKey(fileProps.getProperty("gm.sm2.public-key", ""));
        }
        if (!isPresent(properties.getSm2().getPrivateKey())) {
            properties.getSm2().setPrivateKey(fileProps.getProperty("gm.sm2.private-key", ""));
        }
        if (!isPresent(properties.getSm4().getKey())) {
            properties.getSm4().setKey(fileProps.getProperty("gm.sm4.key", ""));
        }
        if (!isPresent(properties.getSm4().getIv())) {
            properties.getSm4().setIv(fileProps.getProperty("gm.sm4.iv", ""));
        }
    }

    private static void generateMissing(GmCryptoProperties properties) {
        try {
            if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                Security.addProvider(new BouncyCastleProvider());
            }
            if (!isPresent(properties.getSm2().getPublicKey()) || !isPresent(properties.getSm2().getPrivateKey())) {
                KeyPairGenerator generator = KeyPairGenerator.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME);
                generator.initialize(new ECGenParameterSpec("sm2p256v1"), new SecureRandom());
                KeyPair keyPair = generator.generateKeyPair();
                properties.getSm2().setPublicKey(Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));
                properties.getSm2().setPrivateKey(Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()));
            }
            SecureRandom random = new SecureRandom();
            if (!isPresent(properties.getSm4().getKey())) {
                byte[] key = new byte[16];
                random.nextBytes(key);
                properties.getSm4().setKey(Base64.getEncoder().encodeToString(key));
            }
            if (!isPresent(properties.getSm4().getIv())) {
                byte[] iv = new byte[16];
                random.nextBytes(iv);
                properties.getSm4().setIv(Base64.getEncoder().encodeToString(iv));
            }
        } catch (Exception e) {
            throw new GmCryptoException("自动生成国密密钥失败", e);
        }
    }

    private static void saveToStore(GmCryptoProperties properties) {
        Path path = resolveStorePath(properties);
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Properties fileProps = new Properties();
            fileProps.setProperty("gm.sm2.public-key", nullToEmpty(properties.getSm2().getPublicKey()));
            fileProps.setProperty("gm.sm2.private-key", nullToEmpty(properties.getSm2().getPrivateKey()));
            fileProps.setProperty("gm.sm4.key", nullToEmpty(properties.getSm4().getKey()));
            fileProps.setProperty("gm.sm4.iv", nullToEmpty(properties.getSm4().getIv()));
            try (OutputStream out = Files.newOutputStream(path)) {
                fileProps.store(out, "LS-Aegis GM keys (DO NOT COMMIT). Private key must stay server-side only.");
            }
        } catch (IOException e) {
            throw new GmCryptoException("写入国密密钥库失败: " + path, e);
        }
    }

    private static Path resolveStorePath(GmCryptoProperties properties) {
        String configured = properties.getKeyStorePath();
        if (configured == null || configured.isBlank()) {
            configured = "./data/gm/keys.properties";
        }
        return Paths.get(configured).toAbsolutePath().normalize();
    }

    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
