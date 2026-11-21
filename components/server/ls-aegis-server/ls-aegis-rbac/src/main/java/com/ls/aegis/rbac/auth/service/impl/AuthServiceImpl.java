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

package com.ls.aegis.rbac.auth.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.lang.tree.TreeNodeConfig;
import cn.hutool.core.lang.tree.TreeUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.ls.aegis.rbac.auth.LoginHandler;
import com.ls.aegis.rbac.auth.LoginHandlerFactory;
import com.ls.aegis.rbac.auth.enums.AuthTypeEnum;
import com.ls.aegis.rbac.auth.model.req.LoginReq;
import com.ls.aegis.rbac.auth.model.resp.LoginResp;
import com.ls.aegis.rbac.auth.model.resp.RouteResp;
import com.ls.aegis.rbac.auth.service.AuthService;
import com.ls.aegis.common.context.RoleContext;
import com.ls.aegis.common.enums.DisEnableStatusEnum;
import com.ls.aegis.rbac.constant.SystemConstants;
import com.ls.aegis.rbac.enums.MenuTypeEnum;
import com.ls.aegis.rbac.model.resp.ClientResp;
import com.ls.aegis.rbac.model.resp.MenuResp;
import com.ls.aegis.rbac.service.ClientService;
import com.ls.aegis.rbac.service.MenuService;
import com.ls.aegis.rbac.service.ModuleService;
import com.ls.aegis.rbac.service.RoleService;
import top.continew.starter.core.util.validation.ValidationUtils;
import top.continew.starter.extension.crud.annotation.TreeField;
import top.continew.starter.extension.crud.autoconfigure.CrudProperties;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 认证业务实现
 *
 * @author Charles7c
 * @since 2022/12/21 21:49
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final LoginHandlerFactory loginHandlerFactory;
    private final ClientService clientService;
    private final RoleService roleService;
    private final MenuService menuService;
    private final ModuleService moduleService;
    private final CrudProperties crudProperties;

    @Override
    public LoginResp login(LoginReq req, HttpServletRequest request) {
        AuthTypeEnum authType = req.getAuthType();
        // 校验客户端
        ClientResp client = clientService.getByClientId(req.getClientId());
        ValidationUtils.throwIfNull(client, "客户端不存在");
        ValidationUtils.throwIf(DisEnableStatusEnum.DISABLE.equals(client.getStatus()), "客户端已禁用");
        ValidationUtils.throwIf(!client.getAuthType().contains(authType.getValue()), "该客户端暂未授权 [{}] 认证", authType
            .getDescription());
        // 获取处理器
        LoginHandler<LoginReq> loginHandler = loginHandlerFactory.getHandler(authType);
        // 登录前置处理
        loginHandler.preLogin(req, client, request);
        // 登录
        LoginResp loginResp = loginHandler.login(req, client, request);
        // 登录后置处理
        loginHandler.postLogin(req, client, request);
        return loginResp;
    }

    @Override
    public List<RouteResp> buildRouteTree(Long userId) {
        Set<RoleContext> roleSet = roleService.listByUserId(userId);
        if (CollUtil.isEmpty(roleSet)) {
            return new ArrayList<>(0);
        }
        // 查询菜单列表
        Set<MenuResp> menuSet = new LinkedHashSet<>();
        if (roleSet.stream().anyMatch(r -> SystemConstants.SUPER_ADMIN_ROLE_ID.equals(r.getId()))) {
            menuSet.addAll(menuService.listByRoleId(SystemConstants.SUPER_ADMIN_ROLE_ID));
        } else {
            roleSet.forEach(r -> menuSet.addAll(menuService.listByRoleId(r.getId())));
        }
        List<MenuResp> menuList = menuSet.stream().filter(m -> !MenuTypeEnum.BUTTON.equals(m.getType())).toList();
        menuList = filterByModuleSwitch(menuList);
        if (CollUtil.isEmpty(menuList)) {
            return new ArrayList<>(0);
        }
        // 构建路由树
        TreeField treeField = MenuResp.class.getDeclaredAnnotation(TreeField.class);
        TreeNodeConfig treeNodeConfig = crudProperties.getTreeDictModel().genTreeNodeConfig(treeField);
        List<Tree<Long>> treeList = TreeUtil.build(menuList, treeField.rootId(), treeNodeConfig, (m, tree) -> {
            tree.setId(m.getId());
            tree.setParentId(m.getParentId());
            tree.setName(m.getTitle());
            tree.setWeight(m.getSort());
            tree.putExtra("type", m.getType().getValue());
            tree.putExtra("path", m.getPath());
            tree.putExtra("name", m.getName());
            tree.putExtra("component", m.getComponent());
            tree.putExtra("redirect", m.getRedirect());
            tree.putExtra("icon", m.getIcon());
            tree.putExtra("isExternal", m.getIsExternal());
            tree.putExtra("isCache", m.getIsCache());
            tree.putExtra("isHidden", m.getIsHidden());
            tree.putExtra("permission", m.getPermission());
            // 目录始终展开：单子菜单不被侧栏展平
            tree.putExtra("alwaysShow", MenuTypeEnum.DIR.equals(m.getType()));
        });
        return BeanUtil.copyToList(treeList, RouteResp.class);
    }

    /**
     * 按功能模块总开关剔除已关闭模块的菜单树
     */
    private List<MenuResp> filterByModuleSwitch(List<MenuResp> menuList) {
        Set<Long> disabledRoots = moduleService.disabledMenuRootIds();
        if (CollUtil.isEmpty(disabledRoots)) {
            return menuList;
        }
        Set<Long> excludeIds = new HashSet<>(disabledRoots);
        boolean changed = true;
        while (changed) {
            changed = false;
            for (MenuResp menu : menuList) {
                if (excludeIds.contains(menu.getParentId()) && excludeIds.add(menu.getId())) {
                    changed = true;
                }
            }
        }
        return menuList.stream().filter(m -> !excludeIds.contains(m.getId())).toList();
    }
}
