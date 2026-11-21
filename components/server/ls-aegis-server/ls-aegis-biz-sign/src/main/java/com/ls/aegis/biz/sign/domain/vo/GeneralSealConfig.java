package com.ls.aegis.biz.sign.domain.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 合并签章中的「普通章」配置
 */
public class GeneralSealConfig {

    /** 印章ID，必填 */
    @NotNull(message = "印章ID不能为空")
    private Long sealId;

    /** 印章模式白名单 */
    @Pattern(regexp = "^(single|fixed|custom)$", message = "普通章模式仅支持 single/fixed/custom")
    private String mode;

    /** 印章坐标列表 — 至少一个位置 */
    @NotEmpty(message = "普通章签章位置不能为空")
    @Size(max = 100, message = "单次普通章位置数量过大（上限100）")
    @Valid
    private List<Position> positions;

    public Long getSealId() {
        return sealId;
    }

    public void setSealId(Long sealId) {
        this.sealId = sealId;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public List<Position> getPositions() {
        return positions;
    }

    public void setPositions(List<Position> positions) {
        this.positions = positions;
    }
}
