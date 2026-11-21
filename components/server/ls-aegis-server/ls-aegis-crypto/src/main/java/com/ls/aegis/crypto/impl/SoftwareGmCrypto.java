package com.ls.aegis.crypto.impl;

import com.ls.aegis.crypto.api.IGmCrypto;
import com.ls.aegis.crypto.config.GmCryptoProperties;
import com.ls.aegis.crypto.exception.GmCryptoException;
import org.bouncycastle.asn1.gm.GMNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.digests.SM3Digest;
import org.bouncycastle.crypto.engines.SM2Engine;
import org.bouncycastle.crypto.engines.SM4Engine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PKCS7Padding;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.crypto.signers.SM2Signer;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPrivateKeySpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Objects;

/**
 * 基于 BouncyCastle 的 GM/T 软件实现（SM2/SM3/SM4）。
 */
public class SoftwareGmCrypto implements IGmCrypto {

    private static final String CURVE_NAME = "sm2p256v1";
    private static final int SM4_BLOCK = 16;

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private final GmCryptoProperties properties;
    private final ECDomainParameters domainParameters;
    private final ECParameterSpec ecParameterSpec;

    public SoftwareGmCrypto(GmCryptoProperties properties) {
        this.properties = Objects.requireNonNull(properties, "gm 配置不能为空");
        X9ECParameters x9 = GMNamedCurves.getByName(CURVE_NAME);
        this.domainParameters = new ECDomainParameters(x9.getCurve(), x9.getG(), x9.getN(), x9.getH());
        this.ecParameterSpec = new ECParameterSpec(x9.getCurve(), x9.getG(), x9.getN(), x9.getH());
    }

    @Override
    public boolean isEnabled() {
        return properties.isEnable();
    }

    @Override
    public byte[] sm2Sign(byte[] data) {
        return sm2Sign(data, decodeBase64Required(properties.getSm2().getPrivateKey(), "gm.sm2.private-key"));
    }

    @Override
    public boolean sm2Verify(byte[] data, byte[] signature) {
        return sm2Verify(data, signature, decodeBase64Required(properties.getSm2().getPublicKey(), "gm.sm2.public-key"));
    }

    @Override
    public byte[] sm2Sign(byte[] data, byte[] privateKeyBytes) {
        requireNonEmpty(data, "签名原文不能为空");
        requireNonEmpty(privateKeyBytes, "SM2 私钥不能为空");
        try {
            ECPrivateKeyParameters privateKeyParameters = toPrivateKeyParameters(privateKeyBytes);
            SM2Signer signer = new SM2Signer();
            signer.init(true, new ParametersWithRandom(privateKeyParameters, new SecureRandom()));
            signer.update(data, 0, data.length);
            return signer.generateSignature();
        } catch (GmCryptoException e) {
            throw e;
        } catch (Exception e) {
            throw new GmCryptoException("SM2 签名失败", e);
        }
    }

    @Override
    public boolean sm2Verify(byte[] data, byte[] signature, byte[] publicKeyBytes) {
        requireNonEmpty(data, "验签原文不能为空");
        requireNonEmpty(signature, "签名不能为空");
        requireNonEmpty(publicKeyBytes, "SM2 公钥不能为空");
        try {
            ECPublicKeyParameters publicKeyParameters = toPublicKeyParameters(publicKeyBytes);
            SM2Signer signer = new SM2Signer();
            signer.init(false, publicKeyParameters);
            signer.update(data, 0, data.length);
            return signer.verifySignature(signature);
        } catch (Exception e) {
            throw new GmCryptoException("SM2 验签失败", e);
        }
    }

    @Override
    public String sm2EncryptToHex(String plainUtf8) {
        if (plainUtf8 == null) {
            throw new GmCryptoException("SM2 明文不能为空");
        }
        try {
            ECPublicKeyParameters publicKey = toPublicKeyParameters(
                decodeBase64Required(properties.getSm2().getPublicKey(), "gm.sm2.public-key"));
            SM2Engine engine = new SM2Engine(SM2Engine.Mode.C1C3C2);
            engine.init(true, new ParametersWithRandom(publicKey, new SecureRandom()));
            byte[] input = plainUtf8.getBytes(StandardCharsets.UTF_8);
            byte[] cipher = engine.processBlock(input, 0, input.length);
            return Hex.toHexString(cipher);
        } catch (GmCryptoException e) {
            throw e;
        } catch (Exception e) {
            throw new GmCryptoException("SM2 加密失败", e);
        }
    }

    @Override
    public String sm2DecryptFromHex(String cipherHex) {
        if (cipherHex == null || cipherHex.isBlank()) {
            throw new GmCryptoException("SM2 密文不能为空");
        }
        try {
            ECPrivateKeyParameters privateKey = toPrivateKeyParameters(
                decodeBase64Required(properties.getSm2().getPrivateKey(), "gm.sm2.private-key"));
            // 兼容 sm-crypto（无 04 前缀）与 BC 自加密（含 04 前缀）
            byte[] cipher = normalizeSm2CipherBytes(Hex.decode(cipherHex.trim()));
            SM2Engine engine = new SM2Engine(SM2Engine.Mode.C1C3C2);
            engine.init(false, privateKey);
            byte[] plain = engine.processBlock(cipher, 0, cipher.length);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (GmCryptoException e) {
            throw e;
        } catch (InvalidCipherTextException e) {
            throw new GmCryptoException("SM2 解密失败，密文无效", e);
        } catch (Exception e) {
            throw new GmCryptoException("SM2 解密失败", e);
        }
    }

    /**
     * BouncyCastle SM2Engine 要求 C1 为未压缩点（首字节 0x04）。
     * 前端 sm-crypto.doEncrypt 默认不带该前缀，解密前补齐。
     */
    private static byte[] normalizeSm2CipherBytes(byte[] cipher) {
        if (cipher == null || cipher.length == 0) {
            return cipher;
        }
        if (cipher[0] == 0x04) {
            return cipher;
        }
        byte[] withPrefix = new byte[cipher.length + 1];
        withPrefix[0] = 0x04;
        System.arraycopy(cipher, 0, withPrefix, 1, cipher.length);
        return withPrefix;
    }

    @Override
    public String getSm2PublicKeyBase64() {
        return properties.getSm2().getPublicKey();
    }

    @Override
    public String getSm2PublicKeyHex() {
        try {
            PublicKey publicKey = parsePublicKey(
                decodeBase64Required(properties.getSm2().getPublicKey(), "gm.sm2.public-key"));
            if (publicKey instanceof BCECPublicKey bcPublicKey) {
                return Hex.toHexString(bcPublicKey.getQ().getEncoded(false));
            }
            throw new GmCryptoException("无法导出 SM2 公钥十六进制");
        } catch (GmCryptoException e) {
            throw e;
        } catch (Exception e) {
            throw new GmCryptoException("导出 SM2 公钥失败", e);
        }
    }

    @Override
    public byte[] sm3(byte[] data) {
        requireNonEmpty(data, "SM3 原文不能为空");
        SM3Digest digest = new SM3Digest();
        digest.update(data, 0, data.length);
        byte[] out = new byte[digest.getDigestSize()];
        digest.doFinal(out, 0);
        return out;
    }

    @Override
    public String sm3Hex(byte[] data) {
        return Hex.toHexString(sm3(data));
    }

    @Override
    public String sm3Hex(String utf8) {
        if (utf8 == null) {
            throw new GmCryptoException("SM3 原文不能为空");
        }
        return sm3Hex(utf8.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public byte[] sm4Encrypt(byte[] plain) {
        return sm4Process(true, plain);
    }

    @Override
    public byte[] sm4Decrypt(byte[] cipher) {
        return sm4Process(false, cipher);
    }

    @Override
    public String sm4EncryptToBase64(byte[] plain) {
        return Base64.getEncoder().encodeToString(sm4Encrypt(plain));
    }

    @Override
    public byte[] sm4DecryptFromBase64(String cipherBase64) {
        if (cipherBase64 == null || cipherBase64.isBlank()) {
            throw new GmCryptoException("SM4 密文不能为空");
        }
        try {
            return sm4Decrypt(Base64.getDecoder().decode(cipherBase64.trim()));
        } catch (IllegalArgumentException e) {
            throw new GmCryptoException("SM4 密文 Base64 非法", e);
        }
    }

    private byte[] sm4Process(boolean encrypt, byte[] input) {
        requireNonEmpty(input, encrypt ? "SM4 明文不能为空" : "SM4 密文不能为空");
        byte[] key = decodeBase64Required(properties.getSm4().getKey(), "gm.sm4.key");
        byte[] iv = decodeBase64Required(properties.getSm4().getIv(), "gm.sm4.iv");
        if (key.length != SM4_BLOCK) {
            throw new GmCryptoException("gm.sm4.key 解码后须为 16 字节");
        }
        if (iv.length != SM4_BLOCK) {
            throw new GmCryptoException("gm.sm4.iv 解码后须为 16 字节");
        }
        try {
            PaddedBufferedBlockCipher cipher = new PaddedBufferedBlockCipher(
                new CBCBlockCipher(new SM4Engine()),
                new PKCS7Padding());
            cipher.init(encrypt, new ParametersWithIV(new KeyParameter(key), iv));
            byte[] out = new byte[cipher.getOutputSize(input.length)];
            int len = cipher.processBytes(input, 0, input.length, out, 0);
            len += cipher.doFinal(out, len);
            if (len == out.length) {
                return out;
            }
            byte[] trimmed = new byte[len];
            System.arraycopy(out, 0, trimmed, 0, len);
            return trimmed;
        } catch (GmCryptoException e) {
            throw e;
        } catch (Exception e) {
            throw new GmCryptoException(encrypt ? "SM4 加密失败" : "SM4 解密失败", e);
        }
    }

    private ECPrivateKeyParameters toPrivateKeyParameters(byte[] privateKeyBytes) throws Exception {
        PrivateKey privateKey = parsePrivateKey(privateKeyBytes);
        if (privateKey instanceof BCECPrivateKey bcPrivateKey) {
            return new ECPrivateKeyParameters(bcPrivateKey.getD(), domainParameters);
        }
        throw new GmCryptoException("不支持的 SM2 私钥类型: " + privateKey.getClass().getName());
    }

    private ECPublicKeyParameters toPublicKeyParameters(byte[] publicKeyBytes) throws Exception {
        PublicKey publicKey = parsePublicKey(publicKeyBytes);
        if (publicKey instanceof BCECPublicKey bcPublicKey) {
            return new ECPublicKeyParameters(bcPublicKey.getQ(), domainParameters);
        }
        throw new GmCryptoException("不支持的 SM2 公钥类型: " + publicKey.getClass().getName());
    }

    private PrivateKey parsePrivateKey(byte[] encoded) throws Exception {
        KeyFactory keyFactory = KeyFactory.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME);
        try {
            return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(encoded));
        } catch (Exception ignore) {
            // 原始 D 值（32 字节）或正整数编码
            BigInteger d = new BigInteger(1, encoded);
            return keyFactory.generatePrivate(new ECPrivateKeySpec(d, ecParameterSpec));
        }
    }

    private PublicKey parsePublicKey(byte[] encoded) throws Exception {
        KeyFactory keyFactory = KeyFactory.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME);
        try {
            return keyFactory.generatePublic(new X509EncodedKeySpec(encoded));
        } catch (Exception ignore) {
            ECPoint q = domainParameters.getCurve().decodePoint(encoded);
            return keyFactory.generatePublic(new ECPublicKeySpec(q, ecParameterSpec));
        }
    }

    private static byte[] decodeBase64Required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new GmCryptoException(name + " 未配置，请通过环境变量/配置中心注入");
        }
        try {
            return Base64.getDecoder().decode(value.trim());
        } catch (IllegalArgumentException e) {
            throw new GmCryptoException(name + " 不是合法 Base64", e);
        }
    }

    private static void requireNonEmpty(byte[] data, String message) {
        if (data == null || data.length == 0) {
            throw new GmCryptoException(message);
        }
    }
}
