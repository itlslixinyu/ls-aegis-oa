package com.ls.aegis.biz.eoms.utils;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
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
 * 水印直接绘入图片像素；不等同于数字签名或防篡改证明。
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
    private static final float CARD_WIDTH_RATIO = 0.82f;
    /** 卡片距离底部边距占图片高度比例 */
    private static final float CARD_BOTTOM_RATIO = 0.03f;
    /** 卡片最小字号 */
    private static final int MIN_FONT_SIZE = 12;
    /** 卡片最大字号 */
    private static final int MAX_FONT_SIZE = 36;
    /** 轻量半透明底板，保留现场照片纹理。 */
    private static final float CARD_ALPHA = 0.24f;
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
    private static final Color LABEL_COLOR = new Color(67, 80, 98);
    /** 值颜色（纯黑色，确保文字清晰可读） */
    private static final Color VALUE_COLOR = new Color(23, 36, 58);
    /** 标题颜色（深色透明标题栏中的白色文字） */
    private static final Color TITLE_COLOR = Color.WHITE;
    /** 资料盾牌图标颜色 */
    private static final Color ICON_COLOR = new Color(36, 164, 226);
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

            // Camera JPEGs may store rotation/mirroring only in EXIF. Normalize after scaling,
            // before placing the watermark, so its baseline follows the displayed photograph.
            BufferedImage orientedImage = JpegOrientation.apply(originalImage, JpegOrientation.read(fileBytes));
            if (orientedImage != originalImage) {
                originalImage.flush();
                originalImage = orientedImage;
                width = originalImage.getWidth();
                height = originalImage.getHeight();
            }

            BufferedImage watermarked = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = watermarked.createGraphics();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, width, height);
                g.drawImage(originalImage, 0, 0, width, height, null);
                drawWatermarkCard(g, width, height, info);
            } finally {
                g.dispose();
                originalImage.flush();
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

    record WatermarkLine(String label, List<String> values) {}
    record CardLayout(int x, int y, int width, int height, int padding, int fontSize,
                      int lineHeight, int headerHeight, int labelWidth, List<WatermarkLine> lines) {}

    static List<String[]> watermarkFields(WatermarkInfo info) {
        List<String[]> fields = new ArrayList<>();
        String address = info.getServiceAddress();
        boolean coordinates = address != null && address.trim().matches("[-+]?\\d+(?:\\.\\d+)?\\s*,\\s*[-+]?\\d+(?:\\.\\d+)?");
        String addressLabel = info.getAddressLabel();
        if (addressLabel == null || addressLabel.isBlank()) addressLabel = coordinates ? "定位坐标" : "服务地址";
        else if (coordinates) addressLabel = addressLabel.replace("地址", "坐标");
        addField(fields, addressLabel, address);
        addField(fields, "报修客户", info.getCustomerName());
        addField(fields, "任务名称", info.getTaskName());
        addField(fields, "项目名称", info.getProjectName());
        addField(fields, info.getWorkTimeLabel() == null || info.getWorkTimeLabel().isBlank() ? "施工时间" : info.getWorkTimeLabel(),
            info.getWorkTime() == null || info.getWorkTime().isBlank() ? LocalDateTime.now().format(TIME_FMT) : info.getWorkTime());
        addField(fields, info.getExecutorLabel() == null || info.getExecutorLabel().isBlank() ? "执行人" : info.getExecutorLabel(), info.getExecutor());
        addField(fields, "备注", info.getRemark());
        return fields;
    }

    private static void addField(List<String[]> fields, String label, String value) {
        if (value != null && !value.isBlank()) fields.add(new String[]{label, value.replaceAll("[\\r\\n\\t]+", " ").trim()});
    }

    /** Measure wrapped rows before positioning the card; keep even tiny images bounded. */
    static CardLayout cardLayout(Graphics2D g, int width, int height, List<String[]> fields) {
        int marginX = Math.min(Math.max(1, Math.round(width * 0.035f)), width / 4);
        int marginY = Math.min(Math.max(1, Math.round(height * CARD_BOTTOM_RATIO)), height / 4);
        int cardWidth = Math.max(1, Math.min(width - marginX * 2, Math.round(width * CARD_WIDTH_RATIO)));
        int availableHeight = Math.max(1, height - marginY * 2);
        int fontSize = Math.max(MIN_FONT_SIZE, Math.min(MAX_FONT_SIZE, width / 45));
        while (true) {
            int padding = Math.min(Math.max(2, Math.round(fontSize * 0.85f)), Math.max(0, cardWidth / 8));
            int lineHeight = Math.max(1, Math.round(fontSize * 1.6f));
            int headerHeight = Math.max(1, Math.round(fontSize * 2.6f));
            FontMetrics fm = g.getFontMetrics(createCJKFont(Font.PLAIN, fontSize));
            int contentWidth = Math.max(1, cardWidth - padding * 2);
            int labelWidth = 0;
            for (String[] field : fields) labelWidth = Math.max(labelWidth, fm.stringWidth(field[0] + "："));
            labelWidth = Math.min(labelWidth + Math.max(1, fontSize / 3), contentWidth * 2 / 5);
            List<WatermarkLine> rows = new ArrayList<>();
            int rowCount = 0;
            for (String[] field : fields) {
                List<String> values = wrapString(fm, field[1], Math.max(1, contentWidth - labelWidth), fontSize > 8 ? 2 : 1);
                rows.add(new WatermarkLine(field[0], values));
                rowCount += values.size();
            }
            int measuredHeight = headerHeight + padding * 2 + rowCount * lineHeight;
            if (measuredHeight <= availableHeight || fontSize <= 8) {
                int cardHeight = Math.min(measuredHeight, availableHeight);
                return new CardLayout(marginX, height - marginY - cardHeight, cardWidth, cardHeight,
                    padding, fontSize, lineHeight, headerHeight, labelWidth, rows);
            }
            fontSize--;
        }
    }

    private static void drawWatermarkCard(Graphics2D source, int width, int height, WatermarkInfo info) {
        CardLayout c = cardLayout(source, width, height, watermarkFields(info));
        Graphics2D g = (Graphics2D) source.create();
        try {
            int radius = Math.max(2, Math.round(c.fontSize() * CARD_RADIUS_RATIO));
            g.clip(new RoundRectangle2D.Float(c.x(), c.y(), c.width(), c.height(), radius, radius));
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, CARD_ALPHA));
            g.setColor(CARD_BG_COLOR);
            g.fillRect(c.x(), c.y(), c.width(), c.height());
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.32f));
            g.setColor(new Color(29, 48, 75));
            g.fillRect(c.x(), c.y(), c.width(), c.headerHeight());
            g.setComposite(AlphaComposite.SrcOver);
            int iconSize = Math.max(1, c.headerHeight() - c.padding());
            drawDocumentBadge(g, c.x() + c.padding(), c.y() + (c.headerHeight() - iconSize) / 2, iconSize);
            Font titleFont = createCJKFont(Font.BOLD, Math.max(1, Math.round(c.fontSize() * 1.15f)));
            g.setFont(titleFont);
            FontMetrics titleMetrics = g.getFontMetrics();
            String title = info.getTitle() == null || info.getTitle().isBlank() ? "现场资料" : info.getTitle();
            int titleX = c.x() + c.padding() + iconSize + c.padding() / 2;
            g.setColor(TITLE_COLOR);
            g.drawString(truncateString(titleMetrics, title, Math.max(1, c.x() + c.width() - c.padding() - titleX)),
                titleX, c.y() + (c.headerHeight() - titleMetrics.getHeight()) / 2 + titleMetrics.getAscent());
            g.setFont(createCJKFont(Font.PLAIN, c.fontSize()));
            FontMetrics fm = g.getFontMetrics();
            int cursorY = c.y() + c.headerHeight() + c.padding();
            for (WatermarkLine line : c.lines()) {
                g.setColor(LABEL_COLOR);
                g.drawString(truncateString(fm, line.label() + "：", c.labelWidth()), c.x() + c.padding(), cursorY + fm.getAscent());
                g.setColor(VALUE_COLOR);
                for (String value : line.values()) {
                    g.drawString(value, c.x() + c.padding() + c.labelWidth(), cursorY + fm.getAscent());
                    cursorY += c.lineHeight();
                }
            }
        } finally { g.dispose(); }
    }

    /** Small vector badge: an Aegis shield with layered records and a confirmation tick. */
    private static void drawDocumentBadge(Graphics2D source, int x, int y, int size) {
        Graphics2D g = (Graphics2D) source.create();
        try {
            g.translate(x, y); g.scale(size / 32.0, size / 32.0);
            g.setColor(ICON_COLOR); g.fillRoundRect(0, 0, 32, 32, 10, 10);
            GeneralPath shield = new GeneralPath();
            shield.moveTo(16, 5); shield.lineTo(25, 9); shield.lineTo(24, 19);
            shield.quadTo(22, 25, 16, 28); shield.quadTo(10, 25, 8, 19);
            shield.lineTo(7, 9); shield.closePath();
            g.setColor(Color.WHITE); g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(shield);
            g.drawLine(12, 11, 20, 11); g.drawLine(12, 15, 18, 15);
            GeneralPath tick = new GeneralPath(); tick.moveTo(12, 20); tick.lineTo(15, 23); tick.lineTo(21, 18); g.draw(tick);
        } finally { g.dispose(); }
    }

    private static String truncateString(FontMetrics fm, String text, int maxWidth) {
        if (text == null || text.isEmpty() || maxWidth <= 0) return "";
        if (fm.stringWidth(text) <= maxWidth) return text;
        String ellipsis = "…";
        if (fm.stringWidth(ellipsis) > maxWidth) return "";
        int end = 0;
        while (end < text.length()) {
            int next = end + Character.charCount(text.codePointAt(end));
            if (fm.stringWidth(text.substring(0, next) + ellipsis) > maxWidth) break;
            end = next;
        }
        return text.substring(0, end) + ellipsis;
    }

    static List<String> wrapString(FontMetrics fm, String text, int maxWidth, int maxLines) {
        List<String> result = new ArrayList<>();
        if (text == null || text.isEmpty() || maxWidth <= 0 || maxLines <= 0) return List.of("");
        int start = 0;
        while (start < text.length() && result.size() < maxLines) {
            if (result.size() == maxLines - 1) {
                result.add(truncateString(fm, text.substring(start), maxWidth));
                break;
            }
            int end = start;
            while (end < text.length()) {
                int next = end + Character.charCount(text.codePointAt(end));
                if (fm.stringWidth(text.substring(start, next)) > maxWidth) break;
                end = next;
            }
            if (end == start) { result.add(truncateString(fm, text.substring(start), maxWidth)); break; }
            result.add(text.substring(start, end));
            start = end;
        }
        return result;
    }
}
