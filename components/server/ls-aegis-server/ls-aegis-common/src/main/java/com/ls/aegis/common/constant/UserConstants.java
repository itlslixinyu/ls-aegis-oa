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

package com.ls.aegis.common.constant;

import java.time.LocalDateTime;

/**
 * 用户相关常量
 *
 * @author LS-Aegis
 * @since 2026/3/16
 */
public final class UserConstants {

    /**
     * 初始口令改密时间哨兵值（与种子账号一致）。
     * <p>用于标记「尚未正式改密」，登录后强制进入改密流程；与密码有效期天数无关。</p>
     */
    public static final LocalDateTime INITIAL_PASSWORD_RESET_TIME = LocalDateTime.of(2000, 1, 1, 0, 0, 0);

    /**
     * 自动生成初始密码默认长度
     */
    public static final int DEFAULT_GENERATED_PASSWORD_LENGTH = 12;

    private UserConstants() {
    }
}
