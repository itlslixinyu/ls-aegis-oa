package com.ls.aegis.biz.sign.service.impl;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfString;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.StampingProperties;
import com.itextpdf.signatures.BouncyCastleDigest;
import com.itextpdf.signatures.DigestAlgorithms;
import com.itextpdf.signatures.PdfSigner;
import com.itextpdf.signatures.PrivateKeySignature;
import com.ls.aegis.biz.sign.mapper.SignFileMapper;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.security.cert.Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SignUploadVerificationTest {
    private static final String AUTHOR = "document author";
    private static final String SIGNER = "signature identity";
    private static byte[] unsignedPdf;
    private static byte[] signedPdf;
    private SignExecuteServiceImpl service;
    private SignPdfServiceImpl pdf;

    @BeforeAll
    static void createPdfFixtures() throws Exception {
        var original = new ByteArrayOutputStream();
        try (var doc = new PdfDocument(new PdfWriter(original))) {
            doc.addNewPage();
            doc.getDocumentInfo().setAuthor(AUTHOR);
        }
        unsignedPdf = original.toByteArray();

        // Ephemeral self-signed identity; this fixture verifies integrity, never CA trust.
        var provider = new BouncyCastleProvider();
        Security.addProvider(provider);
        var generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        var keys = generator.generateKeyPair();
        var name = new X500Name("CN=" + SIGNER);
        var now = Instant.now();
        var builder = new JcaX509v3CertificateBuilder(name, BigInteger.ONE,
            Date.from(now.minus(1, ChronoUnit.DAYS)), Date.from(now.plus(1, ChronoUnit.DAYS)), name, keys.getPublic());
        var certificate = new JcaX509CertificateConverter().setProvider(provider).getCertificate(
            builder.build(new JcaContentSignerBuilder("SHA256withRSA").setProvider(provider).build(keys.getPrivate())));

        var output = new ByteArrayOutputStream();
        try (var reader = new PdfReader(new ByteArrayInputStream(unsignedPdf))) {
            var signer = new PdfSigner(reader, output, new StampingProperties().useAppendMode());
            signer.setSignatureEvent(dictionary -> dictionary.put(PdfName.Name, new PdfString(SIGNER)));
            signer.signDetached(new BouncyCastleDigest(),
                new PrivateKeySignature(keys.getPrivate(), DigestAlgorithms.SHA256, provider.getName()),
                new Certificate[]{certificate}, null, null, null, 0, PdfSigner.CryptoStandard.CMS);
        }
        signedPdf = output.toByteArray();
    }

    @BeforeEach
    void setup() {
        service = new SignExecuteServiceImpl();
        pdf = spy(new SignPdfServiceImpl());
        ReflectionTestUtils.setField(service, "pdfService", pdf);
        ReflectionTestUtils.setField(service, "fileMapper", mock(SignFileMapper.class));
    }

    private static MockMultipartFile file(String name, byte[] bytes) {
        return new MockMultipartFile("file", name, "application/pdf", bytes.clone());
    }

    private static Map<?, ?> section(Map<String, Object> result, String key) {
        return assertInstanceOf(Map.class, result.get(key));
    }

    @Test
    void integrityDoesNotClaimCertificateTrust() {
        var result = service.verifyFingerprintUpload(file("own.pdf", signedPdf));
        assertEquals(true, result.get("valid"));
        assertEquals(true, result.get("signaturePresent"));
        assertEquals(false, result.get("trustVerified"));
        assertEquals("信任链未验证", section(result, "ca").get("result"));
        assertEquals("warning", section(result, "ca").get("resultType"));
    }

    @Test
    void signerComesFromSignatureRatherThanDocumentAuthor() {
        var result = service.verifyFingerprintUpload(file("own.pdf", signedPdf));
        assertEquals(true, result.get("valid"));
        assertEquals(AUTHOR, section(result, "fingerprint").get("user"));
        assertEquals(SIGNER, section(result, "ca").get("signer"));
    }

    @Test
    void unsignedPdfDoesNotAssertTampering() {
        var result = service.verifyFingerprintUpload(file("unsigned.pdf", unsignedPdf));
        assertEquals(false, result.get("valid"));
        assertEquals(false, result.get("signaturePresent"));
        var fingerprint = section(result, "fingerprint");
        assertEquals("未检测到数字签名", fingerprint.get("result"));
        assertEquals("warning", fingerprint.get("resultType"));
        assertFalse(fingerprint.get("compareTip").toString().contains("被修改"));
        verify(pdf, never()).verifySignature(any());
    }

    @Test
    void invalidPdfIsReportedAsFormatFailure() {
        var result = service.verifyFingerprintUpload(file("invalid.pdf", new byte[]{1, 2, 3}));
        assertEquals(false, result.get("valid"));
        assertEquals("PDF解析或验签失败，请确认文件完整且未加密", result.get("message"));
    }

    @Test
    void uploadReadFailureDoesNotExposeImplementationDetails() throws IOException {
        var unreadable = mock(MultipartFile.class);
        when(unreadable.getOriginalFilename()).thenReturn("unreadable.pdf");
        when(unreadable.getBytes()).thenThrow(new IOException("internal-test-only-path"));
        var result = service.verifyFingerprintUpload(unreadable);
        assertEquals(false, result.get("valid"));
        assertEquals("PDF解析或验签失败，请确认文件完整且未加密", result.get("message"));
        assertFalse(result.toString().contains("internal-test-only-path"));
    }

    @Test
    void uppercaseExtensionRetainsNormalVerification() {
        assertEquals(true, service.verifyFingerprintUpload(file("own.PDF", signedPdf)).get("valid"));
    }

    @Test
    void emptyAndUnsupportedFilesDoNotInvokePdfParser() {
        assertEquals(false, service.verifyFingerprintUpload(file("empty.pdf", new byte[0])).get("valid"));
        assertEquals(false, service.verifyFingerprintUpload(file("own.txt", signedPdf)).get("valid"));
        verifyNoInteractions(pdf);
    }

    @Test
    void actualUnsignedPdfMetadataContainsSignatureCount() {
        var meta = pdf.readMetadata(unsignedPdf);
        assertEquals("1", meta.get("pageCount"));
        assertEquals("0", meta.get("signatureCount"));
        assertFalse(pdf.verifySignature(unsignedPdf));
    }

    @Test
    void unsignedUpdateAfterSigningIsNotReportedAsValid() throws Exception {
        var output = new ByteArrayOutputStream();
        try (var doc = new PdfDocument(new PdfReader(new ByteArrayInputStream(signedPdf)),
            new PdfWriter(output), new StampingProperties().useAppendMode())) {
            doc.getDocumentInfo().setTitle("unsigned revision");
        }
        var result = service.verifyFingerprintUpload(file("edited.pdf", output.toByteArray()));
        assertEquals(true, result.get("signaturePresent"));
        assertEquals(false, result.get("valid"));
        assertEquals("存在签后未签署修改", section(result, "fingerprint").get("result"));
    }
}
