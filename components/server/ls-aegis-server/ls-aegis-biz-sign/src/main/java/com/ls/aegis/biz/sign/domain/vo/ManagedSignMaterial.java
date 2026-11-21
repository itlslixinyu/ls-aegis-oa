package com.ls.aegis.biz.sign.domain.vo;

import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.List;

public class ManagedSignMaterial {

    private Long certId;
    private String keyRef;
    private PrivateKey privateKey;
    private List<Certificate> certificateChain;

    public Long getCertId() {
        return certId;
    }

    public void setCertId(Long certId) {
        this.certId = certId;
    }

    public String getKeyRef() {
        return keyRef;
    }

    public void setKeyRef(String keyRef) {
        this.keyRef = keyRef;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(PrivateKey privateKey) {
        this.privateKey = privateKey;
    }

    public List<Certificate> getCertificateChain() {
        return certificateChain;
    }

    public void setCertificateChain(List<Certificate> certificateChain) {
        this.certificateChain = certificateChain;
    }
}
