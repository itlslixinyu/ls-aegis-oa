package com.ls.aegis.biz.sign.domain.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;

/**
 * 合并签章中的「骑缝章」配置
 * <p>
 * 【安全】所有 int/float 基本类型改成包装类型（否则没传会默认0通过校验）
 * <p>
 * 【注意】字段名 xPosition/yPosition 必须显式 {@link JsonProperty}：
 * JavaBeans 对 getXPosition/getYPosition 会推导出 XPosition/YPosition，
 * 导致前端 camelCase（xPosition）无法反序列化，校验报「X/Y 坐标不能为空」。
 */
public class SpanSealConfig {

    /** 印章ID — 必填 */
    @NotNull(message = "骑缝章印章ID不能为空")
    private Long sealId;

    /** 起始页（从1开始） */
    @NotNull(message = "骑缝章起始页不能为空")
    @Min(value = 1, message = "骑缝章起始页必须 ≥ 1")
    @Max(value = 100000, message = "骑缝章起始页过大")
    private Integer startPage;

    /** 结束页（必须 ≥ 起始页） */
    @NotNull(message = "骑缝章结束页不能为空")
    @Min(value = 1, message = "骑缝章结束页必须 ≥ 1")
    @Max(value = 100000, message = "骑缝章结束页过大")
    private Integer endPage;

    /**
     * X 坐标（pt，≥ 0）。当前渲染固定右缘，前端可传 0 占位。
     */
    @JsonProperty("xPosition")
    @NotNull(message = "骑缝章 X 坐标不能为空")
    @DecimalMin(value = "0.0", message = "骑缝章 X 坐标不能为负数")
    @DecimalMax(value = "14400.0", message = "骑缝章 X 坐标超出上限 14400pt")
    private Float xPosition;

    /** Y 坐标（pt，≥ 0，自页顶向下的印章中心） */
    @JsonProperty("yPosition")
    @NotNull(message = "骑缝章 Y 坐标不能为空")
    @DecimalMin(value = "0.0", message = "骑缝章 Y 坐标不能为负数")
    @DecimalMax(value = "14400.0", message = "骑缝章 Y 坐标超出上限 14400pt")
    private Float yPosition;

    /** 印章尺寸（pt，≥ 0） */
    @NotNull(message = "骑缝章印章尺寸不能为空")
    @DecimalMin(value = "0.0", message = "骑缝章印章尺寸不能为负数")
    @DecimalMax(value = "2000.0", message = "骑缝章印章尺寸超出上限 2000pt")
    private Float sealSize;

    public Float getSealSize() {
        return sealSize;
    }

    public void setSealSize(Float sealSize) {
        this.sealSize = sealSize;
    }

    public Long getSealId() {
        return sealId;
    }

    public void setSealId(Long sealId) {
        this.sealId = sealId;
    }

    public Integer getStartPage() {
        return startPage;
    }

    public void setStartPage(Integer startPage) {
        this.startPage = startPage;
    }

    public Integer getEndPage() {
        return endPage;
    }

    public void setEndPage(Integer endPage) {
        this.endPage = endPage;
    }

    public Float getXPosition() {
        return xPosition;
    }

    public void setXPosition(Float xPosition) {
        this.xPosition = xPosition;
    }

    public Float getYPosition() {
        return yPosition;
    }

    public void setYPosition(Float yPosition) {
        this.yPosition = yPosition;
    }
}
