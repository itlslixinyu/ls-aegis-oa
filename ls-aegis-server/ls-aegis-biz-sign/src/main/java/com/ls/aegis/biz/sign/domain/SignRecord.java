package com.ls.aegis.biz.sign.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ls.aegis.mybatis.base.model.entity.XmlMapperDO;

import java.util.Date;

/**
 * 签章记录表 sign_record
 */
public class SignRecord extends XmlMapperDO implements com.ls.aegis.biz.sign.utils.SignDataScopeHelper.DeptAware {

    private static final long serialVersionUID = 1L;

    private String recordId;
    private Long fileId;
    private Long sealId;
    private Long userId;
    private Long projectId;
    /** 租户ID（DataScope） */
    private Long tenantId;
    /** 部门ID（DataScope） */
    private Long deptId;
    /** 签章模式(single/fixed/custom/span/unified)，兼容旧值 multi/continuous */

    private String signMode;
    /** 签章位置JSON */
    private String positions;
    /** 是否包含时间戳(0否 1是) */

    private String hasTimestamp;
    /** 跨页配置JSON */
    private String spanConfig;

    private String clientIp;

    private String ipLocation;
    private String osVersion;
    private String browserVersion;

    private String status;
    private Long signedFileId;
    private Long certId;
    private String serialNo;
    private String fingerprintSha256;
    private String verifyStatus;
    private String signHash;
    private String pageFingerprint;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")

    private Date signTime;

    // --- 扩展字段 ---

    private String projectName;

    private String sealName;

    private String userName;
    private String nickName;

    private String fileName;

    private String signedFileName;

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public Long getSealId() {
        return sealId;
    }

    public void setSealId(Long sealId) {
        this.sealId = sealId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    @Override
    public Long getTenantId() {
        return tenantId;
    }

    @Override
    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    @Override
    public Long getDeptId() {
        return deptId;
    }

    @Override
    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }

    public String getSignMode() {
        return signMode;
    }

    public void setSignMode(String signMode) {
        this.signMode = signMode;
    }

    public String getPositions() {
        return positions;
    }

    public void setPositions(String positions) {
        this.positions = positions;
    }

    public String getHasTimestamp() {
        return hasTimestamp;
    }

    public void setHasTimestamp(String hasTimestamp) {
        this.hasTimestamp = hasTimestamp;
    }

    public String getSpanConfig() {
        return spanConfig;
    }

    public void setSpanConfig(String spanConfig) {
        this.spanConfig = spanConfig;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public String getIpLocation() {
        return ipLocation;
    }

    public void setIpLocation(String ipLocation) {
        this.ipLocation = ipLocation;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }

    public String getBrowserVersion() {
        return browserVersion;
    }

    public void setBrowserVersion(String browserVersion) {
        this.browserVersion = browserVersion;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getSignedFileId() {
        return signedFileId;
    }

    public void setSignedFileId(Long signedFileId) {
        this.signedFileId = signedFileId;
    }

    public Long getCertId() {
        return certId;
    }

    public void setCertId(Long certId) {
        this.certId = certId;
    }

    public String getSerialNo() {
        return serialNo;
    }

    public void setSerialNo(String serialNo) {
        this.serialNo = serialNo;
    }

    public String getFingerprintSha256() {
        return fingerprintSha256;
    }

    public void setFingerprintSha256(String fingerprintSha256) {
        this.fingerprintSha256 = fingerprintSha256;
    }

    public String getVerifyStatus() {
        return verifyStatus;
    }

    public void setVerifyStatus(String verifyStatus) {
        this.verifyStatus = verifyStatus;
    }

    public String getSignHash() {
        return signHash;
    }

    public void setSignHash(String signHash) {
        this.signHash = signHash;
    }

    public String getPageFingerprint() {
        return pageFingerprint;
    }

    public void setPageFingerprint(String pageFingerprint) {
        this.pageFingerprint = pageFingerprint;
    }

    public Date getSignTime() {
        return signTime;
    }

    public void setSignTime(Date signTime) {
        this.signTime = signTime;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getSealName() {
        return sealName;
    }

    public void setSealName(String sealName) {
        this.sealName = sealName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getSignedFileName() {
        return signedFileName;
    }

    public void setSignedFileName(String signedFileName) {
        this.signedFileName = signedFileName;
    }
}
