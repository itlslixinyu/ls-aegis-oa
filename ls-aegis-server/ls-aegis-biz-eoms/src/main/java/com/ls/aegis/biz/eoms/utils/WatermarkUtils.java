package com.ls.aegis.biz.eoms.utils;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;
import cn.hutool.extra.spring.SpringUtil;
import com.ls.aegis.biz.eoms.constant.EomsConstants;
import com.ls.aegis.common.api.system.SysOptionApi;
import com.ls.aegis.compat.common.exception.ServiceException;
import com.ls.aegis.compat.common.utils.StringUtils;

/**
 * 图片水印工具类
 *
 * 在图片底部烧录半透明水印条，包含：标题/任务名称/执行人/备注/服务地址/项目名称/时间（标签按阶段）。
 * 水印不可篡改，符合企业审计要求。
 * 输出统一为高质量 JPEG（0.95），兼容 PNG/JPG/JPEG 输入。
 *
 * @author EOMS
 */
public class WatermarkUtils
{
    private static final Logger log = LoggerFactory.getLogger(WatermarkUtils.class);

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** JPEG 输出质量（0.0-1.0，越高质量越好） */
    private static final float JPEG_QUALITY = 0.95f;

    /** 卡片宽度占图片宽度比例（左右留白） */
    private static final float CARD_WIDTH_RATIO = 0.85f;
    /** 卡片距离底部边距占图片高度比例 */
    private static final float CARD_BOTTOM_RATIO = 0.03f;
    /** 卡片最小字号 */
    private static final int MIN_FONT_SIZE = 12;
    /** 卡片最大字号 */
    private static final int MAX_FONT_SIZE = 36;
    /** 卡片背景透明度（值越小越透明，0.5~0.7 可透出背景图片） */
    private static final float CARD_ALPHA = 0.55f;
    /** 卡片圆角半径占字号比例 */
    private static final float CARD_RADIUS_RATIO = 1.2f;

    /** 支持的图片格式 */
    private static final String[] SUPPORTED_TYPES = { "jpg", "jpeg", "png" };

    /** 图片最大允许像素维度（单边），超过拒绝处理，防止解压炸弹 OOM */
    private static final int MAX_IMAGE_DIMENSION = 5000;

    /** 图片最大允许总像素数（width * height），超过拒绝处理 */
    private static final long MAX_IMAGE_PIXELS = 25_000_000L;

    /** 系统字体文件候选（Docker 挂载 / 镜像内嵌 / Linux / Windows） */
    private static final String[] CJK_FONT_FILE_CANDIDATES = {
        // Compose 挂载：docker/ls-aegis-server/fonts → /app/fonts（与 PDF 导出同源）
        "/app/fonts/simhei.ttf",
        "/usr/share/fonts/truetype/aegis/simhei.ttf",
        "/usr/share/fonts/truetype/wqy/wqy-microhei.ttc",
        "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc",
        "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
        "/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc",
        "C:/Windows/Fonts/msyh.ttc",
        "C:/Windows/Fonts/msyhbd.ttc",
        "C:/Windows/Fonts/simhei.ttf",
        "C:/Windows/Fonts/simsun.ttc",
    };

    /** classpath 内置字体（可选，作为无系统字体的兜底） */
    private static final String CLASSPATH_CJK_FONT = "/fonts/watermark-cjk.ttf";

    /** 已解析的中文字体（deriveFont 复用，避免重复 IO） */
    private static volatile Font CJK_BASE_FONT;

    /** 标签颜色（纯黑色，确保文字清晰可读） */
    private static final Color LABEL_COLOR = Color.BLACK;
    /** 值颜色（纯黑色，确保文字清晰可读） */
    private static final Color VALUE_COLOR = Color.BLACK;
    /** 标题/品牌颜色（纯黑色粗体） */
    private static final Color TITLE_COLOR = Color.BLACK;
    /** 图标填充颜色（蓝色定位图标） */
    private static final Color ICON_COLOR = new Color(30, 136, 229);
    /** 卡片背景颜色 */
    private static final Color CARD_BG_COLOR = Color.WHITE;

    private WatermarkUtils() {}

    /**
     * 创建中文字体：优先从字体文件 / classpath 加载，避免 Linux 容器无字体时出现方框。
     */
    private static Font createCJKFont(int style, int size)
    {
        return getCjkBaseFont().deriveFont(style, (float) size);
    }

    private static Font getCjkBaseFont()
    {
        Font cached = CJK_BASE_FONT;
        if (cached != null)
        {
            return cached;
        }
        synchronized (WatermarkUtils.class)
        {
            if (CJK_BASE_FONT != null)
            {
                return CJK_BASE_FONT;
            }
            CJK_BASE_FONT = loadCjkBaseFont();
            return CJK_BASE_FONT;
        }
    }

    private static Font loadCjkBaseFont()
    {
        for (String path : CJK_FONT_FILE_CANDIDATES)
        {
            Font font = loadFontFromFile(new File(path));
            if (font != null)
            {
                log.info("水印中文字体已从文件加载: {}", path);
                return font;
            }
        }
        try (InputStream in = WatermarkUtils.class.getResourceAsStream(CLASSPATH_CJK_FONT))
        {
            if (in != null)
            {
                Font font = Font.createFont(Font.TRUETYPE_FONT, in);
                if (supportsCjk(font))
                {
                    log.info("水印中文字体已从 classpath 加载: {}", CLASSPATH_CJK_FONT);
                    return font;
                }
            }
        }
        catch (Exception e)
        {
            log.warn("classpath 水印字体加载失败: {}", e.getMessage());
        }

        String[] familyCandidates = {
            "Noto Sans CJK SC",
            "Noto Sans SC",
            "Microsoft YaHei",
            "SimHei",
            "WenQuanYi Micro Hei",
        };
        for (String family : familyCandidates)
        {
            Font font = new Font(family, Font.PLAIN, 12);
            if (supportsCjk(font) && isPhysicalFontFamily(font, family))
            {
                log.info("水印中文字体已从系统字体族加载: {}", family);
                return font;
            }
        }

        log.warn("未找到可用的中文字体文件，水印中文可能显示异常；请在 Docker 镜像安装 fonts-noto-cjk");
        return new Font(Font.SANS_SERIF, Font.PLAIN, 12);
    }

    private static Font loadFontFromFile(File file)
    {
        if (!file.isFile())
        {
            return null;
        }
        try
        {
            Font font = Font.createFont(Font.TRUETYPE_FONT, file);
            return supportsCjk(font) ? font : null;
        }
        catch (Exception e)
        {
            log.debug("加载字体文件失败 {}: {}", file.getAbsolutePath(), e.getMessage());
            return null;
        }
    }

    /** 能否渲染常见中文（比 canDisplayUpTo 单独使用更严格） */
    private static boolean supportsCjk(Font font)
    {
        return font != null && font.canDisplayUpTo("中文水印现场照片") == -1;
    }

    /** 排除 Java 在找不到字体时回退到 Dialog/SansSerif 逻辑字体的情况 */
    private static boolean isPhysicalFontFamily(Font font, String requestedFamily)
    {
        String family = font.getFamily(Locale.ROOT).toLowerCase(Locale.ROOT);
        String fontName = font.getFontName(Locale.ROOT).toLowerCase(Locale.ROOT);
        String req = requestedFamily.toLowerCase(Locale.ROOT).replace(" ", "");
        if ("dialog".equals(family) || "sansserif".equals(family) || "dialog".equals(fontName))
        {
            return false;
        }
        return family.replace(" ", "").contains(req) || fontName.replace(" ", "").contains(req);
    }

    /**
     * 校验文件是否为支持的图片类型（扩展名或 Content-Type，兼容移动端无扩展名拍照）
     */
    public static boolean isSupportedImage(MultipartFile file)
    {
        if (file == null || file.isEmpty())
        {
            return false;
        }
        String fileName = file.getOriginalFilename();
        if (fileName != null && !fileName.isEmpty())
        {
            String lower = fileName.toLowerCase(Locale.ROOT);
            for (String type : SUPPORTED_TYPES)
            {
                if (lower.endsWith("." + type))
                {
                    return true;
                }
            }
        }
        String contentType = file.getContentType();
        if (contentType != null)
        {
            String ct = contentType.toLowerCase(Locale.ROOT);
            if (ct.contains("jpeg") || ct.contains("jpg") || ct.contains("png"))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * 为图片添加水印（兼容旧接口：定位地址 + 时间 + 经纬度）
     *
     * @param fileBytes 原图字节数组
     * @param location  定位地址
     * @param longitude 经度
     * @param latitude  纬度
     * @return 加水印后的图片字节数组（JPEG 格式）
     */
    public static byte[] addWatermark(byte[] fileBytes, String location, String longitude, String latitude)
    {
        WatermarkInfo info = new WatermarkInfo();
        info.setTitle("现场照片");
        info.setServiceAddress(location != null ? location : "");
        info.setWorkTime(LocalDateTime.now().format(TIME_FMT));
        info.setLongitude(longitude);
        info.setLatitude(latitude);
        return addWatermark(fileBytes, info);
    }

    /**
     * 为图片添加水印（新接口：完整元数据）
     *
     * @param fileBytes 原图字节数组
     * @param info      水印元数据
     * @return 加水印后的图片字节数组（JPEG 格式）
     */
    public static byte[] addWatermark(byte[] fileBytes, WatermarkInfo info)
    {
        if (fileBytes == null || fileBytes.length == 0)
        {
            throw new ServiceException("图片数据为空，无法添加水印");
        }
        if (info == null)
        {
            info = new WatermarkInfo();
        }
        try
        {
            // Step 1: 先读取图片维度（不解码像素），防止解压炸弹 OOM
            int[] dims = getImageDimensions(fileBytes);
            int origWidth = dims[0];
            int origHeight = dims[1];
            if (origWidth <= 0 || origHeight <= 0)
            {
                throw new ServiceException("无法解析图片数据，请上传有效的图片文件");
            }
            if (origWidth > MAX_IMAGE_DIMENSION || origHeight > MAX_IMAGE_DIMENSION
                    || (long) origWidth * origHeight > MAX_IMAGE_PIXELS)
            {
                throw new ServiceException(StringUtils.format(
                        "图片尺寸 {}x{} 超过限制 {}x{}（或总像素 {}），请压缩后上传",
                        origWidth, origHeight, MAX_IMAGE_DIMENSION, MAX_IMAGE_DIMENSION, MAX_IMAGE_PIXELS));
            }

            // 维度安全，全量解码
            BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(fileBytes));
            if (originalImage == null)
            {
                throw new ServiceException("无法解析图片数据，请上传有效的图片文件");
            }

            int width = originalImage.getWidth();
            int height = originalImage.getHeight();

            // 对超大图片进行缩放处理，缩放后立即释放原图
            BufferedImage scaledImage = scaleIfNeeded(originalImage);
            if (scaledImage != originalImage)
            {
                originalImage.flush(); // 释放原图像素内存，减少同时持有的副本数
                originalImage = scaledImage;
                width = originalImage.getWidth();
                height = originalImage.getHeight();
                log.info("图片尺寸已缩放至 {}x{}", width, height);
            }

            // Step 2: 计算水印卡片参数
            // 根据图片宽度计算基准字号
            int baseFontSize = Math.max(MIN_FONT_SIZE, Math.min(MAX_FONT_SIZE, width / 45));
            int titleFontSize = (int) (baseFontSize * 1.25);
            int lineHeight = (int) (baseFontSize * 1.45);
            int padding = (int) (baseFontSize * 1.4);
            int titleHeight = (int) (titleFontSize * 1.5);

            // 收集水印行（label-value 对）
            java.util.List<String[]> lines = new java.util.ArrayList<>();
            String taskName = info.getTaskName();
            String executor = info.getExecutor();
            String remark = info.getRemark();
            String serviceAddress = info.getServiceAddress();
            String projectName = info.getProjectName();
            String workTime = info.getWorkTime();
            if (workTime == null || workTime.isEmpty())
            {
                workTime = LocalDateTime.now().format(TIME_FMT);
            }
            String workTimeLabel = info.getWorkTimeLabel();
            if (workTimeLabel == null || workTimeLabel.isBlank())
            {
                workTimeLabel = "施工时间";
            }

            if (taskName != null && !taskName.isEmpty())
            {
                lines.add(new String[]{"任务名称", taskName});
            }
            if (executor != null && !executor.isEmpty())
            {
                lines.add(new String[]{"执行人", executor});
            }
            if (remark != null && !remark.isEmpty())
            {
                lines.add(new String[]{"备注", remark});
            }
            if (serviceAddress != null && !serviceAddress.isEmpty())
            {
                lines.add(new String[]{"服务地址", serviceAddress});
            }
            if (projectName != null && !projectName.isEmpty())
            {
                lines.add(new String[]{"项目名称", projectName});
            }
            if (workTime != null && !workTime.isEmpty())
            {
                lines.add(new String[]{workTimeLabel, workTime});
            }

            // 如果没有任何有效行，至少显示地址和时间
            if (lines.isEmpty())
            {
                if (info.getServiceAddress() != null && !info.getServiceAddress().isEmpty())
                {
                    lines.add(new String[]{"服务地址", info.getServiceAddress()});
                }
                lines.add(new String[]{workTimeLabel, workTime});
                if (info.getLongitude() != null && !info.getLongitude().isEmpty()
                        && info.getLatitude() != null && !info.getLatitude().isEmpty())
                {
                    lines.add(new String[]{"坐标", info.getLongitude() + ", " + info.getLatitude()});
                }
            }

            // 计算卡片尺寸
            int cardWidth = (int) (width * CARD_WIDTH_RATIO);
            int cardX = (width - cardWidth) / 2;
            int lineCount = lines.size();
            int cardContentHeight = titleHeight + padding + lineCount * lineHeight + padding;
            int cornerRadius = (int) (baseFontSize * CARD_RADIUS_RATIO);
            int bottomMargin = (int) (height * CARD_BOTTOM_RATIO);
            int cardY = height - cardContentHeight - bottomMargin;
            if (cardY < 0)
            {
                cardY = 0;
            }

            // Step 3: 创建新图片（与原图等尺寸，卡片叠加在底部）
            BufferedImage watermarked = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = watermarked.createGraphics();
            try
            {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

                // 填充白色背景（防止 PNG 透明区域变黑）
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, width, height);

                // 绘制原图
                g.drawImage(originalImage, 0, 0, width, height, null);

                // ========== 绘制圆角卡片（半透明白色） ==========
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, CARD_ALPHA));
                g.setColor(CARD_BG_COLOR);
                g.fillRoundRect(cardX, cardY, cardWidth, cardContentHeight, cornerRadius, cornerRadius);
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));

                int cursorY = cardY + padding;

                // ========== 绘制标题行（定位图标 + 标题文字） ==========
                String title = info.getTitle();
                if (title == null || title.isEmpty())
                {
                    title = "现场照片";
                }

                // 1) 绘制蓝色定位图标（水滴形）
                int pinW = (int) (titleFontSize * 1.1);
                int pinH = (int) (titleFontSize * 1.35);
                int pinX = cardX + padding;
                int pinY = cursorY;
                drawLocationPin(g, pinX, pinY, pinW, pinH, ICON_COLOR);

                // 2) 绘制标题文字（黑色粗体）
                Font titleFont = createCJKFont(Font.BOLD, titleFontSize);
                g.setFont(titleFont);
                FontMetrics titleFm = g.getFontMetrics(titleFont);
                int titleX = pinX + pinW + (int) (padding * 0.6);
                int titleBaseline = cursorY + pinH / 2 + titleFm.getAscent() / 2 - titleFm.getDescent() / 2;
                g.setColor(TITLE_COLOR);
                String safeTitle = truncateString(titleFm, title, cardWidth - (titleX - cardX) - padding);
                g.drawString(safeTitle, titleX, titleBaseline);

                // 3) 标题与内容分隔线（浅灰色细线）
                cursorY += titleHeight;
                g.setColor(new Color(220, 220, 230, 180));
                int sepLineY = cursorY - padding / 2;
                g.drawLine(cardX + padding, sepLineY, cardX + cardWidth - padding, sepLineY);

                // ========== 绘制 label-value 信息行 ==========
                Font labelFont = createCJKFont(Font.PLAIN, baseFontSize);
                g.setFont(labelFont);
                FontMetrics fm = g.getFontMetrics(labelFont);

                // 计算标签对齐宽度
                int labelWidth = 0;
                for (String[] line : lines)
                {
                    int w = fm.stringWidth(line[0] + "：");
                    if (w > labelWidth)
                    {
                        labelWidth = w;
                    }
                }
                int maxLabelWidth = (int) ((cardWidth - padding * 2) * 0.25);
                if (labelWidth > maxLabelWidth)
                {
                    labelWidth = maxLabelWidth;
                }

                int valueMaxWidth = cardWidth - padding * 2 - labelWidth;

                for (String[] line : lines)
                {
                    String label = line[0] + "：";
                    String value = line[1];
                    int baselineY = cursorY + fm.getAscent();

                    g.setColor(LABEL_COLOR);
                    g.drawString(label, cardX + padding, baselineY);

                    g.setColor(VALUE_COLOR);
                    String displayValue = truncateString(fm, value, valueMaxWidth);
                    // 如果值很长，尝试换行（最多显示2行，超出截断）
                    if (fm.stringWidth(value) > valueMaxWidth)
                    {
                        List<String> wrapped = wrapString(fm, value, valueMaxWidth, 2);
                        for (int i = 0; i < wrapped.size(); i++)
                        {
                            g.drawString(wrapped.get(i), cardX + padding + labelWidth, baselineY + i * lineHeight);
                        }
                        if (wrapped.size() > 1)
                        {
                            cursorY += (wrapped.size() - 1) * lineHeight;
                        }
                    }
                    else
                    {
                        g.drawString(displayValue, cardX + padding + labelWidth, baselineY);
                    }
                    cursorY += lineHeight;
                }
            }
            finally
            {
                g.dispose();
            }

            // Step 4: 以高质量 JPEG 输出
            byte[] result = writeJpeg(watermarked);
            watermarked.flush();
            return result;
        }
        catch (ServiceException e)
        {
            throw e;
        }
        catch (IOException e)
        {
            log.error("图片水印处理失败", e);
            throw new ServiceException("图片水印处理失败：" + e.getMessage());
        }
    }

    /**
     * 仅读取图片维度元数据（不解码像素），用于在 ImageIO.read 前做安全校验
     * 防止"解压炸弹"——5MB 的 PNG 可达 46000×46000 像素，全量解码需 8.5GB 内存
     *
     * @param fileBytes 图片字节数组
     * @return int[2] = {width, height}，解析失败返回 {0, 0}
     */
    private static int[] getImageDimensions(byte[] fileBytes)
    {
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(fileBytes)))
        {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext())
            {
                return new int[]{0, 0};
            }
            ImageReader reader = readers.next();
            try
            {
                reader.setInput(iis);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                return new int[]{width, height};
            }
            finally
            {
                reader.dispose();
            }
        }
        catch (IOException e)
        {
            log.warn("读取图片维度失败: {}", e.getMessage());
            return new int[]{0, 0};
        }
    }

    /**
     * 对超大图片进行缩放（2C1G 下默认最长边 1280px）。
     * 高清需求可在系统配置「运维报修」调整 WATERMARK_MAX_SIDE。
     */
    private static BufferedImage scaleIfNeeded(BufferedImage original)
    {
        int maxSide = resolveMaxSide();
        int width = original.getWidth();
        int height = original.getHeight();
        int maxDim = Math.max(width, height);

        if (maxDim <= maxSide)
        {
            return original;
        }

        double scale = (double) maxSide / maxDim;
        int newWidth = (int) (width * scale);
        int newHeight = (int) (height * scale);

        log.info("图片尺寸 {}x{} 超过 {}px，缩放至 {}x{}", width, height, maxSide, newWidth, newHeight);

        BufferedImage scaled = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaled.createGraphics();
        try
        {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, newWidth, newHeight);
            g.drawImage(original, 0, 0, newWidth, newHeight, null);
        }
        finally
        {
            g.dispose();
        }
        return scaled;
    }

    private static int resolveMaxSide() {
        int fallback = EomsConstants.DEFAULT_WATERMARK_MAX_SIDE;
        try {
            SysOptionApi api = SpringUtil.getBean(SysOptionApi.class);
            String raw = api.getValue(EomsConstants.OPTION_CATEGORY, EomsConstants.OPTION_CODE_WATERMARK_MAX_SIDE);
            if (StringUtils.isNotEmpty(raw)) {
                int parsed = Integer.parseInt(raw.trim());
                if (parsed >= 640 && parsed <= 4096) {
                    return parsed;
                }
            }
        } catch (Exception e) {
            log.debug("读取水印最长边配置失败，使用默认 {}: {}", fallback, e.getMessage());
        }
        return fallback;
    }

    /**
     * 以高质量 JPEG 输出图片
     */
    private static byte[] writeJpeg(BufferedImage image) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        ImageWriteParam params = writer.getDefaultWriteParam();
        params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        params.setCompressionQuality(JPEG_QUALITY);
        params.setProgressiveMode(ImageWriteParam.MODE_DEFAULT);

        try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos))
        {
            writer.setOutput(ios);
            writer.write(null, new javax.imageio.IIOImage(image, null, null), params);
        }
        finally
        {
            writer.dispose();
        }
        return baos.toByteArray();
    }

    /**
     * 绘制定位水滴图标（类似 Google Maps / 高德 的 pin）
     * 绘制区域：(x, y) 为左上角，宽 pinW，高 pinH
     */
    private static void drawLocationPin(Graphics2D g, int x, int y, int pinW, int pinH, Color color)
    {
        int centerX = x + pinW / 2;
        int circleR = pinW / 2;
        int circleCy = y + circleR;

        // 上半部分：圆形（主体圆的下半部分 + 水滴尖角）
        GeneralPath path = new GeneralPath();
        // 圆形主体：使用贝塞尔曲线绘制圆
        double k = 0.5522847498;
        path.moveTo(centerX, y);
        // 右半圆
        path.curveTo(centerX + circleR * k, y,
                x + pinW, circleCy - circleR * k,
                x + pinW, circleCy);
        // 右下角过渡 -> 水滴尖
        path.lineTo(centerX, y + pinH);
        // 水滴尖 -> 左下角过渡
        path.lineTo(x, circleCy);
        // 左半圆
        path.curveTo(x, circleCy - circleR * k,
                centerX - circleR * k, y,
                centerX, y);
        path.closePath();

        // 填充水滴
        g.setColor(color);
        g.fill(path);

        // 内部白色小圆（高光点）
        int innerR = (int) (circleR * 0.35);
        int innerCx = centerX - (int) (circleR * 0.1);
        int innerCy = circleCy - (int) (circleR * 0.15);
        g.setColor(Color.WHITE);
        g.fill(new Ellipse2D.Double(
                innerCx - innerR, innerCy - innerR, innerR * 2, innerR * 2));
    }

    /**
     * 字符串截断显示（超出最大宽度时末尾加省略号）
     */
    private static String truncateString(FontMetrics fm, String text, int maxWidth)
    {
        if (text == null || text.isEmpty() || maxWidth <= 0)
        {
            return text;
        }
        if (fm.stringWidth(text) <= maxWidth)
        {
            return text;
        }
        String ellipsis = "...";
        int ellipsisWidth = fm.stringWidth(ellipsis);
        int textWidth = maxWidth - ellipsisWidth;
        if (textWidth <= 0)
        {
            return ellipsis;
        }
        int idx = 0;
        int currentWidth = 0;
        while (idx < text.length())
        {
            int cw = fm.charWidth(text.charAt(idx));
            if (currentWidth + cw > textWidth)
            {
                break;
            }
            currentWidth += cw;
            idx++;
        }
        return text.substring(0, idx) + ellipsis;
    }

    /**
     * 字符串按宽度换行（最多 maxLines 行，超出末尾加省略号）
     */
    private static List<String> wrapString(FontMetrics fm, String text, int maxWidth, int maxLines)
    {
        List<String> result = new ArrayList<>();
        if (text == null || text.isEmpty() || maxWidth <= 0)
        {
            result.add(text);
            return result;
        }

        StringBuilder currentLine = new StringBuilder();
        int currentWidth = 0;

        for (int i = 0; i < text.length(); i++)
        {
            char ch = text.charAt(i);
            int cw = fm.charWidth(ch);

            if (currentWidth + cw > maxWidth && currentLine.length() > 0)
            {
                result.add(currentLine.toString());
                currentLine = new StringBuilder();
                currentWidth = 0;

                if (result.size() >= maxLines)
                {
                    // 剩余内容合并为一行并截断
                    StringBuilder rest = new StringBuilder();
                    for (int j = i; j < text.length(); j++)
                    {
                        rest.append(text.charAt(j));
                    }
                    String lastLine = truncateString(fm, rest.toString(), maxWidth);
                    result.set(result.size() - 1, lastLine);
                    return result;
                }
            }

            currentLine.append(ch);
            currentWidth += cw;
        }

        if (currentLine.length() > 0)
        {
            result.add(currentLine.toString());
        }
        return result;
    }
}
