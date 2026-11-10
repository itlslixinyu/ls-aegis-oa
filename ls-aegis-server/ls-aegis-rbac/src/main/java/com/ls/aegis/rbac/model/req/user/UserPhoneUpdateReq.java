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

package com.ls.aegis.rbac.model.req.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import top.continew.starter.validation.constraints.Mobile;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户手机号登记/修改请求参数（仅登记，不校验短信验证码）
 *
 * @author Charles7c
 * @since 2023/10/27 20:11
 */
@Data
@Schema(description = "用户手机号登记/修改请求参数")
public class UserPhoneUpdateReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 新手机号
     */
    @Schema(description = "新手机号", example = "13811111111")
    @NotBlank(message = "新手机号不能为空")
    @Mobile(message = "新手机号格式不正确")
    private String phone;

    /**
     * 当前密码
     */
    @Schema(description = "当前密码", example = "RSA 公钥加密的当前密码")
    @NotBlank(message = "当前密码不能为空")
    private String oldPassword;
}
