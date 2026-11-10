package com.ls.aegis.biz.sign.domain.vo;

import java.io.Serializable;
import java.util.Date;

/**
 * 签章结果 VO
 */
public class SignResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private String recordId;
    private Long signedFileId;
    private String signedFileName;
    private String downloadUrl;
    private Date signTime;
    private String certInfo;

    public static SignResult success(Long signedFileId) {
        SignResult result = new SignResult();
        result.setSignedFileId(signedFileId);
        return result;
    }

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    public Long getSignedFileId() {
        return signedFileId;
    }

    public void setSignedFileId(Long signedFileId) {
        this.signedFileId = signedFileId;
    }

    public String getSignedFileName() {
        return signedFileName;
    }

    public void setSignedFileName(String signedFileName) {
        this.signedFileName = signedFileName;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public Date getSignTime() {
        return signTime;
    }

    public void setSignTime(Date signTime) {
        this.signTime = signTime;
    }

    public String getCertInfo() {
        return certInfo;
    }

    public void setCertInfo(String certInfo) {
        this.certInfo = certInfo;
    }
}
