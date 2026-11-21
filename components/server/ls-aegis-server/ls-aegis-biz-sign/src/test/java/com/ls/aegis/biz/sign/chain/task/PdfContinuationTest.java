package com.ls.aegis.biz.sign.chain.task;

import com.itextpdf.kernel.pdf.*;
import com.itextpdf.signatures.*;
import com.ls.aegis.biz.sign.chain.SignChainContext;
import com.ls.aegis.biz.sign.domain.vo.*;
import com.ls.aegis.biz.sign.service.impl.SignPdfServiceImpl;
import com.ls.aegis.biz.sign.utils.PdfSignatureGuard;
import com.ls.aegis.compat.common.exception.ServiceException;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.*;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.Certificate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class PdfContinuationTest {
    static KeyPair keys;
    static List<Certificate> certificates;

    @BeforeAll static void certificate() throws Exception {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        var generator = KeyPairGenerator.getInstance("RSA"); generator.initialize(2048);
        keys = generator.generateKeyPair();
        var name = new X500Name("CN=ITLS-TEST-CONTINUATION");
        var builder = new JcaX509v3CertificateBuilder(name, BigInteger.ONE,
            new Date(System.currentTimeMillis() - 60000), new Date(System.currentTimeMillis() + 86400000), name, keys.getPublic());
        certificates = List.of(new JcaX509CertificateConverter().setProvider("BC").getCertificate(
            builder.build(new JcaContentSignerBuilder("SHA256withRSA").setProvider("BC").build(keys.getPrivate()))));
    }

    byte[] original() throws Exception {
        var output = new ByteArrayOutputStream();
        try (var doc = new PdfDocument(new PdfWriter(output, new WriterProperties().setCompressionLevel(0)))) {
            for (int i = 0; i < 5; i++) doc.addNewPage();
            doc.getDocumentInfo().setTitle("ORIGINAL_SIGNED_CONTENT");
        }
        return output.toByteArray();
    }

    byte[] sign(byte[] bytes) {
        return ReflectionTestUtils.invokeMethod(new DigitalSignTask(), "doDetachedSignature", bytes,
            keys.getPrivate(), certificates, 1, 80f, 100f, 80f, 80f, (byte[]) null);
    }

    SignChainContext context(byte[] source) {
        var context = new SignChainContext(); context.setSourcePdf(source); context.setPageCount(5);
        var image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        var graphics = image.createGraphics(); graphics.setColor(Color.RED); graphics.fillOval(0, 0, 100, 100); graphics.dispose();
        context.getSealImageCache().put(1L, image);
        var position = new Position(); position.setPage(1); position.setX(100f); position.setY(100f);
        position.setWidth(80f); position.setHeight(80f);
        var seal = new GeneralSealConfig(); seal.setSealId(1L); seal.setPositions(List.of(position));
        context.setGeneralSeals(List.of(seal));
        var span = new SpanSealConfig(); span.setSealId(1L); span.setStartPage(1); span.setEndPage(5);
        span.setSealSize(42f); span.setYPosition(200f); context.setSpanSeals(List.of(span));
        return context;
    }

    @Test void threeSigningsPreserveEverySignatureAndOriginalBytes() throws Exception {
        byte[] bytes = original();
        for (int round = 1; round <= 3; round++) {
            certificate(); // Different ephemeral signing keys for each party.
            var before = PdfSignatureGuard.beforeSigning(bytes);
            var context = context(bytes);
            new SpanSealRenderTask().execute(context);
            new GeneralSealRenderTask().execute(context);
            byte[] output = sign(context.getRenderedPdf());
            var result = PdfSignatureGuard.afterSigning(before, output, true);
            assertEquals(round, result.afterCount()); assertEquals(1, result.addedCount());
            assertTrue(result.previousSignaturesPreserved()); assertTrue(result.integrityVerified());
            if (round > 1) assertArrayEquals(bytes, Arrays.copyOf(output, bytes.length));
            var report = PdfSignatureGuard.inspect(output);
            assertTrue(report.continuationAllowed()); assertTrue(report.allSignaturesIntact());
            assertTrue(report.latestSignatureCoversWholeDocument());
            assertEquals(round, report.signatures().stream().map(PdfSignatureReport.Entry::fieldName).distinct().count());
            if (round > 1) assertFalse(report.signatures().get(0).coversWholeDocument());
            bytes = output;
        }
    }

    @Test void legacyRendererAlsoPreservesExistingSignature() throws Exception {
        byte[] first = sign(original()); var before = PdfSignatureGuard.beforeSigning(first);
        var image = new ByteArrayOutputStream(); ImageIO.write(context(first).getSealImageCache().get(1L), "png", image);
        var position = new SignPosition(); position.setPage(2); position.setX(100f); position.setY(100f);
        position.setWidth(80f); position.setHeight(80f); position.setSealFilePath("test-image");
        byte[] rendered = ReflectionTestUtils.invokeMethod(new SignPdfServiceImpl(), "renderPdfWithSealsCachedItext",
            first, List.of(position), Map.of("test-image", image.toByteArray()));
        assertEquals(2, PdfSignatureGuard.afterSigning(before, sign(rendered), true).afterCount());
    }

    @Test void alteredSignedContentIsRejectedBeforeSigning() throws Exception {
        byte[] bytes = sign(original());
        byte[] marker = "ORIGINAL_SIGNED_CONTENT".getBytes(StandardCharsets.US_ASCII);
        int offset = new String(bytes, StandardCharsets.ISO_8859_1).indexOf(new String(marker, StandardCharsets.US_ASCII));
        assertTrue(offset >= 0); bytes[offset] = 'X';
        assertFalse(PdfSignatureGuard.inspect(bytes).allSignaturesIntact());
        assertThrows(ServiceException.class, () -> PdfSignatureGuard.beforeSigning(bytes));
    }

    @Test void fullRewriteAndLostSignaturesCannotBeSaved() throws Exception {
        byte[] first = sign(original()); var before = PdfSignatureGuard.beforeSigning(first);
        var output = new ByteArrayOutputStream();
        try (var document = new PdfDocument(new PdfReader(new ByteArrayInputStream(first)), new PdfWriter(output))) { }
        byte[] rewrittenAndSigned = sign(output.toByteArray());
        assertThrows(ServiceException.class, () -> PdfSignatureGuard.afterSigning(before, rewrittenAndSigned, true));
        assertThrows(ServiceException.class, () -> PdfSignatureGuard.afterSigning(before, sign(original()), true));
    }

    @Test void unsignedAppendedChangesAreNotAutomaticallyAccepted() throws Exception {
        byte[] first = sign(original()); var output = new ByteArrayOutputStream();
        try (var doc = new PdfDocument(new PdfReader(new ByteArrayInputStream(first)), new PdfWriter(output),
            new StampingProperties().useAppendMode())) { doc.getDocumentInfo().setTitle("UNSIGNED CHANGE"); }
        var report = PdfSignatureGuard.inspect(output.toByteArray());
        assertTrue(report.allSignaturesIntact()); assertFalse(report.latestSignatureCoversWholeDocument());
        assertFalse(report.continuationAllowed());
        assertThrows(ServiceException.class, () -> PdfSignatureGuard.beforeSigning(output.toByteArray()));
    }

    @Test void lockedOrCertifiedFilesAreRejectedRatherThanClaimedSafe() throws Exception {
        for (int permission : List.of(PdfSigner.CERTIFIED_NO_CHANGES_ALLOWED, PdfSigner.CERTIFIED_FORM_FILLING)) {
            var output = new ByteArrayOutputStream();
            var signer = new PdfSigner(new PdfReader(new ByteArrayInputStream(original())), output, new StampingProperties().useAppendMode());
            signer.setCertificationLevel(permission);
            signer.signDetached(new BouncyCastleDigest(), new PrivateKeySignature(keys.getPrivate(), DigestAlgorithms.SHA256, "BC"),
                certificates.toArray(Certificate[]::new), null, null, null, 0, PdfSigner.CryptoStandard.CMS);
            assertTrue(PdfSignatureGuard.inspect(output.toByteArray()).allSignaturesIntact());
            assertFalse(PdfSignatureGuard.inspect(output.toByteArray()).continuationAllowed());
            assertThrows(ServiceException.class, () -> PdfSignatureGuard.beforeSigning(output.toByteArray()));
        }
    }

    @Test void imageRenderingWithoutNewDigitalSignatureCannotCountAsContinuation() throws Exception {
        byte[] first = sign(original()); var before = PdfSignatureGuard.beforeSigning(first);
        var context = context(first); new GeneralSealRenderTask().execute(context);
        assertThrows(ServiceException.class, () -> PdfSignatureGuard.afterSigning(before, context.getRenderedPdf(), true));
    }

    @Test void unsignedPdfDoesNotClaimSignatureIntegrityVerified() throws Exception {
        var report = PdfSignatureGuard.inspect(original());
        assertEquals(0, report.signatureCount()); assertFalse(report.allSignaturesIntact());
        assertTrue(report.continuationAllowed()); assertEquals("SIGNATURE_INTEGRITY_ONLY", report.verificationScope());
    }

    @Test void missingRequestedSealFailsWithoutPublishingPartialRender() throws Exception {
        var context = context(sign(original())); context.getSealImageCache().clear();
        assertThrows(ServiceException.class, () -> new GeneralSealRenderTask().execute(context));
        assertNull(context.getRenderedPdf());
        assertThrows(ServiceException.class, () -> new SpanSealRenderTask().execute(context));
        assertNull(context.getRenderedPdf());
    }

    @Test void emptySignatureFieldWithNullValueIsNotAnInvalidExistingSignature() throws Exception {
        var output = new ByteArrayOutputStream();
        try (var doc = new PdfDocument(new PdfWriter(output))) {
            doc.addNewPage();
            var field = new PdfDictionary(); field.put(PdfName.FT, PdfName.Sig);
            field.put(PdfName.T, new PdfString("EmptyApproval")); field.put(PdfName.V, PdfNull.PDF_NULL);
            field.makeIndirect(doc);
            var fields = new PdfArray(); fields.add(field);
            var form = new PdfDictionary(); form.put(PdfName.Fields, fields);
            doc.getCatalog().getPdfObject().put(PdfName.AcroForm, form);
        }
        assertEquals(0, PdfSignatureGuard.inspect(output.toByteArray()).signatureCount());
        assertTrue(PdfSignatureGuard.inspect(output.toByteArray()).continuationAllowed());
    }

    @Test void postVerificationFailureDoesNotUploadOrCreateSuccessfulRecords() throws Exception {
        byte[] first = sign(original());
        var service = new com.ls.aegis.biz.sign.service.impl.UnifiedSignServiceImpl();
        var project = new com.ls.aegis.biz.sign.domain.SignProject(); project.setStatus("2");
        var file = new com.ls.aegis.biz.sign.domain.SignFile(); file.setFileId(1L); file.setFileName("fixture.pdf");
        var projectMapper = mapper(com.ls.aegis.biz.sign.mapper.SignProjectMapper.class,
            Map.of("selectProjectById", project, "refreshExpiredProjects", 0));
        var fileMapper = mapper(com.ls.aegis.biz.sign.mapper.SignFileMapper.class, Map.of("selectFileById", file));
        var files = new com.ls.aegis.biz.sign.service.impl.SignFileServiceImpl() {
            @Override public Long resolveToSignFileId(Long fileId, Long sysFileId) { return 1L; }
            @Override public byte[] getFileBytes(Long fileId) { return first; }
            @Override public com.ls.aegis.biz.sign.domain.SignFile uploadBytes(byte[] data, String name, String type) {
                throw new AssertionError("Failed verification must not upload any result");
            }
        };
        var records = mapper(com.ls.aegis.biz.sign.mapper.SignRecordMapper.class, Map.of());
        var logs = mapper(com.ls.aegis.biz.sign.mapper.SignLogMapper.class, Map.of());
        var executor = new com.ls.aegis.biz.sign.chain.TaskChainExecutor() {
            @Override public SignChainContext executeSequential(List<com.ls.aegis.biz.sign.chain.SignTask> tasks, SignChainContext context) {
                try { context.setFinalPdf(sign(original())); return context; }
                catch (Exception e) { throw new AssertionError(e); }
            }
        };
        ReflectionTestUtils.setField(service, "projectMapper", projectMapper);
        ReflectionTestUtils.setField(service, "fileMapper", fileMapper);
        ReflectionTestUtils.setField(service, "fileService", files);
        ReflectionTestUtils.setField(service, "recordMapper", records);
        ReflectionTestUtils.setField(service, "logMapper", logs);
        ReflectionTestUtils.setField(service, "taskChainExecutor", executor);
        var request = new UnifiedSignRequest(); request.setFileId(1L); request.setProjectId(3L); request.setSealId(1L);
        assertThrows(ServiceException.class, () -> service.executeUnifiedSign(request));
    }

    @SuppressWarnings("unchecked")
    <T> T mapper(Class<T> type, Map<String, Object> replies) {
        return (T) java.lang.reflect.Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type},
            (proxy, method, args) -> {
                if (method.getName().equals("toString")) return "TestMapper";
                if (replies.containsKey(method.getName())) return replies.get(method.getName());
                throw new AssertionError("Unexpected persistence call: " + method.getName());
            });
    }
}
