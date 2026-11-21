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

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 文件指纹迁移配置
 */
@Data
@ConfigurationProperties(prefix = "file.digest")
public class FileDigestProperties {

    /**
     * 启动时是否执行「存量 file_digest：SHA256 → 重读原文算 SM3」迁移（默认关闭）
     */
    private boolean migrateToSm3 = false;

    /**
     * 每批处理条数
     */
    private int migrateBatchSize = 100;

    /**
     * 是否包含回收站文件（deleted≠0）
     */
    private boolean migrateRecycleBin = true;
}
