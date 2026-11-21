package com.ls.aegis.biz.sign.service.impl;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

/**
 * 证书服务实现
 *
 * 支持：
 * - 自签名证书生成（测试/演示用）
 * - PFX/P12 证书读取
 * - 证书信息解析
 */
@Service
public class CertService {

    private static final Logger log = LoggerFactory.getLogger(CertService.class);

    private static final String ALGORITHM = "RSA";
    private static final int KEY_SIZE = 2048;

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    /**
     * 生成 RSA 密钥对
     */
    public KeyPair generateRSAKeyPair() {
        return generateRSAKeyPair(KEY_SIZE);
    }

    /**
     * 生成 RSA 密钥对（指定密钥长度）
     */
    public KeyPair generateRSAKeyPair(int keySize) {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ALGORITHM, BouncyCastleProvider.PROVIDER_NAME);
            keyGen.initialize(keySize, new SecureRandom());
            return keyGen.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException("生成 RSA 密钥对失败", e);
        }
    }

    public KeyPair generateKeyPair() {
        return generateRSAKeyPair();
    }

    /**
     * 生成自签名 X.509 证书
     *
     * @param commonName  通用名称（CN），如公司名或印章名
     * @param orgName    组织名称（O）
     * @param validDays  有效期（天）
     */
    public X509Certificate generateSelfSignedCert(String commonName, String orgName, int validDays) {
        try {
            // 1. 生成密钥对
            KeyPair keyPair = generateRSAKeyPair();

            // 2. 构建 X.509 证书
            long now = System.currentTimeMillis();
            Date startDate = new Date(now);
            Date endDate = new Date(now + (long) validDays * 24 * 3600 * 1000);

            X500Name issuer = new X500Name(BCStyle.INSTANCE, "CN=" + commonName + ", O=" + (orgName != null ? orgName : "SignSystem"));

            X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                    issuer,
                    BigInteger.valueOf(now),         // 序列号
                    startDate,                        // 生效时间
                    endDate,                          // 过期时间
                    issuer,                           // 颁发者（自签）
                    keyPair.getPublic()               // 公钥
            );

            // 3. 添加扩展
            // BasicConstraints: CA=FALSE（非 CA 证书）
            // KeyUsage: Digital Signature + Non Repudiation
            certBuilder.addExtension(
                    org.bouncycastle.asn1.x509.Extension.keyUsage,
                    false,
                    new org.bouncycastle.asn1.x509.KeyUsage(
                            org.bouncycastle.asn1.x509.KeyUsage.nonRepudiation
                                    | org.bouncycastle.asn1.x509.KeyUsage.digitalSignature)
            );

            // 4. 签名
            JcaContentSignerBuilder signerBuilder = new JcaContentSignerBuilder("SHA256WithRSA");
            signerBuilder.setProvider(BouncyCastleProvider.PROVIDER_NAME);
            ContentSigner contentSigner = signerBuilder.build(keyPair.getPrivate());

            // 5. 生成证书
            X509Certificate cert = new JcaX509CertificateConverter()
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                    .getCertificate(certBuilder.build(contentSigner));

            log.info("自签名证书生成成功: subject={}, 有效期={}天", cert.getSubjectX500Principal().getName(), validDays);
            return cert;

        } catch (Exception e) {
            log.error("生成自签名证书失败", e);
            throw new RuntimeException("生成自签名证书失败", e);
        }
    }

    public X509Certificate generateSelfSignedCertificate(KeyPair keyPair, String subjectDn, int validDays) {
        try {
            long now = System.currentTimeMillis();
            Date startDate = new Date(now);
            Date endDate = new Date(now + (long) validDays * 24 * 3600 * 1000);
            X500Name subject = new X500Name(subjectDn);

            X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                    subject,
                    BigInteger.valueOf(now),
                    startDate,
                    endDate,
                    subject,
                    keyPair.getPublic()
            );
            certBuilder.addExtension(
                    org.bouncycastle.asn1.x509.Extension.keyUsage,
                    false,
                    new org.bouncycastle.asn1.x509.KeyUsage(
                            org.bouncycastle.asn1.x509.KeyUsage.nonRepudiation
                                    | org.bouncycastle.asn1.x509.KeyUsage.digitalSignature)
            );

            ContentSigner contentSigner = new JcaContentSignerBuilder("SHA256WithRSA")
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                    .build(keyPair.getPrivate());

            return new JcaX509CertificateConverter()
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                    .getCertificate(certBuilder.build(contentSigner));
        } catch (Exception e) {
            throw new RuntimeException("生成自签名证书失败", e);
        }
    }

    /**
     * 从 PFX 文件字节数组加载证书和私钥（签章时用：证书文件在 MinIO，使用字节数组直接加载）
     *
     * @param pfxBytes  PFX 字节数组
     * @param password  密码（空字符串表示无密码）
     * @return KeyStore 对象（含私钥和证书链）
     */
    public KeyStore loadFromPfxBytes(byte[] pfxBytes, String password) {
        try (InputStream is = new ByteArrayInputStream(pfxBytes)) {
            KeyStore keyStore = KeyStore.getInstance("PKCS12", BouncyCastleProvider.PROVIDER_NAME);
            char[] pwd = (password == null) ? new char[0] : password.toCharArray();
            keyStore.load(is, pwd);
            log.info("PFX 证书加载成功（字节数组加载）");
            return keyStore;
        } catch (Exception e) {
            log.error("加载 PFX 证书失败", e);
            throw new RuntimeException("加载 PFX 证书失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从 PFX 文件加载证书和私钥
     *
     * @param pfxPath   PFX 文件路径
     * @param password  密码
     * @return KeyStore 对象（含私钥）
     */
    public KeyStore loadFromPfx(String pfxPath, String password) {
        try (InputStream is = new FileInputStream(pfxPath)) {
            KeyStore keyStore = KeyStore.getInstance("PKCS12", BouncyCastleProvider.PROVIDER_NAME);
            char[] pwd = (password == null) ? new char[0] : password.toCharArray();
            keyStore.load(is, pwd);
            log.info("PFX 证书加载成功: {}", pfxPath);
            return keyStore;
        } catch (Exception e) {
            log.error("加载 PFX 证书失败: {}", pfxPath, e);
            throw new RuntimeException("加载 PFX 证书失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从 KeyStore 获取私钥（自动寻找第一个含私钥的别名）
     */
    public PrivateKey getPrivateKey(KeyStore keyStore, String password) {
        try {
            char[] pwd = (password == null) ? new char[0] : password.toCharArray();
            java.util.Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                Key key = keyStore.getKey(alias, pwd);
                if (key instanceof PrivateKey) {
                    return (PrivateKey) key;
                }
            }
            throw new RuntimeException("在 KeyStore 中未找到私钥，请确认证书包含私钥");
        } catch (Exception e) {
            throw new RuntimeException("获取私钥失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从 KeyStore 获取证书链（自动寻找第一个含私钥的别名）
     */
    public Certificate[] getCertificateChain(KeyStore keyStore) {
        try {
            java.util.Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                if (keyStore.isKeyEntry(alias)) {
                    Certificate[] chain = keyStore.getCertificateChain(alias);
                    if (chain != null && chain.length > 0) {
                        return chain;
                    }
                    Certificate cert = keyStore.getCertificate(alias);
                    if (cert != null) {
                        return new Certificate[]{cert};
                    }
                }
            }
            throw new RuntimeException("在 KeyStore 中未找到证书链");
        } catch (Exception e) {
            throw new RuntimeException("获取证书链失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从 PFX 获取私钥（指定别名）
     */
    public PrivateKey getPrivateKey(KeyStore keyStore, String alias, String password) {
        try {
            Key key = keyStore.getKey(alias, password.toCharArray());
            if (key instanceof PrivateKey) {
                return (PrivateKey) key;
            }
            throw new RuntimeException("指定的别名不是私钥");
        } catch (Exception e) {
            throw new RuntimeException("获取私钥失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从 PFX 获取证书链（指定别名）
     */
    public Certificate[] getCertificateChain(KeyStore keyStore, String alias) {
        try {
            Certificate[] chain = keyStore.getCertificateChain(alias);
            if (chain == null || chain.length == 0) {
                Certificate cert = keyStore.getCertificate(alias);
                if (cert != null) {
                    return new Certificate[]{cert};
                }
                throw new RuntimeException("未找到证书链");
            }
            return chain;
        } catch (Exception e) {
            throw new RuntimeException("获取证书链失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析证书信息（完整信息），返回 JSON 字符串（与 PHP 项目对齐：含 fingerprint_sha256）
     */
    public String parseCertInfo(Certificate cert) {
        if (!(cert instanceof X509Certificate x509)) {
            return "{}";
        }

        try {
            // SHA-256 证书指纹
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] fingerprint = md.digest(x509.getEncoded());
            StringBuilder hex = new StringBuilder();
            for (byte b : fingerprint) {
                hex.append(String.format("%02x", b));
            }
            String sha256Fingerprint = hex.toString();

            // 序列号（hex 格式）
            String serialNumber = x509.getSerialNumber().toString(16);

            // 签名算法
            String sigAlg = x509.getSigAlgName();

            // 有效期（ISO 格式字符串）
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            sdf.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
            String notBefore = sdf.format(x509.getNotBefore());
            String notAfter = sdf.format(x509.getNotAfter());

            // 主题和颁发者
            String subject = x509.getSubjectX500Principal().getName();
            String issuer = x509.getIssuerX500Principal().getName();

            // 检查证书是否已过期（签章时用于快速判定）
            boolean isExpired;
            try {
                x509.checkValidity();
                isExpired = false;
            } catch (Exception ex) {
                isExpired = true;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("{");
            sb.append("\"subject\":\"").append(escape(subject)).append("\",");
            sb.append("\"issuer\":\"").append(escape(issuer)).append("\",");
            sb.append("\"serialNumber\":\"").append(serialNumber).append("\",");
            sb.append("\"notBefore\":\"").append(notBefore).append("\",");
            sb.append("\"notAfter\":\"").append(notAfter).append("\",");
            sb.append("\"sigAlg\":\"").append(sigAlg).append("\",");
            sb.append("\"fingerprint_sha256\":\"").append(sha256Fingerprint).append("\",");
            sb.append("\"isExpired\":").append(isExpired).append(",");
            sb.append("\"version\":").append(x509.getVersion());
            sb.append("}");
            return sb.toString();
        } catch (Exception e) {
            log.error("解析证书信息失败", e);
            return "{\"error\":\"" + escape(e.getMessage()) + "\"}";
        }
    }

    /**
     * 将证书导出为 PEM 格式字符串
     */
    public String exportCertToPem(Certificate cert) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("-----BEGIN CERTIFICATE-----\n");
            String base64 = Base64.getEncoder().encodeToString(cert.getEncoded());
            // 每64字符换行
            for (int i = 0; i < base64.length(); i += 64) {
                sb.append(base64, i, Math.min(i + 64, base64.length()));
                sb.append("\n");
            }
            sb.append("-----END CERTIFICATE-----\n");
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("导出证书 PEM 失败", e);
        }
    }

    public String toPem(X509Certificate certificate) throws Exception {
        String base64 = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(certificate.getEncoded());
        return "-----BEGIN CERTIFICATE-----\n" + base64 + "\n-----END CERTIFICATE-----\n";
    }

    public String sha256Fingerprint(X509Certificate certificate) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(certificate.getEncoded());
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format(Locale.ROOT, "%02X", b));
        }
        return sb.toString();
    }

    public PrivateKey loadPrivateKey(String encodedPrivateKey) {
        try {
            byte[] decoded = Base64.getDecoder().decode(encodedPrivateKey);
            return KeyFactory.getInstance(ALGORITHM).generatePrivate(new PKCS8EncodedKeySpec(decoded));
        } catch (Exception e) {
            throw new RuntimeException("解析托管私钥失败", e);
        }
    }

    public List<Certificate> parseCertificates(String pemText) {
        if (pemText == null || pemText.trim().isEmpty()) {
            return List.of();
        }
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(pemText.getBytes(java.nio.charset.StandardCharsets.US_ASCII))) {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            return new ArrayList<>(factory.generateCertificates(inputStream));
        } catch (Exception e) {
            throw new RuntimeException("解析 PEM 证书失败", e);
        }
    }

    /**
     * 将私钥和证书打包为 P12（PKCS#12）文件
     */
    public void exportToP12(PrivateKey privateKey, Certificate cert, String p12Path, String password) {
        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12", BouncyCastleProvider.PROVIDER_NAME);
            keyStore.load(null, null);
            keyStore.setKeyEntry("sign-key", privateKey, password.toCharArray(), new Certificate[]{cert});

            try (FileOutputStream fos = new FileOutputStream(p12Path)) {
                keyStore.store(fos, password.toCharArray());
            }
            log.info("P12 文件导出成功: {}", p12Path);
        } catch (Exception e) {
            throw new RuntimeException("导出 P12 失败: " + e.getMessage(), e);
        }
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
}
