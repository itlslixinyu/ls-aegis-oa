package com.ls.aegis.biz.sign.domain;

import com.ls.aegis.mybatis.base.model.entity.XmlMapperDO;

/**
 * 印章信息表 sign_seal
 */
public class SignSeal extends XmlMapperDO {

    private static final long serialVersionUID = 1L;

    private Long sealId;
    private String sealName;
    private String sealCode;
    private String sealCategory;
    private String sealType;
    /** 启用日期 yyyy-MM-dd */
    private String enableDate;
    private String orgName;
    private Long deptId;
    private String sealFilePath;
    private Integer defaultSizeMm;
    private String sizeOptions;
    private String certPath;
    private String keyPath;
    private String certInfo;
    /** 证书密码（加密存储） */
    private String certPassword;
    private Long certId;
    private String keyRef;
    private String certBindMode;
    /** 状态(0启用 1禁用) */
    private String status;
    private String delFlag;

    // --- 扩展字段 ---
    private String deptName;
    private String categoryLabel;

    public Long getSealId() {
        return sealId;
    }

    public void setSealId(Long sealId) {
        this.sealId = sealId;
    }

    public String getSealName() {
        return sealName;
    }

    public void setSealName(String sealName) {
        this.sealName = sealName;
    }

    public String getSealCode() {
        return sealCode;
    }

    public void setSealCode(String sealCode) {
        this.sealCode = sealCode;
    }

    public String getSealCategory() {
        return sealCategory;
    }

    public void setSealCategory(String sealCategory) {
        this.sealCategory = sealCategory;
    }

    public String getSealType() {
        return sealType;
    }

    public void setSealType(String sealType) {
        this.sealType = sealType;
    }

    public String getEnableDate() {
        return enableDate;
    }

    public void setEnableDate(String enableDate) {
        this.enableDate = enableDate;
    }

    public String getOrgName() {
        return orgName;
    }

    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }

    public Long getDeptId() {
        return deptId;
    }

    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }

    public String getSealFilePath() {
        return sealFilePath;
    }

    public void setSealFilePath(String sealFilePath) {
        this.sealFilePath = sealFilePath;
    }

    public Integer getDefaultSizeMm() {
        return defaultSizeMm;
    }

    public void setDefaultSizeMm(Integer defaultSizeMm) {
        this.defaultSizeMm = defaultSizeMm;
    }

    public String getSizeOptions() {
        return sizeOptions;
    }

    public void setSizeOptions(String sizeOptions) {
        this.sizeOptions = sizeOptions;
    }

    public String getCertPath() {
        return certPath;
    }

    public void setCertPath(String certPath) {
        this.certPath = certPath;
    }

    public String getKeyPath() {
        return keyPath;
    }

    public void setKeyPath(String keyPath) {
        this.keyPath = keyPath;
    }

    public String getCertInfo() {
        return certInfo;
    }

    public void setCertInfo(String certInfo) {
        this.certInfo = certInfo;
    }

    public String getCertPassword() {
        return certPassword;
    }

    public void setCertPassword(String certPassword) {
        this.certPassword = certPassword;
    }

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

    public String getCertBindMode() {
        return certBindMode;
    }

    public void setCertBindMode(String certBindMode) {
        this.certBindMode = certBindMode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(String delFlag) {
        this.delFlag = delFlag;
    }

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }

    public String getCategoryLabel() {
        return categoryLabel;
    }

    public void setCategoryLabel(String categoryLabel) {
        this.categoryLabel = categoryLabel;
    }
}
