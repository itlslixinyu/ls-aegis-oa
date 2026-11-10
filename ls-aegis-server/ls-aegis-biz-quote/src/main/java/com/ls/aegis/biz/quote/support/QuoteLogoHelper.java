package com.ls.aegis.biz.quote.support;

import org.springframework.util.StringUtils;

/**
 * 报价单导出 Logo 处理：PDF/Word 仅嵌入可内嵌的图片，跳过 SVG/外链。
 */
public final class QuoteLogoHelper {

    private QuoteLogoHelper() {
    }

    /**
     * 返回可用于 HTML/PDF/Word 内嵌的 Logo 地址；不可内嵌时返回空字符串。
     */
    public static String embeddableLogoSrc(String logoUrl) {
        if (!StringUtils.hasText(logoUrl)) {
            return "";
        }
        String raw = logoUrl.trim();
        if (!raw.startsWith("data:image/") || !raw.contains(",")) {
            return "";
        }
        String meta = raw.substring(5, raw.indexOf(',')).toLowerCase();
        if (meta.contains("svg")) {
            return "";
        }
        return raw;
    }
}
