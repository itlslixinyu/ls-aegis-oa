package com.ls.aegis.biz.sign.domain;

import com.ls.aegis.mybatis.base.model.entity.XmlMapperDO;

/**
 * 印章提取记录表 sign_seal_extract
 */
public class SignSealExtract extends XmlMapperDO {

    private static final long serialVersionUID = 1L;

    private Long sealId;
    private Long originalFileId;
    private String originalFileName;
    private Long originalFileSize;
    private Long extractFileId;
    private String extractFileName;
    private String extractFilePath;
    private Long extractFileSize;
    private Integer defaultSizeMm;
    private Long createUserId;
    private String thumbnailFilePath;

    // --- 扩展字段 ---
    private String sealName;
    private String extractFileDisplayName;
    private String extractFileUrl;
    private String originalFileUrl;
    private String thumbnailFileUrl;

    public Long getSealId() {
        return sealId;
    }

    public void setSealId(Long sealId) {
        this.sealId = sealId;
    }

    public Long getOriginalFileId() {
        return originalFileId;
    }

    public void setOriginalFileId(Long originalFileId) {
        this.originalFileId = originalFileId;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public Long getOriginalFileSize() {
        return originalFileSize;
    }

    public void setOriginalFileSize(Long originalFileSize) {
        this.originalFileSize = originalFileSize;
    }

    public Long getExtractFileId() {
        return extractFileId;
    }

    public void setExtractFileId(Long extractFileId) {
        this.extractFileId = extractFileId;
    }

    public String getExtractFileName() {
        return extractFileName;
    }

    public void setExtractFileName(String extractFileName) {
        this.extractFileName = extractFileName;
    }

    public String getExtractFilePath() {
        return extractFilePath;
    }

    public void setExtractFilePath(String extractFilePath) {
        this.extractFilePath = extractFilePath;
    }

    public Long getExtractFileSize() {
        return extractFileSize;
    }

    public void setExtractFileSize(Long extractFileSize) {
        this.extractFileSize = extractFileSize;
    }

    public Integer getDefaultSizeMm() {
        return defaultSizeMm;
    }

    public void setDefaultSizeMm(Integer defaultSizeMm) {
        this.defaultSizeMm = defaultSizeMm;
    }

    public Long getCreateUserId() {
        return createUserId;
    }

    public void setCreateUserId(Long createUserId) {
        this.createUserId = createUserId;
    }

    public String getSealName() {
        return sealName;
    }

    public void setSealName(String sealName) {
        this.sealName = sealName;
    }

    public String getExtractFileDisplayName() {
        return extractFileDisplayName;
    }

    public void setExtractFileDisplayName(String extractFileDisplayName) {
        this.extractFileDisplayName = extractFileDisplayName;
    }

    public String getExtractFileUrl() {
        return extractFileUrl;
    }

    public void setExtractFileUrl(String extractFileUrl) {
        this.extractFileUrl = extractFileUrl;
    }

    public String getOriginalFileUrl() {
        return originalFileUrl;
    }

    public void setOriginalFileUrl(String originalFileUrl) {
        this.originalFileUrl = originalFileUrl;
    }

    public String getThumbnailFilePath() {
        return thumbnailFilePath;
    }

    public void setThumbnailFilePath(String thumbnailFilePath) {
        this.thumbnailFilePath = thumbnailFilePath;
    }

    public String getThumbnailFileUrl() {
        return thumbnailFileUrl;
    }

    public void setThumbnailFileUrl(String thumbnailFileUrl) {
        this.thumbnailFileUrl = thumbnailFileUrl;
    }
}
