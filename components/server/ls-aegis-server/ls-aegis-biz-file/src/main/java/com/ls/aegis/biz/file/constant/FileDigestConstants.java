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

package com.ls.aegis.biz.file.constant;

/**
 * 文件指纹相关常量（统一 SM3）
 */
public final class FileDigestConstants {

    private FileDigestConstants() {
    }

    /**
     * 上传预处理 attr：文件 SM3 指纹（十六进制）
     */
    public static final String ATTR_FILE_DIGEST = "fileDigest";
}
