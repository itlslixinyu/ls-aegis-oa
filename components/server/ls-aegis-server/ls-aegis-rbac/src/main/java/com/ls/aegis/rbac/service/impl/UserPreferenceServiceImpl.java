package com.ls.aegis.rbac.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.ls.aegis.rbac.mapper.user.UserPreferenceMapper;
import com.ls.aegis.rbac.model.entity.user.UserPreferenceDO;
import com.ls.aegis.rbac.service.UserPreferenceService;
import top.continew.starter.core.util.validation.CheckUtils;
import top.continew.starter.core.util.validation.ValidationUtils;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;

/**
 * 用户界面偏好业务实现
 */
@Service
@RequiredArgsConstructor
public class UserPreferenceServiceImpl implements UserPreferenceService {

    /** 界面配置 JSON 最大长度（防滥用） */
    private static final int MAX_UI_JSON_LENGTH = 8192;

    private final UserPreferenceMapper baseMapper;

    @Override
    public void deleteByUserIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return;
        List<Long> ids = userIds.stream().filter(java.util.Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) return;
        baseMapper.delete(com.baomidou.mybatisplus.core.toolkit.Wrappers.<UserPreferenceDO>lambdaQuery()
            .in(UserPreferenceDO::getUserId, ids));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getUiSettings(Long userId) {
        UserPreferenceDO preference = baseMapper.lambdaQuery().eq(UserPreferenceDO::getUserId, userId).one();
        if (preference == null || StrUtil.isBlank(preference.getUiJson())) {
            return null;
        }
        return JSONUtil.toBean(preference.getUiJson(), Map.class);
    }

    @Override
    public void saveUiSettings(Map<String, Object> settings, Long userId) {
        ValidationUtils.throwIf(settings == null || settings.isEmpty(), "界面配置不能为空");
        String uiJson = JSONUtil.toJsonStr(settings);
        CheckUtils.throwIf(uiJson.length() > MAX_UI_JSON_LENGTH, "界面配置内容过长");

        UserPreferenceDO existing = baseMapper.lambdaQuery().eq(UserPreferenceDO::getUserId, userId).one();
        if (existing == null) {
            UserPreferenceDO preference = new UserPreferenceDO();
            preference.setUserId(userId);
            preference.setUiJson(uiJson);
            baseMapper.insert(preference);
            return;
        }
        baseMapper.lambdaUpdate()
            .set(UserPreferenceDO::getUiJson, uiJson)
            .set(UserPreferenceDO::getUpdateTime, LocalDateTime.now())
            .eq(UserPreferenceDO::getUserId, userId)
            .update();
    }
}
