package com.ls.aegis.biz.sign.service.impl;

import com.ls.aegis.biz.sign.mapper.SignFileMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import java.io.ByteArrayOutputStream;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class PdfMetadataDateTest {
    byte[] unsignedPdf() throws Exception {
        try (var doc = new PDDocument(); var bytes = new ByteArrayOutputStream()) {
            doc.addPage(new PDPage());
            Calendar created = new GregorianCalendar(TimeZone.getTimeZone("GMT+08:00"));
            created.clear(); created.set(2026, Calendar.NOVEMBER, 12, 17, 4, 34);
            doc.getDocumentInformation().setCreationDate(created);
            doc.save(bytes);
            return bytes.toByteArray();
        }
    }
    @Test void formatsPdfCalendarWithoutExposingInternalObject() throws Exception {
        var metadata = new SignPdfServiceImpl().readMetadata(unsignedPdf());
        assertEquals("2026-11-12 17:04:34", metadata.get("created"));
        assertEquals("-", metadata.get("signTime"));
        assertFalse(metadata.toString().contains("GregorianCalendar"));
    }
    @Test void unsignedDocumentCreationTimeIsNotPresentedAsSigningTime() throws Exception {
        var service = new SignExecuteServiceImpl();
        ReflectionTestUtils.setField(service, "pdfService", new SignPdfServiceImpl());
        ReflectionTestUtils.setField(service, "fileMapper", mock(SignFileMapper.class));
        var result = service.verifyFingerprintUpload(new MockMultipartFile("file", "synthetic.pdf", "application/pdf", unsignedPdf()));
        assertEquals(false, result.get("valid"));
        assertEquals(false, result.get("signaturePresent"));
        assertEquals("-", ((Map<?, ?>) result.get("ca")).get("signTime"));
        assertEquals("2026-11-12 17:04:34", ((Map<?, ?>) result.get("fingerprint")).get("timestamp"));
    }
}
