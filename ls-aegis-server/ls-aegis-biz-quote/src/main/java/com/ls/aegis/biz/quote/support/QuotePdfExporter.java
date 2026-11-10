package com.ls.aegis.biz.quote.support;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import top.continew.starter.core.exception.BusinessException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * HTML 转 PDF（openhtmltopdf，兼容 PDFBox 3）。
 */
public final class QuotePdfExporter {

    private QuotePdfExporter() {
    }

    public static byte[] htmlToPdf(String html) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.useDefaultPageSize(210, 297, PdfRendererBuilder.PageSizeUnits.MM);
            registerChineseFont(builder);
            builder.withHtmlContent(html, null);
            builder.toStream(bos);
            builder.run();
            return bos.toByteArray();
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            Throwable root = ex;
            while (root.getCause() != null && root.getCause() != root) {
                root = root.getCause();
            }
            String detail = root.getMessage() != null ? root.getMessage() : root.getClass().getSimpleName();
            throw new BusinessException("PDF 导出失败：" + detail);
        }
    }

    /**
     * 注册 TrueType 中文字体（须含 glyf 表）。
     * Noto CJK 等 OTF/CFF 会报 “OTF fonts do not have a glyf table”，不可用。
     */
    private static void registerChineseFont(PdfRendererBuilder builder) {
        String[] candidates = {
            // 镜像内置 / 挂载（优先）
            "/usr/share/fonts/truetype/aegis/simhei.ttf",
            "/app/fonts/simhei.ttf",
            // Linux 文泉驿等 TrueType
            "/usr/share/fonts/truetype/wqy/wqy-microhei.ttc",
            "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc",
            // Windows 宿主机直跑
            "C:/Windows/Fonts/simhei.ttf",
            "C:/Windows/Fonts/simsun.ttc",
            "C:/Windows/Fonts/msyh.ttf",
            "C:/Windows/Fonts/msyh.ttc",
        };
        for (String path : candidates) {
            File file = new File(path);
            if (!file.isFile()) {
                continue;
            }
            try {
                builder.useFont(file, "SimSun");
                return;
            } catch (Exception ignored) {
                // 尝试下一个
            }
        }
        //  classpath 兜底（打包进 jar 的 TrueType）
        if (registerClasspathFont(builder, "/fonts/simhei.ttf")) {
            return;
        }
        throw new BusinessException("未找到可用 TrueType 中文字体（需含 glyf），无法导出 PDF");
    }

    private static boolean registerClasspathFont(PdfRendererBuilder builder, String resource) {
        try (InputStream in = QuotePdfExporter.class.getResourceAsStream(resource)) {
            if (in == null) {
                return false;
            }
            Path tmp = Files.createTempFile("quote-font-", ".ttf");
            tmp.toFile().deleteOnExit();
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
            builder.useFont(tmp.toFile(), "SimSun");
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
