package com.ls.aegis.biz.sign.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.ls.aegis.compat.common.exception.ServiceException;
import com.ls.aegis.biz.sign.service.ISignStorageService;
import com.ls.aegis.biz.sign.util.SealImageProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 印章图片本地缓存
 * 使用 Caffeine 实现高性能内存缓存，避免每次签章都从 MinIO 下载印章图片
 * 同时缓存净化后的 PNG 图片，避免重复逐像素处理
 */
@Component
public class SealImageCache {

    private static final Logger log = LoggerFactory.getLogger(SealImageCache.class);

    /** 印章图像处理器（统一使用） */
    private final SealImageProcessor imageProcessor = new SealImageProcessor();

    /** 原始印章图片缓存（MinIO 下载的原始 bytes） */
    private final Cache<String, byte[]> rawImageCache = Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .recordStats()
            .build();

    /** 净化后的印章图片缓存（透明背景 PNG） */
    private final Cache<String, byte[]> sanitizedImageCache = Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .recordStats()
            .build();

    @Autowired
    private ISignStorageService signStorage;

    /**
     * 获取原始印章图片（从缓存或 MinIO 下载）
     * @param sealFilePath MinIO 文件路径
     * @return 原始图片字节数组
     */
    public byte[] getRawImage(String sealFilePath) {
        if (sealFilePath == null || sealFilePath.isEmpty()) {
            return null;
        }
        byte[] image = rawImageCache.getIfPresent(sealFilePath);
        if (image != null) {
            log.debug("原始印章图片缓存命中: {}", sealFilePath);
            return image;
        }
        try {
            byte[] imgBytes = signStorage.download(sealFilePath);
            if (imgBytes != null && imgBytes.length > 0) {
                rawImageCache.put(sealFilePath, imgBytes);
                log.debug("原始印章图片已缓存: {}", sealFilePath);
                return imgBytes;
            }
        } catch (Exception e) {
            log.warn("下载印章图片失败: {} -> {}", sealFilePath, e.getMessage());
        }
        return null;
    }

    /**
     * 获取净化后的印章图片（白色背景透明化）
     * 优先从缓存获取，缓存未命中则从原始图片净化并缓存
     * @param sealFilePath MinIO 文件路径
     * @return 净化后的图片字节数组
     */
    public byte[] getSanitizedImage(String sealFilePath) {
        if (sealFilePath == null || sealFilePath.isEmpty()) {
            return null;
        }
        byte[] sanitized = sanitizedImageCache.getIfPresent(sealFilePath);
        if (sanitized != null) {
            log.debug("净化后印章图片缓存命中: {}", sealFilePath);
            return sanitized;
        }
        byte[] rawImage = getRawImage(sealFilePath);
        if (rawImage == null) {
            return null;
        }
        byte[] result = sanitizePngForStamp(rawImage);
        sanitizedImageCache.put(sealFilePath, result);
        log.debug("净化后印章图片已缓存: {}", sealFilePath);
        return result;
    }

    /**
     * 获取印章图片（根据印章对象获取）
     * 统一的印章图片加载入口
     * @param sealFilePath 印章文件路径
     * @param sanitized 是否需要净化（透明背景）
     * @return 图片字节数组
     */
    public byte[] getSealImage(String sealFilePath, boolean sanitized) {
        if (sanitized) {
            return getSanitizedImage(sealFilePath);
        }
        return getRawImage(sealFilePath);
    }

    /**
     * PNG 净化：白色/近白色像素 → 完全透明
     * 委托给 SealImageProcessor 统一处理
     * @param imageBytes 原始图片字节数组
     * @return 净化后的图片字节数组
     */
    private byte[] sanitizePngForStamp(byte[] imageBytes) {
        // 使用统一的图像处理器进行净化
        return imageProcessor.sanitizeImage(imageBytes);
    }

    /**
     * 清空所有缓存
     */
    public void clearAll() {
        rawImageCache.invalidateAll();
        sanitizedImageCache.invalidateAll();
        log.info("印章图片缓存已全部清空");
    }

    /**
     * 获取缓存统计信息
     */
    public String getStats() {
        return String.format("raw[hitRate=%.2f%%, size=%d], sanitized[hitRate=%.2f%%, size=%d]",
                rawImageCache.stats().hitRate() * 100, rawImageCache.estimatedSize(),
                sanitizedImageCache.stats().hitRate() * 100, sanitizedImageCache.estimatedSize());
    }

    /**
     * 获取原始图片缓存条目数
     */
    public long rawCacheSize() {
        return rawImageCache.estimatedSize();
    }

    /**
     * 获取净化后图片缓存条目数
     */
    public long sanitizedCacheSize() {
        return sanitizedImageCache.estimatedSize();
    }
}
