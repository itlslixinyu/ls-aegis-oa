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

package com.ls.aegis.rbac.auth.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import com.ls.aegis.crypto.api.IGmCrypto;
import com.xkcoding.justauth.autoconfigure.JustAuthProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.zhyd.oauth.AuthRequestBuilder;
import me.zhyd.oauth.config.AuthConfig;
import me.zhyd.oauth.request.AuthRequest;
import me.zhyd.oauth.utils.AuthStateUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ls.aegis.rbac.auth.model.req.ForgotPasswordReq;
import com.ls.aegis.rbac.auth.model.req.LoginReq;
import com.ls.aegis.compat.common.annotation.RateLimiter;
import com.ls.aegis.compat.common.enums.LimitType;
import com.ls.aegis.rbac.auth.model.resp.GmPublicKeyResp;
import com.ls.aegis.rbac.auth.model.resp.LoginResp;
import com.ls.aegis.rbac.auth.model.resp.RouteResp;
import com.ls.aegis.rbac.auth.model.resp.SocialAuthAuthorizeResp;
import com.ls.aegis.rbac.auth.model.resp.UserInfoResp;
import com.ls.aegis.rbac.auth.service.AuthService;
import com.ls.aegis.common.context.UserContext;
import com.ls.aegis.common.context.UserContextHolder;
import com.ls.aegis.common.util.SecureUtils;
import com.ls.aegis.rbac.model.resp.user.UserDetailResp;
import com.ls.aegis.rbac.service.UserService;
import top.continew.starter.core.exception.BadRequestException;
import top.continew.starter.log.annotation.Log;

import java.util.List;

/**
 * 认证 API
 *
 * @author Charles7c
 * @since 2022/12/21 20:37
 */
@Tag(name = "认证 API")
@Log(module = "登录")
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final JustAuthProperties authProperties;
    private final ObjectProvider<IGmCrypto> gmCryptoProvider;

    @SaIgnore
    @Operation(summary = "国密公钥", description = "获取 SM2 公钥（不下发私钥），供前端传输加密")
    @GetMapping("/gm/public-key")
    public GmPublicKeyResp gmPublicKey() {
        GmPublicKeyResp resp = new GmPublicKeyResp();
        resp.setAlgorithm("SM2");
        IGmCrypto gmCrypto = gmCryptoProvider.getIfAvailable();
        if (gmCrypto == null || !gmCrypto.isEnabled()) {
            resp.setEnable(false);
            return resp;
        }
        resp.setEnable(true);
        resp.setPublicKeyHex(gmCrypto.getSm2PublicKeyHex());
        resp.setPublicKeyBase64(gmCrypto.getSm2PublicKeyBase64());
        return resp;
    }

    @SaIgnore
    @RateLimiter(key = "auth_login", time = 60, count = 30, limitType = LimitType.IP)
    @Operation(summary = "登录", description = "用户登录")
    @PostMapping("/login")
    public LoginResp login(@RequestBody @Valid LoginReq req, HttpServletRequest request) {
        return authService.login(req, request);
    }

    @SaIgnore
    @RateLimiter(key = "auth_forgot_password", time = 60, count = 10, limitType = LimitType.IP)
    @Operation(summary = "忘记密码", description = "通过邮箱验证码自助重置登录密码")
    @PostMapping("/forgot-password")
    public void forgotPassword(@RequestBody @Valid ForgotPasswordReq req) {
        String newPassword = SecureUtils.decryptPasswordByRsaPrivateKey(req.getNewPassword(), "新密码解密失败", true);
        userService.forgotPassword(req.getEmail(), req.getCaptcha(), newPassword);
    }

    @Operation(summary = "登出", description = "注销用户的当前登录")
    @Parameter(name = "Authorization", description = "令牌", required = true, example = "Bearer xxxx-xxxx-xxxx-xxxx", in = ParameterIn.HEADER)
    @PostMapping("/logout")
    public Object logout() {
        Object loginId = StpUtil.getLoginId(-1L);
        StpUtil.logout();
        return loginId;
    }

    @SaIgnore
    @Operation(summary = "三方账号登录授权", description = "三方账号登录授权")
    @Parameter(name = "source", description = "来源", example = "gitee", in = ParameterIn.PATH)
    @GetMapping("/{source}")
    public SocialAuthAuthorizeResp authorize(@PathVariable String source) {
        if (!authProperties.isEnabled()) {
            throw new BadRequestException("社交登录未启用");
        }
        AuthRequest authRequest = this.getAuthRequest(source);
        return SocialAuthAuthorizeResp.builder()
            .authorizeUrl(authRequest.authorize(AuthStateUtils.createState()))
            .build();
    }

    @Log(ignore = true)
    @Operation(summary = "获取用户信息", description = "获取登录用户信息")
    @GetMapping("/user/info")
    public UserInfoResp getUserInfo() {
        UserContext userContext = UserContextHolder.getContext();
        UserDetailResp userDetailResp = userService.get(userContext.getId());
        UserInfoResp userInfoResp = BeanUtil.copyProperties(userDetailResp, UserInfoResp.class);
        userInfoResp.setPermissions(userContext.getPermissions());
        userInfoResp.setRoles(userContext.getRoleCodes());
        userInfoResp.setPwdExpired(userContext.isPasswordExpired());
        return userInfoResp;
    }

    @Log(ignore = true)
    @Operation(summary = "获取路由信息", description = "获取登录用户的路由信息")
    @GetMapping("/user/route")
    public List<RouteResp> listRoute() {
        return authService.buildRouteTree(UserContextHolder.getUserId());
    }

    private AuthRequest getAuthRequest(String source) {
        try {
            AuthConfig authConfig = authProperties.getType().get(source.toUpperCase());
            return AuthRequestBuilder.builder().source(source).authConfig(authConfig).build();
        } catch (Exception e) {
            throw new BadRequestException("暂不支持 [%s] 平台账号登录".formatted(source));
        }
    }
}
