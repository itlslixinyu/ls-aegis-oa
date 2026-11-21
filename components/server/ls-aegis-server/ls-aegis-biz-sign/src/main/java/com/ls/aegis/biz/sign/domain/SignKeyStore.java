package com.ls.aegis.biz.sign.domain;

import com.ls.aegis.mybatis.base.model.entity.XmlMapperDO;

/**
 * 托管私钥存储信息。
 */
public class SignKeyStore extends XmlMapperDO {

    private static final long serialVersionUID = 1L;

    private String keyRef;
    private String storageType;
    private String encryptedKeyBlob;
    private String encryptVersion;
    private String status;

    public String getKeyRef() { return keyRef; }
    public void setKeyRef(String keyRef) { this.keyRef = keyRef; }
    public String getStorageType() { return storageType; }
    public void setStorageType(String storageType) { this.storageType = storageType; }
    public String getEncryptedKeyBlob() { return encryptedKeyBlob; }
    public void setEncryptedKeyBlob(String encryptedKeyBlob) { this.encryptedKeyBlob = encryptedKeyBlob; }
    public String getEncryptVersion() { return encryptVersion; }
    public void setEncryptVersion(String encryptVersion) { this.encryptVersion = encryptVersion; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
