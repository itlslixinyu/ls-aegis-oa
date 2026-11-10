package com.ls.aegis.biz.sign.chain.task;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.extgstate.PdfExtGState;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.ls.aegis.biz.sign.chain.SignChainContext;
import com.ls.aegis.biz.sign.chain.SignTask;
import com.ls.aegis.biz.sign.domain.vo.GeneralSealConfig;
import com.ls.aegis.biz.sign.domain.vo.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

@Component
public class GeneralSealRenderTask implements SignTask {

    private static final Logger log = LoggerFactory.getLogger(GeneralSealRenderTask.class);

    private static final float SEAL_ALPHA = 0.65f;

    @Override
    public void execute(SignChainContext context) throws Exception {
        byte[] sourcePdf = context.getRenderedPdf() != null
            ? context.getRenderedPdf()
            : context.getSourcePdf();

        List<GeneralSealConfig> seals = context.getGeneralSeals();
        Long skipSealId = context.getSkipSealId();
        log.info("开始渲染普通章，共{}个印章配置, skipSealId={}", seals != null ? seals.size() : 0, skipSealId);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfDocument pdfDoc = new PdfDocument(new PdfReader(new ByteArrayInputStream(sourcePdf)), new PdfWriter(baos));

        try {
            Document layoutDoc = new Document(pdfDoc);

            int totalPositions = 0;
            for (GeneralSealConfig seal : seals) {
                boolean isSkipSeal = skipSealId != null && skipSealId.equals(seal.getSealId());
                int skipCount = 0;

                BufferedImage sealImg = context.getSealImageCache().get(seal.getSealId());
                if (sealImg == null) {
                    log.warn("印章图片不存在，跳过: sealId={}", seal.getSealId());
                    continue;
                }

                byte[] imgBytes = bufferedImageToBytes(sealImg);
                ImageData imageData = ImageDataFactory.create(imgBytes);

                for (Position pos : seal.getPositions()) {
                    if (isSkipSeal && skipCount == 0) {
                        skipCount++;
                        log.info("跳过主印章第1个位置（将由数字签名外观替代）: sealId={}, page={}", seal.getSealId(), pos.getPage());
                        continue;
                    }

                    int pageNum = pos.getPage();
                    if (pageNum < 1 || pageNum > pdfDoc.getNumberOfPages()) {
                        log.warn("页码超出范围，跳过: page={}", pageNum);
                        continue;
                    }

                    Rectangle pageSize = pdfDoc.getPage(pageNum).getPageSize();
                    float drawX = pos.getX();
                    float drawY = pageSize.getHeight() - pos.getY() - pos.getHeight();
                    float drawW = pos.getWidth();
                    float drawH = pos.getHeight();

                    PdfCanvas canvas = new PdfCanvas(pdfDoc.getPage(pageNum));
                    canvas.saveState();

                    PdfExtGState gs = new PdfExtGState();
                    gs.setFillOpacity(SEAL_ALPHA);
                    gs.setStrokeOpacity(SEAL_ALPHA);
                    canvas.setExtGState(gs);

                    Image image = new Image(imageData);
                    image.scaleToFit(drawW, drawH);
                    image.setFixedPosition(pageNum, drawX, drawY);
                    layoutDoc.add(image);

                    canvas.restoreState();
                    totalPositions++;
                }
            }

            layoutDoc.close();
            log.info("普通章渲染完成，共{}个位置", totalPositions);

        } finally {
            pdfDoc.close();
        }

        context.setRenderedPdf(baos.toByteArray());
    }

    private byte[] bufferedImageToBytes(BufferedImage img) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "PNG", baos);
        return baos.toByteArray();
    }

    @Override
    public String getTaskName() {
        return "普通章渲染";
    }

    @Override
    public boolean canSkip(SignChainContext context) {
        return context.getGeneralSeals() == null || context.getGeneralSeals().isEmpty();
    }
}
