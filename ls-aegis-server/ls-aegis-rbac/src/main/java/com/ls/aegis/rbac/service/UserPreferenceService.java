package com.ls.aegis.rbac.service;

import java.util.Map;
import java.util.List;

/**
 * 用户界面偏好业务接口
 */
public interface UserPreferenceService {

    /**
     * 查询当前用户界面配置
     *
     * @param userId 用户 ID
     * @return 界面配置（无则返回 null）
     */
    Map<String, Object> getUiSettings(Long userId);

    /**
     * 保存当前用户界面配置（覆盖）
     *
     * @param settings 界面配置
     * @param userId   用户 ID
     */
    void saveUiSettings(Map<String, Object> settings, Long userId);

    /** Remove preferences when their users are deleted in the same transaction. */
    void deleteByUserIds(List<Long> userIds);
}
