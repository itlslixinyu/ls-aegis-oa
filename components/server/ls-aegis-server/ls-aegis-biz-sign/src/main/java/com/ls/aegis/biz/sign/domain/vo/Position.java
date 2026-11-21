package com.ls.aegis.biz.sign.domain.vo;

import jakarta.validation.constraints.*;

/**
 * 合并签章中通用坐标对象
 * <p>
 * 【安全】原基本类型 int/float 改为包装类型 Integer/Float，
 * 否则未传参时默认值0会"绕过"@NotNull校验。
 */
public class Position {

    private static final float MAX_COORD = 14400f;
    private static final float MAX_SIZE = 2000f;

    /** 页码（从1开始） */
    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码必须 ≥ 1")
    @Max(value = 100000, message = "页码过大，最大支持 100000")
    private Integer page;

    /** X 坐标（pt） */
    @NotNull(message = "X 坐标不能为空")
    @DecimalMin(value = "0.0", message = "X 坐标不能为负数")
    @DecimalMax(value = "" + MAX_COORD, message = "X 坐标超出上限 " + MAX_COORD + "pt")
    private Float x;

    /** Y 坐标（pt） */
    @NotNull(message = "Y 坐标不能为空")
    @DecimalMin(value = "0.0", message = "Y 坐标不能为负数")
    @DecimalMax(value = "" + MAX_COORD, message = "Y 坐标超出上限 " + MAX_COORD + "pt")
    private Float y;

    /** 印章宽度（pt）*/
    @DecimalMin(value = "0.0", message = "印章宽度不能为负数")
    @DecimalMax(value = "" + MAX_SIZE, message = "印章宽度超出上限 " + MAX_SIZE + "pt")
    private Float width;

    /** 印章高度（pt）*/
    @DecimalMin(value = "0.0", message = "印章高度不能为负数")
    @DecimalMax(value = "" + MAX_SIZE, message = "印章高度超出上限 " + MAX_SIZE + "pt")
    private Float height;

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Float getX() {
        return x;
    }

    public void setX(Float x) {
        this.x = x;
    }

    public Float getY() {
        return y;
    }

    public void setY(Float y) {
        this.y = y;
    }

    public Float getWidth() {
        return width;
    }

    public void setWidth(Float width) {
        this.width = width;
    }

    public Float getHeight() {
        return height;
    }

    public void setHeight(Float height) {
        this.height = height;
    }
}
