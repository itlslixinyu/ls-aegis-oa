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

package com.ls.aegis.biz.file.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.springframework.stereotype.Service;
import com.ls.aegis.biz.file.config.file.FileDigestProperties;
import com.ls.aegis.biz.file.mapper.FileMapper;
import com.ls.aegis.biz.file.model.entity.FileDO;
import com.ls.aegis.biz.file.model.entity.StorageDO;
import com.ls.aegis.biz.file.model.resp.file.FileDigestMigrationResp;
import com.ls.aegis.biz.file.service.FileDigestMigrationService;
import com.ls.aegis.biz.file.service.StorageService;
import com.ls.aegis.biz.file.util.FileDigestUtils;
import top.continew.starter.cache.redisson.util.RedisUtils;
import top.continew.starter.core.constant.StringConstants;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 存量 file_digest 迁移：无法从 SHA256 换算，须按存储重读原文再算 SM3。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileDigestMigrationServiceImpl implements FileDigestMigrationService {

    private static final String LEGACY_REDIS_PREFIX = "multipart:md5_to_upload:";

    private final FileMapper fileMapper;
    private final StorageService storageService;
    private final FileStorageService fileStorageService;
    private final FileDigestProperties properties;

    @Override
    public FileDigestMigrationResp migrateAllToSm3() {
        AtomicLong scanned = new AtomicLong();
        AtomicLong updated = new AtomicLong();
        AtomicLong unchanged = new AtomicLong();
        AtomicLong missing = new AtomicLong();
        AtomicLong failed = new AtomicLong();

        Map<Long, StorageDO> storageCache = new HashMap<>();
        int batchSize = Math.max(1, properties.getMigrateBatchSize());
        boolean includeRecycleBin = properties.isMigrateRecycleBin();
        long lastId = 0L;

        log.info("开始迁移文件指纹为 SM3（batchSize={}, includeRecycleBin={}）", batchSize, includeRecycleBin);

        while (true) {
            List<FileDO> records = includeRecycleBin
                ? fileMapper.selectActiveAndRecycleForDigestMigration(lastId, batchSize)
                : fileMapper.selectActiveForDigestMigration(lastId, batchSize);
            if (CollUtil.isEmpty(records)) {
                break;
            }
            for (FileDO file : records) {
                scanned.incrementAndGet();
                migrateOne(file, storageCache, updated, unchanged, missing, failed);
                lastId = file.getId();
            }
            if (records.size() < batchSize) {
                break;
            }
        }

        long legacyCleared = clearLegacyRedisMappings();
        FileDigestMigrationResp resp = FileDigestMigrationResp.builder()
            .scanned(scanned.get())
            .updated(updated.get())
            .unchanged(unchanged.get())
            .missing(missing.get())
            .failed(failed.get())
            .legacyRedisKeysCleared(legacyCleared)
            .build();
        log.info("文件指纹 SM3 迁移结束: scanned={}, updated={}, unchanged={}, missing={}, failed={}, legacyRedisKeysCleared={}",
            resp.getScanned(), resp.getUpdated(), resp.getUnchanged(), resp.getMissing(), resp.getFailed(), resp
                .getLegacyRedisKeysCleared());
        return resp;
    }

    private void migrateOne(FileDO file,
                            Map<Long, StorageDO> storageCache,
                            AtomicLong updated,
                            AtomicLong unchanged,
                            AtomicLong missing,
                            AtomicLong failed) {
        try {
            StorageDO storage = storageCache.computeIfAbsent(file.getStorageId(), storageService::getById);
            if (storage == null) {
                log.warn("迁移跳过：存储不存在 fileId={}, storageId={}", file.getId(), file.getStorageId());
                missing.incrementAndGet();
                return;
            }
            FileInfo fileInfo = file.toFileInfo(storage);
            // 回收站物理路径带 recycleBinPath 前缀
            if (file.getDeleted() != null && file.getDeleted() != 0L && StrUtil.isNotBlank(storage
                .getRecycleBinPath())) {
                fileInfo.setPath(storage.getRecycleBinPath() + fileInfo.getPath());
            }
            if (!fileStorageService.exists(fileInfo)) {
                log.warn("迁移跳过：存储中无此文件 fileId={}, path={}", file.getId(), file.getPath());
                missing.incrementAndGet();
                return;
            }
            // 流式重算，避免大文件整文件进内存
            String[] sm3Holder = new String[1];
            fileStorageService.download(fileInfo).inputStream(in -> sm3Holder[0] = FileDigestUtils.sm3Hex(in));
            String sm3 = sm3Holder[0];
            if (StrUtil.isBlank(sm3)) {
                failed.incrementAndGet();
                log.error("迁移失败：未能计算 SM3 fileId={}, path={}", file.getId(), file.getPath());
                return;
            }
            if (Objects.equals(sm3, file.getFileDigest())) {
                unchanged.incrementAndGet();
                return;
            }
            String oldDigest = file.getFileDigest();
            fileMapper.updateFileDigestById(file.getId(), sm3);
            updated.incrementAndGet();
            log.debug("已更新文件指纹 fileId={}, old={}, new={}", file.getId(), oldDigest, sm3);
        } catch (Exception e) {
            failed.incrementAndGet();
            log.error("迁移文件指纹失败 fileId={}, path={}", file.getId(), file.getPath(), e);
        }
    }

    /**
     * 清理历史 Redis 指纹映射前缀（过渡期残留）
     */
    private long clearLegacyRedisMappings() {
        try {
            Collection<String> keys = RedisUtils.keys(LEGACY_REDIS_PREFIX + StringConstants.ASTERISK);
            if (CollUtil.isEmpty(keys)) {
                return 0L;
            }
            for (String key : keys) {
                RedisUtils.delete(key);
            }
            return keys.size();
        } catch (Exception e) {
            log.warn("清理历史 Redis 指纹映射失败: {}", e.getMessage());
            return 0L;
        }
    }
}
