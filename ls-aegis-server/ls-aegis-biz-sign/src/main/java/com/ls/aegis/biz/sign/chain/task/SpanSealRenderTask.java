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
import com.ls.aegis.biz.sign.domain.vo.SpanSealConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 骑缝章渲染任务
 * 
 * 正确的骑缝章加盖逻辑步骤：
 * 1. 文档解析预处理：获取总页数N，判断分段规则（N≤20单组，N＞20双组上下骑缝）
 * 2. 印章图像预处理：裁剪空白边框，缩放匹配侧边宽度，保留Alpha透明通道
 * 3. 印章自动分割：按页数M均等横向切割为M张子图
 * 4. 坐标计算与图层嵌入：固定位置（默认左侧装订边）叠加至每页
 */
@Component
public class SpanSealRenderTask implements SignTask {

    private static final Logger log = LoggerFactory.getLogger(SpanSealRenderTask.class);

    // 默认骑缝章尺寸（mm）
    private static final float DEFAULT_SEAL_SIZE_MM = 42f;

    private static final float SEAL_ALPHA = 1.0f;
    private static final int MAX_PAGES_SINGLE_GROUP = 20;
    private static final float SPAN_MARGIN_PT = 10f;
    private static final float SEAL_ASPECT_RATIO = 1.0f;

    @Override
    public void execute(SignChainContext context) throws Exception {
        byte[] sourcePdf = context.getRenderedPdf() != null
            ? context.getRenderedPdf()
            : context.getSourcePdf();

        List<SpanSealConfig> spanSeals = context.getSpanSeals();
        if (spanSeals == null || spanSeals.isEmpty()) {
            log.info("无骑缝章配置，跳过");
            return;
        }

        int totalPages = context.getPageCount();
        log.info("开始渲染骑缝章，共{}个配置，文档总页数: {}", spanSeals.size(), totalPages);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfDocument pdfDoc = new PdfDocument(new PdfReader(new ByteArrayInputStream(sourcePdf)), new PdfWriter(baos));

        try {
            Document layoutDoc = new Document(pdfDoc);

            for (SpanSealConfig span : spanSeals) {
                BufferedImage originalSeal = context.getSealImageCache().get(span.getSealId());
                if (originalSeal == null) {
                    log.warn("骑缝章图片不存在，跳过: sealId={}", span.getSealId());
                    continue;
                }

                int startPage = span.getStartPage();
                int endPage = span.getEndPage();
                if (startPage < 1) startPage = 1;
                if (endPage > totalPages) endPage = totalPages;
                if (startPage > endPage) {
                    log.warn("骑缝章起止页码无效，跳过: start={}, end={}", startPage, endPage);
                    continue;
                }

                int pageCount = endPage - startPage + 1;
                log.info("处理骑缝章: sealId={}, 起止页={}-{}, 页数={}", 
                    span.getSealId(), startPage, endPage, pageCount);

                // 计算骑缝章分段策略
                boolean dualGroup = pageCount > MAX_PAGES_SINGLE_GROUP;
                int group1Count = dualGroup ? pageCount / 2 : pageCount;
                int group2Start = dualGroup ? startPage + group1Count : -1;
                
                log.info("分段策略: {}组, 第一组{}页{}, 第二组起始页={}", 
                    dualGroup ? "双" : "单", group1Count, dualGroup ? ", 第二组" + (pageCount - group1Count) + "页" : "", group2Start);

                // 获取文档宽度（用于计算印章尺寸）
                Rectangle firstPageSize = pdfDoc.getPage(startPage).getPageSize();
                float docWidth = firstPageSize.getWidth();
                float docHeight = firstPageSize.getHeight();

                // 计算印章尺寸：使用前端设置的印章尺寸（mm转pt）
                float sealSizeMm = span.getSealSize() > 0 ? span.getSealSize() : DEFAULT_SEAL_SIZE_MM;
                float sealWidth = sealSizeMm * 72f / 25.4f;
                float sealHeight = sealWidth * SEAL_ASPECT_RATIO;

                // X坐标：骑缝章固定在页面最右侧边缘，确保所有纸张拼起来可以看到完整印章
                float xOffset = docWidth - sealWidth;

                // Y坐标：用户点击的是从顶部算起的中心位置，转换为iText的底部坐标系
                Float yPositionFloat = span.getYPosition();
                float yOffset;
                if (yPositionFloat != null && yPositionFloat > 0 && yPositionFloat < docHeight) {
                    yOffset = docHeight - yPositionFloat - sealHeight / 2;
                } else {
                    yOffset = docHeight / 2 - sealHeight / 2;
                }
                yOffset = Math.max(SPAN_MARGIN_PT, Math.min(yOffset, docHeight - sealHeight - SPAN_MARGIN_PT));

                log.info("骑缝章位置: x={}, y={}(底部坐标系), 印章尺寸={}x{}", xOffset, yOffset, sealWidth, sealHeight);

                // 第一组骑缝章
                renderSpanSealGroup(pdfDoc, layoutDoc, originalSeal, startPage, group1Count,
                    sealWidth, sealHeight, yOffset, xOffset, false);

                // 第二组骑缝章（如需要）
                if (dualGroup) {
                    int group2Count = pageCount - group1Count;
                    renderSpanSealGroup(pdfDoc, layoutDoc, originalSeal, group2Start, group2Count,
                        sealWidth, sealHeight, yOffset, xOffset, true);
                }
            }

            layoutDoc.close();
            log.info("骑缝章渲染完成");

        } finally {
            pdfDoc.close();
        }

        context.setRenderedPdf(baos.toByteArray());
    }

    /**
     * 渲染一组骑缝章
     * @param pdfDoc PDF文档
     * @param layoutDoc 布局文档
     * @param originalSeal 原始印章图片
     * @param startPage 起始页
     * @param pageCount 页数
     * @param sealWidth 印章宽度
     * @param sealHeight 印章高度
     * @param yOffset Y轴偏移（底部坐标系）
     * @param xOffset X轴偏移（左边位置）
     * @param fromBottom 是否从底部开始（双组场景的第二组）
     */
    private void renderSpanSealGroup(PdfDocument pdfDoc, Document layoutDoc,
            BufferedImage originalSeal, int startPage, int pageCount,
            float sealWidth, float sealHeight, float yOffset, float xOffset, boolean fromBottom) {
        
        if (pageCount <= 0) return;

        // 印章均等分割
        List<BufferedImage> slices = sliceSealImage(originalSeal, pageCount);
        float sliceWidth = sealWidth / pageCount;
        log.info("印章分割完成，共{}片，每片宽度={}, 印章尺寸={}x{}", slices.size(), sliceWidth, sealWidth, sealHeight);

        for (int i = 0; i < pageCount; i++) {
            int pageNum = startPage + i;
            if (pageNum > pdfDoc.getNumberOfPages()) break;

            BufferedImage slice = slices.get(i);
            if (slice == null) continue;

            Rectangle pageSize = pdfDoc.getPage(pageNum).getPageSize();
            float pageWidth = pageSize.getWidth();
            float pageHeight = pageSize.getHeight();

            // 计算当前页印章的实际Y偏移
            float currentYOffset;
            if (fromBottom) {
                currentYOffset = pageHeight - yOffset - sealHeight;
            } else {
                currentYOffset = yOffset;
            }

            // X坐标：每片都右对齐到页面最右侧
            float sliceXOffset = pageWidth - sliceWidth;

            try {
                byte[] sliceBytes = bufferedImageToBytes(slice);
                ImageData imageData = ImageDataFactory.create(sliceBytes);

                PdfCanvas canvas = new PdfCanvas(pdfDoc.getPage(pageNum));
                canvas.saveState();

                PdfExtGState gs = new PdfExtGState();
                gs.setFillOpacity(SEAL_ALPHA);
                gs.setStrokeOpacity(SEAL_ALPHA);
                canvas.setExtGState(gs);

                Image image = new Image(imageData);
                image.scaleToFit(sliceWidth, sealHeight);
                image.setFixedPosition(pageNum, sliceXOffset, currentYOffset);
                layoutDoc.add(image);

                canvas.restoreState();
                
                log.debug("骑缝章渲染: 页={}, 位置=({},{}_{}), 碎片尺寸=({}x{})", 
                    pageNum, sliceXOffset, currentYOffset, fromBottom ? "底部" : "顶部",
                    sliceWidth, sealHeight);
            } catch (Exception e) {
                log.error("渲染骑缝章失败: 页={}, error={}", pageNum, e.getMessage(), e);
            }
        }
    }

    /**
     * 将印章图片均等横向分割为N份
     * @param original 原始印章图片
     * @param slices 分割份数
     * @return 分割后的图片列表
     */
    private List<BufferedImage> sliceSealImage(BufferedImage original, int slices) {
        List<BufferedImage> result = new ArrayList<>();
        
        int origWidth = original.getWidth();
        int origHeight = original.getHeight();
        int baseSliceWidth = origWidth / slices;
        int remainder = origWidth % slices;

        for (int i = 0; i < slices; i++) {
            int x = i * baseSliceWidth + Math.min(i, remainder);
            int w = baseSliceWidth + (i < remainder ? 1 : 0);
            if (x + w > origWidth) {
                w = origWidth - x;
            }
            BufferedImage slice = original.getSubimage(x, 0, w, origHeight);
            result.add(slice);
        }

        return result;
    }

    private byte[] bufferedImageToBytes(BufferedImage img) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "PNG", baos);
        return baos.toByteArray();
    }

    @Override
    public String getTaskName() {
        return "骑缝章渲染";
    }

    @Override
    public boolean canSkip(SignChainContext context) {
        return context.getSpanSeals() == null || context.getSpanSeals().isEmpty();
    }
}
