package com.ls.aegis.biz.sign.service.provider;

import com.ls.aegis.biz.sign.domain.vo.ManagedSignMaterial;

public interface SigningProvider {

    byte[] signDigest(ManagedSignMaterial material, byte[] digest, String algorithm) throws Exception;
}
