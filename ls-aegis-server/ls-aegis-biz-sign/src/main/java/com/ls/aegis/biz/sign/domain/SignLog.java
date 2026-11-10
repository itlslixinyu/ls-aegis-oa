package com.ls.aegis.biz.sign.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ls.aegis.mybatis.base.model.entity.XmlMapperDO;

import java.util.Date;

/**
 * 签章日志表 sign_log
 */
public class SignLog extends XmlMapperDO implements com.ls.aegis.biz.sign.utils.SignDataScopeHelper.DeptAware {

    private static final long serialVersionUID = 1L;

    private Long logId;
    private Long projectId;
    private Long sealId;
    private Long userId;
    /** 租户ID（DataScope） */
    private Long tenantId;
    /** 部门ID（DataScope） */
    private Long deptId;
    private String fileHash;
    private String clientIp;
    private String ipLocation;
    private Long outputFileId;
    private Long certId;
    private String actionType;
    private String resultCode;
    private String traceId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date signTime;

    // --- 扩展字段 ---
    private String projectName;
    private String sealName;
    private String userName;
    private String nickName;

    public Long getLogId() {
        return logId;
    }

    public void setLogId(Long logId) {
        this.logId = logId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
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

    public String getFileHash() {
        return fileHash;
    }

    public void setFileHash(String fileHash) {
        this.fileHash = fileHash;
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

    public Long getOutputFileId() {
        return outputFileId;
    }

    public void setOutputFileId(Long outputFileId) {
        this.outputFileId = outputFileId;
    }

    public Long getCertId() {
        return certId;
    }

    public void setCertId(Long certId) {
        this.certId = certId;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getResultCode() {
        return resultCode;
    }

    public void setResultCode(String resultCode) {
        this.resultCode = resultCode;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
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
}
