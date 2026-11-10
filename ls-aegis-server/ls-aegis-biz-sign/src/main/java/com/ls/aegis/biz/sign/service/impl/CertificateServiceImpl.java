package com.ls.aegis.biz.sign.service.impl;

import com.ls.aegis.biz.sign.domain.SignCert;
import com.ls.aegis.biz.sign.mapper.SignCertRevocationMapper;
import com.ls.aegis.biz.sign.domain.SignKeyStore;
import com.ls.aegis.biz.sign.domain.vo.ManagedCertBundle;
import com.ls.aegis.biz.sign.service.ICertificateService;
import com.ls.aegis.biz.sign.service.provider.RevocationChecker;
import com.ls.aegis.mybatis.base.model.entity.XmlMapperDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Service
public class CertificateServiceImpl implements ICertificateService, RevocationChecker {

    private final CertService certService;
    private final SignCertRevocationMapper revocationMapper;

    public CertificateServiceImpl(CertService certService) {
        this(certService, null);
    }

    @Autowired
    public CertificateServiceImpl(CertService certService, SignCertRevocationMapper revocationMapper) {
        this.certService = certService;
        this.revocationMapper = revocationMapper;
    }

    @Override
    public SignCert issueForSeal(Long sealId, String sealName, String companyName) throws Exception {
        return issueManagedCert(sealId, sealName, companyName).getCert();
    }

    @Override
    public ManagedCertBundle issueManagedCert(Long sealId, String sealName, String companyName) throws Exception {
        KeyPair keyPair = certService.generateKeyPair();
        X509Certificate certificate = certService.generateSelfSignedCertificate(
                keyPair,
                "CN=" + sealName + ",O=" + companyName + ",OU=PrivateCA,SN=" + sealId,
                365);
        Date issuedAt = new Date();
        String keyRef = UUID.randomUUID().toString();

        SignCert cert = new SignCert();
        cert.setSealId(sealId);
        cert.setCertType("SEAL");
        cert.setSubjectDn(certificate.getSubjectX500Principal().getName());
        cert.setIssuerDn(certificate.getIssuerX500Principal().getName());
        cert.setSerialNo(certificate.getSerialNumber().toString(16));
        cert.setFingerprintSha256(certService.sha256Fingerprint(certificate));
        cert.setCertPem(certService.toPem(certificate));
        cert.setKeyRef(keyRef);
        cert.setStatus("0");
        cert.setNotBefore(certificate.getNotBefore());
        cert.setNotAfter(certificate.getNotAfter());
        cert.setIssuedAt(issuedAt);
        cert.setCreateBy("system");
        cert.setCreateTime(XmlMapperDO.from(issuedAt));
        cert.setUpdateBy("system");
        cert.setUpdateTime(XmlMapperDO.from(issuedAt));

        SignKeyStore keyStore = new SignKeyStore();
        keyStore.setKeyRef(keyRef);
        keyStore.setStorageType("SERVER_ENCRYPTED");
        keyStore.setEncryptedKeyBlob(Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()));
        keyStore.setEncryptVersion("plain-v1");
        keyStore.setStatus("0");
        keyStore.setCreateBy("system");
        keyStore.setCreateTime(XmlMapperDO.from(issuedAt));
        keyStore.setUpdateBy("system");
        keyStore.setUpdateTime(XmlMapperDO.from(issuedAt));

        ManagedCertBundle bundle = new ManagedCertBundle();
        bundle.setCert(cert);
        bundle.setKeyStore(keyStore);
        return bundle;
    }

    @Override
    public boolean ensureCertUsable(SignCert cert) {
        Date now = new Date();
        return cert != null
                && "0".equals(cert.getStatus())
                && cert.getNotAfter() != null
                && cert.getNotAfter().after(now)
                && (cert.getNotBefore() == null || !cert.getNotBefore().after(now))
                && !isRevoked(cert.getCertId());
    }

    @Override
    public boolean isRevoked(Long certId) {
        if (certId == null || revocationMapper == null) {
            return false;
        }
        var revocations = revocationMapper.selectByCertId(certId);
        return revocations != null && !revocations.isEmpty();
    }
}
