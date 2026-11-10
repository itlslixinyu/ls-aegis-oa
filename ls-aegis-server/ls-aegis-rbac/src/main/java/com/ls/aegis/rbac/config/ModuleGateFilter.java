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

package com.ls.aegis.rbac.config;

import cn.hutool.json.JSONUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.ls.aegis.rbac.service.ModuleService;
import top.continew.starter.web.model.R;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 功能模块总开关：关闭时拦截对应业务 API
 *
 * @author ls
 * @since 2026/8/16
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 40)
@RequiredArgsConstructor
public class ModuleGateFilter extends OncePerRequestFilter {

    private final ModuleService moduleService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        if (path.startsWith("/tenant/") && !path.startsWith("/tenant/common") && !moduleService.isTenantEnabled()) {
            writeDisabled(response, "租户管理模块已禁用，请在「系统配置 - 功能模块」中开启");
            return;
        }
        if (path.startsWith("/schedule/") && !moduleService.isScheduleEnabled()) {
            writeDisabled(response, "任务调度模块已禁用，请在「系统配置 - 功能模块」中开启");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private void writeDisabled(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.OK.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter()
            .write(JSONUtil.toJsonStr(R.fail(String.valueOf(HttpStatus.FORBIDDEN.value()), message)));
    }
}
