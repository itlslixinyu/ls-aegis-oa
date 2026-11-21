package com.ls.aegis.biz.psi.support;

import top.continew.starter.core.exception.BusinessException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * 轻量 Word(.docx) 占位符替换（不引入 POI：按 ZIP 内 document.xml 做字符串替换）。
 * <p>模板中占位符须落在同一文本 run，例如 {@code {{docNo}}}；跨 run 拆分时请改模板。</p>
 */
public final class DocxPlaceholderFiller {

    private static final String DOCUMENT_XML = "word/document.xml";

    private DocxPlaceholderFiller() {
    }

    /**
     * 无内置最小 docx（含 {{占位符}}）并填充；无上传 Word 模板时的兜底套打。
     */
    public static byte[] fillFromBuiltin(Map<String, String> placeholders) {
        Map<String, String> data = placeholders == null ? Map.of() : placeholders;
        return fill(buildBuiltinTemplate(data.keySet()), data);
    }

    /**
     * 生成含指定占位符的最小合法 docx（ZIP + document.xml）。
     */
    public static byte[] buildBuiltinTemplate(Iterable<String> keys) {
        StringBuilder paras = new StringBuilder();
        paras.append("<w:p><w:r><w:t>PSI 打印套打</w:t></w:r></w:p>");
        if (keys != null) {
            for (String key : keys) {
                if (key == null || key.isBlank() || "itemsHtml".equals(key)) {
                    continue;
                }
                String token = key.startsWith("{{") ? key : "{{" + key + "}}";
                paras.append("<w:p><w:r><w:t xml:space=\"preserve\">")
                    .append(escapeXml(key))
                    .append(": ")
                    .append(token)
                    .append("</w:t></w:r></w:p>");
            }
        }
        String documentXml = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
              <w:body>
            """ + paras + """
                <w:sectPr/>
              </w:body>
            </w:document>
            """;
        String contentTypes = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
              <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
              <Default Extension="xml" ContentType="application/xml"/>
              <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
            </Types>
            """;
        String rels = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
              <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
            </Relationships>
            """;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(bos)) {
            putEntry(zos, "[Content_Types].xml", contentTypes);
            putEntry(zos, "_rels/.rels", rels);
            putEntry(zos, DOCUMENT_XML, documentXml);
            zos.finish();
            return bos.toByteArray();
        } catch (IOException ex) {
            throw new BusinessException("生成 Word 模板失败：" + ex.getMessage());
        }
    }

    private static void putEntry(ZipOutputStream zos, String name, String content) throws IOException {
        zos.putNextEntry(new ZipEntry(name));
        zos.write(content.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }

    public static byte[] fill(byte[] templateDocx, Map<String, String> placeholders) {
        if (templateDocx == null || templateDocx.length == 0) {
            throw new BusinessException("Word 模板内容为空");
        }
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(templateDocx));
             ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(bos)) {
            ZipEntry entry;
            boolean foundDoc = false;
            byte[] buffer = new byte[8192];
            while ((entry = zis.getNextEntry()) != null) {
                ZipEntry outEntry = new ZipEntry(entry.getName());
                zos.putNextEntry(outEntry);
                if (DOCUMENT_XML.equals(entry.getName())) {
                    foundDoc = true;
                    ByteArrayOutputStream xmlBuf = new ByteArrayOutputStream();
                    int n;
                    while ((n = zis.read(buffer)) > 0) {
                        xmlBuf.write(buffer, 0, n);
                    }
                    String xml = xmlBuf.toString(StandardCharsets.UTF_8);
                    for (Map.Entry<String, String> e : placeholders.entrySet()) {
                        String key = e.getKey();
                        String token = key.startsWith("{{") ? key : "{{" + key + "}}";
                        String val = e.getValue() == null ? "" : escapeXml(e.getValue());
                        xml = xml.replace(token, val);
                    }
                    zos.write(xml.getBytes(StandardCharsets.UTF_8));
                } else {
                    int n;
                    while ((n = zis.read(buffer)) > 0) {
                        zos.write(buffer, 0, n);
                    }
                }
                zos.closeEntry();
                zis.closeEntry();
            }
            if (!foundDoc) {
                throw new BusinessException("非法 Word 模板：缺少 word/document.xml");
            }
            zos.finish();
            return bos.toByteArray();
        } catch (BusinessException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new BusinessException("Word 模板填充失败：" + ex.getMessage());
        }
    }

    private static String escapeXml(String raw) {
        return raw.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;");
    }
}
