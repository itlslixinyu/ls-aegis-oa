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

package com.ls.aegis.rbac.constant;

/**
 * 功能模块总开关参数码与菜单根 ID
 *
 * @author ls
 * @since 2026/8/16
 */
public final class ModuleOptionConstants {

    /** 租户管理 */
    public static final String TENANT_ENABLED = "MODULE_TENANT_ENABLED";

    /** 任务调度 */
    public static final String SCHEDULE_ENABLED = "MODULE_SCHEDULE_ENABLED";

    /** 租户管理菜单根 ID */
    public static final Long MENU_ROOT_TENANT = 3000L;

    /** 任务调度菜单根 ID */
    public static final Long MENU_ROOT_SCHEDULE = 8000L;

    private ModuleOptionConstants() {
    }
}
