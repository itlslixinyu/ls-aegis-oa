package com.ls.aegis.rbac.api;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.ls.aegis.common.api.docno.DocNoPrefixApi;
import com.ls.aegis.rbac.enums.OptionCategoryEnum;
import com.ls.aegis.rbac.service.OptionService;

/**
 * 编号前缀：读 {@code sys_option} EOMS 类别
 */
@Service
@RequiredArgsConstructor
public class DocNoPrefixApiImpl implements DocNoPrefixApi {

    private static final String PREFIX_PATTERN = "^[A-Z]{2,4}$";

    private final OptionService optionService;

    @Override
    public String resolvePrefix(String optionCode, String defaultPrefix) {
        String value = optionService.getByCategory(OptionCategoryEnum.EOMS).get(optionCode);
        if (StrUtil.isNotBlank(value)) {
            String normalized = value.trim().toUpperCase();
            if (normalized.matches(PREFIX_PATTERN)) {
                return normalized;
            }
        }
        return defaultPrefix;
    }
}
