package com.ls.aegis.biz.psi.support;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.idev.excel.EasyExcel;
import com.ls.aegis.biz.psi.constant.PsiConstants;
import com.ls.aegis.common.enums.DisEnableStatusEnum;
import jakarta.servlet.http.HttpServletResponse;
import top.continew.starter.cache.redisson.util.RedisUtils;
import top.continew.starter.core.exception.BusinessException;
import top.continew.starter.core.util.validation.CheckUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * PSI 主数据 Excel 导入辅助（模板下载 / Redis 会话缓存）
 */
public final class PsiImportHelper {

    private static final Duration IMPORT_TTL = Duration.ofMinutes(10);

    private PsiImportHelper() {
    }

    public static String cacheRows(List<?> rows) {
        String importKey = IdUtil.fastSimpleUUID();
        RedisUtils.set(PsiConstants.IMPORT_KEY_PREFIX + importKey, JSONUtil.toJsonStr(rows), IMPORT_TTL);
        return importKey;
    }

    public static <T> List<T> loadRows(String importKey, Class<T> clazz) {
        CheckUtils.throwIfBlank(importKey, "导入会话不能为空");
        String data = RedisUtils.get(PsiConstants.IMPORT_KEY_PREFIX + importKey);
        CheckUtils.throwIfBlank(data, "导入已过期，请重新上传");
        List<T> list = JSONUtil.toList(data, clazz);
        CheckUtils.throwIfEmpty(list, "导入已过期，请重新上传");
        return list;
    }

    public static void clear(String importKey) {
        if (StrUtil.isNotBlank(importKey)) {
            RedisUtils.delete(PsiConstants.IMPORT_KEY_PREFIX + importKey);
        }
    }

    public static void downloadTemplate(HttpServletResponse response, Class<?> headClass, String fileName) {
        try {
            String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encoded);
            EasyExcel.write(response.getOutputStream(), headClass).sheet("导入数据").doWrite(Collections.emptyList());
        } catch (Exception e) {
            throw new BusinessException("下载导入模板失败：" + e.getMessage());
        }
    }

    public static <T> List<T> readExcel(java.io.InputStream in, Class<T> headClass) {
        try {
            return EasyExcel.read(in).head(headClass).sheet().headRowNumber(1).doReadSync();
        } catch (Exception e) {
            throw new BusinessException("数据文件解析异常，请检查模板格式");
        }
    }

    public static DisEnableStatusEnum parseStatus(String raw, Integer defaultStatus) {
        if (StrUtil.isBlank(raw)) {
            if (defaultStatus != null && defaultStatus == 2) {
                return DisEnableStatusEnum.DISABLE;
            }
            return DisEnableStatusEnum.ENABLE;
        }
        String v = raw.trim();
        if ("2".equals(v) || "禁用".equals(v) || "停用".equalsIgnoreCase(v) || "DISABLE".equalsIgnoreCase(v)) {
            return DisEnableStatusEnum.DISABLE;
        }
        return DisEnableStatusEnum.ENABLE;
    }

    public static Boolean parseYesNo(String raw, boolean defaultValue) {
        if (StrUtil.isBlank(raw)) {
            return defaultValue;
        }
        String v = raw.trim();
        return "是".equals(v) || "Y".equalsIgnoreCase(v) || "YES".equalsIgnoreCase(v)
            || "TRUE".equalsIgnoreCase(v) || "1".equals(v);
    }
}
