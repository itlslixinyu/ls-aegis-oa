package com.ls.aegis.compat.system.service;

import com.ls.aegis.compat.common.core.domain.entity.SysUser;
import java.util.List;

/** 过渡桩：用户查询（后续改为 Aegis User API） */
public interface ISysUserService {
    SysUser selectUserById(Long userId);
    SysUser selectUserByUserName(String userName);
    List<SysUser> selectUserList(SysUser user);
}