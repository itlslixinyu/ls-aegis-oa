package com.ls.aegis.rbac.mapper.user;

import org.apache.ibatis.annotations.Mapper;
import com.ls.aegis.rbac.model.entity.user.UserPreferenceDO;
import top.continew.starter.data.mapper.BaseMapper;

/**
 * 用户界面偏好 Mapper
 */
@Mapper
public interface UserPreferenceMapper extends BaseMapper<UserPreferenceDO> {
}
