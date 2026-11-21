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

package com.ls.aegis.biz.tenant.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.alicp.jetcache.anno.Cached;
import lombok.RequiredArgsConstructor;
import me.ahoo.cosid.provider.IdGeneratorProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ls.aegis.common.api.system.RoleApi;
import com.ls.aegis.common.api.system.RoleMenuApi;
import com.ls.aegis.common.api.tenant.TenantDataApi;
import com.ls.aegis.mybatis.base.service.BaseServiceImpl;
import com.ls.aegis.common.config.TenantExtensionProperties;
import com.ls.aegis.redis.constant.CacheConstants;
import com.ls.aegis.common.enums.DisEnableStatusEnum;
import com.ls.aegis.common.enums.RoleCodeEnum;
import com.ls.aegis.common.model.dto.TenantDTO;
import com.ls.aegis.biz.tenant.constant.TenantCacheConstants;
import com.ls.aegis.biz.tenant.constant.TenantConstants;
import com.ls.aegis.biz.tenant.mapper.TenantMapper;
import com.ls.aegis.biz.tenant.model.entity.TenantDO;
import com.ls.aegis.biz.tenant.model.query.TenantQuery;
import com.ls.aegis.biz.tenant.model.req.TenantReq;
import com.ls.aegis.biz.tenant.model.resp.TenantDetailResp;
import com.ls.aegis.biz.tenant.model.resp.TenantResp;
import com.ls.aegis.biz.tenant.service.PackageMenuService;
import com.ls.aegis.biz.tenant.service.PackageService;
import com.ls.aegis.biz.tenant.service.TenantService;
import top.continew.starter.cache.redisson.util.RedisUtils;
import top.continew.starter.core.constant.StringConstants;
import top.continew.starter.core.util.validation.CheckUtils;
import top.continew.starter.extension.tenant.util.TenantUtils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 租户业务实现
 *
 * @author 小熊
 * @author Charles7c
 * @since 2024/11/26 17:20
 */
@Service
@RequiredArgsConstructor
public class TenantServiceImpl extends BaseServiceImpl<TenantMapper, TenantDO, TenantResp, TenantDetailResp, TenantQuery, TenantReq> implements TenantService {

    private final Map<String, TenantDataApi> tenantDataApiMap = SpringUtil.getBeansOfType(TenantDataApi.class);
    private final TenantExtensionProperties tenantExtensionProperties;
    private final PackageService packageService;
    private final PackageMenuService packageMenuService;
    private final IdGeneratorProvider idGeneratorProvider;
    private final RoleMenuApi roleMenuApi;
    private final RoleApi roleApi;

    @Override
    public Long create(TenantReq req) {
        this.checkNameRepeat(req.getName(), null);
        this.checkDomainRepeat(req.getDomain(), null);
        this.checkExpireTime(req.getExpireTime(), null);
        // 检查套餐
        packageService.checkStatus(req.getPackageId());
        // 生成租户编码
        req.setCode(this.generateCode());
        // 新增信息
        Long id = super.create(req);
        // 初始化租户数据
        req.setId(id);
        tenantDataApiMap.forEach((key, value) -> value.init(BeanUtil.copyProperties(req, TenantDTO.class)));
        return id;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(TenantReq req, Long id) {
        TenantDO tenant = super.getById(id);
        Long oldPackageId = tenant.getPackageId();
        super.update(req, id);
        // 变更套餐后，按新套餐菜单同步该租户权限
        if (!oldPackageId.equals(req.getPackageId())) {
            List<Long> menuIds = packageMenuService.listMenuIdsByPackageId(req.getPackageId());
            this.syncTenantMenus(id, menuIds);
            RedisUtils.deleteByPattern(CacheConstants.ROLE_MENU_KEY_PREFIX + StringConstants.ASTERISK);
        }
    }

    @Override
    public void beforeUpdate(TenantReq req, Long id) {
        this.checkNameRepeat(req.getName(), id);
        this.checkDomainRepeat(req.getDomain(), id);
        TenantDO tenant = super.getById(id);
        this.checkExpireTime(req.getExpireTime(), tenant.getExpireTime());
        // 变更套餐
        if (!tenant.getPackageId().equals(req.getPackageId())) {
            packageService.checkStatus(req.getPackageId());
        }
    }

    @Override
    public void afterUpdate(TenantReq req, TenantDO entity) {
        RedisUtils.deleteByPattern(TenantCacheConstants.TENANT_KEY_PREFIX + StringConstants.ASTERISK);
    }

    @Override
    public void beforeDelete(List<Long> ids) {
        // 在租户中执行数据清除
        for (Long id : ids) {
            TenantUtils.execute(id, () -> tenantDataApiMap.forEach((key, value) -> value.clear()));
        }
    }

    @Override
    public void afterDelete(List<Long> ids) {
        RedisUtils.deleteByPattern(TenantCacheConstants.TENANT_KEY_PREFIX + StringConstants.ASTERISK);
    }

    @Override
    @Cached(name = TenantCacheConstants.TENANT_KEY_PREFIX, key = "#domain")
    public Long getIdByDomain(String domain) {
        return baseMapper.lambdaQuery()
            .select(TenantDO::getId)
            .eq(TenantDO::getDomain, domain)
            .oneOpt()
            .map(TenantDO::getId)
            .orElse(null);
    }

    @Override
    @Cached(name = TenantCacheConstants.TENANT_KEY_PREFIX, key = "#code")
    public Long getIdByCode(String code) {
        return baseMapper.lambdaQuery()
            .select(TenantDO::getId)
            .eq(TenantDO::getCode, code)
            .oneOpt()
            .map(TenantDO::getId)
            .orElse(null);
    }

    @Override
    public void checkStatus(Long id) {
        // 默认租户
        if (tenantExtensionProperties.getDefaultTenantId().equals(id)) {
            return;
        }
        TenantDO tenant = this.getById(id);
        CheckUtils.throwIfEqual(DisEnableStatusEnum.DISABLE, tenant.getStatus(), "租户已被禁用");
        CheckUtils.throwIf(tenant.getExpireTime() != null && tenant.getExpireTime()
            .isBefore(LocalDateTime.now()), "租户已过期");
        // 检查套餐
        packageService.checkStatus(tenant.getPackageId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTenantMenu(List<Long> newMenuIds, Long packageId) {
        List<Long> tenantIdList = this.listIdByPackageId(packageId);
        if (CollUtil.isEmpty(tenantIdList)) {
            return;
        }
        tenantIdList.forEach(tenantId -> this.syncTenantMenus(tenantId, newMenuIds));
        // 删除缓存
        RedisUtils.deleteByPattern(CacheConstants.ROLE_MENU_KEY_PREFIX + StringConstants.ASTERISK);
    }

    /**
     * 同步单个租户的菜单权限（清理不在套餐内的菜单，并为租户管理员补齐套餐菜单）
     *
     * @param tenantId   租户 ID
     * @param newMenuIds 套餐菜单 ID 列表
     */
    private void syncTenantMenus(Long tenantId, List<Long> newMenuIds) {
        TenantUtils.execute(tenantId, () -> {
            // 所有角色：删除不在新套餐内的菜单
            roleMenuApi.deleteByNotInMenuIds(newMenuIds);
            Set<Long> roleIdSet = roleMenuApi.listRoleIdByNotInMenuIds(newMenuIds);
            roleIdSet.forEach(roleApi::updateUserContext);
            // 租户管理员：补齐套餐菜单
            Long roleId = roleApi.getIdByCode(RoleCodeEnum.TENANT_ADMIN.getCode());
            roleMenuApi.add(newMenuIds, roleId);
            roleApi.updateUserContext(roleId);
        });
    }

    /**
     * 校验过期时间：新增必须为未来；修改允许保留原已过期值，不允许改为新的过去时间
     *
     * @param expireTime    请求过期时间
     * @param oldExpireTime 原过期时间（新增时为 null）
     */
    private void checkExpireTime(LocalDateTime expireTime, LocalDateTime oldExpireTime) {
        if (expireTime == null) {
            return;
        }
        if (expireTime.isAfter(LocalDateTime.now())) {
            return;
        }
        CheckUtils.throwIf(oldExpireTime == null || !isSameDateTime(expireTime, oldExpireTime), "过期时间必须是未来时间");
    }

    private boolean isSameDateTime(LocalDateTime left, LocalDateTime right) {
        return left.truncatedTo(ChronoUnit.SECONDS).equals(right.truncatedTo(ChronoUnit.SECONDS));
    }

    @Override
    public Long countByPackageIds(List<Long> packageIds) {
        return baseMapper.lambdaQuery().in(TenantDO::getPackageId, packageIds).count();
    }

    /**
     * 检查名称是否重复
     *
     * @param name 名称
     * @param id   ID
     */
    private void checkNameRepeat(String name, Long id) {
        CheckUtils.throwIf(baseMapper.lambdaQuery()
            .eq(TenantDO::getName, name)
            .ne(id != null, TenantDO::getId, id)
            .exists(), "名称为 [{}] 的租户已存在", name);
    }

    /**
     * 检查域名是否重复
     *
     * @param domain 域名
     * @param id     ID
     */
    private void checkDomainRepeat(String domain, Long id) {
        CheckUtils.throwIf(baseMapper.lambdaQuery()
            .eq(TenantDO::getDomain, domain)
            .ne(id != null, TenantDO::getId, id)
            .exists(), "域名为 [{}] 的租户已存在", domain);
    }

    /**
     * 生成租户编码
     *
     * @return 租户编码
     */
    private String generateCode() {
        String code;
        do {
            code = idGeneratorProvider.getRequired(TenantConstants.CODE_GENERATOR_KEY).generateAsString();
        } while (baseMapper.lambdaQuery().eq(TenantDO::getCode, code).exists());
        return code;
    }

    /**
     * 根据套餐 ID 查询租户 ID 列表
     *
     * @param id 套餐 ID
     * @return 租户 ID 列表
     */
    private List<Long> listIdByPackageId(Long id) {
        return baseMapper.lambdaQuery()
            .select(TenantDO::getId)
            .eq(TenantDO::getPackageId, id)
            .list()
            .stream()
            .map(TenantDO::getId)
            .toList();
    }
}