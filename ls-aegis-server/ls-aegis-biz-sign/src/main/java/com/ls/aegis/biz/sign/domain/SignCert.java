package com.ls.aegis.biz.sign.domain;

import com.ls.aegis.mybatis.base.model.entity.XmlMapperDO;

import java.util.Date;

/**
 * 平台托管证书。
 */
public class SignCert extends XmlMapperDO {

    private static final long serialVersionUID = 1L;

    private Long certId;
    private Long orgId;
    private Long projectId;
    private Long sealId;
    private String certType;
    private String subjectDn;
    private String issuerDn;
    private String serialNo;
    private String fingerprintSha256;
    private Date notBefore;
    private Date notAfter;
    private String status;
    private String certPem;
    private String chainPem;
    private String keyRef;
    private Date issuedAt;
    private Date revokedAt;

    public Long getCertId() { return certId; }
    public void setCertId(Long certId) { this.certId = certId; }
    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public Long getSealId() { return sealId; }
    public void setSealId(Long sealId) { this.sealId = sealId; }
    public String getCertType() { return certType; }
    public void setCertType(String certType) { this.certType = certType; }
    public String getSubjectDn() { return subjectDn; }
    public void setSubjectDn(String subjectDn) { this.subjectDn = subjectDn; }
    public String getIssuerDn() { return issuerDn; }
    public void setIssuerDn(String issuerDn) { this.issuerDn = issuerDn; }
    public String getSerialNo() { return serialNo; }
    public void setSerialNo(String serialNo) { this.serialNo = serialNo; }
    public String getFingerprintSha256() { return fingerprintSha256; }
    public void setFingerprintSha256(String fingerprintSha256) { this.fingerprintSha256 = fingerprintSha256; }
    public Date getNotBefore() { return notBefore; }
    public void setNotBefore(Date notBefore) { this.notBefore = notBefore; }
    public Date getNotAfter() { return notAfter; }
    public void setNotAfter(Date notAfter) { this.notAfter = notAfter; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCertPem() { return certPem; }
    public void setCertPem(String certPem) { this.certPem = certPem; }
    public String getChainPem() { return chainPem; }
    public void setChainPem(String chainPem) { this.chainPem = chainPem; }
    public String getKeyRef() { return keyRef; }
    public void setKeyRef(String keyRef) { this.keyRef = keyRef; }
    public Date getIssuedAt() { return issuedAt; }
    public void setIssuedAt(Date issuedAt) { this.issuedAt = issuedAt; }
    public Date getRevokedAt() { return revokedAt; }
    public void setRevokedAt(Date revokedAt) { this.revokedAt = revokedAt; }
}
