package com.ls.aegis.biz.sign.domain.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.io.Serializable;
import java.util.List;

/**
 * 签章请求 VO
 * <p>
 * 【安全】所有签章入参必须经过 JSR303 校验，防止非法页码、负数坐标、空印章ID等
 * 导致 PDF 解析异常、数组越界或 OOM。
 */
public class SignRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 项目ID（可为空，兼容无项目场景） */
    private Long projectId;

    /** 印章ID — 普通签章必填 */
    private Long sealId;

    /** 源文件ID — 存量 sign_file；与 sysFileId 二选一 */
    private Long fileId;

    /** 平台 sys_file.id，选 PDF 走 /system/common/file 时传入 */
    private Long sysFileId;

    /**
     * 签章模式白名单：
     * single=单页盖章 / fixed=定页定位 / custom=自定义坐标 / span=骑缝章
     * 兼容旧值：multi continuous
     */
    @NotBlank(message = "签章模式不能为空")
    @Pattern(regexp = "^(single|fixed|custom|span|multi|continuous)$",
             message = "签章模式非法，仅支持 single/fixed/custom/span/multi/continuous")
    private String signMode;

    /** 是否包含时间戳(0否 1是)，接受空 */
    @Pattern(regexp = "^[01]?$", message = "hasTimestamp 仅允许 0、1 或空")
    private String hasTimestamp;

    /** 签章位置列表 — 非骑缝模式时必填；骑缝模式允许空（由 spanConfig 控制） */
    @Valid
    private List<@NotNull(message = "签章位置项不能为null") SignPosition> positions;

    /** 跨页配置（骑缝章使用） */
    @Valid
    private SpanConfig spanConfig;

    /** 是否使用平台托管证书(0否 1是) */
    @Pattern(regexp = "^[01]?$", message = "useManagedKey 仅允许 0、1 或空")
    private String useManagedKey;

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

    public String getSignMode() {
        return signMode;
    }

    public void setSignMode(String signMode) {
        this.signMode = signMode;
    }

    public String getHasTimestamp() {
        return hasTimestamp;
    }

    public void setHasTimestamp(String hasTimestamp) {
        this.hasTimestamp = hasTimestamp;
    }

    public List<SignPosition> getPositions() {
        return positions;
    }

    public void setPositions(List<SignPosition> positions) {
        this.positions = positions;
    }

    public SpanConfig getSpanConfig() {
        return spanConfig;
    }

    public void setSpanConfig(SpanConfig spanConfig) {
        this.spanConfig = spanConfig;
    }

    public String getUseManagedKey() {
        return useManagedKey;
    }

    public void setUseManagedKey(String useManagedKey) {
        this.useManagedKey = useManagedKey;
    }
}
