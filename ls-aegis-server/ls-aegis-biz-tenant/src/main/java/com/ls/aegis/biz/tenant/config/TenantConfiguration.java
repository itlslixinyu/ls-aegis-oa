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

package com.ls.aegis.biz.tenant.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.ls.aegis.common.config.TenantExtensionProperties;
import com.ls.aegis.biz.tenant.service.TenantService;
import top.continew.starter.extension.tenant.annotation.ConditionalOnEnabledTenant;
import top.continew.starter.extension.tenant.config.TenantProvider;

/**
 * 租户配置
 * <p>TenantExtensionProperties 仅由 ls-aegis-starter 的 @EnableConfigurationProperties 注册，
 * 此处禁止再声明 @Bean，否则会出现双实例，登录写 Session 时 SpringUtil.getBean 失败。</p>
 *
 * @author Charles7c
 * @since 2025/7/12 13:30
 */
@Configuration
public class TenantConfiguration {

    /**
     * 租户提供者
     */
    @Bean
    @ConditionalOnEnabledTenant
    public TenantProvider tenantProvider(TenantExtensionProperties tenantExtensionProperties,
                                         TenantService tenantService) {
        return new DefaultTenantProvider(tenantExtensionProperties, tenantService);
    }

    /**
     * API 文档分组配置
     */
    @Bean
    public GroupedOpenApi tenantApi() {
        return GroupedOpenApi.builder().group("tenant").displayName("租户管理").pathsToMatch("/tenant/**").build();
    }
}
