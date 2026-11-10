package com.ls.aegis.biz.sign.domain.vo;

import jakarta.validation.Valid;

import java.util.List;

/**
 * 合并签章请求 — 一次性完成普通章+骑缝章+时间戳
 * <p>【安全】所有字段均通过 JSR303 做范围与必填校验
 */
public class UnifiedSignRequest {

    /** 源文件ID — 存量 sign_file；与 sysFileId 二选一 */
    private Long fileId;

    /** 平台 sys_file.id，选 PDF 走 /system/common/file 时传入 */
    private Long sysFileId;

    private Long projectId;

    /** 公共印章ID（当 generalSeals 没单独指定 sealId 时使用该值，可为空） */
    private Long sealId;

    /** 普通章配置（允许空数组） */
    @Valid
    private List<GeneralSealConfig> generalSeals;

    /** 骑缝章配置（允许空数组） */
    @Valid
    private List<SpanSealConfig> spanSeals;

    private Boolean hasTimestamp;

    private Boolean appendMode;

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public Long getSysFileId() {
        return sysFileId;
    }

    public void setSysFileId(Long sysFileId) {
        this.sysFileId = sysFileId;
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

    public List<GeneralSealConfig> getGeneralSeals() {
        return generalSeals;
    }

    public void setGeneralSeals(List<GeneralSealConfig> generalSeals) {
        this.generalSeals = generalSeals;
    }

    public List<SpanSealConfig> getSpanSeals() {
        return spanSeals;
    }

    public void setSpanSeals(List<SpanSealConfig> spanSeals) {
        this.spanSeals = spanSeals;
    }

    public Boolean getHasTimestamp() {
        return hasTimestamp;
    }

    public void setHasTimestamp(Boolean hasTimestamp) {
        this.hasTimestamp = hasTimestamp;
    }

    public Boolean getAppendMode() {
        return appendMode;
    }

    public void setAppendMode(Boolean appendMode) {
        this.appendMode = appendMode;
    }
}
