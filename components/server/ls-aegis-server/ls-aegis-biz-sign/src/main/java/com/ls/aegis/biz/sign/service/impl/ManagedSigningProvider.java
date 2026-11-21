package com.ls.aegis.biz.sign.service.impl;

import com.ls.aegis.biz.sign.domain.vo.ManagedSignMaterial;
import com.ls.aegis.biz.sign.service.provider.SigningProvider;
import org.springframework.stereotype.Service;

import java.security.Signature;

@Service
public class ManagedSigningProvider implements SigningProvider {

    @Override
    public byte[] signDigest(ManagedSignMaterial material, byte[] digest, String algorithm) throws Exception {
        Signature signature = Signature.getInstance(algorithm);
        signature.initSign(material.getPrivateKey());
        signature.update(digest);
        return signature.sign();
    }

    byte[] debugExtractDigestForTest(byte[] digest) {
        return digest;
    }
}
