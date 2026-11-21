package com.ls.aegis.biz.sign.service.impl;

import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Div;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.ls.aegis.biz.sign.domain.SignVerifyReceipt;
import com.ls.aegis.biz.sign.mapper.SignVerifyReceiptMapper;
import com.ls.aegis.biz.sign.service.IReceiptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 验签回单服务实现。
 */
@Service("signReceiptServiceImpl")
public class ReceiptServiceImpl implements IReceiptService {

    @Autowired(required = false)
    private SignVerifyReceiptMapper signVerifyReceiptMapper;

    @Override
    public SignVerifyReceipt buildReceipt(String recordId, Long originFileId, Long signedFileId,
                                          Long certId, String serialNo, String fingerprintSha256) {
        SignVerifyReceipt receipt = new SignVerifyReceipt();
        receipt.setRecordId(recordId);
        receipt.setOriginFileId(originFileId);
        receipt.setSignedFileId(signedFileId);
        receipt.setCertId(certId);
        receipt.setSerialNo(serialNo);
        receipt.setFingerprintSha256(fingerprintSha256);
        receipt.setReceiptNo("SVR-" + System.currentTimeMillis());
        receipt.setVerifyStatus("1");
        receipt.setVerifiedAt(new Date());
        return receipt;
    }

    @Override
    public SignVerifyReceipt selectByRecordId(String recordId) {
        return signVerifyReceiptMapper == null ? null : signVerifyReceiptMapper.selectByRecordId(recordId);
    }

    @Override
    public byte[] renderReceiptPdf(SignVerifyReceipt receipt) {
        if (receipt == null) {
            throw new IllegalArgumentException("receipt must not be null");
        }
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfFont font = createFont();
            PdfWriter writer = new PdfWriter(outputStream);
            PdfDocument pdfDocument = new PdfDocument(writer);
            Document document = new Document(pdfDocument, PageSize.A4);
            document.setMargins(36, 36, 36, 36);

            document.add(new Paragraph("签章回单")
                    .setFont(font)
                    .setBold()
                    .setFontSize(18)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(4));
            document.add(new Paragraph("签署记录与证书快照")
                    .setFont(font)
                    .setFontSize(10)
                    .setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(18));

            document.add(createSection("回单信息", font, new String[][]{
                    {"回单编号", defaultText(receipt.getReceiptNo()), "生成时间", formatDate(receipt.getVerifiedAt())}
            }));
            document.add(createSection("签章信息", font, new String[][]{
                    {"业务记录ID", defaultText(receipt.getRecordId()), "原始文件ID", defaultText(receipt.getOriginFileId())},
                    {"签章文件ID", defaultText(receipt.getSignedFileId()), "回单生成状态", "已生成"}
            }));
            document.add(createSection("证书信息（SHA-256指纹）", font, new String[][]{
                    {"证书ID", defaultText(receipt.getCertId()), "证书序列号", defaultText(receipt.getSerialNo())},
                    {"证书指纹", defaultText(receipt.getFingerprintSha256()), "", ""}
            }));
            document.add(createConclusion(receipt, font));
            document.add(createSystemSealBlock(receipt, font));
            document.add(new Paragraph("此回单由系统实时生成，可用于留档与核验。")
                    .setFont(font)
                    .setFontSize(9)
                    .setFontColor(ColorConstants.GRAY)
                    .setMarginTop(14));
            document.close();
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("生成回单PDF失败", e);
        }
    }

    private PdfFont createFont() throws java.io.IOException {
        try {
            return PdfFontFactory.createFont("STSongStd-Light", "UniGB-UCS2-H");
        } catch (Exception ignored) {
            return PdfFontFactory.createFont(StandardFonts.HELVETICA, PdfEncodings.WINANSI);
        }
    }

    private Div createSection(String title, PdfFont font, String[][] rows) {
        Div section = new Div()
                .setBorder(new SolidBorder(ColorConstants.LIGHT_GRAY, 1))
                .setPadding(12)
                .setMarginBottom(12);
        section.add(new Paragraph(title)
                .setFont(font)
                .setBold()
                .setFontSize(12)
                .setMarginTop(0)
                .setMarginBottom(10));
        Table table = new Table(UnitValue.createPercentArray(new float[]{18, 32, 18, 32}))
                .useAllAvailableWidth();
        for (String[] row : rows) {
            addPair(table, font, row[0], row[1]);
            addPair(table, font, row[2], row[3]);
        }
        section.add(table);
        return section;
    }

    private Div createConclusion(SignVerifyReceipt receipt, PdfFont font) {
        Div section = new Div()
                .setBorder(new SolidBorder(ColorConstants.LIGHT_GRAY, 1))
                .setPadding(12);
        section.add(new Paragraph("回单说明")
                .setFont(font)
                .setBold()
                .setFontSize(12)
                .setMarginTop(0)
                .setMarginBottom(10));

        String statusText = statusText(receipt.getVerifyStatus());
        section.add(new Paragraph(statusText)
                .setFont(font)
                .setBold()
                .setFontSize(16)
                .setFontColor("1".equals(receipt.getVerifyStatus()) ? ColorConstants.GREEN : ColorConstants.RED)
                .setMarginBottom(6));
        section.add(new Paragraph(statusDescription(receipt))
                .setFont(font)
                .setFontSize(10)
                .setMarginBottom(8));
        section.add(new Paragraph("回单状态编码: " + defaultText(receipt.getVerifyStatus()))
                .setFont(font)
                .setFontSize(10)
                .setFontColor(ColorConstants.GRAY));
        return section;
    }

    private Div createSystemSealBlock(SignVerifyReceipt receipt, PdfFont font) {
        Div section = new Div()
                .setBorder(new SolidBorder(ColorConstants.RED, 1.5f))
                .setPadding(12)
                .setMarginTop(12);
        section.add(new Paragraph("系统签章回单")
                .setFont(font)
                .setBold()
                .setFontSize(14)
                .setFontColor(ColorConstants.RED)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(0)
                .setMarginBottom(8));
        section.add(new Paragraph("本回单根据电子签章系统的签署记录生成。")
                .setFont(font)
                .setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(4));
        section.add(new Paragraph("回单状态: " + statusText(receipt.getVerifyStatus()))
                .setFont(font)
                .setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(4));
        section.add(new Paragraph("回单编号: " + defaultText(receipt.getReceiptNo()) + "    生成时间: " + formatDate(receipt.getVerifiedAt()))
                .setFont(font)
                .setFontSize(9)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(ColorConstants.GRAY)
                .setMarginBottom(0));
        return section;
    }

    private void addPair(Table table, PdfFont font, String label, String value) {
        boolean emptyLabel = label == null || label.trim().isEmpty();
        table.addCell(createLabelCell(emptyLabel ? "" : label, font, emptyLabel));
        table.addCell(createValueCell(emptyLabel ? "" : value, font, emptyLabel));
    }

    private Cell createLabelCell(String text, PdfFont font, boolean blank) {
        Cell cell = new Cell()
                .setBorder(new SolidBorder(ColorConstants.LIGHT_GRAY, 1))
                .setPadding(8);
        if (!blank) {
            cell.setBackgroundColor(ColorConstants.LIGHT_GRAY);
        }
        cell.add(new Paragraph(blank ? "" : text)
                .setFont(font)
                .setBold()
                .setFontSize(9));
        return cell;
    }

    private Cell createValueCell(String text, PdfFont font, boolean blank) {
        return new Cell()
                .setBorder(new SolidBorder(ColorConstants.LIGHT_GRAY, 1))
                .setPadding(8)
                .add(new Paragraph(blank ? "" : defaultText(text))
                        .setFont(font)
                        .setFontSize(9));
    }

    private String defaultText(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value;
    }

    private String defaultText(Long value) {
        return value == null ? "-" : String.valueOf(value);
    }

    private String formatDate(Date value) {
        if (value == null) {
            return "-";
        }
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(value);
    }

    private String statusText(String verifyStatus) {
        if ("1".equals(verifyStatus)) {
            return "签署快照已生成";
        }
        if ("0".equals(verifyStatus)) {
            return "验证未通过";
        }
        return "待人工确认";
    }

    private String statusDescription(SignVerifyReceipt receipt) {
        String verifyStatus = receipt == null ? null : receipt.getVerifyStatus();
        if ("1".equals(verifyStatus)) {
            boolean hasCertSnapshot = receipt.getCertId() != null
                    || (receipt.getSerialNo() != null && !receipt.getSerialNo().isBlank())
                    || (receipt.getFingerprintSha256() != null && !receipt.getFingerprintSha256().isBlank());
            if (hasCertSnapshot) {
                return "本回单记录签署时的文件关联和证书快照；证书指纹不是文件内容哈希。请上传已签PDF执行签名完整性校验；证书信任链及撤销状态未验证。";
            }
            return "本回单未记录证书快照，不代表数字签名或证书信任验证通过。请上传已签PDF另行核验。";
        }
        if ("0".equals(verifyStatus)) {
            return "签章核验未通过，请结合原始文件、证书状态和签章链路进一步排查。";
        }
        return "当前回单缺少明确的验证结果，请结合业务记录做进一步确认。";
    }
}
