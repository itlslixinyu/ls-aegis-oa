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

package com.ls.aegis.rbac.config.mail;

import cn.hutool.core.util.StrUtil;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import top.continew.starter.core.exception.BadRequestException;
import top.continew.starter.messaging.mail.util.MailUtils;

/**
 * 邮件发送辅助（统一中文错误提示）
 */
public final class MailSendHelper {

    private MailSendHelper() {
    }

    /**
     * 发送 HTML 邮件；失败时抛出可读的业务异常，避免前端只看到「服务器异常」
     */
    public static void sendHtml(String to, String subject, String content) {
        try {
            MailUtils.sendHtml(to, subject, content);
        } catch (MailAuthenticationException e) {
            throw new BadRequestException(buildAuthFailMessage(e));
        } catch (MailSendException e) {
            throw new BadRequestException("邮件发送失败：无法连接邮件服务器，请检查地址、端口及 SSL 配置。详情：%s"
                .formatted(rootMessage(e)));
        } catch (MailException e) {
            throw new BadRequestException("邮件发送失败：%s".formatted(rootMessage(e)));
        } catch (Exception e) {
            throw new BadRequestException("邮件发送失败：%s".formatted(StrUtil.blankToDefault(e.getMessage(), e
                .getClass()
                .getSimpleName())));
        }
    }

    private static String buildAuthFailMessage(MailAuthenticationException e) {
        String detail = rootMessage(e);
        return "邮件服务器认证失败（常见 526）：请确认①邮箱账号为完整地址；②密码为网页登录密码或「三方客户端安全密码」（开启后必须用安全密码）；③主机与端口正确（阿里企业邮常见 smtp.qiye.aliyun.com:465 并启用 SSL）。详情：%s"
            .formatted(detail);
    }

    private static String rootMessage(Throwable e) {
        Throwable root = e;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return StrUtil.blankToDefault(root.getMessage(), e.getMessage());
    }
}
