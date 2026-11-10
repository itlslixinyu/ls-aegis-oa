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

package com.ls.aegis.rbac.service.impl;

import cn.hutool.core.map.MapUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.ls.aegis.common.constant.GlobalConstants;
import com.ls.aegis.rbac.constant.ModuleOptionConstants;
import com.ls.aegis.rbac.enums.OptionCategoryEnum;
import com.ls.aegis.rbac.service.ModuleService;
import com.ls.aegis.rbac.service.OptionService;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 功能模块总开关
 *
 * <p>侧栏菜单仅由本页产品开关控制。租户中间件、SnailJob 调度中心等基础设施为运维配置，
 * 不在此强制隐藏菜单（避免开关已开但侧栏消失）；实际调度接口仍依赖 snail-job 是否装载。</p>
 *
 * @author ls
 * @since 2026/8/16
 */
@Service
@RequiredArgsConstructor
public class ModuleServiceImpl implements ModuleService {

    private final OptionService optionService;

    @Override
    public boolean isTenantEnabled() {
        return isEnabled(ModuleOptionConstants.TENANT_ENABLED, false);
    }

    @Override
    public boolean isScheduleEnabled() {
        return isEnabled(ModuleOptionConstants.SCHEDULE_ENABLED, false);
    }

    @Override
    public Set<Long> disabledMenuRootIds() {
        Set<Long> roots = new HashSet<>(Set.of(7000L, 9000L));
        if (!isTenantEnabled()) {
            roots.add(ModuleOptionConstants.MENU_ROOT_TENANT);
        }
        if (!isScheduleEnabled()) {
            roots.add(ModuleOptionConstants.MENU_ROOT_SCHEDULE);
        }
        return roots;
    }

    private boolean isEnabled(String code, boolean defaultEnabled) {
        Map<String, String> map = optionService.getByCategory(OptionCategoryEnum.MODULE);
        String fallback = defaultEnabled
            ? String.valueOf(GlobalConstants.Boolean.YES)
            : String.valueOf(GlobalConstants.Boolean.NO);
        String value = MapUtil.getStr(map, code, fallback);
        return String.valueOf(GlobalConstants.Boolean.YES).equals(value);
    }
}
