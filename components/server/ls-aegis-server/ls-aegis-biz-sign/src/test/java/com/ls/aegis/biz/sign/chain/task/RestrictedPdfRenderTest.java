package com.ls.aegis.biz.sign.chain.task;

import com.ls.aegis.compat.common.exception.ServiceException;
import com.itextpdf.kernel.pdf.*;
import com.ls.aegis.biz.sign.chain.SignChainContext;
import com.ls.aegis.biz.sign.domain.vo.GeneralSealConfig;
import com.ls.aegis.biz.sign.domain.vo.Position;
import com.ls.aegis.biz.sign.domain.vo.SpanSealConfig;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RestrictedPdfRenderTest {
    byte[] pdf(boolean openingPassword) throws Exception {
        var out = new ByteArrayOutputStream();
        var props = new WriterProperties().setStandardEncryption(
            openingPassword ? "open-test".getBytes(StandardCharsets.UTF_8) : null,
            "owner-test".getBytes(StandardCharsets.UTF_8), EncryptionConstants.ALLOW_PRINTING,
            EncryptionConstants.ENCRYPTION_AES_128);
        try (var doc = new PdfDocument(new PdfWriter(out, props))) {
            for (int i = 0; i < 5; i++) doc.addNewPage();
        }
        return out.toByteArray();
    }

    SignChainContext context(boolean openingPassword) throws Exception {
        var context = new SignChainContext();
        context.setSourcePdf(pdf(openingPassword));
        context.setPageCount(5);
        var image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        var graphics = image.createGraphics();
        graphics.setColor(Color.RED);
        graphics.fillOval(0, 0, 100, 100);
        graphics.dispose();
        context.getSealImageCache().put(1L, image);
        var span = new SpanSealConfig();
        span.setSealId(1L); span.setStartPage(1); span.setEndPage(5);
        span.setSealSize(42f); span.setYPosition(200f);
        context.setSpanSeals(List.of(span));
        return context;
    }

    @Test void spanRendersAllFiveRestrictedPages() throws Exception {
        var context = context(false);
        new SpanSealRenderTask().execute(context);
        assertRendered(context, 5);
    }

    @Test void generalThenSpanRendersRestrictedPdf() throws Exception {
        var context = context(false);
        var position = new Position();
        position.setPage(1); position.setX(100f); position.setY(100f);
        position.setWidth(80f); position.setHeight(80f);
        var seal = new GeneralSealConfig();
        seal.setSealId(1L); seal.setPositions(List.of(position));
        context.setGeneralSeals(List.of(seal));
        new GeneralSealRenderTask().execute(context);
        new SpanSealRenderTask().execute(context);
        assertRendered(context, 5);
    }

    @Test void openingPasswordIsStillRequired() throws Exception {
        var context = context(true);
        assertThrows(ServiceException.class, () -> new SpanSealRenderTask().execute(context));
        assertNull(context.getRenderedPdf());
    }

    void assertRendered(SignChainContext context, int pages) throws Exception {
        assertNotNull(context.getRenderedPdf());
        try (var doc = new PdfDocument(new PdfReader(new ByteArrayInputStream(context.getRenderedPdf())))) {
            assertEquals(pages, doc.getNumberOfPages());
            for (int i = 1; i <= pages; i++) {
                assertFalse(doc.getPage(i).getResources().getResource(PdfName.XObject).isEmpty());
            }
        }
    }
}
