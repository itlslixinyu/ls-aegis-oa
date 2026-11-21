package com.ls.aegis.biz.sign.domain.vo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.io.Serializable;

/**
 * 跨页骑缝章配置 VO
 */
public class SpanConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 起始页（从1开始） */
    @NotNull(message = "骑缝章起始页不能为空")
    @Min(value = 1, message = "骑缝章起始页必须 ≥ 1")
    @Max(value = 100000, message = "骑缝章起始页过大")
    private Integer startPage;

    /** 结束页 */
    @NotNull(message = "骑缝章结束页不能为空")
    @Min(value = 1, message = "骑缝章结束页必须 ≥ 1")
    @Max(value = 100000, message = "骑缝章结束页过大")
    private Integer endPage;

    /** 方向：horizontal / vertical */
    @Pattern(regexp = "^(horizontal|vertical)?$", message = "骑缝章方向仅允许 horizontal/vertical")
    private String direction;

    /** 位置：left / right / top / bottom */
    @Pattern(regexp = "^(left|right|top|bottom)?$",
             message = "骑缝章位置仅允许 left/right/top/bottom")
    private String position;

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

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }
}
