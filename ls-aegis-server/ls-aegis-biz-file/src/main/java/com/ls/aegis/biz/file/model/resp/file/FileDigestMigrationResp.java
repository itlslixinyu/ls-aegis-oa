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

package com.ls.aegis.biz.file.model.resp.file;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 文件指纹 SM3 迁移结果
 */
@Data
@Builder
@Schema(description = "文件指纹 SM3 迁移结果")
public class FileDigestMigrationResp {

    @Schema(description = "扫描文件数（不含目录）")
    private long scanned;

    @Schema(description = "已更新为 SM3 的条数")
    private long updated;

    @Schema(description = "已是目标指纹（重算后相同）跳过条数")
    private long unchanged;

    @Schema(description = "存储中找不到文件")
    private long missing;

    @Schema(description = "处理失败条数")
    private long failed;

    @Schema(description = "清理的历史 Redis 指纹映射键数量")
    private long legacyRedisKeysCleared;
}
