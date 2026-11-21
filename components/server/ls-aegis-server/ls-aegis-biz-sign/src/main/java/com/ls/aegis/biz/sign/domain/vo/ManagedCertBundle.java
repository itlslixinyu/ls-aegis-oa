package com.ls.aegis.biz.sign.domain.vo;

import com.ls.aegis.biz.sign.domain.SignCert;
import com.ls.aegis.biz.sign.domain.SignKeyStore;

/**
 * 平台托管证书签发结果，包含证书元数据与私钥存储材料。
 */
public class ManagedCertBundle {

    private SignCert cert;
    private SignKeyStore keyStore;

    public SignCert getCert() {
        return cert;
    }

    public void setCert(SignCert cert) {
        this.cert = cert;
    }

    public SignKeyStore getKeyStore() {
        return keyStore;
    }

    public void setKeyStore(SignKeyStore keyStore) {
        this.keyStore = keyStore;
    }
}
