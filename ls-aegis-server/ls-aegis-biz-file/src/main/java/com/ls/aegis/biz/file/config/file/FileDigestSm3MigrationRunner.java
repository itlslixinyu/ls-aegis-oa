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

package com.ls.aegis.biz.file.config.file;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import com.ls.aegis.biz.file.model.resp.file.FileDigestMigrationResp;
import com.ls.aegis.biz.file.service.FileDigestMigrationService;

/**
 * 启动时可选执行：存量 file_digest 重读原文迁移为 SM3。
 * <p>须在 {@link FileStorageConfigLoader} 之后（Order 更大）。默认关闭，运维窗口开启一次即可。</p>
 */
@Slf4j
@Component
@Order(100)
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "file.digest", name = "migrate-to-sm3", havingValue = "true")
public class FileDigestSm3MigrationRunner implements ApplicationRunner {

    private final FileDigestMigrationService fileDigestMigrationService;

    @Override
    public void run(ApplicationArguments args) {
        log.warn("检测到 file.digest.migrate-to-sm3=true，开始执行存量文件指纹 SM3 迁移…");
        FileDigestMigrationResp resp = fileDigestMigrationService.migrateAllToSm3();
        log.warn("存量文件指纹 SM3 迁移完成: {}", resp);
    }
}
