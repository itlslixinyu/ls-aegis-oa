package com.ls.aegis.biz.sign.mapper;

import com.ls.aegis.biz.sign.domain.SignKeyStore;

/**
 * 托管密钥 Mapper。
 */
public interface SignKeyStoreMapper {

    SignKeyStore selectByKeyRef(String keyRef);

    int insertSignKeyStore(SignKeyStore keyStore);
}
