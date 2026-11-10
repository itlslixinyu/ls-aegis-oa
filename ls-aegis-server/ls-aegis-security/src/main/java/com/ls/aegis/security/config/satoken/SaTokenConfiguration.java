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

package com.ls.aegis.security.config.satoken;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.strategy.SaStrategy;
import cn.hutool.core.collection.CollUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.PatternMatchUtils;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;
import com.ls.aegis.common.api.system.SysOptionApi;
import com.ls.aegis.security.crud.CrudApiPermissionPrefixCache;
import com.ls.aegis.common.context.UserContext;
import com.ls.aegis.common.context.UserContextHolder;
import top.continew.starter.auth.satoken.autoconfigure.SaTokenExtensionProperties;
import top.continew.starter.core.constant.StringConstants;
import top.continew.starter.core.util.validation.CheckUtils;
import top.continew.starter.extension.crud.annotation.CrudRequestMapping;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Sa-Token 配置
 *
 * @author Charles7c
 * @author chengzi
 * @since 2022/12/19 22:13
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class SaTokenConfiguration {

    private static final List<String> SITE_IMAGE_OPTIONS = List.of("SITE_LOGO", "SITE_FAVICON", "SITE_BEIAN_GONGAN_ICON");
    private static final Pattern SITE_IMAGE_PATH = Pattern.compile("/file/\\d{4}/\\d{2}/\\d{2}/[a-f0-9]{24}\\.png");

    private final SaTokenExtensionProperties properties;
    private final LoginPasswordProperties loginPasswordProperties;
    private final ApplicationContext applicationContext;

    /**
     * 超管权限 {@code *:*:*} 需匹配两段式权限（如 {@code quote:list}）。
     * Sa-Token 默认按模式串匹配时，三段通配无法覆盖两段 permission。
     */
    @PostConstruct
    public void configurePermissionMatch() {
        SaStrategy.instance.setHasElement((list, element) -> {
            if (CollUtil.isEmpty(list) || element == null) {
                return false;
            }
            if (list.contains("*:*:*") || list.contains("*")) {
                return true;
            }
            return list.stream().anyMatch(pattern -> PatternMatchUtils.simpleMatch(pattern, element));
        });
    }

    /**
     * Sa-Token 权限认证配置
     */
    @Bean
    public StpInterface stpInterface() {
        return new SaTokenPermissionImpl();
    }

    /**
     * SaToken 拦截器配置
     */
    @Bean
    public SaInterceptor saInterceptor() {
        return new SaExtensionInterceptor(handle -> SaRouter.match(StringConstants.PATH_PATTERN)
            .notMatch(properties.getSecurity().getExcludes())
            .check(r -> {
                if (isConfiguredSiteImage(handle)) {
                    return;
                }
                // 所有受保护接口统一校验登录态。
                StpUtil.checkLogin();
                if (SaRouter.isMatchCurrURI(loginPasswordProperties.getExcludes())) {
                    return;
                }
                UserContext userContext = UserContextHolder.getContext();
                CheckUtils.throwIf(userContext.isPasswordExpired(), "密码已过期，请修改密码");
            }));
    }

    private boolean isConfiguredSiteImage(Object handler) {
        if (!(handler instanceof ResourceHttpRequestHandler)) {
            return false;
        }
        var request = SaHolder.getRequest();
        String path = request.getRequestPath();
        if (!("GET".equals(request.getMethod()) || "HEAD".equals(request.getMethod()))
            || !SITE_IMAGE_PATH.matcher(path).matches()) {
            return false;
        }
        SysOptionApi options = applicationContext.getBeanProvider(SysOptionApi.class).getIfAvailable();
        return options != null && SITE_IMAGE_OPTIONS.stream()
            .anyMatch(code -> ("/api" + path).equals(options.getValue("SITE", code)));
    }

    /**
     * 配置 sa-token {@link SaIgnore} 注解排除路径
     * <p>主要针对 @CrudRequestMapping 注解</p>
     */
    @EventListener(ApplicationReadyEvent.class)
    public void configureSaTokenExcludes() {
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        List<String> additionalExcludes = Arrays.stream(beanNames).map(beanName -> {
            Object bean = applicationContext.getBean(beanName);
            Class<?> clazz = bean.getClass();
            if (AopUtils.isAopProxy(bean)) {
                clazz = AopProxyUtils.ultimateTargetClass(bean);
            }
            CrudRequestMapping crudRequestMapping = AnnotationUtils.findAnnotation(clazz, CrudRequestMapping.class);
            SaIgnore saIgnore = AnnotationUtils.findAnnotation(clazz, SaIgnore.class);
            if (crudRequestMapping != null) {
                // 缓存权限前缀
                CrudApiPermissionPrefixCache.put(clazz, crudRequestMapping.value());
                // 使用 @CrudRequestMapping 的 Controller，如果使用了 @SaIgnore 注解，则表示忽略认证和权限校验
                if (saIgnore != null) {
                    return crudRequestMapping.value() + StringConstants.PATH_PATTERN;
                }
            }
            return null;
        }).filter(Objects::nonNull).toList();
        if (!additionalExcludes.isEmpty()) {
            // 合并现有的 excludes 和新扫描到的
            String[] existingExcludes = Optional.ofNullable(properties.getSecurity().getExcludes())
                .orElse(new String[0]);
            String[] combinedExcludes = Stream.concat(Arrays.stream(existingExcludes), additionalExcludes.stream())
                .toArray(String[]::new);
            properties.getSecurity().setExcludes(combinedExcludes);
        }
        log.debug("缓存 CRUD API 权限前缀完成：{}", CrudApiPermissionPrefixCache.getAll().values());
    }
}
