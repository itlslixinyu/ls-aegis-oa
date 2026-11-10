package com.ls.aegis.biz.sign.service.impl;

import com.ls.aegis.biz.sign.domain.SignCert;
import com.ls.aegis.biz.sign.domain.SignKeyStore;
import com.ls.aegis.biz.sign.domain.vo.ManagedSignMaterial;
import com.ls.aegis.biz.sign.mapper.SignCertMapper;
import com.ls.aegis.biz.sign.mapper.SignKeyStoreMapper;
import com.ls.aegis.biz.sign.service.provider.KeyProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.List;

@Component
public class ManagedKeyProvider implements KeyProvider {

    private final SignCertMapper signCertMapper;
    private final SignKeyStoreMapper signKeyStoreMapper;
    private final CertService certService;

    public ManagedKeyProvider(SignCertMapper signCertMapper, SignKeyStoreMapper signKeyStoreMapper) {
        this(signCertMapper, signKeyStoreMapper, new CertService());
    }

    @Autowired
    public ManagedKeyProvider(SignCertMapper signCertMapper,
                              SignKeyStoreMapper signKeyStoreMapper,
                              CertService certService) {
        this.signCertMapper = signCertMapper;
        this.signKeyStoreMapper = signKeyStoreMapper;
        this.certService = certService;
    }

    @Override
    public ManagedSignMaterial loadBySealId(Long sealId) {
        SignCert cert = signCertMapper.selectActiveBySealId(sealId);
        if (cert == null || "1".equals(cert.getStatus())) {
            return null;
        }
        SignKeyStore keyStore = signKeyStoreMapper.selectByKeyRef(cert.getKeyRef());
        if (keyStore == null || "1".equals(keyStore.getStatus()) || keyStore.getEncryptedKeyBlob() == null) {
            return null;
        }

        ManagedSignMaterial material = new ManagedSignMaterial();
        material.setCertId(cert.getCertId());
        material.setKeyRef(cert.getKeyRef());
        material.setPrivateKey(certService.loadPrivateKey(keyStore.getEncryptedKeyBlob()));
        material.setCertificateChain(loadCertificateChain(cert));
        return material;
    }

    private List<Certificate> loadCertificateChain(SignCert cert) {
        List<Certificate> certificates = new ArrayList<>(certService.parseCertificates(cert.getCertPem()));
        certificates.addAll(certService.parseCertificates(cert.getChainPem()));
        return certificates;
    }
}
