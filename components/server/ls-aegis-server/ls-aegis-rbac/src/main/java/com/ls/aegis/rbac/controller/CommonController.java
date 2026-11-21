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

import com.ls.aegis.biz.file.service.FileService;
import com.ls.aegis.biz.file.model.resp.file.FileResp;
import com.ls.aegis.biz.file.model.resp.file.FileUploadResp;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.JakartaServletUtil;
import com.alicp.jetcache.anno.Cached;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.x.file.storage.core.FileInfo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.ls.aegis.redis.constant.CacheConstants;
import com.ls.aegis.rbac.enums.OptionCategoryEnum;
import com.ls.aegis.rbac.model.query.OptionQuery;
import com.ls.aegis.rbac.model.resp.WeatherNowResp;
import com.ls.aegis.rbac.service.DictItemService;
import com.ls.aegis.rbac.service.ModuleService;
import com.ls.aegis.rbac.service.OptionService;
import com.ls.aegis.rbac.service.WeatherService;
import top.continew.starter.core.util.validation.ValidationUtils;
import top.continew.starter.extension.crud.model.resp.LabelValueResp;
import top.continew.starter.extension.tenant.annotation.TenantIgnore;
import top.continew.starter.extension.tenant.context.TenantContextHolder;
import top.continew.starter.log.annotation.Log;

import java.io.IOException;
import java.util.List;

/**
 * 公共 API
 *
 * @author Charles7c
 * @since 2023/1/22 21:48
 */
@Tag(name = "公共 API")
@Log(ignore = true)
@Validated
@RestController("systemCommonController")
@RequiredArgsConstructor
@RequestMapping("/system/common")
public class CommonController {

    private final FileService fileService;
    private final DictItemService dictItemService;
    private final OptionService optionService;
    private final ModuleService moduleService;
    private final WeatherService weatherService;

    @Operation(summary = "上传文件", description = "上传文件")
    @Parameter(name = "parentPath", description = "上级目录", example = "/", in = ParameterIn.QUERY)
    @PostMapping("/file")
    public FileUploadResp upload(@RequestPart @NotNull(message = "文件不能为空") MultipartFile file,
                                 @RequestParam(required = false) String parentPath) throws IOException {
        ValidationUtils.throwIf(file::isEmpty, "文件不能为空");
        FileInfo fileInfo = fileService.upload(file, parentPath);
        return FileUploadResp.builder()
            .id(fileInfo.getId())
            .url(fileInfo.getUrl())
            .thUrl(fileInfo.getThUrl())
            .metadata(fileInfo.getMetadata())
            .build();
    }

    @Operation(summary = "查询文件信息", description = "按文件 ID 查询名称等元数据（需登录）")
    @Parameter(name = "id", description = "文件 ID", example = "1", in = ParameterIn.PATH)
    @GetMapping("/file/{id}/info")
    public FileResp fileInfo(@PathVariable Long id) {
        return fileService.get(id);
    }

    @Operation(summary = "下载/预览文件", description = "按文件 ID 流式输出（需登录；供前端带 Token 拉取，勿用 /file/** 直链新开页）")
    @Parameter(name = "id", description = "文件 ID", example = "1", in = ParameterIn.PATH)
    @GetMapping("/file/{id}")
    public void download(@PathVariable Long id, HttpServletResponse response) throws IOException {
        fileService.download(id, response);
    }

    @TenantIgnore
    @SaIgnore
    @Operation(summary = "查询字典", description = "查询字典列表（标签非敏感，供 PC/H5 共用）")
    @Parameter(name = "code", description = "字典编码", example = "notice_type", in = ParameterIn.PATH)
    @GetMapping("/dict/{code}")
    public List<LabelValueResp> listDict(@PathVariable String code) {
        return dictItemService.listByDictCode(code);
    }

    @TenantIgnore
    @SaIgnore
    @Operation(summary = "查询系统配置参数", description = "查询系统配置参数")
    @GetMapping("/dict/option/site")
    @Cached(key = "'SITE'", name = CacheConstants.OPTION_KEY_PREFIX)
    public List<LabelValueResp<String>> listSiteOptionDict() {
        OptionQuery optionQuery = new OptionQuery();
        optionQuery.setCategory(OptionCategoryEnum.SITE.name());
        return optionService.list(optionQuery)
            .stream()
            .map(option -> new LabelValueResp<>(option.getCode(), StrUtil.nullToDefault(option.getValue(), option
                .getDefaultValue())))
            .toList();
    }

    @TenantIgnore
    @SaIgnore
    @Operation(summary = "查询天气配置参数", description = "运营中枢顶栏天气展示用（不含敏感密钥）")
    @GetMapping("/dict/option/weather")
    @Cached(key = "'WEATHER'", name = CacheConstants.OPTION_KEY_PREFIX)
    public List<LabelValueResp<String>> listWeatherOptionDict() {
        OptionQuery optionQuery = new OptionQuery();
        optionQuery.setCategory(OptionCategoryEnum.WEATHER.name());
        return optionService.list(optionQuery)
            .stream()
            .filter(option -> !"WEATHER_API_KEY".equals(option.getCode())
                && !"WEATHER_JWT_PRIVATE_KEY".equals(option.getCode()))
            .map(option -> new LabelValueResp<>(option.getCode(), StrUtil.nullToDefault(option.getValue(), option
                .getDefaultValue())))
            .toList();
    }

    @TenantIgnore
    @SaIgnore
    @Operation(summary = "查询大屏配置参数", description = "运营中枢标题与展示开关")
    @GetMapping("/dict/option/dashboard")
    @Cached(key = "'DASHBOARD'", name = CacheConstants.OPTION_KEY_PREFIX)
    public List<LabelValueResp<String>> listDashboardOptionDict() {
        OptionQuery optionQuery = new OptionQuery();
        optionQuery.setCategory(OptionCategoryEnum.DASHBOARD.name());
        return optionService.list(optionQuery)
            .stream()
            .map(option -> new LabelValueResp<>(option.getCode(), StrUtil.nullToDefault(option.getValue(), option
                .getDefaultValue())))
            .toList();
    }

    @TenantIgnore
    @SaIgnore
    @Operation(summary = "查询租户开启状态", description = "中间件已装载且功能模块「租户管理」开启时，登录页显示租户编码框")
    @GetMapping("/dict/option/tenant")
    public Boolean tenantEnabled() {
        // 禁止缓存：YAML 或模块开关变更后须立即生效
        return TenantContextHolder.isTenantEnabled() && moduleService.isTenantEnabled();
    }

    @TenantIgnore
    @SaIgnore
    @Operation(summary = "查询实时天气", description = "运营中枢顶栏：支持本地模拟 / 和风天气（API Key 或 JWT）；城市可自动定位")
    @Parameter(name = "lat", description = "纬度（浏览器定位可选）", example = "39.9", in = ParameterIn.QUERY)
    @Parameter(name = "lon", description = "经度（浏览器定位可选）", example = "116.4", in = ParameterIn.QUERY)
    @GetMapping("/weather/now")
    public WeatherNowResp weatherNow(@RequestParam(required = false) Double lat,
                                     @RequestParam(required = false) Double lon,
                                     HttpServletRequest request) {
        return weatherService.now(JakartaServletUtil.getClientIP(request), lat, lon);
    }
}
