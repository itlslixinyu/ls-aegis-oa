package com.ls.aegis.biz.quote.support;

import com.ls.aegis.biz.quote.constant.QuoteConstants;
import com.ls.aegis.biz.quote.domain.Quote;
import com.ls.aegis.biz.quote.domain.QuoteDetail;
import org.springframework.util.StringUtils;
import top.continew.starter.core.exception.BusinessException;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 报价单 Word(.docx) 导出。
 * <p>对齐本地参考实现 lsbj：手写 OOXML + store ZIP，避免 POI/html-docx 在 WPS 下 Logo 撑大、单元格缩进错乱。
 */
public final class QuoteDocxExporter {

    private static final int MATERIAL_ROW_MIN = 10;
    private static final int SERVICE_ROW_MIN = 1;
    /** 与 lsbj 一致：显示约 80px / 2.12cm；EMU = cm × 360000（只控版面，不砍原图像素） */
    private static final long LOGO_EMU = 762000L;
    private static final int CONTENT_WIDTH = 10540;
    private static final String FONT = "Microsoft YaHei";
    private static final Pattern DATA_URL = Pattern.compile("^data:(image/[a-zA-Z0-9.+-]+);base64,([\\s\\S]+)$");

    private QuoteDocxExporter() {
    }

    public static byte[] export(Quote quote, String logoUrl) {
        try {
            List<QuoteDetail> materials = filterLines(quote.getDetails(), QuoteConstants.LINE_TYPE_MATERIAL);
            List<QuoteDetail> services = filterLines(quote.getDetails(), QuoteConstants.LINE_TYPE_SERVICE);
            padRows(materials, MATERIAL_ROW_MIN, QuoteConstants.LINE_TYPE_MATERIAL);
            padRows(services, SERVICE_ROW_MIN, QuoteConstants.LINE_TYPE_SERVICE);

            LogoAsset logo = prepareLogo(logoUrl);
            boolean hasLogo = logo != null;
            String documentXml = buildDocumentXml(quote, materials, services, hasLogo);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(bos)) {
                // 仅 store，与 lsbj zipStore 一致，兼容性更好
                zos.setMethod(ZipOutputStream.STORED);
                putStored(zos, "[Content_Types].xml", contentTypesXml(logo));
                putStored(zos, "_rels/.rels", RELS_ROOT);
                putStored(zos, "word/document.xml", documentXml);
                putStored(zos, "word/_rels/document.xml.rels", docRelsXml(logo));
                putStored(zos, "word/styles.xml", STYLES_XML);
                if (hasLogo) {
                    putStored(zos, "word/media/logo." + logo.ext(), logo.bytes());
                }
            }
            return bos.toByteArray();
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("Word 导出失败：" + ex.getMessage());
        }
    }

    private static String buildDocumentXml(Quote quote, List<QuoteDetail> materials, List<QuoteDetail> services,
        boolean hasLogo) {
        String projectName = nullToEmpty(quote.getProjectName()).trim();
        String subtitle = "设备采购及技术服务报价单";
        BigDecimal materialSub = nvl(quote.getMaterialSubtotal());
        BigDecimal serviceSub = nvl(quote.getServiceSubtotal());
        BigDecimal preTax = QuoteAmountCalculator.preTaxTotal(quote);
        BigDecimal tax = QuoteAmountCalculator.taxAmount(quote);
        BigDecimal total = nvl(quote.getTotalAmount());
        String rate = quote.getTaxRate() == null ? "4" : quote.getTaxRate().stripTrailingZeros().toPlainString();
        String upper = QuoteAmountCalculator.amountUpper(quote);

        StringBuilder body = new StringBuilder(16384);
        body.append(buildHeaderBlock(projectName, subtitle, hasLogo));
        body.append(buildMetaBlock(List.of(
            "报价单位：" + nullToEmpty(quote.getQuoteUnit()),
            "报价单号：" + nullToEmpty(quote.getQuoteNo()),
            "报价时间：" + formatQuoteDate(quote)
        )));
        body.append(pLeft("一、材料部分", true, 20));
        body.append(buildMaterialTable(materials, materialSub));
        body.append(pLeft("", false, 10));
        body.append(pLeft("二、技术服务费部分", true, 20));
        body.append(buildServiceTable(services, serviceSub));
        body.append(pLeft("", false, 10));
        body.append(pLeft("三、费用合计", true, 20));
        body.append(buildSummaryTable(materialSub, serviceSub, preTax, tax, total, rate, upper));
        body.append(pCenter(summaryNote(rate), false, 13));
        body.append(pLeft("四、备注 / 条款", true, 20));
        appendTerms(body, quote.getTerms());

        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"
              xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"
              xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing"
              xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"
              xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture">
              <w:body>
            """ + body + """
                <w:sectPr>
                  <w:pgSz w:w="11906" w:h="16838"/>
                  <w:pgMar w:top="680" w:right="680" w:bottom="680" w:left="680" w:header="0" w:footer="0" w:gutter="0"/>
                </w:sectPr>
              </w:body>
            </w:document>
            """;
    }

    private static String buildHeaderBlock(String mainTitle, String subTitle, boolean hasLogo) {
        int w1 = 1900;
        int w3 = 1900;
        int w2 = CONTENT_WIDTH - w1 - w3;
        String logoInner = hasLogo ? wLogoDrawing("rIdLogo") : pCenter(" ", false, 15);
        String titleInner = pCenterTitle(mainTitle, 36) + pCenterTitle(subTitle, 22);
        return """
            <w:tbl><w:tblPr><w:tblW w:w="%d" w:type="dxa"/><w:tblBorders>
              <w:top w:val="nil"/><w:left w:val="nil"/><w:bottom w:val="nil"/><w:right w:val="nil"/>
              <w:insideH w:val="nil"/><w:insideV w:val="nil"/>
            </w:tblBorders><w:tblLayout w:type="fixed"/></w:tblPr>
            <w:tblGrid><w:gridCol w:w="%d"/><w:gridCol w:w="%d"/><w:gridCol w:w="%d"/></w:tblGrid>
            <w:tr>
              <w:tc><w:tcPr><w:tcW w:w="%d" w:type="dxa"/><w:vAlign w:val="center"/></w:tcPr>%s</w:tc>
              <w:tc><w:tcPr><w:tcW w:w="%d" w:type="dxa"/><w:vAlign w:val="center"/></w:tcPr>%s</w:tc>
              <w:tc><w:tcPr><w:tcW w:w="%d" w:type="dxa"/><w:vAlign w:val="center"/></w:tcPr><w:p><w:pPr><w:jc w:val="center"/></w:pPr></w:p></w:tc>
            </w:tr></w:tbl>
            """.formatted(CONTENT_WIDTH, w1, w2, w3, w1, logoInner, w2, titleInner, w3);
    }

    private static String buildMetaBlock(List<String> lines) {
        StringBuilder paras = new StringBuilder();
        for (String line : lines) {
            if (!StringUtils.hasText(line)) {
                continue;
            }
            paras.append(pRight(line.trim(), 15));
        }
        if (paras.isEmpty()) {
            return "";
        }
        return """
            <w:tbl><w:tblPr><w:tblW w:w="%d" w:type="dxa"/><w:jc w:val="left"/><w:tblInd w:w="0" w:type="dxa"/><w:tblBorders>
              <w:top w:val="nil"/><w:left w:val="nil"/><w:bottom w:val="nil"/><w:right w:val="nil"/>
              <w:insideH w:val="nil"/><w:insideV w:val="nil"/>
            </w:tblBorders><w:tblLayout w:type="fixed"/></w:tblPr>
            <w:tblGrid><w:gridCol w:w="%d"/></w:tblGrid>
            <w:tr><w:tc><w:tcPr><w:tcW w:w="%d" w:type="dxa"/><w:tcMar><w:top w:w="0" w:type="dxa"/><w:left w:w="0" w:type="dxa"/><w:bottom w:w="0" w:type="dxa"/><w:right w:w="0" w:type="dxa"/></w:tcMar></w:tcPr>%s</w:tc></w:tr>
            </w:tbl>
            """.formatted(CONTENT_WIDTH, CONTENT_WIDTH, CONTENT_WIDTH, paras);
    }

    private static String buildMaterialTable(List<QuoteDetail> rows, BigDecimal subtotal) {
        int[] pct = {8, 30, 14, 8, 9, 13, 18};
        int[] widths = widthsFromPct(pct);
        String[] headers = {"序号", "材料名称/规格", "品牌/型号", "单位", "数量", "单价(¥)", "金额(¥)"};
        StringBuilder sb = new StringBuilder();
        sb.append(tableStart(widths));
        sb.append(headerRow(headers, widths));
        for (int i = 0; i < rows.size(); i++) {
            QuoteDetail line = rows.get(i);
            boolean empty = !StringUtils.hasText(line.getItemName());
            sb.append(dataRow(widths, new String[]{
                String.valueOf(i + 1),
                nullToEmpty(line.getItemName()),
                empty ? "" : nullToEmpty(line.getBrandModel()),
                empty ? "" : nullToEmpty(line.getUnit()),
                empty ? "" : formatQty(line.getQuantity()),
                empty ? "" : formatMoney(line.getPrice()),
                empty ? "" : formatMoney(line.getAmount())
            }, false));
        }
        sb.append(subtotalRow(widths, "材料小计", formatMoney(subtotal)));
        sb.append("</w:tbl>");
        return sb.toString();
    }

    private static String buildServiceTable(List<QuoteDetail> rows, BigDecimal subtotal) {
        int[] pct = {8, 22, 20, 8, 10, 13, 19};
        int[] widths = widthsFromPct(pct);
        String[] headers = {"序号", "服务项目", "说明", "单位", "工作量", "单价(¥)", "金额(¥)"};
        StringBuilder sb = new StringBuilder();
        sb.append(tableStart(widths));
        sb.append(headerRow(headers, widths));
        for (int i = 0; i < rows.size(); i++) {
            QuoteDetail line = rows.get(i);
            boolean empty = !StringUtils.hasText(line.getItemName());
            sb.append(dataRow(widths, new String[]{
                String.valueOf(i + 1),
                nullToEmpty(line.getItemName()),
                empty ? "" : nullToEmpty(line.getDescription()),
                empty ? "" : nullToEmpty(line.getUnit()),
                empty ? "" : formatQty(line.getQuantity()),
                empty ? "" : formatMoney(line.getPrice()),
                empty ? "" : formatMoney(line.getAmount())
            }, false));
        }
        sb.append(subtotalRow(widths, "服务小计", formatMoney(subtotal)));
        sb.append("</w:tbl>");
        return sb.toString();
    }

    private static String buildSummaryTable(BigDecimal materialSub, BigDecimal serviceSub, BigDecimal preTax,
        BigDecimal tax, BigDecimal total, String rate, String upper) {
        int[] widths = widthsFromPct(new int[]{16, 17, 16, 17, 16, 18});
        StringBuilder sb = new StringBuilder();
        sb.append(tableStart(widths));
        sb.append(dataRow(widths, new String[]{
            "材料小计", formatMoney(materialSub),
            "税前合计", formatMoney(preTax),
            "总价(含税)", formatMoney(total)
        }, true));
        sb.append(dataRow(widths, new String[]{
            "技术服务费小计", formatMoney(serviceSub),
            "增值税综合税率*" + rate + "%", formatMoney(tax),
            "人民币大写", upper
        }, true));
        sb.append("</w:tbl>");
        return sb.toString();
    }

    private static int[] widthsFromPct(int[] pct) {
        int[] widths = new int[pct.length];
        int used = 0;
        for (int i = 0; i < pct.length; i++) {
            if (i == pct.length - 1) {
                widths[i] = CONTENT_WIDTH - used;
            } else {
                widths[i] = CONTENT_WIDTH * pct[i] / 100;
                used += widths[i];
            }
        }
        return widths;
    }

    private static String tableStart(int[] widths) {
        StringBuilder grid = new StringBuilder();
        for (int w : widths) {
            grid.append("<w:gridCol w:w=\"").append(w).append("\"/>");
        }
        return """
            <w:tbl><w:tblPr><w:tblStyle w:val="TableGrid"/><w:tblW w:w="%d" w:type="dxa"/><w:jc w:val="left"/><w:tblInd w:w="0" w:type="dxa"/><w:tblLayout w:type="fixed"/><w:tblCellMar><w:top w:w="40" w:type="dxa"/><w:left w:w="40" w:type="dxa"/><w:bottom w:w="40" w:type="dxa"/><w:right w:w="40" w:type="dxa"/></w:tblCellMar></w:tblPr>
            <w:tblGrid>%s</w:tblGrid>
            """.formatted(CONTENT_WIDTH, grid);
    }

    private static String headerRow(String[] headers, int[] widths) {
        StringBuilder tcs = new StringBuilder();
        for (int i = 0; i < headers.length; i++) {
            tcs.append(tc(headers[i], widths[i], true, 1));
        }
        return "<w:tr><w:trPr><w:trHeight w:val=\"300\" w:hRule=\"atLeast\"/></w:trPr>" + tcs + "</w:tr>";
    }

    private static String dataRow(int[] widths, String[] cells, boolean boldLabels) {
        StringBuilder tcs = new StringBuilder();
        for (int i = 0; i < cells.length; i++) {
            boolean bold = boldLabels && i % 2 == 0;
            tcs.append(tc(cells[i], widths[i], bold, 1));
        }
        return "<w:tr><w:trPr><w:trHeight w:val=\"270\" w:hRule=\"atLeast\"/></w:trPr>" + tcs + "</w:tr>";
    }

    private static String subtotalRow(int[] widths, String label, String amount) {
        int span = widths.length - 1;
        int spanW = 0;
        for (int i = 0; i < span; i++) {
            spanW += widths[i];
        }
        return "<w:tr><w:trPr><w:trHeight w:val=\"300\" w:hRule=\"atLeast\"/></w:trPr>"
            + tc(label, spanW, true, span)
            + tc(amount, widths[widths.length - 1], true, 1)
            + "</w:tr>";
    }

    private static String tc(String text, int width, boolean bold, int span) {
        String gridSpan = span > 1 ? "<w:gridSpan w:val=\"" + span + "\"/>" : "";
        return "<w:tc><w:tcPr>" + gridSpan
            + "<w:tcW w:w=\"" + width + "\" w:type=\"dxa\"/>"
            + "<w:tcBorders>" + border("top") + border("left") + border("bottom") + border("right") + "</w:tcBorders>"
            + "<w:tcMar><w:top w:w=\"40\" w:type=\"dxa\"/><w:left w:w=\"40\" w:type=\"dxa\"/><w:bottom w:w=\"40\" w:type=\"dxa\"/><w:right w:w=\"40\" w:type=\"dxa\"/></w:tcMar>"
            + "<w:vAlign w:val=\"center\"/></w:tcPr>"
            + pCell(text, bold, 15)
            + "</w:tc>";
    }

    private static String border(String side) {
        return "<w:" + side + " w:val=\"single\" w:sz=\"4\" w:space=\"0\" w:color=\"999999\"/>";
    }

    private static String fontPr() {
        return "<w:rFonts w:ascii=\"" + FONT + "\" w:hAnsi=\"" + FONT + "\" w:eastAsia=\"" + FONT
            + "\" w:cs=\"" + FONT + "\" w:hint=\"eastAsia\"/>";
    }

    private static String wRun(String text, boolean bold, int size) {
        String t = text == null ? "" : text;
        // 空单元格也要有可见段落高度；空 w:t 在部分 WPS 下异常
        String tNode = t.isEmpty()
            ? "<w:t xml:space=\"preserve\"> </w:t>"
            : "<w:t>" + xmlEscape(t) + "</w:t>";
        return "<w:r><w:rPr>" + fontPr() + (bold ? "<w:b/><w:bCs/>" : "")
            + "<w:sz w:val=\"" + size + "\"/><w:szCs w:val=\"" + size + "\"/></w:rPr>"
            + tNode + "</w:r>";
    }

    /** 表格单元格：强制居中，清掉中文默认首行缩进（WPS 关键点） */
    private static String pCell(String text, boolean bold, int fontSz) {
        int line = Math.max(240, fontSz * 20);
        return "<w:p><w:pPr><w:pStyle w:val=\"TableCenter\"/><w:spacing w:before=\"0\" w:after=\"0\" w:line=\""
            + line + "\" w:lineRule=\"exact\"/><w:ind w:start=\"0\" w:end=\"0\" w:left=\"0\" w:right=\"0\" w:firstLine=\"0\" w:firstLineChars=\"0\"/><w:jc w:val=\"center\"/></w:pPr>"
            + wRun(text, bold, fontSz) + "</w:p>";
    }

    private static String pLeft(String text, boolean bold, int size) {
        return "<w:p><w:pPr><w:pStyle w:val=\"Normal\"/><w:spacing w:before=\"0\" w:after=\"40\" w:line=\"276\" w:lineRule=\"auto\"/><w:ind w:left=\"0\" w:firstLine=\"0\" w:firstLineChars=\"0\"/><w:jc w:val=\"left\"/></w:pPr>"
            + wRun(text, bold, size) + "</w:p>";
    }

    private static String pRight(String text, int size) {
        return "<w:p><w:pPr><w:pStyle w:val=\"Normal\"/><w:spacing w:before=\"0\" w:after=\"20\"/><w:ind w:left=\"0\" w:firstLine=\"0\" w:firstLineChars=\"0\"/><w:jc w:val=\"right\"/></w:pPr>"
            + wRun(text, false, size) + "</w:p>";
    }

    private static String pCenter(String text, boolean bold, int size) {
        return "<w:p><w:pPr><w:pStyle w:val=\"Normal\"/><w:spacing w:before=\"40\" w:after=\"40\"/><w:ind w:left=\"0\" w:firstLine=\"0\" w:firstLineChars=\"0\"/><w:jc w:val=\"center\"/></w:pPr>"
            + wRun(text, bold, size) + "</w:p>";
    }

    private static String pCenterTitle(String text, int size) {
        return "<w:p><w:pPr><w:pStyle w:val=\"Normal\"/><w:spacing w:before=\"0\" w:after=\"60\"/><w:ind w:left=\"0\" w:firstLine=\"0\" w:firstLineChars=\"0\"/><w:jc w:val=\"center\"/></w:pPr>"
            + wRun(text, true, size) + "</w:p>";
    }

    /** 与 lsbj wLogoDrawing 一致：wp:extent 与 a:ext 同为 762000 EMU */
    private static String wLogoDrawing(String rId) {
        return """
            <w:p><w:pPr><w:spacing w:before="0" w:after="0"/><w:ind w:left="0" w:firstLine="0"/><w:jc w:val="left"/></w:pPr><w:r><w:drawing>
              <wp:inline distT="0" distB="0" distL="0" distR="0">
                <wp:extent cx="%d" cy="%d"/>
                <wp:effectExtent l="0" t="0" r="0" b="0"/>
                <wp:docPr id="1" name="logo"/>
                <wp:cNvGraphicFramePr><a:graphicFrameLocks xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" noChangeAspect="1"/></wp:cNvGraphicFramePr>
                <a:graphic xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main">
                  <a:graphicData uri="http://schemas.openxmlformats.org/drawingml/2006/picture">
                    <pic:pic xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture">
                      <pic:nvPicPr><pic:cNvPr id="0" name="logo"/><pic:cNvPicPr><a:picLocks noChangeAspect="1"/></pic:cNvPicPr></pic:nvPicPr>
                      <pic:blipFill><a:blip r:embed="%s"/><a:stretch><a:fillRect/></a:stretch></pic:blipFill>
                      <pic:spPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="%d" cy="%d"/></a:xfrm><a:prstGeom prst="rect"><a:avLst/></a:prstGeom></pic:spPr>
                    </pic:pic>
                  </a:graphicData>
                </a:graphic>
              </wp:inline>
            </w:drawing></w:r></w:p>
            """.formatted(LOGO_EMU, LOGO_EMU, rId, LOGO_EMU, LOGO_EMU);
    }

    private static String summaryNote(String rate) {
        return "本报价为含税总价。增值税综合税率" + rate
            + "%(含增值税及附加等);开票增值税税率以实际票面为准(小规模纳税人常见为1%)，无需另行支付税款。";
    }

    private static void appendTerms(StringBuilder body, String terms) {
        String content = StringUtils.hasText(terms) ? terms : defaultTermsText();
        for (String line : content.split("\\r?\\n")) {
            if (!StringUtils.hasText(line)) {
                continue;
            }
            body.append(pLeft(line.trim(), false, 13));
        }
    }

    /**
     * 与 lsbj 一致：嵌入原图像素（不压成 80px），仅用 OOXML EMU 控制版面显示大小，避免 Word/WPS 发糊。
     */
    private static LogoAsset prepareLogo(String logoUrl) {
        String src = QuoteLogoHelper.embeddableLogoSrc(logoUrl);
        if (!StringUtils.hasText(src)) {
            return null;
        }
        Matcher m = DATA_URL.matcher(src);
        if (!m.matches()) {
            return null;
        }
        try {
            String mime = m.group(1).toLowerCase(Locale.ROOT);
            byte[] bytes = Base64.getDecoder().decode(m.group(2).replaceAll("\\s", ""));
            if (bytes.length == 0) {
                return null;
            }
            String ext;
            String contentType;
            if (mime.contains("jpeg") || mime.contains("jpg")) {
                ext = "jpg";
                contentType = "image/jpeg";
            } else if (mime.contains("gif")) {
                ext = "gif";
                contentType = "image/gif";
            } else {
                // png / webp 等：Word 对 webp 支持差，仍按 png 扩展名写入原字节（与 lsbj 一致）
                ext = "png";
                contentType = "image/png";
            }
            return new LogoAsset(bytes, ext, contentType);
        } catch (Exception ex) {
            return null;
        }
    }

    private record LogoAsset(byte[] bytes, String ext, String contentType) {
    }

    private static void putStored(ZipOutputStream zos, String name, String content) throws Exception {
        putStored(zos, name, content.getBytes(StandardCharsets.UTF_8));
    }

    private static void putStored(ZipOutputStream zos, String name, byte[] data) throws Exception {
        ZipEntry entry = new ZipEntry(name);
        entry.setMethod(ZipEntry.STORED);
        entry.setSize(data.length);
        entry.setCompressedSize(data.length);
        CRC32 crc = new CRC32();
        crc.update(data);
        entry.setCrc(crc.getValue());
        zos.putNextEntry(entry);
        zos.write(data);
        zos.closeEntry();
    }

    private static final String RELS_ROOT = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
          <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
        </Relationships>
        """;

    private static String docRelsXml(LogoAsset logo) {
        String logoRel = logo == null
            ? ""
            : "<Relationship Id=\"rIdLogo\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/image\" Target=\"media/logo."
                + logo.ext() + "\"/>";
        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
              <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
              %s
            </Relationships>
            """.formatted(logoRel);
    }

    private static String contentTypesXml(LogoAsset logo) {
        String img = "";
        if (logo != null) {
            img = "<Default Extension=\"" + logo.ext() + "\" ContentType=\"" + logo.contentType() + "\"/>";
        }
        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
              <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
              <Default Extension="xml" ContentType="application/xml"/>
              %s
              <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
              <Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>
            </Types>
            """.formatted(img);
    }

    private static final String STYLES_XML = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
          <w:docDefaults>
            <w:rPrDefault><w:rPr><w:rFonts w:ascii="Microsoft YaHei" w:hAnsi="Microsoft YaHei" w:eastAsia="Microsoft YaHei" w:cs="Microsoft YaHei" w:hint="eastAsia"/><w:sz w:val="15"/><w:szCs w:val="15"/></w:rPr></w:rPrDefault>
            <w:pPrDefault><w:pPr><w:spacing w:after="0" w:line="240" w:lineRule="auto"/><w:ind w:firstLine="0" w:firstLineChars="0"/><w:jc w:val="left"/></w:pPr></w:pPrDefault>
          </w:docDefaults>
          <w:style w:type="paragraph" w:default="1" w:styleId="Normal">
            <w:name w:val="Normal"/><w:qFormat/>
            <w:pPr><w:spacing w:after="0"/><w:ind w:left="0" w:firstLine="0" w:firstLineChars="0"/><w:jc w:val="left"/></w:pPr>
            <w:rPr><w:rFonts w:ascii="Microsoft YaHei" w:hAnsi="Microsoft YaHei" w:eastAsia="Microsoft YaHei" w:cs="Microsoft YaHei" w:hint="eastAsia"/><w:sz w:val="15"/><w:szCs w:val="15"/></w:rPr>
          </w:style>
          <w:style w:type="paragraph" w:styleId="TableCenter">
            <w:name w:val="Table Center"/><w:basedOn w:val="Normal"/><w:qFormat/>
            <w:pPr><w:spacing w:before="0" w:after="0"/><w:ind w:left="0" w:right="0" w:firstLine="0" w:firstLineChars="0"/><w:jc w:val="center"/></w:pPr>
          </w:style>
          <w:style w:type="table" w:styleId="TableGrid">
            <w:name w:val="Table Grid"/>
            <w:tblPr>
              <w:tblInd w:w="0" w:type="dxa"/>
              <w:tblCellMar>
                <w:top w:w="40" w:type="dxa"/><w:left w:w="40" w:type="dxa"/>
                <w:bottom w:w="40" w:type="dxa"/><w:right w:w="40" w:type="dxa"/>
              </w:tblCellMar>
            </w:tblPr>
          </w:style>
        </w:styles>
        """;

    private static List<QuoteDetail> filterLines(List<QuoteDetail> details, String lineType) {
        if (details == null) {
            return new ArrayList<>();
        }
        return details.stream()
            .filter(d -> d != null && lineType.equals(d.getLineType()))
            .collect(Collectors.toCollection(ArrayList::new));
    }

    private static void padRows(List<QuoteDetail> rows, int min, String lineType) {
        while (rows.size() < min) {
            QuoteDetail blank = new QuoteDetail();
            blank.setLineType(lineType);
            rows.add(blank);
        }
    }

    private static String defaultTermsText() {
        return """
            1. 本报价单自出具之日起 7 个自然日内有效，有效期届满本报价自动失效，如需沿用本报价，需经供需双方重新书面确认。
            2. 报价为含税价，包含本市范围内运费，无其他额外费用。
            3. 付款方式：合同签订后预付 30%，设备到货后支付 50%，验收合格后支付余款 20%。""";
    }

    private static String formatQuoteDate(Quote quote) {
        if (quote.getQuoteDate() != null) {
            return formatDate(quote.getQuoteDate());
        }
        LocalDateTime createTime = quote.getCreateTime();
        if (createTime != null) {
            return createTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }
        return "";
    }

    private static String formatDate(Date date) {
        return date == null ? "" : new SimpleDateFormat("yyyy-MM-dd").format(date);
    }

    private static String formatMoney(BigDecimal val) {
        if (val == null) {
            return "";
        }
        return val.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String formatQty(BigDecimal val) {
        if (val == null) {
            return "";
        }
        return val.stripTrailingZeros().toPlainString();
    }

    private static BigDecimal nvl(BigDecimal val) {
        return val == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
            : val.setScale(2, RoundingMode.HALF_UP);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String xmlEscape(String raw) {
        return raw.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
