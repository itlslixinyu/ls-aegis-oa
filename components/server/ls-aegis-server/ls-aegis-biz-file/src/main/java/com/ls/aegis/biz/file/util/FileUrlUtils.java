package com.ls.aegis.biz.file.util;

import cn.hutool.core.util.URLUtil;

/** 本地文件使用同源入口，绝对地址仍沿用既有规范化规则。 */
public final class FileUrlUtils {
    private FileUrlUtils() { }

    public static boolean isSameOrigin(String domain) {
        return "/api/file/".equals(domain) || "/api/file".equals(domain);
    }

    public static String join(String prefix, String path) {
        if (isSameOrigin(prefix)) {
            return "/api/file/" + path.replaceFirst("^/+", "");
        }
        return URLUtil.normalize(prefix + path, false, true);
    }

    public static String localHandlerPath(String domain) {
        if (isSameOrigin(domain)) {
            return "/file/";
        }
        String path = URLUtil.url(domain).getPath();
        if (path.startsWith("/api/")) {
            path = path.substring(4);
        }
        if (!path.startsWith("/")) path = "/" + path;
        return path.endsWith("/") ? path : path + "/";
    }

    public static String storagePath(String url) {
        return url.startsWith("/api/file/") ? url.substring("/api/file".length()) : url;
    }
}
