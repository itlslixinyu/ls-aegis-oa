package com.ls.aegis.biz.psi.support;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

class DocxPlaceholderFillerTest {

    @Test
    void fillFromBuiltinReplacesPlaceholders() throws Exception {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("docNo", "DEMO-RK-001");
        data.put("warehouseName", "华东总仓");
        byte[] docx = DocxPlaceholderFiller.fillFromBuiltin(data);
        Assertions.assertTrue(docx.length > 100);
        String xml = readDocumentXml(docx);
        Assertions.assertTrue(xml.contains("DEMO-RK-001"));
        Assertions.assertTrue(xml.contains("华东总仓"));
        Assertions.assertFalse(xml.contains("{{docNo}}"));
    }

    private static String readDocumentXml(byte[] docx) throws Exception {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(docx))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if ("word/document.xml".equals(entry.getName())) {
                    return new String(zis.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        }
        throw new IllegalStateException("missing document.xml");
    }
}
