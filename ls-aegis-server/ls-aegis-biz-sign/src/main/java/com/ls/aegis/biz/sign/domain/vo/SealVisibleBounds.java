package com.ls.aegis.biz.sign.domain.vo;

public class SealVisibleBounds {

    private final int imageWidth;
    private final int imageHeight;
    private final int contentLeft;
    private final int contentTop;
    private final int contentRight;
    private final int contentBottom;
    private final int contentWidth;
    private final int contentHeight;
    private final double visibleWidthRatio;
    private final double visibleHeightRatio;

    private SealVisibleBounds(int imageWidth, int imageHeight, int contentLeft, int contentTop,
                              int contentRight, int contentBottom, int contentWidth, int contentHeight,
                              double visibleWidthRatio, double visibleHeightRatio) {
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.contentLeft = contentLeft;
        this.contentTop = contentTop;
        this.contentRight = contentRight;
        this.contentBottom = contentBottom;
        this.contentWidth = contentWidth;
        this.contentHeight = contentHeight;
        this.visibleWidthRatio = visibleWidthRatio;
        this.visibleHeightRatio = visibleHeightRatio;
    }

    public static SealVisibleBounds of(int imageWidth, int imageHeight, int contentLeft, int contentTop,
                                       int contentRight, int contentBottom) {
        int contentWidth = contentRight - contentLeft + 1;
        int contentHeight = contentBottom - contentTop + 1;
        double visibleWidthRatio = imageWidth > 0 ? (double) contentWidth / imageWidth : 0d;
        double visibleHeightRatio = imageHeight > 0 ? (double) contentHeight / imageHeight : 0d;
        return new SealVisibleBounds(
                imageWidth,
                imageHeight,
                contentLeft,
                contentTop,
                contentRight,
                contentBottom,
                contentWidth,
                contentHeight,
                visibleWidthRatio,
                visibleHeightRatio
        );
    }

    public int getImageWidth() {
        return imageWidth;
    }

    public int getImageHeight() {
        return imageHeight;
    }

    public int getContentLeft() {
        return contentLeft;
    }

    public int getContentTop() {
        return contentTop;
    }

    public int getContentRight() {
        return contentRight;
    }

    public int getContentBottom() {
        return contentBottom;
    }

    public int getContentWidth() {
        return contentWidth;
    }

    public int getContentHeight() {
        return contentHeight;
    }

    public double getVisibleWidthRatio() {
        return visibleWidthRatio;
    }

    public double getVisibleHeightRatio() {
        return visibleHeightRatio;
    }
}
