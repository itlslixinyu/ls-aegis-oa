package com.ls.aegis.biz.sign.chain.task;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.StampingProperties;
import com.itextpdf.signatures.*;
import com.ls.aegis.biz.sign.chain.SignChainContext;
import com.ls.aegis.biz.sign.chain.SignTask;
import com.ls.aegis.biz.sign.domain.SignSeal;
import com.ls.aegis.biz.sign.domain.vo.GeneralSealConfig;
import com.ls.aegis.biz.sign.domain.vo.ManagedSignMaterial;
import com.ls.aegis.biz.sign.domain.vo.Position;
import com.ls.aegis.biz.sign.domain.vo.SignPosition;
import com.ls.aegis.biz.sign.domain.vo.SpanSealConfig;
import com.ls.aegis.biz.sign.service.ISignStorageService;
import com.ls.aegis.biz.sign.service.impl.SignPdfServiceImpl;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.List;

@Component
public class DigitalSignTask implements SignTask {

    private static final Logger log = LoggerFactory.getLogger(DigitalSignTask.class);

    // 默认签名位置参数
    private static final float DEFAULT_SIGN_X_OFFSET = 150f;
    private static final float DEFAULT_SIGN_Y = 30f;
    private static final float DEFAULT_SIGN_WIDTH = 120f;
    private static final float DEFAULT_SIGN_HEIGHT = 50f;
    private static final float SIGN_SEAL_OPACITY = 0.75f;
    private static final int SIGNER_NAME_MAX_LENGTH = 50;

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @Autowired
    private SignPdfServiceImpl pdfService;

    @Autowired
    private com.ls.aegis.biz.sign.mapper.SignSealMapper sealMapper;

    @Autowired
    private ISignStorageService signStorage;

    @Autowired(required = false)
    private com.ls.aegis.biz.sign.service.provider.KeyProvider keyProvider;

    @Value("${itls.sign.default-sign-page:1}")
    private int defaultSignPage;

    @Override
    public void execute(SignChainContext context) throws Exception {
        context.setChainStatus("signing");

        byte[] renderedPdf = context.getRenderedPdf() != null
            ? context.getRenderedPdf()
            : context.getSourcePdf();

        Long primarySealId = context.getPrimarySealId();
        if (primarySealId == null) {
            log.info("未指定签名印章，跳过数字签名");
            context.setFinalPdf(renderedPdf);
            return;
        }

        SignSeal seal = sealMapper.selectSealById(primarySealId);
        if (seal == null) {
            log.warn("签名印章不存在，跳过数字签名: sealId={}", primarySealId);
            context.setFinalPdf(renderedPdf);
            return;
        }

        boolean hasCert = hasValidCert(seal);
        boolean hasManagedCert = seal.getCertId() != null;
        if (!hasCert && !hasManagedCert) {
            log.info("印章无有效证书（本地证书或托管证书），跳过数字签名");
            context.setFinalPdf(renderedPdf);
            return;
        }

        boolean useManaged = hasManagedCert || Boolean.TRUE.equals(context.getUseManagedCert());
        log.info("执行数字签名: sealId={}, useManagedCert={}", primarySealId, useManaged);

        byte[] signedPdf;
        if (useManaged) {
            signedPdf = signWithManagedCertDetached(renderedPdf, seal, context);
        } else {
            signedPdf = signWithLocalCertDetached(renderedPdf, seal, context);
        }

        context.setFinalPdf(signedPdf);
        log.info("数字签名完成");
    }

    private byte[] signWithManagedCertDetached(byte[] pdfBytes, SignSeal seal, SignChainContext context) throws Exception {
        if (keyProvider == null) {
            log.warn("keyProvider未注入，跳过数字签名");
            return pdfBytes;
        }

        ManagedSignMaterial material = keyProvider.loadBySealId(seal.getSealId());
        if (material == null || material.getPrivateKey() == null || material.getCertificateChain() == null || material.getCertificateChain().isEmpty()) {
            log.warn("托管证书材料无效，跳过数字签名");
            return pdfBytes;
        }

        SignPosition signPos = findSignaturePosition(context);
        byte[] sealImgBytes = getSealImageBytes(context, seal.getSealId());
        return doDetachedSignature(pdfBytes, material.getPrivateKey(), material.getCertificateChain(),
            signPos.getPage(), signPos.getX(), signPos.getY(), signPos.getWidth(), signPos.getHeight(),
            sealImgBytes);
    }

    private byte[] signWithLocalCertDetached(byte[] pdfBytes, SignSeal seal, SignChainContext context) throws Exception {
        if (!hasValidCert(seal)) {
            log.warn("本地证书无效，跳过数字签名");
            return pdfBytes;
        }

        byte[] pfxBytes = null;
        try {
            pfxBytes = signStorage.download(seal.getCertPath());
        } catch (Exception e) {
            log.warn("下载证书失败: sealId={}, certPath={}, error={}", seal.getSealId(), seal.getCertPath(), e.getMessage());
            return pdfBytes;
        }

        if (pfxBytes == null || pfxBytes.length == 0) {
            log.warn("证书文件为空，跳过数字签名");
            return pdfBytes;
        }

        String password = seal.getCertInfo() != null ? extractPasswordFromCertInfo(seal.getCertInfo()) : "";
        KeyStore ks = null;
        try {
            ks = KeyStore.getInstance("PKCS12");
            ks.load(new ByteArrayInputStream(pfxBytes), password.toCharArray());
        } catch (Exception e) {
            log.warn("加载证书失败: sealId={}, error={}", seal.getSealId(), e.getMessage());
            return pdfBytes;
        }

        // 查找包含私钥的别名（KeyStore 中可能包含多个条目，必须找带私钥的）
        String alias = null;
        java.util.Enumeration<String> aliases = ks.aliases();
        while (aliases.hasMoreElements()) {
            String a = aliases.nextElement();
            if (ks.isKeyEntry(a)) {
                alias = a;
                break;
            }
        }
        if (alias == null) {
            log.warn("KeyStore 中未找到包含私钥的条目: sealId={}", seal.getSealId());
            return pdfBytes;
        }

        PrivateKey pk = (PrivateKey) ks.getKey(alias, password.toCharArray());
        java.security.cert.Certificate[] certChain = ks.getCertificateChain(alias);

        if (pk == null || certChain == null || certChain.length == 0) {
            log.warn("无法从证书获取私钥或证书链: sealId={}, alias={}", seal.getSealId(), alias);
            return pdfBytes;
        }

        List<Certificate> certList = Arrays.asList(certChain);
        SignPosition signPos = findSignaturePosition(context);
        byte[] sealImgBytes = getSealImageBytes(context, seal.getSealId());

        return doDetachedSignature(pdfBytes, pk, certList,
            signPos.getPage(), signPos.getX(), signPos.getY(), signPos.getWidth(), signPos.getHeight(),
            sealImgBytes);
    }

    private byte[] doDetachedSignature(byte[] pdfBytes, PrivateKey privateKey, List<Certificate> certChain,
                                        int pageNum, float x, float y, float width, float height,
                                        byte[] sealImageBytes) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        StampingProperties props = new StampingProperties().useAppendMode();
        PdfSigner signer;
        try (PdfReader reader = new PdfReader(new ByteArrayInputStream(pdfBytes))) {
            signer = new PdfSigner(reader, baos, props);

            PdfSignatureAppearance appearance = signer.getSignatureAppearance();
            appearance.setReason("签章系统自动签名");
            appearance.setLocation("Digital Signing Service");

            Rectangle rect = new Rectangle(x, y, width, height);
            appearance.setPageRect(rect);
            appearance.setPageNumber(pageNum);

            if (sealImageBytes != null && sealImageBytes.length > 0) {
                byte[] transparentSealBytes = makeSealTransparent(sealImageBytes, SIGN_SEAL_OPACITY);
                ImageData sealImage = ImageDataFactory.create(transparentSealBytes);
                appearance.setRenderingMode(PdfSignatureAppearance.RenderingMode.GRAPHIC);
                appearance.setSignatureGraphic(sealImage);
                log.info("使用半透明印章图片作为签名外观");
            } else {
                appearance.setRenderingMode(PdfSignatureAppearance.RenderingMode.DESCRIPTION);
                String signerName = extractSignerName(certChain.get(0), "印章");
                appearance.setLayer2Text(signerName);
                log.info("无印章图片，使用文本描述签名外观");
            }

            IExternalSignature pks = new PrivateKeySignature(privateKey, DigestAlgorithms.SHA256,
                BouncyCastleProvider.PROVIDER_NAME);
            IExternalDigest digest = new BouncyCastleDigest();
            Certificate[] chainArray = certChain.toArray(new Certificate[0]);
            signer.signDetached(digest, pks, chainArray, null, null, null, 0, PdfSigner.CryptoStandard.CMS);

            String signerName = extractSignerName(certChain.get(0), "印章");
            log.info("数字签名完成: signer={}, page={}, rect=({},{},{},{})", signerName, pageNum, x, y, width, height);
        }
        return baos.toByteArray();
    }

    private SignPosition findSignaturePosition(SignChainContext context) {
        SignPosition pos = new SignPosition();

        List<GeneralSealConfig> generalSeals = context.getGeneralSeals();
        if (generalSeals != null && !generalSeals.isEmpty()) {
            GeneralSealConfig firstGeneral = generalSeals.get(0);
            if (firstGeneral.getPositions() != null && !firstGeneral.getPositions().isEmpty()) {
                Position firstPos = firstGeneral.getPositions().get(0);
                float docHeight = context.getPageHeight();
                pos.setPage(firstPos.getPage() > 0 ? firstPos.getPage() : defaultSignPage);
                pos.setX(firstPos.getX() > 0 ? firstPos.getX() : (context.getPageWidth() - DEFAULT_SIGN_X_OFFSET));
                float topY = firstPos.getY() > 0 ? firstPos.getY() : DEFAULT_SIGN_Y;
                float h = firstPos.getHeight() > 0 ? firstPos.getHeight() : DEFAULT_SIGN_HEIGHT;
                pos.setY(docHeight - topY - h);
                pos.setWidth(firstPos.getWidth() > 0 ? firstPos.getWidth() : DEFAULT_SIGN_WIDTH);
                pos.setHeight(h);
                log.info("使用第一个普通印章位置作为签名位置: page={}, x={}, y={}, w={}, h={}",
                    pos.getPage(), pos.getX(), pos.getY(), pos.getWidth(), pos.getHeight());
                return pos;
            }
        }

        List<SpanSealConfig> spanSeals = context.getSpanSeals();
        if (spanSeals != null && !spanSeals.isEmpty()) {
            SpanSealConfig firstSpan = spanSeals.get(0);
            pos.setPage(firstSpan.getStartPage() > 0 ? firstSpan.getStartPage() : 1);
            pos.setX(context.getPageWidth() - DEFAULT_SIGN_X_OFFSET);
            pos.setY(DEFAULT_SIGN_Y);
            pos.setWidth(DEFAULT_SIGN_WIDTH);
            pos.setHeight(DEFAULT_SIGN_HEIGHT);
            log.info("使用骑缝章第一页作为签名位置: page={}", pos.getPage());
            return pos;
        }

        pos.setPage(defaultSignPage);
        pos.setX(context.getPageWidth() - 150f);
        pos.setY(30f);
        pos.setWidth(120f);
        pos.setHeight(50f);
        return pos;
    }

    private boolean hasValidCert(SignSeal seal) {
        return seal != null
            && seal.getCertPath() != null
            && !seal.getCertPath().trim().isEmpty();
    }

    private byte[] getSealImageBytes(SignChainContext context, Long sealId) {
        if (context.getSealImageCache() == null) return null;
        java.awt.image.BufferedImage img = context.getSealImageCache().get(sealId);
        if (img == null) return null;
        try {
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(img, "PNG", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.warn("获取印章图片字节失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 将印章图片处理为半透明
     * @param sealImageBytes 原始印章图片字节
     * @param alpha 透明度，0.0f~1.0f（1.0f为完全不透明）
     * @return 处理后的半透明图片字节
     */
    private byte[] makeSealTransparent(byte[] sealImageBytes, float alpha) throws Exception {
        BufferedImage original = ImageIO.read(new ByteArrayInputStream(sealImageBytes));
        if (original == null) {
            return sealImageBytes;
        }

        BufferedImage transparent = new BufferedImage(
            original.getWidth(), original.getHeight(), BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = transparent.createGraphics();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2d.drawImage(original, 0, 0, null);
        g2d.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(transparent, "PNG", baos);
        return baos.toByteArray();
    }

    private String extractSignerName(Certificate cert, String fallback) {
        if (cert instanceof java.security.cert.X509Certificate x509) {
            String name = x509.getSubjectX500Principal().getName();
            if (name != null && name.length() > SIGNER_NAME_MAX_LENGTH) {
                return name.substring(0, SIGNER_NAME_MAX_LENGTH) + "...";
            }
            return name;
        }
        return fallback;
    }

    private String extractPasswordFromCertInfo(String certInfo) {
        if (certInfo == null || certInfo.isEmpty()) return "";
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            java.util.Map<String, Object> map = mapper.readValue(certInfo, java.util.Map.class);
            Object pwd = map.get("password");
            return pwd != null ? pwd.toString() : "";
        } catch (Exception e) {
            log.debug("解析证书信息JSON失败: certInfo={}, error={}", certInfo, e.getMessage());
            return "";
        }
    }

    @Override
    public String getTaskName() {
        return "数字签名";
    }

    @Override
    public boolean canSkip(SignChainContext context) {
        return context.getPrimarySealId() == null;
    }
}