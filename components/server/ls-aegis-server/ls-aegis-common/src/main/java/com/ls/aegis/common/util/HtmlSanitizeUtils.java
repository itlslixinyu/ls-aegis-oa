package com.ls.aegis.common.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;

/**
 * 富文本 HTML 白名单消毒，防存储型 XSS。
 */
public final class HtmlSanitizeUtils {

    private static final Safelist NOTICE_SAFELIST = Safelist.relaxed()
        .addTags("span", "div", "section", "article", "figure", "figcaption")
        .addAttributes(":all", "class", "style")
        .addAttributes("a", "target", "rel")
        .addAttributes("img", "width", "height", "alt")
        .addProtocols("a", "href", "http", "https", "mailto")
        .addProtocols("img", "src", "http", "https", "data");

    private HtmlSanitizeUtils() {
    }

    /**
     * 公告/通知类富文本消毒；空串原样返回。
     */
    public static String sanitizeNoticeHtml(String html) {
        if (html == null || html.isEmpty()) {
            return html;
        }
        return Jsoup.clean(html, "", NOTICE_SAFELIST, new Document.OutputSettings().prettyPrint(false));
    }
}
