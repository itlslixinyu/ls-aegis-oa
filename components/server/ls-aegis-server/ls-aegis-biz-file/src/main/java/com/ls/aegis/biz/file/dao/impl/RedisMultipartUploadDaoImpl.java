/*
 * Copyright (c) 2022-present Charles7c Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ls.aegis.biz.file.dao.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import com.ls.aegis.biz.file.constant.MultipartUploadConstants;
import com.ls.aegis.biz.file.dao.MultipartUploadDao;
import com.ls.aegis.biz.file.model.resp.file.FilePartInfo;
import com.ls.aegis.biz.file.model.resp.file.MultipartUploadInitResp;
import top.continew.starter.cache.redisson.util.RedisUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Redis分片上传缓存实现
 * <p>
 * 核心功能：
 * 1. 文件指纹（SM3）到 uploadId 的映射
 * 2. 分片信息缓存
 * 3. 上传状态缓存
 * </p>
 *
 * @author KAI
 * @since 2025/7/30 17:40
 */
@Slf4j
@Repository
public class RedisMultipartUploadDaoImpl implements MultipartUploadDao {

    @Override
    public String getUploadIdByDigest(String digest) {
        String key = MultipartUploadConstants.DIGEST_TO_UPLOAD_ID_PREFIX + digest;
        try {
            return RedisUtils.hGet(key, "uploadId");
        } catch (Exception e) {
            log.error("根据文件指纹获取uploadId失败: digest={}", digest, e);
            return null;
        }
    }

    @Override
    public void setDigestMapping(String digest, String uploadId) {
        String key = MultipartUploadConstants.DIGEST_TO_UPLOAD_ID_PREFIX + digest;
        try {
            RedisUtils.hSet(key, "uploadId", uploadId);
            RedisUtils.expire(key, Duration.ofHours(MultipartUploadConstants.DEFAULT_EXPIRE_HOURS));
            log.debug("缓存文件指纹映射: digest={}, uploadId={}", digest, uploadId);
        } catch (Exception e) {
            log.error("缓存文件指纹映射失败: digest={}, uploadId={}", digest, uploadId, e);
            throw new RuntimeException("缓存文件指纹映射失败", e);
        }
    }

    @Override
    public void deleteDigestMapping(String digest) {
        try {
            RedisUtils.delete(MultipartUploadConstants.DIGEST_TO_UPLOAD_ID_PREFIX + digest);
            log.debug("删除文件指纹映射: digest={}", digest);
        } catch (Exception e) {
            log.error("删除文件指纹映射失败: digest={}", digest, e);
        }
    }

    @Override
    public void setMultipartUpload(String uploadId, MultipartUploadInitResp initResp, Map<String, String> metadata) {
        String key = MultipartUploadConstants.MULTIPART_UPLOAD_PREFIX + uploadId;
        String metadataKey = MultipartUploadConstants.MULTIPART_METADATA_PREFIX + uploadId;

        try {
            // 缓存初始化信息
            RedisUtils.set(key, JSONUtil.toJsonStr(initResp), Duration
                .ofHours(MultipartUploadConstants.DEFAULT_EXPIRE_HOURS));

            // 缓存元数据
            if (metadata != null && !metadata.isEmpty()) {
                for (Map.Entry<String, String> entry : metadata.entrySet()) {
                    RedisUtils.hSet(metadataKey, entry.getKey(), entry.getValue());
                }
                RedisUtils.expire(metadataKey, Duration.ofHours(MultipartUploadConstants.DEFAULT_EXPIRE_HOURS));
            }

            log.debug("缓存分片上传信息: uploadId={}", uploadId);
        } catch (Exception e) {
            log.error("缓存分片上传信息失败: uploadId={}", uploadId, e);
            throw new RuntimeException("缓存分片上传信息失败", e);
        }
    }

    @Override
    public MultipartUploadInitResp getMultipartUpload(String uploadId) {
        String key = MultipartUploadConstants.MULTIPART_UPLOAD_PREFIX + uploadId;
        try {
            Object value = RedisUtils.get(key);
            if (value != null) {
                return JSONUtil.toBean(value.toString(), MultipartUploadInitResp.class);
            }
            return null;
        } catch (Exception e) {
            log.error("获取分片上传信息失败: uploadId={}", uploadId, e);
            return null;
        }
    }

    @Override
    public void deleteMultipartUpload(String uploadId) {
        try {
            String key = MultipartUploadConstants.MULTIPART_UPLOAD_PREFIX + uploadId;
            String metadataKey = MultipartUploadConstants.MULTIPART_METADATA_PREFIX + uploadId;
            String expireKey = MultipartUploadConstants.MULTIPART_EXPIRE_PREFIX + uploadId;

            // 先取指纹再删映射与数据
            MultipartUploadInitResp initResp = getMultipartUpload(uploadId);
            if (initResp != null) {
                String digest = initResp.getFileDigest();
                if (StrUtil.isNotBlank(digest)) {
                    deleteDigestMapping(digest);
                }
            }

            // 删除分片上传相关数据
            RedisUtils.delete(key);
            RedisUtils.delete(metadataKey);
            RedisUtils.delete(expireKey);

            log.debug("删除分片上传信息: uploadId={}", uploadId);
        } catch (Exception e) {
            log.error("删除分片上传信息失败: uploadId={}", uploadId, e);
        }
    }

    @Override
    public void deleteMultipartUploadAll(String uploadId) {
        this.deleteMultipartUpload(uploadId);
        this.deleteFileParts(uploadId);
    }

    @Override
    public void setFilePart(String uploadId, FilePartInfo partInfo) {
        String key = MultipartUploadConstants.MULTIPART_PARTS_PREFIX + uploadId;
        String partKey = partInfo.getPartNumber().toString();

        try {
            RedisUtils.hSet(key, partKey, JSONUtil.toJsonStr(partInfo));
            RedisUtils.expire(key, Duration.ofHours(MultipartUploadConstants.DEFAULT_EXPIRE_HOURS));
            log.debug("缓存分片信息: uploadId={}, partNumber={}", uploadId, partKey);
        } catch (Exception e) {
            log.error("缓存分片信息失败: uploadId={}, partNumber={}", uploadId, partKey, e);
            throw new RuntimeException("缓存分片信息失败", e);
        }
    }

    @Override
    public List<FilePartInfo> getFileParts(String uploadId) {
        String key = MultipartUploadConstants.MULTIPART_PARTS_PREFIX + uploadId;

        try {
            Map<String, Object> entries = RedisUtils.hGetAll(key);
            if (entries.isEmpty()) {
                return new ArrayList<>();
            }

            return entries.values()
                .stream()
                .map(value -> JSONUtil.toBean(value.toString(), FilePartInfo.class))
                .sorted(Comparator.comparing(FilePartInfo::getPartNumber))
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("获取分片列表失败: uploadId={}", uploadId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public void deleteFileParts(String uploadId) {
        String key = MultipartUploadConstants.MULTIPART_PARTS_PREFIX + uploadId;

        try {
            RedisUtils.delete(key);
            log.debug("删除所有分片信息: uploadId={}", uploadId);
        } catch (Exception e) {
            log.error("删除所有分片信息失败: uploadId={}", uploadId, e);
        }
    }

    @Override
    public boolean existsFilePart(String uploadId, int partNumber) {
        String key = MultipartUploadConstants.MULTIPART_PARTS_PREFIX + uploadId;
        String partKey = String.valueOf(partNumber);
        return RedisUtils.hExists(key, partKey);
    }

    @Override
    public void cleanupExpiredData() {
        try {
            // 获取所有分片上传的过期时间
            Collection<String> keys = RedisUtils.keys(MultipartUploadConstants.MULTIPART_EXPIRE_PREFIX + "*");
            if (keys.isEmpty()) {
                return;
            }

            LocalDateTime now = LocalDateTime.now();
            List<String> expiredUploadIds = new ArrayList<>();

            for (String key : keys) {
                String uploadId = key.substring(MultipartUploadConstants.MULTIPART_EXPIRE_PREFIX.length());
                Object value = RedisUtils.get(key);

                if (value != null) {
                    try {
                        LocalDateTime expireTime = LocalDateTime.parse(value
                            .toString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                        if (now.isAfter(expireTime)) {
                            expiredUploadIds.add(uploadId);
                        }
                    } catch (Exception e) {
                        log.warn("解析过期时间失败: uploadId={}, value={}", uploadId, value);
                        expiredUploadIds.add(uploadId);
                    }
                }
            }

            // 删除过期的数据
            for (String uploadId : expiredUploadIds) {
                deleteMultipartUpload(uploadId);
                deleteFileParts(uploadId);
                log.info("清理过期数据: uploadId={}", uploadId);
            }

            log.info("清理过期数据完成: count={}", expiredUploadIds.size());
        } catch (Exception e) {
            log.error("清理过期数据失败", e);
        }
    }
}