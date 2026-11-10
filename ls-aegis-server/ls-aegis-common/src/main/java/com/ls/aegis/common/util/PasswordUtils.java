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

package com.ls.aegis.common.util;

import cn.hutool.core.util.RandomUtil;
import com.ls.aegis.common.constant.UserConstants;
import top.continew.starter.core.util.validation.ValidationUtils;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 口令工具
 *
 * @author LS-Aegis
 * @since 2026/3/16
 */
public final class PasswordUtils {

    /** 排除易混淆字符 */
    private static final String LOWER = "abcdefghijkmnopqrstuvwxyz";
    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String DIGIT = "23456789";
    private static final String SPECIAL = "!@#$%&*";
    private static final String ALL = LOWER + UPPER + DIGIT + SPECIAL;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private PasswordUtils() {
    }

    /**
     * 生成符合系统复杂度规则的随机口令（含大小写、数字、特殊字符）
     *
     * @param length 长度，最小 8
     * @return 明文口令
     */
    public static String generateRandomPassword(int length) {
        int len = Math.max(length, 8);
        ValidationUtils.throwIf(len > 32, "生成密码长度不能超过 32");
        List<Character> chars = new ArrayList<>(len);
        chars.add(LOWER.charAt(SECURE_RANDOM.nextInt(LOWER.length())));
        chars.add(UPPER.charAt(SECURE_RANDOM.nextInt(UPPER.length())));
        chars.add(DIGIT.charAt(SECURE_RANDOM.nextInt(DIGIT.length())));
        chars.add(SPECIAL.charAt(SECURE_RANDOM.nextInt(SPECIAL.length())));
        for (int i = chars.size(); i < len; i++) {
            chars.add(ALL.charAt(SECURE_RANDOM.nextInt(ALL.length())));
        }
        Collections.shuffle(chars, SECURE_RANDOM);
        StringBuilder sb = new StringBuilder(len);
        for (Character c : chars) {
            sb.append(c);
        }
        return sb.toString();
    }

    /**
     * 按默认长度生成随机口令
     */
    public static String generateRandomPassword() {
        return generateRandomPassword(UserConstants.DEFAULT_GENERATED_PASSWORD_LENGTH);
    }

    /**
     * 兼容旧调用：生成指定长度的字母数字串（不保证复杂度）
     */
    public static String randomAlphanumeric(int length) {
        return RandomUtil.randomString(length);
    }
}
