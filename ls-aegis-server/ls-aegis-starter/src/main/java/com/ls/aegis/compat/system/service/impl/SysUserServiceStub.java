package com.ls.aegis.compat.system.service.impl;

import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.ls.aegis.compat.common.core.domain.entity.SysUser;
import com.ls.aegis.compat.system.service.ISysUserService;
import com.ls.aegis.common.api.system.UserApi;
import cn.hutool.extra.spring.SpringUtil;

/**
 * 用户查询过渡桩：尽量从 UserApi 取昵称，其余字段留空
 */
@Service
public class SysUserServiceStub implements ISysUserService
{
    private static final Logger log = LoggerFactory.getLogger(SysUserServiceStub.class);

    @Override
    public SysUser selectUserById(Long userId)
    {
        if (userId == null)
        {
            return null;
        }
        SysUser user = new SysUser();
        user.setUserId(userId);
        try
        {
            UserApi userApi = SpringUtil.getBean(UserApi.class);
            String nickname = userApi.getNicknameById(userId);
            user.setNickName(nickname);
            user.setUserName(nickname);
        }
        catch (Exception e)
        {
            log.debug("UserApi 不可用，返回最小 SysUser: {}", userId);
        }
        return user;
    }

    @Override
    public SysUser selectUserByUserName(String userName)
    {
        if (userName == null)
        {
            return null;
        }
        SysUser user = new SysUser();
        user.setUserName(userName);
        user.setNickName(userName);
        return user;
    }

    @Override
    public List<SysUser> selectUserList(SysUser user)
    {
        return Collections.emptyList();
    }
}
