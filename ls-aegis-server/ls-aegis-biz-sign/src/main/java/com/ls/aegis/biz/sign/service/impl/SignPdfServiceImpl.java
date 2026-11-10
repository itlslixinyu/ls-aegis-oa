package com.ls.aegis.biz.sign.service.impl;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.extgstate.PdfExtGState;
import com.itextpdf.layout.element.Image;
import com.itextpdf.signatures.*;
import com.ls.aegis.compat.common.exception.ServiceException;
import com.ls.aegis.compat.common.utils.StringUtils;
import com.ls.aegis.biz.sign.domain.SignSeal;
import com.ls.aegis.biz.sign.domain.vo.ManagedSignMaterial;
import com.ls.aegis.biz.sign.domain.vo.SealVisibleBounds;
import com.ls.aegis.biz.sign.domain.vo.SignPosition;
import com.ls.aegis.biz.sign.domain.vo.SignRequest;
import com.ls.aegis.biz.sign.service.ISignStorageService;
import com.ls.aegis.biz.sign.service.provider.KeyProvider;
import com.ls.aegis.biz.sign.service.provider.SigningProvider;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.util.Matrix;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.*;
import java.security.cert.Certificate;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * PDF 签章服务实现
 *
 * 签章模式：single / fixed / custom / span
 * 坐标系：前端 Y 向下 → PDF Y 向上，需翻转
 * 印章透明度：SEAL_ALPHA = 0.65
 * 骑缝章：裁剪区域(clipping path) + 完整印章偏移绘制
 * 数字签名：印章有 cert_path 时执行 PKCS#7 签名
 */
@Service
public class SignPdfServiceImpl {

    private static final Logger log = LoggerFactory.getLogger(SignPdfServiceImpl.class);

    // ==========================================================================
    // 常量定义
    // ==========================================================================

    /** 印章透明度 */
    private static final float SEAL_ALPHA = 0.65f;

    // ================ 资源保护（防 PDF 炸弹 / OOM） ================
    /** 单次签章源 PDF 字节数上限（50MB），超过直接拒绝 */
    private static final long MAX_PDF_BYTES = 50L * 1024 * 1024;
    /** 单个 PDF 允许的最大页数（1000 页），超过拒绝 — 防止无限页 PDF 炸弹 */
    private static final int MAX_PDF_PAGES = 1000;

    /** 毫米转 PDF 点数系数 (72dpi) */
    private static final float PT_PER_MM = 72.0f / 25.4f;

    /** 默认印章尺寸（mm） */
    private static final float DEFAULT_SEAL_MM = 40f;

    /** 默认印章点数大小 */
    private static final float DEFAULT_SEAL_PT = DEFAULT_SEAL_MM * PT_PER_MM;

    /** 骑缝章距离页面边缘的距离（pt） */
    private static final float SPAN_EDGE_MARGIN_PT = 5f;

    /** 默认数字签名宽度（pt） */
    private static final float DEFAULT_SIGNATURE_WIDTH = 120f;

    /** 默认数字签名高度（pt） */
    private static final float DEFAULT_SIGNATURE_HEIGHT = 50f;

    /** 默认数字签名距右边缘距离（pt） */
    private static final float DEFAULT_SIGNATURE_RIGHT_MARGIN = 30f;

    /** 默认数字签名距底部距离（pt） */
    private static final float DEFAULT_SIGNATURE_BOTTOM_MARGIN = 30f;

    /** 时间戳标签内边距（pt） */
    private static final float TIMESTAMP_PADDING = 2f;

    /** 时间戳字体大小与宽度的比例系数 */
    private static final float TIMESTAMP_CHAR_WIDTH_RATIO = 0.55f;

    /** 日期格式 */
    private static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd HH:mm:ss";

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    // ==========================================================================
    // 依赖注入
    // ==========================================================================

    @Autowired private com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    @Autowired private CertService certService;
    @Autowired private ISignStorageService signStorage;
    @Autowired private com.ls.aegis.biz.sign.mapper.SignSealMapper sealMapper;
    @Autowired(required = false) private SigningProvider signingProvider;
    @Autowired(required = false) private KeyProvider keyProvider;
    @Autowired(required = false) private SealVisibleBoundsHelper visibleBoundsHelper;
    @Autowired private SealImageCache sealImageCache;

    // ==========================================================================
    // 入口：执行 PDF 签章
    // ==========================================================================

    public byte[] sign(byte[] sourceBytes, SignSeal seal, SignRequest request) throws Exception {
        // 【资源保护】入口先做大小/魔数/页数校验，避免 PDF 炸弹直接把内存打爆
        assertPdfSafe(sourceBytes);
        String sealName = seal != null ? seal.getSealName() : "时间戳";
        List<SignPosition> positions = request.getPositions();
        log.info("开始 PDF 签章: 印章={}, 模式={}, 位置数={}", sealName, request.getSignMode(),
                positions != null ? positions.size() : 0);

        if (positions == null || positions.isEmpty()) {
            log.warn("签章位置为空，返回原始 PDF");
            return sourceBytes;
        }

        boolean isSpanMode = "span".equalsIgnoreCase(request.getSignMode());

        if (isSpanMode) {
            // P0-3: 骑缝章必须叠加 CA 数字签名，否则无防篡改效力，法律风险极高
            // 骑缝章仅是视觉防伪，没有 PKCS#7 数字签名则 PDF 可被篡改而无法察觉
            byte[] stampedBytes = renderPdfWithSealsItext(sourceBytes, positions);
            if (shouldUseManagedCert(request, seal)) {
                stampedBytes = applyManagedDigitalSignature(stampedBytes, seal.getSealId());
            } else if (seal != null && hasValidCert(seal)) {
                stampedBytes = applyDigitalSignature(stampedBytes, seal);
            } else {
                throw new ServiceException("骑缝章必须绑定 CA 证书才能签章，请先为印章配置数字证书");
            }
            log.info("骑缝章签章完成（含 CA 数字签名）: {} bytes", stampedBytes.length);
            return stampedBytes;
        }

        if (shouldUseManagedCert(request, seal) && isSingleUnifiedSignRequest(request)) {
            return signSingleWithManagedCert(sourceBytes, seal, request);
        }

        byte[] stampedBytes = renderPdfWithSealsItext(sourceBytes, positions);

        if (shouldUseManagedCert(request, seal)) {
            return applyManagedDigitalSignature(stampedBytes, seal.getSealId());
        }

        if (seal != null && hasValidCert(seal)) {
            stampedBytes = applyDigitalSignature(stampedBytes, seal);
        }

        log.info("PDF 签章完成: {} bytes", stampedBytes.length);
        return stampedBytes;
    }

    public byte[] signFromImage(byte[] sourceBytes, byte[] sealImageBytes, SignRequest request) throws Exception {
        assertPdfSafe(sourceBytes);
        List<SignPosition> positions = request.getPositions();
        if (positions == null || positions.isEmpty()) return sourceBytes;
        positions.forEach(p -> p.setSealFilePath("__inline_image__"));
        Map<String, byte[]> imageCache = new HashMap<>();
        imageCache.put("__inline_image__", sealImageBytes);
        return renderPdfWithSealsCached(sourceBytes, positions, imageCache);
    }

    public byte[] signWithManagedCert(byte[] sourceBytes, SignSeal seal, SignRequest request) throws Exception {
        assertPdfSafe(sourceBytes);
        if (seal == null || seal.getSealId() == null) {
            throw new ServiceException("印章未绑定平台证书");
        }
        List<SignPosition> positions = request.getPositions();
        if (positions == null || positions.isEmpty()) {
            return sourceBytes;
        }

        boolean isSpanMode = "span".equalsIgnoreCase(request.getSignMode());
        if (isSpanMode) {
            // P0-3: 托管证书模式下骑缝章也必须叠加 CA 数字签名
            byte[] stampedBytes = renderPdfWithSeals(sourceBytes, positions);
            stampedBytes = applyManagedDigitalSignature(stampedBytes, seal.getSealId());
            log.info("骑缝章签章完成（托管证书 + CA 数字签名）: {} bytes", stampedBytes.length);
            return stampedBytes;
        }

        return signEachPositionWithManagedCert(sourceBytes, seal, request);
    }

    // ==========================================================================
    // 多印章逐位签章（托管证书模式）
    // ==========================================================================

    private byte[] signEachPositionWithManagedCert(byte[] pdfBytes, SignSeal seal, SignRequest request) throws Exception {
        ManagedSignMaterial material = loadManagedMaterial(seal.getSealId());
        List<SignPosition> positions = request.getPositions();
        byte[] currentPdf = pdfBytes;

        // 获取第一页的页面高度（用于 Y 坐标翻转）
        float pageHeight = getPageHeight(currentPdf, 1);

        for (int i = 0; i < positions.size(); i++) {
            SignPosition position = positions.get(i);
            if (!isValidPosition(position)) {
                log.warn("跳过无效签章位置: page={}, x={}, y={}",
                        position.getPage(), position.getX(), position.getY());
                continue;
            }
            if (Boolean.TRUE.equals(position.getIsTimestampOnly())) {
                continue;
            }

            currentPdf = signSinglePositionWithManagedCert(currentPdf, seal, position, material);

            log.info("第 {}/{} 个印章签章完成: page={}, x={}, y={}",
                    i + 1, positions.size(), position.getPage(), position.getX(), position.getY());
        }

        log.info("全部 {} 个印章签章完成", positions.size());
        return currentPdf;
    }

    /**
     * 获取指定页面的高度
     */
    private float getPageHeight(byte[] pdfBytes, int pageNumber) {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            if (pageNumber >= 1 && pageNumber <= document.getNumberOfPages()) {
                PDPage page = document.getPage(pageNumber - 1);
                return page.getMediaBox().getHeight();
            }
        } catch (Exception e) {
            log.warn("获取页面高度失败: page={}, {}", pageNumber, e.getMessage());
        }
        return 842f; // 默认 A4 高度
    }

    private byte[] signSinglePositionWithManagedCert(byte[] pdfBytes, SignSeal seal,
                                                     SignPosition position, ManagedSignMaterial material) throws Exception {
        int pageNumber = position.getPage();
        float x = position.getX() != null ? position.getX() : 0f;
        float y = position.getY() != null ? position.getY() : 0f;
        float width = position.getWidth() != null ? position.getWidth() : DEFAULT_SEAL_PT;
        float height = position.getHeight() != null ? position.getHeight() : DEFAULT_SEAL_PT;

        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            PDPage page = doc.getPage(pageNumber - 1);
            PDRectangle mediaBox = page.getMediaBox();
            float rawW = mediaBox.getWidth();
            float rawH = mediaBox.getHeight();
            int rotation = page.getRotation();

            float viewW;
            float viewH;
            if (rotation == 90 || rotation == 270) {
                viewW = rawH;
                viewH = rawW;
            } else {
                viewW = rawW;
                viewH = rawH;
            }

            float viewBottomY = viewH - y - height;

            float rawX;
            float rawY;
            float rawWout = width;
            float rawHout = height;

            switch (rotation) {
                case 90:
                    rawX = viewBottomY;
                    rawY = x;
                    rawWout = height;
                    rawHout = width;
                    break;
                case 180:
                    rawX = viewW - x - width;
                    rawY = viewBottomY;
                    break;
                case 270:
                    rawX = y;
                    rawY = viewH - x - width;
                    rawWout = height;
                    rawHout = width;
                    break;
                default:
                    rawX = x;
                    rawY = viewBottomY;
                    break;
            }

            byte[] sealImgBytes = sealImageCache.getSanitizedImage(seal.getSealFilePath());
            ImageData stampImg = null;
            Rectangle signatureRect = new Rectangle(rawX, rawY, rawWout, rawHout);
            if (sealImgBytes != null) {
                stampImg = ImageDataFactory.create(sealImgBytes);
                signatureRect = scaleRectToFit(stampImg.getWidth(), stampImg.getHeight(),
                        rawWout, rawHout, new Rectangle(rawX, rawY, rawWout, rawHout));
            }

            String signerName = seal.getSealName() != null ? seal.getSealName() : "印章";

            return doDetachedSignature(pdfBytes, material.getPrivateKey(),
                    material.getCertificateChain(), signatureRect, pageNumber,
                    signerName, null, 0f,
                    PdfSignatureAppearance.RenderingMode.GRAPHIC, stampImg);
        }
    }

    // ==========================================================================
    // 单页单点托管证书签名
    // ==========================================================================

    public byte[] signSingleWithManagedCert(byte[] pdfBytes, SignSeal seal, SignRequest request) throws Exception {
        if (seal == null || seal.getSealId() == null) {
            throw new ServiceException("印章不存在");
        }
        ManagedSignMaterial material = loadManagedMaterial(seal.getSealId());
        return signSingleDetached(pdfBytes, seal, request, material);
    }

    private byte[] signSingleDetached(byte[] pdfBytes, SignSeal seal, SignRequest request,
                                       ManagedSignMaterial material) throws Exception {
        if (request == null || request.getPositions() == null || request.getPositions().size() != 1) {
            throw new ServiceException("当前版本仅支持单页单点签章");
        }
        if (material.getPrivateKey() == null) {
            throw new ServiceException("平台证书私钥不可用");
        }
        if (material.getCertificateChain() == null || material.getCertificateChain().isEmpty()) {
            throw new ServiceException("平台证书链不可用");
        }

        SignPosition position = request.getPositions().get(0);
        if (!isValidPosition(position)) {
            throw new ServiceException("签章位置不完整");
        }
        if (position.getWidth() == null || position.getHeight() == null) {
            throw new ServiceException("签章矩形尺寸不完整");
        }

        return signSinglePositionWithManagedCert(pdfBytes, seal, position, material);
    }

    // ==========================================================================
    // 通用数字签名方法（统一入口）
    // ==========================================================================

    /**
     * 执行 PKCS#7 分离式数字签名
     * @param pdfBytes 源 PDF 字节
     * @param privateKey 私钥
     * @param certChain 证书链
     * @param rect 签名矩形位置
     * @param pageNum 页码
     * @param reason 签名原因
     * @param layer2Text Layer2 文本（可为 null）
     * @param fontSize Layer2 字体大小
     * @param renderMode 渲染模式
     * @param graphicImage 图形图片（可为 null）
     * @return 签名后的 PDF 字节
     */
    private byte[] doDetachedSignature(byte[] pdfBytes, PrivateKey privateKey,
                                        List<Certificate> certChain, Rectangle rect, int pageNum,
                                        String signerName, String layer2Text, float fontSize,
                                        PdfSignatureAppearance.RenderingMode renderMode,
                                        ImageData graphicImage) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        StampingProperties props = new StampingProperties().useAppendMode();
        PdfSigner signer;
        try (PdfReader reader = new PdfReader(new ByteArrayInputStream(pdfBytes))) {
            signer = new PdfSigner(reader, baos, props);

            PdfSignatureAppearance appearance = signer.getSignatureAppearance();
            appearance.setReason("签章系统自动签名");
            appearance.setLocation("Digital Signing Service");

            boolean visible = rect != null && rect.getWidth() > 0 && rect.getHeight() > 0
                    && (graphicImage != null || (layer2Text != null && !layer2Text.isEmpty()));
            if (visible) {
                appearance.setPageRect(rect);
                appearance.setPageNumber(pageNum);
                appearance.setRenderingMode(renderMode);
                if (layer2Text != null && !layer2Text.isEmpty()) {
                    appearance.setLayer2Text(layer2Text);
                    if (fontSize > 0) {
                        appearance.setLayer2FontSize(fontSize);
                    }
                }
                if (graphicImage != null) {
                    appearance.setSignatureGraphic(graphicImage);
                }
            }

            IExternalSignature pks = new PrivateKeySignature(privateKey, DigestAlgorithms.SHA256,
                    BouncyCastleProvider.PROVIDER_NAME);
            IExternalDigest digest = new BouncyCastleDigest();
            Certificate[] chainArray = certChain.toArray(new Certificate[0]);
            signer.signDetached(digest, pks, chainArray, null, null, null, 0, PdfSigner.CryptoStandard.CMS);

            log.info("数字签名完成: signer={}, page={}, visible={}", signerName, pageNum, visible);
        }
        return baos.toByteArray();
    }

    // ==========================================================================
    // 传统证书签名（用户上传证书）
    // ==========================================================================

    private boolean hasValidCert(SignSeal seal) {
        return seal != null && seal.getCertPath() != null && !seal.getCertPath().trim().isEmpty();
    }

    private byte[] applyDigitalSignature(byte[] pdfBytes, SignSeal seal) throws Exception {
        // P0-1: 证书下载/解析失败必须抛异常，禁止静默跳过签名
        // 否则产出无 CA 签名效力的"假签章 PDF"，用户误以为已合法签章
        byte[] pfxBytes;
        try {
            pfxBytes = signStorage.download(seal.getCertPath());
        } catch (Exception e) {
            throw new ServiceException("CA签名失败：证书下载异常 - " + e.getMessage());
        }
        if (pfxBytes == null || pfxBytes.length == 0) {
            throw new ServiceException("CA签名失败：证书内容为空");
        }

        String password = seal.getCertInfo() != null ? extractPasswordFromCertInfo(seal.getCertInfo()) : "";
        java.security.KeyStore ks = certService.loadFromPfxBytes(pfxBytes, password);
        PrivateKey pk = certService.getPrivateKey(ks, password);
        Certificate[] chain = certService.getCertificateChain(ks);

        if (pk == null || chain == null || chain.length == 0) {
            throw new ServiceException("CA签名失败：无法从证书获取私钥或证书链");
        }

        return signDetachedLegacy(pdfBytes, pk, Arrays.asList(chain), seal.getSealName());
    }

    private byte[] applyManagedDigitalSignature(byte[] stampedBytes, Long sealId) throws Exception {
        if (sealId == null || keyProvider == null) {
            throw new ServiceException("印章未绑定平台证书");
        }
        ManagedSignMaterial material = keyProvider.loadBySealId(sealId);
        if (material == null) {
            throw new ServiceException("印章未绑定平台证书");
        }
        if (signingProvider == null) {
            log.debug("托管签名器未注入，当前先复用本地私钥签名链路");
        }
        if (material.getPrivateKey() == null) {
            throw new ServiceException("平台证书私钥不可用");
        }
        if (material.getCertificateChain() == null || material.getCertificateChain().isEmpty()) {
            throw new ServiceException("平台证书链不可用");
        }
        return signDetachedLegacy(stampedBytes, material.getPrivateKey(),
                material.getCertificateChain(), null);
    }

    private byte[] signDetachedLegacy(byte[] pdfBytes, PrivateKey pk, List<Certificate> certChain,
                                       String preferredSignerName) throws Exception {
        int pageCount;
        Rectangle pageSize;
        try (PdfReader reader = new PdfReader(new ByteArrayInputStream(pdfBytes));
             PdfDocument pdfDoc = new PdfDocument(reader)) {
            pageCount = pdfDoc.getNumberOfPages();
            int targetPage = Math.max(1, Math.min(1, pageCount));
            pageSize = pdfDoc.getPage(targetPage).getPageSize();
        }
        Rectangle signatureRect = buildDefaultSignatureRect(pageSize);

        String signerName = extractSignerName(certChain.get(0), preferredSignerName);

        return doDetachedSignature(pdfBytes, pk, certChain, signatureRect, Math.max(1, Math.min(1, pageCount)),
                signerName, null, 0f,
                PdfSignatureAppearance.RenderingMode.DESCRIPTION, null);
    }

    // ==========================================================================
    // 辅助：加载托管证书材料
    // ==========================================================================

    private ManagedSignMaterial loadManagedMaterial(Long sealId) {
        if (keyProvider == null) {
            throw new ServiceException("印章未绑定平台证书");
        }
        ManagedSignMaterial material;
        try {
            material = keyProvider.loadBySealId(sealId);
        } catch (Exception e) {
            log.error("加载印章证书失败: sealId={}", sealId, e);
            throw new ServiceException("加载印章证书失败: " + e.getMessage());
        }
        if (material == null) {
            throw new ServiceException("印章未绑定平台证书");
        }
        if (material.getPrivateKey() == null) {
            throw new ServiceException("平台证书私钥不可用");
        }
        if (material.getCertificateChain() == null || material.getCertificateChain().isEmpty()) {
            throw new ServiceException("平台证书链不可用");
        }
        return material;
    }

    // ==========================================================================
    // 辅助：印章图片加载（统一入口，使用缓存）
    // ==========================================================================

    public byte[] getSealImageBytes(SignSeal seal) throws Exception {
        if (seal == null || seal.getSealFilePath() == null) return null;
        byte[] bytes = sealImageCache.getRawImage(seal.getSealFilePath());
        if (bytes == null || bytes.length == 0) {
            throw new ServiceException("印章图片不存在或为空: " + seal.getSealFilePath());
        }
        return bytes;
    }

    // ==========================================================================
    // 核心渲染：按印章图片分组 → 逐页绘制
    // ==========================================================================

    private byte[] renderPdfWithSeals(byte[] sourceBytes, List<SignPosition> positions) throws Exception {
        Map<String, byte[]> imageCache = new HashMap<>();
        long startTime = System.currentTimeMillis();
        int cacheHit = 0;
        int cacheMiss = 0;

        for (SignPosition pos : positions) {
            String path = pos.getSealFilePath();
            if (path == null || path.isEmpty() || Boolean.TRUE.equals(pos.getIsTimestampOnly())) continue;
            if (imageCache.containsKey(path)) continue;

            byte[] imgBytes = sealImageCache.getSanitizedImage(path);
            if (imgBytes != null) {
                imageCache.put(path, imgBytes);
                cacheHit++;
                continue;
            }

            cacheMiss++;
            try {
                imgBytes = signStorage.download(path);
                if (imgBytes != null) {
                    imageCache.put(path, imgBytes);
                }
            } catch (Exception e) {
                log.warn("加载印章图片失败: {} -> {}", path, e.getMessage());
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("印章图片加载完成: 缓存命中={}, 缓存未命中={}, 耗时={}ms, 缓存统计: {}",
                cacheHit, cacheMiss, elapsed, sealImageCache.getStats());
        return renderPdfWithSealsCached(sourceBytes, positions, imageCache);
    }

    private byte[] renderPdfWithSealsCached(byte[] sourceBytes, List<SignPosition> positions,
                                            Map<String, byte[]> imageCache) throws Exception {
        try (PDDocument document = Loader.loadPDF(sourceBytes)) {
            int totalPages = document.getNumberOfPages();

            Map<Integer, List<SignPosition>> byPage = positions.stream()
                    .filter(p -> p.getPage() != null && p.getPage() >= 1 && p.getPage() <= totalPages)
                    .collect(Collectors.groupingBy(SignPosition::getPage));

            for (int pageNum = 1; pageNum <= totalPages; pageNum++) {
                List<SignPosition> pageSeals = byPage.get(pageNum);
                if (pageSeals == null || pageSeals.isEmpty()) continue;

                PDPage page = document.getPage(pageNum - 1);
                PDRectangle mediaBox = page.getMediaBox();
                float rawWidth = mediaBox.getWidth();
                float rawHeight = mediaBox.getHeight();
                int rotation = page.getRotation();

                float viewWidth;
                float viewHeight;
                if (rotation == 90 || rotation == 270) {
                    viewWidth = rawHeight;
                    viewHeight = rawWidth;
                } else {
                    viewWidth = rawWidth;
                    viewHeight = rawHeight;
                }

                try (PDPageContentStream cs = new PDPageContentStream(
                        document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {

                    for (SignPosition pos : pageSeals) {
                        renderOneSealRotated(document, cs, pos, imageCache,
                                rawWidth, rawHeight, viewWidth, viewHeight, rotation);
                    }
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        }
    }

    private byte[] renderPdfWithSealsItext(byte[] sourceBytes, List<SignPosition> positions) throws Exception {
        Map<String, byte[]> imageCache = new HashMap<>();
        long startTime = System.currentTimeMillis();
        int cacheHit = 0;
        int cacheMiss = 0;

        for (SignPosition pos : positions) {
            String path = pos.getSealFilePath();
            if (path == null || path.isEmpty() || Boolean.TRUE.equals(pos.getIsTimestampOnly())) continue;
            if (imageCache.containsKey(path)) continue;

            byte[] imgBytes = sealImageCache.getSanitizedImage(path);
            if (imgBytes != null) {
                imageCache.put(path, imgBytes);
                cacheHit++;
                continue;
            }

            cacheMiss++;
            try {
                imgBytes = signStorage.download(path);
                if (imgBytes != null) {
                    imageCache.put(path, imgBytes);
                }
            } catch (Exception e) {
                log.warn("加载印章图片失败: {} -> {}", path, e.getMessage());
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("印章图片加载完成: 缓存命中={}, 缓存未命中={}, 耗时={}ms, 缓存统计: {}",
                cacheHit, cacheMiss, elapsed, sealImageCache.getStats());
        return renderPdfWithSealsCachedItext(sourceBytes, positions, imageCache);
    }

    private byte[] renderPdfWithSealsCachedItext(byte[] sourceBytes, List<SignPosition> positions,
                                                 Map<String, byte[]> imageCache) throws Exception {
        try (PdfReader reader = new PdfReader(new ByteArrayInputStream(sourceBytes));
             ByteArrayOutputStream baos = new ByteArrayOutputStream();
             PdfWriter writer = new PdfWriter(baos);
             PdfDocument document = new PdfDocument(reader, writer)) {

            int totalPages = document.getNumberOfPages();

            Map<Integer, List<SignPosition>> byPage = positions.stream()
                    .filter(p -> p.getPage() != null && p.getPage() >= 1 && p.getPage() <= totalPages)
                    .collect(Collectors.groupingBy(SignPosition::getPage));

            for (int pageNum = 1; pageNum <= totalPages; pageNum++) {
                List<SignPosition> pageSeals = byPage.get(pageNum);
                if (pageSeals == null || pageSeals.isEmpty()) continue;

                PdfPage page = document.getPage(pageNum);
                Rectangle mediaBox = page.getMediaBox();
                float rawWidth = mediaBox.getWidth();
                float rawHeight = mediaBox.getHeight();
                int rotation = page.getRotation();

                float viewWidth;
                float viewHeight;
                if (rotation == 90 || rotation == 270) {
                    viewWidth = rawHeight;
                    viewHeight = rawWidth;
                } else {
                    viewWidth = rawWidth;
                    viewHeight = rawHeight;
                }

                PdfCanvas canvas = new PdfCanvas(page.newContentStreamBefore(), page.getResources(), document);

                for (SignPosition pos : pageSeals) {
                    renderOneSealRotatedItext(document, canvas, pos, imageCache,
                            rawWidth, rawHeight, viewWidth, viewHeight, rotation, pageNum);
                }
            }

            document.close();
            return baos.toByteArray();
        }
    }

    private void renderOneSealRotatedItext(PdfDocument document, PdfCanvas canvas, SignPosition seal,
                                           Map<String, byte[]> imageCache,
                                           float rawWidth, float rawHeight,
                                           float viewWidth, float viewHeight,
                                           int rotation, int pageNum) throws java.io.IOException {

        byte[] imageBytes = null;
        if (!Boolean.TRUE.equals(seal.getIsTimestampOnly())) {
            imageBytes = imageCache.get(seal.getSealFilePath());
            if (imageBytes == null) {
                log.warn("印章图片未加载，跳过: page={}, path={}", seal.getPage(), seal.getSealFilePath());
                return;
            }
        }

        float size = seal.getSize() != null ? seal.getSize() : DEFAULT_SEAL_PT;
        float x = seal.getX() != null ? seal.getX() : 0f;
        float y = seal.getY() != null ? seal.getY() : 0f;

        int spanTotal = seal.getSpanTotal() != null ? seal.getSpanTotal() : 0;
        int spanIndex = seal.getSpanIndex() != null ? seal.getSpanIndex() : 0;
        String spanPos = seal.getSpanPosition() != null ? seal.getSpanPosition() : "right";

        canvas.saveState();
        applyRotationTransformItext(canvas, rawWidth, rawHeight, rotation);

        if (Boolean.TRUE.equals(seal.getIsTimestampOnly())) {
            renderTimestampLabelItext(canvas, x, y, size, seal.getTimestamp(), viewWidth, viewHeight);
        } else {
            if (spanTotal > 0) {
                renderSpanSealItext(document, canvas, imageBytes, x, y, size, spanTotal, spanIndex, spanPos, viewWidth, viewHeight, pageNum);
            } else {
                renderNormalSealItext(document, canvas, imageBytes, x, y, size, viewWidth, viewHeight, pageNum);
            }

            if (Boolean.TRUE.equals(seal.getHasTimestamp()) && seal.getTimestamp() != null) {
                renderTimestampLabelItext(canvas, x, y + size, 12f, seal.getTimestamp(), viewWidth, viewHeight);
            }
        }

        canvas.restoreState();
    }

    private void applyRotationTransformItext(PdfCanvas canvas,
                                             float rawW, float rawH,
                                             int rotation) throws java.io.IOException {
        if (rotation == 0) return;
        switch (rotation) {
            case 90:
                canvas.concatMatrix(Math.cos(Math.toRadians(90)), Math.sin(Math.toRadians(90)),
                        -Math.sin(Math.toRadians(90)), Math.cos(Math.toRadians(90)), 0, rawW);
                break;
            case 180:
                canvas.concatMatrix(Math.cos(Math.toRadians(180)), Math.sin(Math.toRadians(180)),
                        -Math.sin(Math.toRadians(180)), Math.cos(Math.toRadians(180)), rawW, rawH);
                break;
            case 270:
                canvas.concatMatrix(Math.cos(Math.toRadians(270)), Math.sin(Math.toRadians(270)),
                        -Math.sin(Math.toRadians(270)), Math.cos(Math.toRadians(270)), rawH, 0);
                break;
            default:
                break;
        }
    }

    private void renderNormalSealItext(PdfDocument document, PdfCanvas canvas, byte[] imageBytes,
                                       float x, float y, float size, float pageWidth, float pageHeight, int pageNum) throws java.io.IOException {
        float drawW = Math.min(size, pageWidth);
        float drawH = Math.min(size, pageHeight);
        float drawX = Math.max(0, Math.min(x, pageWidth - drawW));
        float drawY = pageHeight - y - drawH;
        drawY = Math.max(0, Math.min(drawY, pageHeight - drawH));

        PdfExtGState gs = new PdfExtGState();
        gs.setFillOpacity(SEAL_ALPHA);
        gs.setStrokeOpacity(SEAL_ALPHA);
        canvas.setExtGState(gs);

        ImageData imageData = ImageDataFactory.create(imageBytes);
        com.itextpdf.layout.element.Image image = new com.itextpdf.layout.element.Image(imageData);
        image.scaleToFit(drawW, drawH);
        image.setFixedPosition(pageNum, drawX, drawY);
        com.itextpdf.layout.Document layoutDoc = new com.itextpdf.layout.Document(document);
        layoutDoc.add(image);

        canvas.restoreState();
    }

    private void renderSpanSealItext(PdfDocument document, PdfCanvas canvas, byte[] imageBytes,
                                     float x, float y, float size, int spanTotal, int spanIndex,
                                     String spanPos, float pageWidth, float pageHeight, int pageNum) throws java.io.IOException {
        float sliceWidth = size / Math.max(1, spanTotal);
        float offsetX = spanIndex * sliceWidth;

        float clipY = pageHeight - y - size;
        float clipH = size;
        float clipW = Math.min(sliceWidth, pageWidth);

        float clipX;
        if ("left".equalsIgnoreCase(spanPos)) {
            clipX = x;
        } else {
            clipX = pageWidth - sliceWidth - SPAN_EDGE_MARGIN_PT;
        }
        clipX = Math.max(0, Math.min(clipX, pageWidth - clipW));

        canvas.saveState();

        PdfExtGState gs = new PdfExtGState();
        gs.setFillOpacity(SEAL_ALPHA);
        gs.setStrokeOpacity(SEAL_ALPHA);
        canvas.setExtGState(gs);

        canvas.rectangle(clipX, clipY, clipW, clipH);
        canvas.clip();

        ImageData imageData = ImageDataFactory.create(imageBytes);
        com.itextpdf.layout.element.Image image = new com.itextpdf.layout.element.Image(imageData);
        image.scaleToFit(size, size);
        float imgX = clipX - offsetX;
        image.setFixedPosition(pageNum, imgX, clipY);
        com.itextpdf.layout.Document layoutDoc = new com.itextpdf.layout.Document(document);
        layoutDoc.add(image);

        canvas.restoreState();
    }

    private void renderTimestampLabelItext(PdfCanvas canvas, float x, float y, float fontSize,
                                           String text, float pageWidth, float pageHeight) throws java.io.IOException {
        if (text == null || text.isEmpty()) return;

        float textWidth = text.length() * fontSize * TIMESTAMP_CHAR_WIDTH_RATIO;
        float textHeight = fontSize;
        float w = textWidth + TIMESTAMP_PADDING * 2;
        float h = textHeight + TIMESTAMP_PADDING * 2;

        float drawX = Math.max(0, Math.min(x, pageWidth - w));
        float drawY = pageHeight - y - h;

        canvas.setFillColorRgb(1f, 1f, 1f);
        canvas.rectangle(drawX, drawY, w, h);
        canvas.fill();

        canvas.setStrokeColorRgb(0f, 0f, 0f);
        canvas.setLineWidth(0.5f);
        canvas.rectangle(drawX, drawY, w, h);
        canvas.stroke();

        canvas.beginText();
        canvas.setFillColorRgb(0f, 0f, 0f);
        canvas.setFontAndSize(PdfFontFactory.createFont("Helvetica"), fontSize);
        canvas.setTextMatrix(drawX + TIMESTAMP_PADDING, drawY + TIMESTAMP_PADDING);
        canvas.showText(text);
        canvas.endText();
    }

    // ==========================================================================
    // 单个印章的渲染
    // ==========================================================================

    private void renderOneSealRotated(PDDocument document, PDPageContentStream cs, SignPosition seal,
                                       Map<String, byte[]> imageCache,
                                       float rawWidth, float rawHeight,
                                       float viewWidth, float viewHeight,
                                       int rotation) throws java.io.IOException {

        byte[] imageBytes = null;
        if (!Boolean.TRUE.equals(seal.getIsTimestampOnly())) {
            imageBytes = imageCache.get(seal.getSealFilePath());
            if (imageBytes == null) {
                log.warn("印章图片未加载，跳过: page={}, path={}", seal.getPage(), seal.getSealFilePath());
                return;
            }
        }

        float size = seal.getSize() != null ? seal.getSize() : DEFAULT_SEAL_PT;
        float x = seal.getX() != null ? seal.getX() : 0f;
        float y = seal.getY() != null ? seal.getY() : 0f;

        int spanTotal = seal.getSpanTotal() != null ? seal.getSpanTotal() : 0;
        int spanIndex = seal.getSpanIndex() != null ? seal.getSpanIndex() : 0;
        String spanPos = seal.getSpanPosition() != null ? seal.getSpanPosition() : "right";

        cs.saveGraphicsState();
        applyRotationTransform(cs, rawWidth, rawHeight, rotation);

        if (Boolean.TRUE.equals(seal.getIsTimestampOnly())) {
            renderTimestampLabel(cs, x, y, size, seal.getTimestamp(), viewWidth, viewHeight);
        } else {
            if (spanTotal > 0) {
                renderSpanSeal(document, cs, imageBytes, x, y, size, spanTotal, spanIndex, spanPos, viewWidth, viewHeight);
            } else {
                renderNormalSeal(document, cs, imageBytes, x, y, size, viewWidth, viewHeight);
            }

            if (Boolean.TRUE.equals(seal.getHasTimestamp()) && seal.getTimestamp() != null) {
                renderTimestampLabel(cs, x, y + size, 12f, seal.getTimestamp(), viewWidth, viewHeight);
            }
        }

        cs.restoreGraphicsState();
    }

    private void applyRotationTransform(PDPageContentStream cs,
                                         float rawW, float rawH,
                                         int rotation) throws java.io.IOException {
        if (rotation == 0) return;
        switch (rotation) {
            case 90:
                cs.transform(Matrix.getRotateInstance(Math.toRadians(90), 0, 0));
                cs.transform(Matrix.getTranslateInstance(0, -rawW));
                break;
            case 180:
                cs.transform(Matrix.getRotateInstance(Math.toRadians(180), 0, 0));
                cs.transform(Matrix.getTranslateInstance(-rawW, -rawH));
                break;
            case 270:
                cs.transform(Matrix.getRotateInstance(Math.toRadians(270), 0, 0));
                cs.transform(Matrix.getTranslateInstance(-rawH, 0));
                break;
            default:
                break;
        }
    }

    private void renderOneSeal(PDDocument document, PDPageContentStream cs, SignPosition seal,
                               Map<String, byte[]> imageCache, float pageWidth, float pageHeight) throws java.io.IOException {

        if (Boolean.TRUE.equals(seal.getIsTimestampOnly())) {
            renderTimestampLabel(cs, seal.getX(), seal.getY(),
                    seal.getSize() != null ? seal.getSize() : 15f,
                    seal.getTimestamp(), pageWidth, pageHeight);
            return;
        }

        byte[] imageBytes = imageCache.get(seal.getSealFilePath());
        if (imageBytes == null) {
            log.warn("印章图片未加载，跳过: page={}, path={}", seal.getPage(), seal.getSealFilePath());
            return;
        }

        float size = seal.getSize() != null ? seal.getSize() : DEFAULT_SEAL_PT;
        float x = seal.getX() != null ? seal.getX() : 0f;
        float y = seal.getY() != null ? seal.getY() : 0f;

        int spanTotal = seal.getSpanTotal() != null ? seal.getSpanTotal() : 0;
        int spanIndex = seal.getSpanIndex() != null ? seal.getSpanIndex() : 0;
        String spanPos = seal.getSpanPosition() != null ? seal.getSpanPosition() : "right";

        if (spanTotal > 0) {
            renderSpanSeal(document, cs, imageBytes, x, y, size, spanTotal, spanIndex, spanPos, pageWidth, pageHeight);
        } else {
            renderNormalSeal(document, cs, imageBytes, x, y, size, pageWidth, pageHeight);
        }

        if (Boolean.TRUE.equals(seal.getHasTimestamp()) && seal.getTimestamp() != null) {
            renderTimestampLabel(cs, x, y + size, 12f, seal.getTimestamp(), pageWidth, pageHeight);
        }
    }

    /** 普通印章（single/fixed/custom）：透明度 + 按尺寸绘制 */
    private void renderNormalSeal(PDDocument document, PDPageContentStream cs, byte[] imageBytes,
                                   float x, float y, float size, float pageWidth, float pageHeight) throws java.io.IOException {
        float drawW = Math.min(size, pageWidth);
        float drawH = Math.min(size, pageHeight);
        float drawX = Math.max(0, Math.min(x, pageWidth - drawW));
        float drawY = pageHeight - y - drawH;
        drawY = Math.max(0, Math.min(drawY, pageHeight - drawH));

        applySealAlpha(cs);
        PDImageXObject pdImage = PDImageXObject.createFromByteArray(document, imageBytes, "seal");
        cs.drawImage(pdImage, drawX, drawY, drawW, drawH);
        cs.restoreGraphicsState();
    }

    /** 骑缝章：裁剪区域 + 偏移绘制完整印章 */
    private void renderSpanSeal(PDDocument document, PDPageContentStream cs, byte[] imageBytes,
                                float x, float y, float size, int spanTotal, int spanIndex,
                                String spanPos, float pageWidth, float pageHeight) throws java.io.IOException {
        float sliceWidth = size / Math.max(1, spanTotal);
        float offsetX = spanIndex * sliceWidth;

        float clipY = pageHeight - y - size;
        float clipH = size;
        float clipW = Math.min(sliceWidth, pageWidth);

        float clipX;
        if ("left".equalsIgnoreCase(spanPos)) {
            clipX = x;
        } else {
            clipX = pageWidth - sliceWidth - SPAN_EDGE_MARGIN_PT;
        }
        clipX = Math.max(0, Math.min(clipX, pageWidth - clipW));

        cs.saveGraphicsState();
        applySealAlpha(cs);

        cs.addRect(clipX, clipY, clipW, clipH);
        cs.clip();

        PDImageXObject pdImage = PDImageXObject.createFromByteArray(document, imageBytes, "seal");
        float imgX = clipX - offsetX;
        cs.drawImage(pdImage, imgX, clipY, size, size);

        cs.restoreGraphicsState();
    }

    /** 应用印章透明度 */
    private void applySealAlpha(PDPageContentStream cs) throws java.io.IOException {
        PDExtendedGraphicsState gs = new PDExtendedGraphicsState();
        gs.setNonStrokingAlphaConstant(SEAL_ALPHA);
        gs.setStrokingAlphaConstant(SEAL_ALPHA);
        cs.saveGraphicsState();
        cs.setGraphicsStateParameters(gs);
    }

    /** 时间戳标签：白底 + 黑边 + 黑色文字 */
    private void renderTimestampLabel(PDPageContentStream cs, float x, float y, float fontSize,
                                      String text, float pageWidth, float pageHeight) throws java.io.IOException {
        if (text == null || text.isEmpty()) return;

        float textWidth = text.length() * fontSize * TIMESTAMP_CHAR_WIDTH_RATIO;
        float textHeight = fontSize;
        float w = textWidth + TIMESTAMP_PADDING * 2;
        float h = textHeight + TIMESTAMP_PADDING * 2;

        float drawX = Math.max(0, Math.min(x, pageWidth - w));
        float drawY = pageHeight - y - h;

        cs.setNonStrokingColor(1f, 1f, 1f);
        cs.addRect(drawX, drawY, w, h);
        cs.fill();

        cs.setStrokingColor(0f, 0f, 0f);
        cs.setLineWidth(0.5f);
        cs.addRect(drawX, drawY, w, h);
        cs.stroke();

        cs.beginText();
        cs.setNonStrokingColor(0f, 0f, 0f);
        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), fontSize);
        cs.newLineAtOffset(drawX + TIMESTAMP_PADDING, drawY + TIMESTAMP_PADDING);
        cs.showText(text);
        cs.endText();
    }

    // ==========================================================================
    // 数字签名矩形计算
    // ==========================================================================

    /**
     * 构建签名矩形（已做坐标系转换）
     * 前端 Y 向下 → PDF Y 向上，需翻转
     *
     * @param position 签章位置
     * @param pageHeight PDF页面高度
     * @return iText Rectangle（已翻转坐标系）
     */
    private Rectangle buildSignatureRectFromPosition(SignPosition position, float pageHeight) {
        float x = position.getX();
        float y = position.getY();
        float w = position.getWidth() != null ? position.getWidth() : 100f;
        float h = position.getHeight() != null ? position.getHeight() : 100f;
        // Y 轴翻转：从屏幕坐标转为 PDF 坐标
        float pdfY = pageHeight - y - h;
        return new Rectangle(x, pdfY, w, h);
    }

    private Rectangle scaleRectToFit(float imgW, float imgH, float maxW, float maxH, Rectangle rect) {
        if (imgW <= 0 || imgH <= 0) return rect;
        float ratio = Math.min(maxW / imgW, maxH / imgH);
        float newW = imgW * ratio;
        float newH = imgH * ratio;
        float newX = rect.getX() + (maxW - newW) / 2;
        float newY = rect.getY() + (maxH - newH) / 2;
        return new Rectangle(newX, newY, newW, newH);
    }

    private Rectangle buildSingleSignRect(SignPosition position) {
        return new Rectangle(position.getX(), position.getY(), position.getWidth(), position.getHeight());
    }

    Rectangle scaleRectByVisibleRatio(SignPosition position, double ratio) {
        float targetWidth = position.getWidth();
        float targetHeight = position.getHeight();
        if (ratio <= 0d) {
            return new Rectangle(position.getX(), position.getY(), targetWidth, targetHeight);
        }
        float scaledWidth = (float) (targetWidth / ratio);
        float scaledHeight = (float) (targetHeight / ratio);
        return new Rectangle(position.getX(), position.getY(), scaledWidth, scaledHeight);
    }

    private Rectangle resolveVisibleSignRect(SignSeal seal, SignPosition position) {
        if (seal == null || visibleBoundsHelper == null) {
            return buildSingleSignRect(position);
        }
        try {
            byte[] imageBytes = sealImageCache.getSanitizedImage(seal.getSealFilePath());
            if (imageBytes == null) {
                return buildSingleSignRect(position);
            }
            SealVisibleBounds bounds = visibleBoundsHelper.detectVisibleBounds(imageBytes);
            if (bounds == null) {
                log.warn("seal visible bounds detect failed, fallback to raw image size: sealId={}", seal.getSealId());
                return buildSingleSignRect(position);
            }
            double ratio = Math.max(bounds.getVisibleWidthRatio(), bounds.getVisibleHeightRatio());
            return scaleRectByVisibleRatio(position, ratio);
        } catch (Exception e) {
            log.warn("seal visible bounds detect failed, fallback to raw image size: sealId={}, reason={}",
                    seal.getSealId(), e.getMessage());
            return buildSingleSignRect(position);
        }
    }

    Rectangle buildDefaultSignatureRect(Rectangle pageSize) {
        float signX = pageSize.getWidth() - DEFAULT_SIGNATURE_WIDTH - DEFAULT_SIGNATURE_RIGHT_MARGIN;
        float signY = DEFAULT_SIGNATURE_BOTTOM_MARGIN;
        return new Rectangle(signX, signY, DEFAULT_SIGNATURE_WIDTH, DEFAULT_SIGNATURE_HEIGHT);
    }

    // ==========================================================================
    // 判断与验证方法
    // ==========================================================================

    boolean shouldUseManagedCert(SignRequest request, SignSeal seal) {
        return request != null
                && "1".equals(request.getUseManagedKey())
                && seal != null
                && seal.getSealId() != null
                && seal.getCertId() != null
                && StringUtils.isNotEmpty(seal.getKeyRef());
    }

    private boolean isSingleUnifiedSignRequest(SignRequest request) {
        return request != null
                && ("single".equalsIgnoreCase(request.getSignMode()) || "custom".equalsIgnoreCase(request.getSignMode()))
                && request.getPositions() != null
                && request.getPositions().size() == 1;
    }

    private boolean isValidPosition(SignPosition position) {
        return position != null
                && position.getPage() != null
                && position.getX() != null
                && position.getY() != null;
    }

    // ==========================================================================
    // 资源保护：PDF 输入安全校验（防 PDF 炸弹、伪装文件）
    // ==========================================================================

    /**
     * 签章入口统一安全校验：
     * 1. 字节数组非空 &amp; 长度 &le; 50MB；
     * 2. 文件头魔数必须是 %PDF（防止把可执行文件/脚本改扩展名 .pdf 传入）；
     * 3. 实际页数 &le; 1000（防止无限页数 PDF 炸弹吃满 CPU/内存）。
     * <p>任何一项不通过直接抛 ServiceException，业务层友好返回。
     */
    private void assertPdfSafe(byte[] pdfBytes) {
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new ServiceException("PDF 内容为空，无法签章");
        }

        // 1) 大小限制
        if (pdfBytes.length > MAX_PDF_BYTES) {
            throw new ServiceException(
                    "PDF 过大：当前 " + (pdfBytes.length / 1024 / 1024) + "MB，"
                            + "上限 " + (MAX_PDF_BYTES / 1024 / 1024) + "MB，请先压缩后再签章");
        }

        // 2) 魔数校验（必须以 %PDF 开头）
        boolean isPdfMagic = pdfBytes.length >= 5
                && pdfBytes[0] == 0x25 // %
                && pdfBytes[1] == 0x50 // P
                && pdfBytes[2] == 0x44 // D
                && pdfBytes[3] == 0x46 // F
                && pdfBytes[4] == 0x2D; // -
        if (!isPdfMagic) {
            throw new ServiceException("文件不是合法 PDF（缺少 %PDF 头标记），请上传真实的 PDF 文件");
        }

        // 3) 页数限制：用 iText PdfReader 快速读取，避免真正 PDDocument 加载时就 OOM
        //    PdfReader 构造时只解析交叉引用表，内存消耗低；若解析失败也直接拒绝（损坏/恶意 PDF）
        try (com.itextpdf.kernel.pdf.PdfReader r = new com.itextpdf.kernel.pdf.PdfReader(
                new java.io.ByteArrayInputStream(pdfBytes));
             com.itextpdf.kernel.pdf.PdfDocument doc = new com.itextpdf.kernel.pdf.PdfDocument(r)) {
            int pages = doc.getNumberOfPages();
            if (pages > MAX_PDF_PAGES) {
                throw new ServiceException(
                        "PDF 页数过多：当前 " + pages + " 页，上限 " + MAX_PDF_PAGES
                                + " 页，建议拆分后逐份签章");
            }
            if (pages <= 0) {
                throw new ServiceException("PDF 解析失败：页数为 0，疑似损坏文件");
            }
        } catch (ServiceException se) {
            // 业务异常原样抛出（页数超限等）
            throw se;
        } catch (Exception e) {
            // iText 解析异常：说明 PDF 损坏/加密/恶意构造 — 一律拒绝，不让上层继续试
            log.warn("拒绝签章：PDF 解析失败（可能损坏、加密或构造恶意）, size={}, reason={}",
                    pdfBytes.length, e.getMessage());
            throw new ServiceException("PDF 解析失败（可能已加密或损坏），请检查文件后重试");
        }
    }

    // ==========================================================================
    // 证书信息提取
    // ==========================================================================

    private String extractSignerName(Certificate cert, String fallback) {
        if (cert instanceof java.security.cert.X509Certificate x509) {
            String name = x509.getSubjectX500Principal().getName();
            if (name != null && name.length() > 50) {
                return name.substring(0, 50) + "...";
            }
            return name;
        }
        return fallback;
    }

    private String extractPasswordFromCertInfo(String certInfo) {
        if (certInfo == null || certInfo.isEmpty()) return "";
        try {
            java.util.Map<String, Object> map = objectMapper.readValue(certInfo, java.util.Map.class);
            Object pw = map.get("password");
            return pw != null ? String.valueOf(pw) : "";
        } catch (Exception e) {
            return "";
        }
    }

    private String formatDate(Date date) {
        return new SimpleDateFormat(DATE_FORMAT_PATTERN).format(date);
    }

    // ==========================================================================
    // 证书信息提取与验证（用于验签页面）
    // ==========================================================================

    public String extractCertInfo(byte[] signedPdfBytes) {
        try (PdfDocument pdfDoc = new PdfDocument(new PdfReader(new ByteArrayInputStream(signedPdfBytes)))) {
            SignatureUtil sigUtil = new SignatureUtil(pdfDoc);
            List<String> sigNames = sigUtil.getSignatureNames();

            if (sigNames == null || sigNames.isEmpty()) {
                return "{\"info\":\"该 PDF 不含数字签名\",\"signatureCount\":0}";
            }

            List<String> certs = new ArrayList<>();
            for (String sigName : sigNames) {
                PdfPKCS7 pkcs7 = sigUtil.readSignatureData(sigName);
                if (pkcs7 != null) {
                    Certificate[] chain = pkcs7.getSignCertificateChain();
                    if (chain != null && chain.length > 0) {
                        certs.add(certService.parseCertInfo(chain[0]));
                    }
                }
            }

            return "{\"signatureCount\":" + certs.size() + ",\"signatures\":[" + String.join(",", certs) + "]}";
        } catch (Exception e) {
            log.error("提取证书信息失败: {}", e.getMessage());
            return "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}";
        }
    }

    /** 验证 PDF 签名完整性 */
    public boolean verifySignature(byte[] signedPdfBytes) {
        try (PdfDocument pdfDoc = new PdfDocument(new PdfReader(new ByteArrayInputStream(signedPdfBytes)))) {
            SignatureUtil sigUtil = new SignatureUtil(pdfDoc);
            List<String> sigNames = sigUtil.getSignatureNames();
            if (sigNames == null || sigNames.isEmpty()) {
                log.warn("待验证的 PDF 不含任何数字签名");
                return false;
            }

            boolean allValid = true;
            for (String sigName : sigNames) {
                try {
                    PdfPKCS7 pkcs7 = sigUtil.readSignatureData(sigName);
                    if (pkcs7 == null) { allValid = false; continue; }

                    boolean ok = pkcs7.verifySignatureIntegrityAndAuthenticity();
                    if (!ok) {
                        log.warn("签名 {} 完整性校验失败", sigName);
                        allValid = false;
                        continue;
                    }

                    Certificate[] certs = pkcs7.getSignCertificateChain();
                    if (certs != null && certs.length > 0 && certs[0] instanceof java.security.cert.X509Certificate x509) {
                        // P0-2: 必须用签名时间点校验证书有效期，而非当前时间
                        // 对"签名时证书有效、现在已过期"的合法历史签名不能误判为无效
                        Date signDate = pkcs7.getSignDate() != null
                                ? pkcs7.getSignDate().getTime()
                                : new Date();
                        try { x509.checkValidity(signDate); } catch (Exception ex) {
                            log.warn("签名 {} 在签署时间点证书无效: {}", sigName, ex.getMessage());
                            allValid = false;
                        }
                    }
                    log.info("签名 {} 验证通过", sigName);
                } catch (Exception inner) {
                    log.error("验证签名 {} 异常: {}", sigName, inner.getMessage());
                    allValid = false;
                }
            }
            return allValid;
        } catch (Exception e) {
            log.error("签名验证失败: {}", e.getMessage());
            return false;
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    // ==========================================================================
    // PDF 元数据读取
    // ==========================================================================

    /** 读取 PDF 元数据（作者/创建时间/签名信息等） */
    public java.util.Map<String, String> readMetadata(byte[] pdfBytes) {
        java.util.Map<String, String> meta = new java.util.LinkedHashMap<>();
        readPdfDocumentInfo(pdfBytes, meta);
        readSignatureMetadata(pdfBytes, meta);
        return meta;
    }

    /** 读取 PDF 文档信息（作者/创建者/生成时间等） */
    private void readPdfDocumentInfo(byte[] pdfBytes, java.util.Map<String, String> meta) {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            Object infoObj = document.getDocumentInformation();
            if (infoObj != null) {
                meta.put("author", invokeInfoMethod(infoObj, "getAuthor"));
                meta.put("creator", invokeInfoMethod(infoObj, "getCreator"));
                meta.put("producer", invokeInfoMethod(infoObj, "getProducer"));
                meta.put("created", invokeInfoMethod(infoObj, "getCreationDate"));
                meta.put("modified", invokeInfoMethod(infoObj, "getModificationDate"));
            } else {
                meta.put("author", "-");
                meta.put("creator", "-");
                meta.put("producer", "-");
                meta.put("created", "-");
                meta.put("modified", "-");
            }
            meta.put("pageCount", String.valueOf(document.getNumberOfPages()));
        } catch (Exception e) {
            log.warn("PDFBox 读取元数据失败: {}", e.getMessage());
            meta.put("author", "-"); meta.put("creator", "-"); meta.put("producer", "-");
            meta.put("created", "-"); meta.put("modified", "-"); meta.put("pageCount", "0");
        }
    }

    private String invokeInfoMethod(Object infoObj, String methodName) {
        try {
            java.lang.reflect.Method m = infoObj.getClass().getMethod(methodName);
            Object result = m.invoke(infoObj);
            return result != null ? result.toString() : "-";
        } catch (Exception ignored) {
            return "-";
        }
    }

    /** 读取 PDF 签名元数据（签名时间/签名人/证书信息等） */
    private void readSignatureMetadata(byte[] pdfBytes, java.util.Map<String, String> meta) {
        meta.put("signatureCount", "0");
        String[] defaultKeys = {"signTime", "signer", "certSerial", "certIssuer",
                "certValidFrom", "certValidTo", "certSubject", "certFingerprint"};
        for (String key : defaultKeys) {
            meta.put(key, "-");
        }
        try {
            PdfReader reader = new PdfReader(new ByteArrayInputStream(pdfBytes));
            try (PdfDocument pdfDoc = new PdfDocument(reader)) {
                SignatureUtil sigUtil = new SignatureUtil(pdfDoc);
                List<String> sigNames = sigUtil.getSignatureNames();

                if (sigNames == null || sigNames.isEmpty()) {
                    return;
                }
                meta.put("signatureCount", String.valueOf(sigNames.size()));

                String lastName = sigNames.get(sigNames.size() - 1);
                PdfPKCS7 pkcs7 = sigUtil.readSignatureData(lastName);
                if (pkcs7 != null) {
                    meta.put("signTime", pkcs7.getSignDate() != null ? pkcs7.getSignDate().getTime().toString() : "-");
                    meta.put("signer", pkcs7.getSignName() != null ? pkcs7.getSignName() : "-");

                    java.security.cert.X509Certificate x509 = extractX509Cert(pkcs7);
                    if (x509 != null) {
                        meta.put("certSerial", x509.getSerialNumber() != null ? x509.getSerialNumber().toString() : "-");
                        meta.put("certIssuer", x509.getIssuerX500Principal() != null ? x509.getIssuerX500Principal().getName() : "-");
                        meta.put("certSubject", x509.getSubjectX500Principal() != null ? x509.getSubjectX500Principal().getName() : "-");
                        try {
                            CertService fingerprintService = certService != null ? certService : new CertService();
                            meta.put("certFingerprint", fingerprintService.sha256Fingerprint(x509));
                        } catch (Exception ex) {
                            meta.put("certFingerprint", "-");
                        }
                        try {
                            meta.put("certValidFrom", x509.getNotBefore() != null ? x509.getNotBefore().toString() : "-");
                            meta.put("certValidTo", x509.getNotAfter() != null ? x509.getNotAfter().toString() : "-");
                        } catch (Exception ex) {
                            meta.put("certValidFrom", "-");
                            meta.put("certValidTo", "-");
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("iText 读取签名信息失败: {}", e.getMessage());
        }
    }

    private java.security.cert.X509Certificate extractX509Cert(PdfPKCS7 pkcs7) {
        Certificate[] certs = pkcs7.getSignCertificateChain();
        if (certs != null && certs.length > 0 && certs[0] instanceof java.security.cert.X509Certificate) {
            return (java.security.cert.X509Certificate) certs[0];
        }
        if (pkcs7.getSigningCertificate() instanceof java.security.cert.X509Certificate) {
            return (java.security.cert.X509Certificate) pkcs7.getSigningCertificate();
        }
        return null;
    }
}
