package com.ls.aegis.biz.sign.service;

import com.ls.aegis.biz.sign.domain.SignCert;
import com.ls.aegis.biz.sign.domain.vo.ManagedCertBundle;

/**
 * 平台托管证书服务抽象。
 */
public interface ICertificateService {

    SignCert issueForSeal(Long sealId, String sealName, String companyName) throws Exception;

    ManagedCertBundle issueManagedCert(Long sealId, String sealName, String companyName) throws Exception;

    boolean ensureCertUsable(SignCert cert);
}
