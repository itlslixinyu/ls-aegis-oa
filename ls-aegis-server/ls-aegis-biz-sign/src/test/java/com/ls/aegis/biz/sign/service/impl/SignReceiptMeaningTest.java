package com.ls.aegis.biz.sign.service.impl;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import com.ls.aegis.biz.sign.domain.SignVerifyReceipt;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import static org.junit.jupiter.api.Assertions.*;

class SignReceiptMeaningTest {
    ReceiptServiceImpl service=new ReceiptServiceImpl();
    SignVerifyReceipt receipt(boolean certificate) {
        return service.buildReceipt("0123456789abcdef0123456789abcdef",1L,2L,certificate?3L:null,certificate?"TEST-SERIAL":null,certificate?"TEST-CERTIFICATE-FINGERPRINT":null);
    }
    String text(SignVerifyReceipt receipt) throws Exception {
        byte[] bytes=service.renderReceiptPdf(receipt);
        try(var doc=new PdfDocument(new PdfReader(new ByteArrayInputStream(bytes)))) {
            StringBuilder text=new StringBuilder();
            for(int i=1;i<=doc.getNumberOfPages();i++)text.append(PdfTextExtractor.getTextFromPage(doc.getPage(i)));
            return text.toString().replaceAll("\\s", "");
        }
    }
    @Test void actualPdfLabelsCertificateFingerprintCorrectly() throws Exception {
        String text=text(receipt(true));assertTrue(text.contains("证书信息（SHA-256指纹）"),text);
        assertTrue(text.contains("证书指纹TEST-CERTIFICATE-FINGERPRINT"),text);
        assertFalse(text.contains("文件指纹(SHA-256)"));assertTrue(text.contains("TEST-CERTIFICATE-FINGERPRINT"));
    }
    @Test void actualPdfDoesNotClaimTrustOrAutomaticVerification() throws Exception {
        String text=text(receipt(true));assertTrue(text.contains("信任链及撤销状态未验证"));
        assertFalse(text.contains("自动验签并生成"));assertTrue(text.contains("0123456789abcdef0123456789abcdef"));
    }
    @Test void absentCertificateDoesNotClaimDigitalSignatureSuccess() throws Exception {
        String text=text(receipt(false));assertTrue(text.contains("不代表数字签名或证书信任验证通过"));
    }
}
