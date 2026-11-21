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

package com.ls.aegis.rbac.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import cn.hutool.extra.servlet.JakartaServletUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ls.aegis.rbac.model.query.OptionQuery;
import com.ls.aegis.rbac.model.req.MailTestReq;
import com.ls.aegis.rbac.model.req.OptionReq;
import com.ls.aegis.rbac.model.req.OptionValueResetReq;
import com.ls.aegis.rbac.model.resp.OptionResp;
import com.ls.aegis.rbac.model.resp.WeatherNowResp;
import com.ls.aegis.rbac.service.OptionService;
import com.ls.aegis.rbac.service.WeatherService;

import java.util.List;

/**
 * 参数管理 API
 *
 * @author Bull-BCLS
 * @since 2023/8/26 19:38
 */
@Tag(name = "参数管理 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/system/option")
public class OptionController {

    private final OptionService baseService;
    private final WeatherService weatherService;

    @Operation(summary = "查询参数列表", description = "查询参数列表")
    @SaCheckPermission(value = {"system:siteConfig:get", "system:securityConfig:get", "system:loginConfig:get",
        "system:mailConfig:get", "system:weatherConfig:get", "system:dashboardConfig:get",
        "system:moduleConfig:get", "system:eomsConfig:get", "system:h5Auth:get"}, mode = SaMode.OR)
    @GetMapping
    public List<OptionResp> list(@Valid OptionQuery query) {
        return baseService.list(query);
    }

    @Operation(summary = "修改参数", description = "修改参数")
    @SaCheckPermission(value = {"system:siteConfig:update", "system:securityConfig:update", "system:loginConfig:update",
        "system:mailConfig:update", "system:weatherConfig:update", "system:dashboardConfig:update",
        "system:moduleConfig:update", "system:eomsConfig:update", "system:h5Auth:update"}, mode = SaMode.OR)
    @PutMapping
    public void update(@RequestBody @Valid List<OptionReq> options) {
        baseService.update(options);
        // 天气凭据变更后清进程内 JWT，避免仍用旧令牌
        boolean weatherChanged = options.stream().anyMatch(o -> o.getCode() != null && o.getCode().startsWith("WEATHER_"));
        if (weatherChanged) {
            weatherService.clearTokenCache();
        }
    }

    @Operation(summary = "重置参数", description = "重置参数")
    @SaCheckPermission(value = {"system:siteConfig:update", "system:securityConfig:update", "system:loginConfig:update",
        "system:mailConfig:update", "system:weatherConfig:update", "system:dashboardConfig:update",
        "system:moduleConfig:update", "system:eomsConfig:update", "system:h5Auth:update"}, mode = SaMode.OR)
    @PatchMapping("/value")
    public void resetValue(@RequestBody @Valid OptionValueResetReq req) {
        baseService.resetValue(req);
        if (req.getCategory() != null && "WEATHER".equalsIgnoreCase(req.getCategory())) {
            weatherService.clearTokenCache();
        }
    }

    @Operation(summary = "查看敏感参数明文", description = "仅白名单敏感项；用于配置页点击「显示」时回显真实值")
    @SaCheckPermission(value = {"system:mailConfig:get", "system:mailConfig:update", "system:weatherConfig:get",
        "system:weatherConfig:update", "system:eomsConfig:get", "system:eomsConfig:update"}, mode = SaMode.OR)
    @GetMapping("/sensitive/{code}")
    public String getSensitivePlainValue(@PathVariable String code) {
        return baseService.getSensitivePlainValue(code);
    }

    @Operation(summary = "发送测试邮件", description = "使用当前邮件配置向指定邮箱发送一封测试邮件")
    @SaCheckPermission("system:mailConfig:update")
    @PostMapping("/mail/test")
    public void sendTestMail(@RequestBody @Valid MailTestReq req) {
        baseService.sendTestMail(req.getTo());
    }

    @Operation(summary = "立即刷新天气", description = "不等待刷新间隔，立即拉取当前天气（不清 JWT）")
    @SaCheckPermission(value = {"system:weatherConfig:get", "system:weatherConfig:update"}, mode = SaMode.OR)
    @PostMapping("/weather/refresh")
    public WeatherNowResp refreshWeather(HttpServletRequest request) {
        return weatherService.now(JakartaServletUtil.getClientIP(request), null, null);
    }

    @Operation(summary = "测试天气连接", description = "按已保存配置实测拉取天气（和风不回退模拟）")
    @SaCheckPermission(value = {"system:weatherConfig:get", "system:weatherConfig:update"}, mode = SaMode.OR)
    @PostMapping("/weather/test")
    public WeatherNowResp testWeather(HttpServletRequest request) {
        return weatherService.testConnection(JakartaServletUtil.getClientIP(request));
    }
}