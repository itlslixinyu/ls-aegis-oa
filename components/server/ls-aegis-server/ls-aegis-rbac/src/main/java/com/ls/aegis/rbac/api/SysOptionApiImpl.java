package com.ls.aegis.rbac.api;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.ls.aegis.common.api.system.SysOptionApi;
import com.ls.aegis.rbac.enums.OptionCategoryEnum;
import com.ls.aegis.rbac.service.OptionService;

/**
 * {@code sys_option} 读取：业务域经此接口取值，避免依赖 RBAC 实现细节
 */
@Service
@RequiredArgsConstructor
public class SysOptionApiImpl implements SysOptionApi {

    private final OptionService optionService;

    @Override
    public String getValue(String category, String code) {
        if (StrUtil.isBlank(category) || StrUtil.isBlank(code)) {
            return null;
        }
        OptionCategoryEnum cat;
        try {
            cat = OptionCategoryEnum.valueOf(category.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
        String value = optionService.getByCategory(cat).get(code);
        return StrUtil.isBlank(value) ? null : value;
    }

    @Override
    public String getValue(String category, String code, String defaultValue) {
        return StrUtil.blankToDefault(getValue(category, code), defaultValue);
    }
}
