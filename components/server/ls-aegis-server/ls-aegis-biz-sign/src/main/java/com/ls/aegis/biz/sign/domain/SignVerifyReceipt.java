package com.ls.aegis.biz.sign.domain;

import com.ls.aegis.mybatis.base.model.entity.XmlMapperDO;

import java.util.Date;

/**
 * 验签回单快照。
 */
public class SignVerifyReceipt extends XmlMapperDO {

    private static final long serialVersionUID = 1L;

    private Long receiptId;
    private String recordId;
    private Long originFileId;
    private Long signedFileId;
    private String verifyStatus;
    private Long certId;
    private String serialNo;
    private String fingerprintSha256;
    private String receiptNo;
    private String receiptFilePath;
    private Date verifiedAt;

    public Long getReceiptId() { return receiptId; }
    public void setReceiptId(Long receiptId) { this.receiptId = receiptId; }
    public String getRecordId() { return recordId; }
    public void setRecordId(String recordId) { this.recordId = recordId; }
    public Long getOriginFileId() { return originFileId; }
    public void setOriginFileId(Long originFileId) { this.originFileId = originFileId; }
    public Long getSignedFileId() { return signedFileId; }
    public void setSignedFileId(Long signedFileId) { this.signedFileId = signedFileId; }
    public String getVerifyStatus() { return verifyStatus; }
    public void setVerifyStatus(String verifyStatus) { this.verifyStatus = verifyStatus; }
    public Long getCertId() { return certId; }
    public void setCertId(Long certId) { this.certId = certId; }
    public String getSerialNo() { return serialNo; }
    public void setSerialNo(String serialNo) { this.serialNo = serialNo; }
    public String getFingerprintSha256() { return fingerprintSha256; }
    public void setFingerprintSha256(String fingerprintSha256) { this.fingerprintSha256 = fingerprintSha256; }
    public String getReceiptNo() { return receiptNo; }
    public void setReceiptNo(String receiptNo) { this.receiptNo = receiptNo; }
    public String getReceiptFilePath() { return receiptFilePath; }
    public void setReceiptFilePath(String receiptFilePath) { this.receiptFilePath = receiptFilePath; }
    public Date getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(Date verifiedAt) { this.verifiedAt = verifiedAt; }
}
