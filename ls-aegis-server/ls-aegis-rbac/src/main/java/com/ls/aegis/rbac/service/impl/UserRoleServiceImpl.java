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

package com.ls.aegis.rbac.service.impl;

import cn.crane4j.annotation.AutoOperate;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ls.aegis.common.enums.RoleCodeEnum;
import com.ls.aegis.rbac.constant.SystemConstants;
import com.ls.aegis.rbac.mapper.UserRoleMapper;
import com.ls.aegis.rbac.model.entity.RoleDO;
import com.ls.aegis.rbac.model.entity.UserRoleDO;
import com.ls.aegis.rbac.model.entity.user.UserDO;
import com.ls.aegis.rbac.model.query.RoleUserQuery;
import com.ls.aegis.rbac.model.resp.role.RoleUserResp;
import com.ls.aegis.rbac.service.RoleService;
import com.ls.aegis.rbac.service.UserRoleService;
import com.ls.aegis.rbac.service.UserService;
import top.continew.starter.core.util.CollUtils;
import top.continew.starter.core.util.validation.CheckUtils;
import top.continew.starter.data.util.QueryWrapperHelper;
import top.continew.starter.extension.crud.model.query.PageQuery;
import top.continew.starter.extension.crud.model.resp.PageResp;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 用户和角色业务实现
 *
 * @author Charles7c
 * @since 2023/2/20 21:30
 */
@Service
@RequiredArgsConstructor
public class UserRoleServiceImpl implements UserRoleService {

    private final UserRoleMapper baseMapper;
    @Lazy
    @Resource
    private RoleService roleService;
    @Lazy
    @Resource
    private UserService userService;

    @Override
    @AutoOperate(type = RoleUserResp.class, on = "list")
    public PageResp<RoleUserResp> pageUser(RoleUserQuery query, PageQuery pageQuery) {
        String description = query.getDescription();
        QueryWrapper<UserRoleDO> queryWrapper = new QueryWrapper<UserRoleDO>().eq("t1.role_id", query.getRoleId())
            .and(StrUtil.isNotBlank(description), q -> q.like("t2.username", description)
                .or()
                .like("t2.nickname", description)
                .or()
                .like("t2.description", description));
        QueryWrapperHelper.sort(queryWrapper, pageQuery.getSort());
        IPage<RoleUserResp> page = baseMapper.selectUserPage(new Page<>(pageQuery.getPage(), pageQuery
            .getSize()), queryWrapper);
        return PageResp.build(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean assignRolesToUser(List<Long> roleIds, Long userId) {
        UserDO userDO = userService.getById(userId);
        List<Long> oldRoleIdList = this.listRoleIdByUserId(userId);
        // 角色无变化则直接返回（修改用户资料时会再次提交原角色，不能误拦超级管理员）
        if (CollUtil.isEmpty(CollUtil.disjunction(roleIds, oldRoleIdList))) {
            return false;
        }
        CheckUtils.throwIf(Boolean.TRUE.equals(userDO.getIsSystem()), "[{}] 是系统内置用户，不允许变更角色", userDO
            .getNickname());
        // 仅禁止新分配超级管理员角色
        Collection<Long> addedRoleIds = CollUtil.subtract(roleIds, oldRoleIdList);
        CheckUtils.throwIf(CollUtil.isNotEmpty(addedRoleIds) && addedRoleIds.contains(SystemConstants.SUPER_ADMIN_ROLE_ID), "不允许分配超级管理员角色");
        if (CollUtil.isNotEmpty(addedRoleIds)) {
            Set<String> addedRoleCodes = CollUtils.mapToSet(roleService.listByIds(addedRoleIds), RoleDO::getCode);
            CheckUtils.throwIf(addedRoleCodes.contains(RoleCodeEnum.TENANT_ADMIN.getCode()), "不允许分配系统管理员角色");
        }
        // 删除原有关联
        baseMapper.lambdaUpdate().eq(UserRoleDO::getUserId, userId).remove();
        // 保存最新关联
        List<UserRoleDO> userRoleList = CollUtils.mapToList(roleIds, roleId -> new UserRoleDO(userId, roleId));
        return baseMapper.insertBatch(userRoleList);
    }

    @Override
    public boolean assignRoleToUsers(Long roleId, List<Long> userIds) {
        List<UserRoleDO> userRoleList = CollUtils.mapToList(userIds, userId -> new UserRoleDO(userId, roleId));
        return baseMapper.insertBatch(userRoleList);
    }

    @Override
    public void deleteByIds(List<Long> ids) {
        baseMapper.deleteByIds(ids);
    }

    @Override
    public void deleteByUserIds(List<Long> userIds) {
        if (CollUtil.isEmpty(userIds)) {
            return;
        }
        baseMapper.lambdaUpdate().in(UserRoleDO::getUserId, userIds).remove();
    }

    @Override
    public void saveBatch(List<UserRoleDO> list) {
        baseMapper.insert(list);
    }

    @Override
    public List<Long> listRoleIdByUserId(Long userId) {
        return baseMapper.lambdaQuery()
            .select(UserRoleDO::getRoleId)
            .eq(UserRoleDO::getUserId, userId)
            .list()
            .stream()
            .map(UserRoleDO::getRoleId)
            .toList();
    }

    @Override
    public List<Long> listUserIdByRoleId(Long roleId) {
        return baseMapper.lambdaQuery()
            .select(UserRoleDO::getUserId)
            .eq(UserRoleDO::getRoleId, roleId)
            .list()
            .stream()
            .map(UserRoleDO::getUserId)
            .toList();
    }

    @Override
    public boolean isRoleIdExists(List<Long> roleIds) {
        if (CollUtil.isEmpty(roleIds)) {
            return false;
        }
        return baseMapper.lambdaQuery().in(UserRoleDO::getRoleId, roleIds).exists();
    }
}
