package com.ls.aegis.biz.sign.service.impl;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.ls.aegis.biz.sign.mapper.SignFileMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import java.io.ByteArrayOutputStream;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SignUploadVerificationTest {
    SignExecuteServiceImpl service;
    SignPdfServiceImpl pdf;
    @BeforeEach void setup() {
        service = new SignExecuteServiceImpl(); pdf = mock(SignPdfServiceImpl.class);
        ReflectionTestUtils.setField(service, "pdfService", pdf);
        ReflectionTestUtils.setField(service, "fileMapper", mock(SignFileMapper.class));
    }
    MockMultipartFile file(String name) { return new MockMultipartFile("file", name, "application/pdf", new byte[]{1,2,3}); }
    @SuppressWarnings("unchecked") Map<String,Object> section(Map<String,Object> r, String key) { return (Map<String,Object>)r.get(key); }
    void signed() {
        when(pdf.readMetadata(any())).thenReturn(Map.of("pageCount","1","signatureCount","1", "certSerial","test-serial", "certIssuer","test-self-signed", "author","document author", "signer","signature identity"));
        when(pdf.verifySignature(any())).thenReturn(true);
    }
    @Test void integrityDoesNotClaimCertificateTrust() {
        signed(); var r=service.verifyFingerprintUpload(file("own.pdf"));
        assertEquals(true,r.get("valid")); assertEquals(true,r.get("signaturePresent")); assertEquals(false,r.get("trustVerified"));
        assertEquals("信任链未验证",section(r,"ca").get("result")); assertEquals("warning",section(r,"ca").get("resultType"));
    }
    @Test void signerComesFromSignatureRatherThanDocumentAuthor() {
        signed(); var r=service.verifyFingerprintUpload(file("own.pdf"));
        assertEquals("signature identity",section(r,"ca").get("signer"));
    }
    @Test void unsignedPdfDoesNotAssertTampering() {
        when(pdf.readMetadata(any())).thenReturn(Map.of("pageCount","1","signatureCount","0"));
        var r=service.verifyFingerprintUpload(file("unsigned.pdf"));
        assertEquals(false,r.get("valid")); assertEquals(false,r.get("signaturePresent"));
        assertEquals("未检测到数字签名",section(r,"fingerprint").get("result"));
        assertEquals("warning",section(r,"fingerprint").get("resultType"));
        assertFalse(section(r,"fingerprint").get("compareTip").toString().contains("被修改"));
        verify(pdf,never()).verifySignature(any());
    }
    @Test void invalidPdfIsReportedAsFormatFailure() {
        when(pdf.readMetadata(any())).thenReturn(Map.of("pageCount","0","signatureCount","0"));
        var r=service.verifyFingerprintUpload(file("invalid.pdf"));
        assertEquals(false,r.get("valid"));assertEquals("PDF解析或验签失败，请确认文件完整且未加密",r.get("message"));
    }
    @Test void parserExceptionDoesNotExposeImplementationDetails() {
        when(pdf.readMetadata(any())).thenThrow(new IllegalStateException("internal-test-only-path"));
        var r=service.verifyFingerprintUpload(file("invalid.pdf"));
        assertFalse(r.toString().contains("internal-test-only-path")); assertEquals(false,r.get("valid"));
    }
    @Test void uppercaseExtensionRetainsNormalVerification() {
        signed();assertEquals(true,service.verifyFingerprintUpload(file("own.PDF")).get("valid"));
    }
    @Test void emptyAndUnsupportedFilesDoNotInvokePdfParser() {
        assertEquals(false,service.verifyFingerprintUpload(new MockMultipartFile("file",new byte[0])).get("valid"));
        assertEquals(false,service.verifyFingerprintUpload(file("own.txt")).get("valid"));verifyNoInteractions(pdf);
    }
    @Test void actualUnsignedPdfMetadataContainsSignatureCount() throws Exception {
        var bytes=new ByteArrayOutputStream();try(var doc=new PdfDocument(new PdfWriter(bytes))){doc.addNewPage();}
        var actual=new SignPdfServiceImpl();var meta=actual.readMetadata(bytes.toByteArray());
        assertEquals("1",meta.get("pageCount"));assertEquals("0",meta.get("signatureCount"));assertFalse(actual.verifySignature(bytes.toByteArray()));
    }
}
