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

package com.ls.aegis.biz.file.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.x.file.storage.core.FileInfo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.ls.aegis.security.base.controller.BaseController;
import com.ls.aegis.biz.file.model.query.FileQuery;
import com.ls.aegis.biz.file.model.req.FileReq;
import com.ls.aegis.biz.file.model.resp.file.FileDigestMigrationResp;
import com.ls.aegis.biz.file.model.resp.file.FileDirCalcSizeResp;
import com.ls.aegis.biz.file.model.resp.file.FileResp;
import com.ls.aegis.biz.file.model.resp.file.FileStatisticsResp;
import com.ls.aegis.biz.file.model.resp.file.FileUploadResp;
import com.ls.aegis.biz.file.service.FileDigestMigrationService;
import com.ls.aegis.biz.file.service.FileService;
import top.continew.starter.core.util.validation.ValidationUtils;
import top.continew.starter.extension.crud.annotation.CrudRequestMapping;
import top.continew.starter.extension.crud.enums.Api;
import top.continew.starter.extension.crud.model.resp.IdResp;
import top.continew.starter.log.annotation.Log;

import java.io.IOException;

/**
 * 文件管理 API
 *
 * @author Charles7c
 * @since 2023/12/23 10:38
 */
@Tag(name = "文件管理 API")
@Validated
@RestController
@RequiredArgsConstructor
@CrudRequestMapping(value = "/system/file", api = {Api.PAGE, Api.UPDATE, Api.BATCH_DELETE})
public class FileController extends BaseController<FileService, FileResp, FileResp, FileQuery, FileReq> {

    private final FileDigestMigrationService fileDigestMigrationService;

    /**
     * 上传文件
     * <p>
     * 公共上传文件请使用 {@link CommonController#upload}
     * </p>
     *
     * @param file       文件
     * @param parentPath 上级目录
     * @return 文件上传响应参数
     * @throws IOException /
     */
    @Operation(summary = "上传文件", description = "上传文件")
    @Parameter(name = "parentPath", description = "上级目录（默认：/yyyy/MM/dd）", example = "/", in = ParameterIn.QUERY)
    @SaCheckPermission("system:file:upload")
    @PostMapping("/upload")
    public FileUploadResp upload(@NotNull(message = "文件不能为空") @RequestPart MultipartFile file,
                                 @RequestParam(required = false) String parentPath) throws IOException {
        ValidationUtils.throwIf(file::isEmpty, "文件不能为空");
        FileInfo fileInfo = baseService.upload(file, parentPath);
        return FileUploadResp.builder()
            .id(fileInfo.getId())
            .url(fileInfo.getUrl())
            .thUrl(fileInfo.getThUrl())
            .metadata(fileInfo.getMetadata())
            .build();
    }

    @Operation(summary = "创建文件夹", description = "创建文件夹")
    @SaCheckPermission("system:file:createDir")
    @PostMapping("/dir")
    public IdResp<Long> createDir(@RequestBody @Valid FileReq req) {
        ValidationUtils.throwIfBlank(req.getParentPath(), "上级目录不能为空");
        return new IdResp<>(baseService.createDir(req));
    }

    @Operation(summary = "计算文件夹大小", description = "计算文件夹大小")
    @SaCheckPermission("system:file:calcDirSize")
    @GetMapping("/dir/{id}/size")
    public FileDirCalcSizeResp calcDirSize(@PathVariable Long id) {
        return new FileDirCalcSizeResp(baseService.calcDirSize(id));
    }

    @Log(ignore = true)
    @Operation(summary = "查询文件资源统计", description = "查询文件资源统计")
    @SaCheckPermission("system:file:list")
    @GetMapping("/statistics")
    public FileStatisticsResp statistics() {
        return baseService.statistics();
    }

    @Log(ignore = true)
    @Operation(summary = "检测文件是否存在", description = "按文件指纹（SM3）检测是否已存在")
    @SaCheckPermission("system:file:check")
    @GetMapping("/check")
    public FileResp checkFile(String fileDigest) {
        return baseService.check(fileDigest);
    }

    /**
     * 存量指纹迁移：重读存储原文，将 file_digest 全部重算为 SM3（幂等）。
     * <p>也可启动参数 {@code file.digest.migrate-to-sm3=true} 自动执行一次。</p>
     */
    @Operation(summary = "迁移文件指纹为 SM3", description = "重读存储原文重算 SM3，覆盖历史 SHA256 指纹；可重复执行")
    @SaCheckPermission("system:file:update")
    @PostMapping("/migrate-digest-sm3")
    public FileDigestMigrationResp migrateDigestToSm3() {
        return fileDigestMigrationService.migrateAllToSm3();
    }
}