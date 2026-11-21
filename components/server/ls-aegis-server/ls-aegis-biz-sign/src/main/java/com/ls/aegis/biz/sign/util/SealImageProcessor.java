package com.ls.aegis.biz.sign.util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 印章图像处理器
 * <p>
 * 提供印章图片的提取和净化算法：
 * <ul>
 *   <li>extractSealFromImage - 从照片中提取印章（红色评分 + 形态学 + 连通域筛选）</li>
 *   <li>sanitizeImage - 简单白色阈值去背景</li>
 *   <li>extractSealCircle - 按圆形提取印章，大小按类型生成</li>
 * </ul>
 */
public class SealImageProcessor {

    /** 白色阈值：RGB 均 >= 此值视为白色/近白色，转为透明 */
    private static final int WHITE_THRESHOLD = 200;

    /** 印章类型到直径(mm)的映射 */
    private static final Map<String, Integer> SEAL_SIZE_MAP = new HashMap<>();
    static {
        SEAL_SIZE_MAP.put("企业法人公章", 42);
        SEAL_SIZE_MAP.put("财务专用章", 38);
        SEAL_SIZE_MAP.put("合同专用章", 42);
        SEAL_SIZE_MAP.put("法人私章", 18);
        SEAL_SIZE_MAP.put("公章", 42);
        SEAL_SIZE_MAP.put("其他", 42);
    }

    /** 默认印章直径(mm) */
    private static final int DEFAULT_SEAL_SIZE_MM = 42;

    /** DPI用于转换mm到像素 */
    private static final int DPI = 300;

    /** 强印章像素评分阈值：高置信度印章区域 */
    private static final int STRONG_SEAL_THRESHOLD = 150;

    /** 弱印章像素评分阈值：需邻居验证的印章区域 */
    private static final int WEAK_SEAL_THRESHOLD = 95;

    /** 边缘印章像素评分阈值：需多邻居支持的边缘区域 */
    private static final int RESCUE_SEAL_THRESHOLD = 75;

    /** 边缘像素邻居评分阈值：用于边缘验证 */
    private static final int NEIGHBOR_VERIFY_THRESHOLD = 110;

    /**
     * 根据印章类型获取目标尺寸(mm)
     */
    public static int getSealSizeByCategory(String sealCategory) {
        return SEAL_SIZE_MAP.getOrDefault(sealCategory, DEFAULT_SEAL_SIZE_MM);
    }

    /**
     * 从图片中提取印章（去背景）- 纯 Java 算法
     * <p>
     * 策略：红色评分 + 轻量修复 + 连通域筛选 + 分级 Alpha
     *
     * @param imageBytes 原始图片字节数组
     * @return 提取后的印章图片（PNG格式，透明背景）
     * @throws Exception 如果无法解析图片文件
     */
    public byte[] extractSealFromImage(byte[] imageBytes) throws Exception {
        BufferedImage source = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (source == null) {
            throw new Exception("无法解析图片文件");
        }

        int width = source.getWidth();
        int height = source.getHeight();
        int[][] redScore = computeRedScore(source);
        boolean[][] sealMask = buildSealMask(redScore, width, height);
        boolean[][] repairedMask = applyMorphology(sealMask, width, height);
        boolean[][] filteredMask = filterConnectedComponents(repairedMask, width, height);

        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        renderSealImage(source, output, filteredMask, redScore, width, height);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(output, "PNG", baos);
        return baos.toByteArray();
    }

    /**
     * 白色背景去除
     * 如果图片已经有Alpha通道（透明PNG），直接返回原图，不做任何处理
     * 否则对白色/近白色背景进行透明化处理
     *
     * @param imageBytes 原始图片字节数组
     * @return 净化后的图片（PNG格式，透明背景）
     */
    public byte[] sanitizeImage(byte[] imageBytes) {
        try {
            BufferedImage original = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (original == null) {
                return imageBytes;
            }

            if (original.getColorModel().hasAlpha()) {
                return imageBytes;
            }

            int w = original.getWidth();
            int h = original.getHeight();
            BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int rgba = original.getRGB(x, y);
                    int r = (rgba >> 16) & 0xFF;
                    int g = (rgba >> 8) & 0xFF;
                    int b = rgba & 0xFF;
                    if (r >= WHITE_THRESHOLD && g >= WHITE_THRESHOLD && b >= WHITE_THRESHOLD) {
                        dst.setRGB(x, y, 0x00FFFFFF);
                    } else {
                        dst.setRGB(x, y, 0xFF000000 | (rgba & 0x00FFFFFF));
                    }
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(dst, "png", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            return imageBytes;
        }
    }

    /**
     * 按圆形提取印章，大小按类型生成
     * <p>
     * 策略：先提取印章区域 → 找到圆形边界 → 按圆形裁剪 → 调整为目标尺寸
     *
     * @param imageBytes 原始图片字节数组
     * @param sealCategory 印章类型（用于确定目标尺寸）
     * @return 提取后的圆形印章图片（PNG格式，透明背景，正方形，边长为目标直径）
     * @throws Exception 如果无法解析图片文件
     */
    public byte[] extractSealCircle(byte[] imageBytes, String sealCategory) throws Exception {
        byte[] extractedBytes = extractSealFromImage(imageBytes);
        BufferedImage extracted = ImageIO.read(new ByteArrayInputStream(extractedBytes));
        if (extracted == null) {
            throw new Exception("无法解析提取后的图片");
        }

        int targetSizeMm = SEAL_SIZE_MAP.getOrDefault(sealCategory, DEFAULT_SEAL_SIZE_MM);
        int targetSizePx = mmToPx(targetSizeMm);

        CircleInfo circle = findSealCircle(extracted);
        if (circle == null) {
            return extractSealFromImage(imageBytes);
        }

        BufferedImage cropped = cropToCircle(extracted, circle);
        BufferedImage resized = resizeToTarget(cropped, targetSizePx);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(resized, "PNG", baos);
        return baos.toByteArray();
    }

    /**
     * 按圆形提取印章（默认尺寸）
     */
    public byte[] extractSealCircle(byte[] imageBytes) throws Exception {
        return extractSealCircle(imageBytes, null);
    }

    /**
     * 生成缩略图
     * <p>
     * 将图片缩小到指定宽度，保持宽高比，用于列表快速加载
     *
     * @param imageBytes 原始图片字节数组
     * @param targetWidth 目标宽度（像素），高度自动按比例计算
     * @return 缩略图（PNG格式）
     * @throws Exception 如果无法解析图片
     */
    public byte[] generateThumbnail(byte[] imageBytes, int targetWidth) throws Exception {
        BufferedImage source = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (source == null) {
            throw new Exception("无法解析图片文件");
        }

        int srcWidth = source.getWidth();
        int srcHeight = source.getHeight();

        // 如果原图比目标还小，直接返回原图
        if (srcWidth <= targetWidth) {
            return imageBytes;
        }

        // 计算目标高度，保持宽高比
        int targetHeight = (int) ((srcHeight * targetWidth) / (double) srcWidth);

        BufferedImage thumbnail = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g2d = thumbnail.createGraphics();
        try {
            g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                    java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING,
                    java.awt.RenderingHints.VALUE_RENDER_SPEED);
            g2d.drawImage(source, 0, 0, targetWidth, targetHeight, null);
        } finally {
            g2d.dispose();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(thumbnail, "PNG", baos);
        return baos.toByteArray();
    }

    /**
     * 生成缩略图（默认宽度100px）
     */
    public byte[] generateThumbnail(byte[] imageBytes) throws Exception {
        return generateThumbnail(imageBytes, 100);
    }

    /**
     * 找到印章的圆形边界
     */
    private CircleInfo findSealCircle(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        List<int[]> foregroundPixels = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgba = image.getRGB(x, y);
                int alpha = (rgba >> 24) & 0xFF;
                if (alpha > 50) {
                    foregroundPixels.add(new int[]{x, y});
                }
            }
        }

        if (foregroundPixels.isEmpty()) {
            return null;
        }

        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        long sumX = 0, sumY = 0;
        for (int[] pixel : foregroundPixels) {
            int x = pixel[0], y = pixel[1];
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            sumX += x;
            sumY += y;
        }

        int centerX = (int) (sumX / foregroundPixels.size());
        int centerY = (int) (sumY / foregroundPixels.size());

        int radius = 0;
        for (int[] pixel : foregroundPixels) {
            int dx = pixel[0] - centerX;
            int dy = pixel[1] - centerY;
            int dist = dx * dx + dy * dy;
            radius = Math.max(radius, dist);
        }
        radius = (int) Math.sqrt(radius);

        radius = Math.max(radius, 5);

        return new CircleInfo(centerX, centerY, radius);
    }

    /**
     * 按圆形裁剪图片
     */
    private BufferedImage cropToCircle(BufferedImage source, CircleInfo circle) {
        int radius = circle.radius;
        int diameter = radius * 2;
        BufferedImage output = new BufferedImage(diameter, diameter, BufferedImage.TYPE_INT_ARGB);

        int offsetX = circle.centerX - radius;
        int offsetY = circle.centerY - radius;

        for (int y = 0; y < diameter; y++) {
            for (int x = 0; x < diameter; x++) {
                int srcX = x + offsetX;
                int srcY = y + offsetY;

                int dx = x - radius;
                int dy = y - radius;
                if (dx * dx + dy * dy > radius * radius) {
                    output.setRGB(x, y, 0);
                    continue;
                }

                if (srcX >= 0 && srcX < source.getWidth() && srcY >= 0 && srcY < source.getHeight()) {
                    output.setRGB(x, y, source.getRGB(srcX, srcY));
                } else {
                    output.setRGB(x, y, 0);
                }
            }
        }

        return output;
    }

    /**
     * 调整图片到目标尺寸
     */
    private BufferedImage resizeToTarget(BufferedImage source, int targetSize) {
        BufferedImage output = new BufferedImage(targetSize, targetSize, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g2d = output.createGraphics();
        try {
            g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                    java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2d.drawImage(source, 0, 0, targetSize, targetSize, null);
        } finally {
            g2d.dispose();
        }
        return output;
    }

    /**
     * 毫米转像素
     */
    private int mmToPx(int mm) {
        return (int) (mm * DPI / 25.4);
    }

    /**
     * 圆形信息辅助类
     */
    private static class CircleInfo {
        final int centerX;
        final int centerY;
        final int radius;

        CircleInfo(int centerX, int centerY, int radius) {
            this.centerX = centerX;
            this.centerY = centerY;
            this.radius = radius;
        }
    }

    // ==================== 内部算法实现 ====================

    private int[][] computeRedScore(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        int[][] score = new int[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = source.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                int redDominance = Math.max(0, r - Math.max(g, b));
                int redWeight = Math.max(0, r - ((g + b) / 2));
                int brightnessPenalty = Math.max(0, ((g + b) / 2) - 170);
                score[y][x] = Math.max(0, redDominance * 2 + redWeight - brightnessPenalty);
            }
        }
        return score;
    }

    private boolean[][] buildSealMask(int[][] redScore, int width, int height) {
        boolean[][] mask = new boolean[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int score = redScore[y][x];
                boolean strong = score >= STRONG_SEAL_THRESHOLD;
                boolean weak = score >= WEAK_SEAL_THRESHOLD
                        && hasStrongNeighbor(redScore, x, y, width, height, STRONG_SEAL_THRESHOLD);
                boolean rescued = score >= RESCUE_SEAL_THRESHOLD
                        && countNeighborsAbove(redScore, x, y, width, height, NEIGHBOR_VERIFY_THRESHOLD) >= 2;
                mask[y][x] = strong || weak || rescued;
            }
        }
        return mask;
    }

    private boolean hasStrongNeighbor(int[][] redScore, int x, int y, int width, int height, int threshold) {
        for (int ny = Math.max(0, y - 1); ny <= Math.min(height - 1, y + 1); ny++) {
            for (int nx = Math.max(0, x - 1); nx <= Math.min(width - 1, x + 1); nx++) {
                if ((nx != x || ny != y) && redScore[ny][nx] >= threshold) {
                    return true;
                }
            }
        }
        return false;
    }

    private int countNeighborsAbove(int[][] redScore, int x, int y, int width, int height, int threshold) {
        int count = 0;
        for (int ny = Math.max(0, y - 1); ny <= Math.min(height - 1, y + 1); ny++) {
            for (int nx = Math.max(0, x - 1); nx <= Math.min(width - 1, x + 1); nx++) {
                if (nx == x && ny == y) {
                    continue;
                }
                if (redScore[ny][nx] >= threshold) {
                    count++;
                }
            }
        }
        return count;
    }

    private boolean[][] applyMorphology(boolean[][] mask, int width, int height) {
        return erode(dilate(mask, width, height), width, height);
    }

    private boolean[][] dilate(boolean[][] mask, int width, int height) {
        boolean[][] result = new boolean[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                result[y][x] = countMaskNeighbors(mask, x, y, width, height) >= 2;
            }
        }
        return result;
    }

    private boolean[][] erode(boolean[][] mask, int width, int height) {
        boolean[][] result = new boolean[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!mask[y][x]) {
                    continue;
                }
                result[y][x] = countMaskNeighbors(mask, x, y, width, height) >= 3;
            }
        }
        return result;
    }

    private int countMaskNeighbors(boolean[][] mask, int x, int y, int width, int height) {
        int count = 0;
        for (int ny = Math.max(0, y - 1); ny <= Math.min(height - 1, y + 1); ny++) {
            for (int nx = Math.max(0, x - 1); nx <= Math.min(width - 1, x + 1); nx++) {
                if (mask[ny][nx]) {
                    count++;
                }
            }
        }
        return count;
    }

    private boolean[][] filterConnectedComponents(boolean[][] mask, int width, int height) {
        boolean[][] visited = new boolean[height][width];
        List<ComponentStats> components = new ArrayList<>();
        int largestArea = 0;
        int largestIndex = -1;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!mask[y][x] || visited[y][x]) {
                    continue;
                }
                ComponentStats component = collectComponent(mask, visited, x, y, width, height);
                components.add(component);
                if (component.area > largestArea) {
                    largestArea = component.area;
                    largestIndex = components.size() - 1;
                }
            }
        }

        if (largestIndex < 0) {
            return mask;
        }

        ComponentStats largest = components.get(largestIndex);
        boolean[][] filtered = new boolean[height][width];
        int dominantThreshold = Math.max(8, largestArea / 120);
        int nearbyThreshold = Math.max(4, largestArea / 300);

        for (ComponentStats component : components) {
            boolean keepByArea = component.area >= dominantThreshold;
            boolean keepByProximity = component.area >= nearbyThreshold && component.isNear(largest, 12);
            if (keepByArea || keepByProximity) {
                component.paint(filtered);
            }
        }

        return filtered;
    }

    private ComponentStats collectComponent(boolean[][] mask, boolean[][] visited, int startX, int startY, int width, int height) {
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        ComponentStats component = new ComponentStats();
        queue.add(startY * width + startX);
        visited[startY][startX] = true;

        while (!queue.isEmpty()) {
            int value = queue.removeFirst();
            int x = value % width;
            int y = value / width;
            component.add(x, y);

            for (int ny = Math.max(0, y - 1); ny <= Math.min(height - 1, y + 1); ny++) {
                for (int nx = Math.max(0, x - 1); nx <= Math.min(width - 1, x + 1); nx++) {
                    if (!mask[ny][nx] || visited[ny][nx]) {
                        continue;
                    }
                    visited[ny][nx] = true;
                    queue.addLast(ny * width + nx);
                }
            }
        }

        return component;
    }

    private void renderSealImage(BufferedImage source, BufferedImage output, boolean[][] mask, int[][] redScore, int width, int height) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!mask[y][x]) {
                    output.setRGB(x, y, 0);
                    continue;
                }
                int rgb = source.getRGB(x, y);
                int alpha = computeAlpha(mask, redScore, x, y, width, height);
                output.setRGB(x, y, (alpha << 24) | (rgb & 0x00FFFFFF));
            }
        }
    }

    private int computeAlpha(boolean[][] mask, int[][] redScore, int x, int y, int width, int height) {
        int score = redScore[y][x];
        int alpha;
        if (score >= 170) {
            alpha = 255;
        } else if (score >= 130) {
            alpha = 235;
        } else if (score >= 100) {
            alpha = 210;
        } else {
            alpha = 185;
        }

        if (isWeakOuterEdge(mask, redScore, x, y, width, height)) {
            alpha = Math.max(170, alpha - 24);
        }
        return alpha;
    }

    private boolean isWeakOuterEdge(boolean[][] mask, int[][] redScore, int x, int y, int width, int height) {
        if (redScore[y][x] >= 130) {
            return false;
        }
        int neighborCount = countMaskNeighbors(mask, x, y, width, height);
        boolean hasBackgroundAround = neighborCount <= 4;
        boolean openHorizontal = (x == 0 || !mask[y][x - 1]) || (x == width - 1 || !mask[y][x + 1]);
        boolean openVertical = (y == 0 || !mask[y - 1][x]) || (y == height - 1 || !mask[y + 1][x]);
        return hasBackgroundAround && (openHorizontal || openVertical);
    }

    /**
     * 连通域统计辅助类
     */
    public static final class ComponentStats {
        private final List<int[]> pixels = new ArrayList<>();
        private int area;
        private int minX = Integer.MAX_VALUE;
        private int minY = Integer.MAX_VALUE;
        private int maxX = Integer.MIN_VALUE;
        private int maxY = Integer.MIN_VALUE;

        private void add(int x, int y) {
            pixels.add(new int[]{x, y});
            area++;
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
        }

        private boolean isNear(ComponentStats other, int margin) {
            return minX <= other.maxX + margin
                    && maxX >= other.minX - margin
                    && minY <= other.maxY + margin
                    && maxY >= other.minY - margin;
        }

        private void paint(boolean[][] output) {
            for (int[] pixel : pixels) {
                output[pixel[1]][pixel[0]] = true;
            }
        }
    }
}
