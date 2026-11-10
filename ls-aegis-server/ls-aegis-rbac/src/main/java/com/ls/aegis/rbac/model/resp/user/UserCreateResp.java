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

package com.ls.aegis.rbac.model.resp.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户创建响应（含一次性初始明文密码）
 *
 * @author LS-Aegis
 * @since 2026/3/16
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户创建响应")
public class UserCreateResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户 ID
     */
    @Schema(description = "用户 ID", example = "1")
    private Long id;

    /**
     * 初始明文密码（仅自动生成时返回一次，请妥善保存；库中仅存哈希）
     */
    @Schema(description = "初始明文密码（仅自动生成时返回）", example = "Ab3$kP9mQx2!")
    private String initialPassword;

    /**
     * 是否为系统自动生成的初始密码
     */
    @Schema(description = "是否自动生成初始密码", example = "true")
    private Boolean generated;
}
