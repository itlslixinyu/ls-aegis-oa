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

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ls.aegis.common.context.UserContextHolder;
import com.ls.aegis.biz.file.mapper.FileMapper;
import com.ls.aegis.biz.file.model.entity.FileDO;
import com.ls.aegis.biz.file.model.entity.StorageDO;
import com.ls.aegis.biz.file.enums.StorageTypeEnum;
import com.ls.aegis.biz.file.model.query.FileQuery;
import com.ls.aegis.biz.file.model.resp.file.FileResp;
import com.ls.aegis.biz.file.service.FileRecycleService;
import com.ls.aegis.biz.file.service.StorageService;
import top.continew.starter.core.constant.StringConstants;
import top.continew.starter.core.exception.BusinessException;
import top.continew.starter.data.util.QueryWrapperHelper;
import top.continew.starter.extension.crud.model.query.PageQuery;
import top.continew.starter.extension.crud.model.resp.PageResp;

import java.util.List;
import java.io.FilterInputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.ls.aegis.biz.file.util.FileDigestUtils;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 文件回收站业务实现
 *
 * @author Charles7c
 * @since 2025/11/11 21:28
 */
@Service
@RequiredArgsConstructor
public class FileRecycleServiceImpl implements FileRecycleService {

    private final FileMapper fileMapper;
    private final StorageService storageService;
    private final FileStorageService fileStorageService;

    @Override
    public PageResp<FileResp> page(FileQuery query, PageQuery pageQuery) {
        QueryWrapper<FileDO> queryWrapper = QueryWrapperHelper.build(query, pageQuery.getSort());
        Page<FileDO> page = fileMapper.selectPageInRecycleBin(new Page<>(pageQuery.getPage(), pageQuery
            .getSize()), queryWrapper.lambda().eq(FileDO::getDeleted, 1L));
        return PageResp.build(page, FileResp.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restore(Long id) {
        FileDO file = this.getById(id);
        // Keep the row recycled while SDK moves: its production recorder deletes the old URL.
        // Making it active first lets that callback match and immediately recycle it again.
        // 还原文件
        StorageDO storage = storageService.getById(file.getStorageId());
        FileInfo original = file.toFileInfo(storage);
        FileInfo recycled = file.toFileInfo(storage);
        String newPath = original.getPath();
        recycled.setPath(storage.getRecycleBinPath() + newPath);
        boolean sourceExists = fileStorageService.exists(recycled);
        boolean targetExists = fileStorageService.exists(original);
        if (sourceExists && targetExists) {
            throw new BusinessException("原目录与回收站同时存在文件，请核对后再还原");
        }
        if (sourceExists) {
            fileStorageService.move(recycled)
                .setNotSupportMetadataThrowException(StorageTypeEnum.LOCAL.equals(storage.getType()), false)
                .setPath(newPath).move();
        } else if (targetExists && StorageTypeEnum.LOCAL.equals(storage.getType())) {
            // A previous SDK callback could recycle the DB row after physically restoring it.
            // Reconcile only after streaming verification; never infer identity from a pathname alone.
            verifyRestoredContent(original, file.getSize(), file.getFileDigest(), true);
            if (StrUtil.isNotBlank(file.getThumbnailName())) {
                FileInfo thumbnail = file.toFileInfo(storage);
                thumbnail.setFilename(file.getThumbnailName());
                thumbnail.setThFilename(null);
                String thumbDigest = null;
                if (StrUtil.isNotBlank(file.getThumbnailMetadata())) {
                    thumbDigest = JSONUtil.parseObj(file.getThumbnailMetadata()).getStr("sm3");
                }
                verifyRestoredContent(thumbnail, file.getThumbnailSize(), thumbDigest, false);
            }
        } else {
            throw new BusinessException("回收文件不存在或无法安全核验已还原文件");
        }
        // Only expose the row after physical restoration succeeds and SDK callbacks have completed.
        fileMapper.restoreInRecycleBin(id, UserContextHolder.getUserId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        FileDO file = this.getById(id);
        // 删除记录
        fileMapper.deleteWithoutRecycleBin(List.of(id), UserContextHolder.getUserId());
        // 删除文件
        StorageDO storage = storageService.getById(file.getStorageId());
        FileInfo fileInfo = file.toFileInfo(storage);
        fileInfo.setPath(storage.getRecycleBinPath() + fileInfo.getPath());
        if (!fileStorageService.exists(fileInfo)) {
            return;
        }
        fileStorageService.delete(fileInfo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clean() {
        // 查询文件
        List<FileDO> list = fileMapper.selectListInRecycleBin();
        // 删除记录
        fileMapper.cleanRecycleBin(UserContextHolder.getUserId());
        // 删除文件
        // 批量获取存储配置
        Map<Long, List<FileDO>> fileListGroup = list.stream().collect(Collectors.groupingBy(FileDO::getStorageId));
        List<StorageDO> storageList = storageService.listByIds(fileListGroup.keySet());
        Map<Long, StorageDO> storageGroup = storageList.stream()
            .collect(Collectors.toMap(StorageDO::getId, Function.identity(), (existing, replacement) -> existing));
        // 删除文件
        for (Map.Entry<Long, List<FileDO>> entry : fileListGroup.entrySet()) {
            StorageDO storage = storageGroup.get(entry.getKey());
            // 清空回收站
            FileInfo fileInfo = new FileInfo();
            fileInfo.setPlatform(storage.getCode());
            fileInfo.setBasePath(StringConstants.EMPTY);
            fileInfo.setPath(storage.getRecycleBinPath());
            fileStorageService.delete(fileInfo);
        }
    }

    /** Size is mandatory; main-file identity additionally requires the platform's stored SM3. */
    private void verifyRestoredContent(FileInfo info, Long expectedSize, String expectedDigest, boolean requireDigest) {
        if (expectedSize == null || expectedSize < 0 || !fileStorageService.exists(info)
            || (requireDigest && StrUtil.isBlank(expectedDigest))
            || (StrUtil.isNotBlank(expectedDigest) && !expectedDigest.matches("[0-9a-fA-F]{64}"))) {
            throw new BusinessException("已还原文件缺少可信大小或摘要，请核对后再还原");
        }
        AtomicLong count = new AtomicLong();
        AtomicReference<String> digest = new AtomicReference<>();
        fileStorageService.download(info).inputStream(input -> {
            try (var counted = new FilterInputStream(input) {
                @Override public int read() throws IOException { int n=in.read(); if(n>=0)count.incrementAndGet(); return n; }
                @Override public int read(byte[] b,int off,int len) throws IOException { int n=in.read(b,off,len); if(n>0)count.addAndGet(n); return n; }
            }) { digest.set(FileDigestUtils.sm3Hex(counted)); }
            catch (IOException failure) { throw new BusinessException("核验已还原文件失败"); }
        });
        if (count.get() != expectedSize || (StrUtil.isNotBlank(expectedDigest)
            && !expectedDigest.equalsIgnoreCase(digest.get()))) {
            throw new BusinessException("已还原文件大小或摘要不匹配，请核对后再还原");
        }
    }

    /**
     * 根据 ID 查询
     *
     * @param id ID
     * @return 文件信息
     */
    private FileDO getById(Long id) {
        FileDO file = fileMapper.selectByIdInRecycleBin(id);
        if (file == null) {
            throw new BusinessException("ID 为 [%s] 的文件已不存在".formatted(id));
        }
        return file;
    }
}
