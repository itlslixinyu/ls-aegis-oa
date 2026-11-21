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

package com.ls.aegis.biz.file.util;

import cn.hutool.crypto.digest.DigestUtil;
import com.ls.aegis.crypto.api.IGmCrypto;
import org.springframework.beans.factory.ObjectProvider;

import java.io.InputStream;

/**
 * 文件指纹工具：统一 SM3（国密关闭时仍用 Hutool/BC SM3）。
 */
public final class FileDigestUtils {

    private FileDigestUtils() {
    }

    /**
     * 计算 SM3 十六进制指纹
     */
    public static String sm3Hex(byte[] data, ObjectProvider<IGmCrypto> gmCryptoProvider) {
        IGmCrypto gmCrypto = gmCryptoProvider != null ? gmCryptoProvider.getIfAvailable() : null;
        if (gmCrypto != null) {
            return gmCrypto.sm3Hex(data);
        }
        return DigestUtil.digester("sm3").digestHex(data);
    }

    /**
     * 流式计算 SM3（大文件）
     */
    public static String sm3Hex(InputStream inputStream) {
        return DigestUtil.digester("sm3").digestHex(inputStream);
    }
}
