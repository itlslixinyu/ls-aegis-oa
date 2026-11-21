package com.ls.aegis.biz.sign.domain;

import com.ls.aegis.mybatis.base.model.entity.XmlMapperDO;

import java.util.Date;

/**
 * 证书吊销记录。
 */
public class SignCertRevocation extends XmlMapperDO {

    private static final long serialVersionUID = 1L;

    private Long revokeId;
    private Long certId;
    private String revokeReason;
    private Date revokeTime;
    private Long operatorId;
    private Long crlNo;

    public Long getRevokeId() { return revokeId; }
    public void setRevokeId(Long revokeId) { this.revokeId = revokeId; }
    public Long getCertId() { return certId; }
    public void setCertId(Long certId) { this.certId = certId; }
    public String getRevokeReason() { return revokeReason; }
    public void setRevokeReason(String revokeReason) { this.revokeReason = revokeReason; }
    public Date getRevokeTime() { return revokeTime; }
    public void setRevokeTime(Date revokeTime) { this.revokeTime = revokeTime; }
    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }
    public Long getCrlNo() { return crlNo; }
    public void setCrlNo(Long crlNo) { this.crlNo = crlNo; }
}
