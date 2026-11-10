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

package com.ls.aegis.boot.job;

import java.util.List;
import java.util.function.BooleanSupplier;

import cn.hutool.core.util.StrUtil;
import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import com.aizuda.snailjob.common.log.SnailJobLog;
import com.baomidou.mybatisplus.core.plugins.IgnoreStrategy;
import com.baomidou.mybatisplus.core.plugins.InterceptorIgnoreHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ls.aegis.biz.file.mapper.StorageMapper;
import com.ls.aegis.biz.file.model.entity.StorageDO;
import com.ls.aegis.biz.notice.mapper.MessageLogMapper;
import com.ls.aegis.biz.notice.mapper.MessageMapper;
import com.ls.aegis.biz.notice.mapper.NoticeLogMapper;
import com.ls.aegis.biz.notice.mapper.NoticeMapper;
import com.ls.aegis.biz.notice.model.entity.MessageDO;
import com.ls.aegis.biz.notice.model.entity.MessageLogDO;
import com.ls.aegis.biz.notice.model.entity.NoticeDO;
import com.ls.aegis.biz.notice.model.entity.NoticeLogDO;
import com.ls.aegis.rbac.mapper.ClientMapper;
import com.ls.aegis.rbac.mapper.DeptMapper;
import com.ls.aegis.rbac.mapper.DictItemMapper;
import com.ls.aegis.rbac.mapper.DictMapper;
import com.ls.aegis.rbac.mapper.MenuMapper;
import com.ls.aegis.rbac.mapper.RoleDeptMapper;
import com.ls.aegis.rbac.mapper.RoleMapper;
import com.ls.aegis.rbac.mapper.RoleMenuMapper;
import com.ls.aegis.rbac.mapper.UserRoleMapper;
import com.ls.aegis.rbac.mapper.user.UserMapper;
import com.ls.aegis.rbac.mapper.user.UserSocialMapper;
import com.ls.aegis.rbac.model.entity.ClientDO;
import com.ls.aegis.rbac.model.entity.DeptDO;
import com.ls.aegis.rbac.model.entity.DictDO;
import com.ls.aegis.rbac.model.entity.DictItemDO;
import com.ls.aegis.rbac.model.entity.MenuDO;
import com.ls.aegis.rbac.model.entity.RoleDO;
import com.ls.aegis.rbac.model.entity.RoleDeptDO;
import com.ls.aegis.rbac.model.entity.RoleMenuDO;
import com.ls.aegis.rbac.model.entity.UserRoleDO;
import com.ls.aegis.rbac.model.entity.user.UserDO;
import com.ls.aegis.rbac.model.entity.user.UserSocialDO;
import com.ls.aegis.redis.constant.CacheConstants;
import top.continew.starter.cache.redisson.util.RedisUtils;
import top.continew.starter.core.constant.StringConstants;
import top.continew.starter.extension.tenant.annotation.TenantIgnore;

/**
 * 演示环境任务（仅 {@code demo} Profile 加载，生产默认不注册）。
 * <p>多租户关闭后不再依赖 ls-aegis-biz-tenant Mapper；租户/套餐清理略过。</p>
 *
 * @author Charles7c
 * @since 2024/8/4 15:30
 */
@Profile("demo")
@Component
@RequiredArgsConstructor
public class DemoEnvironmentJob {

    private final DictItemMapper dictItemMapper;
    private final DictMapper dictMapper;
    private final StorageMapper storageMapper;
    private final NoticeMapper noticeMapper;
    private final NoticeLogMapper noticeLogMapper;
    private final MessageMapper messageMapper;
    private final MessageLogMapper messageLogMapper;
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final UserSocialMapper userSocialMapper;
    private final RoleMapper roleMapper;
    private final RoleDeptMapper roleDeptMapper;
    private final RoleMenuMapper roleMenuMapper;
    private final MenuMapper menuMapper;
    private final DeptMapper deptMapper;
    private final ClientMapper clientsMapper;

    private static final Long DELETE_FLAG = 10000L;
    private static final Long MESSAGE_FLAG = 0L;
    /** 保留：超级管理员 + 测试用户 */
    private static final List<Long> USER_FLAG = List.of(1L, 547889293968801822L);
    /** 保留：超级管理员 / 系统管理员 / 普通角色 */
    private static final List<Long> ROLE_FLAG = List.of(1L, 2L, 3L);
    /** 保留部门 id<=2：总公司、总经办；大于此值的演示部门会被清理 */
    private static final Long DEPT_FLAG = 2L;

    /**
     * 重置演示环境数据
     */
    @TenantIgnore
    @JobExecutor(name = "ResetEnvironmentData")
    @Transactional(rollbackFor = Exception.class)
    public void resetEnvironmentData() {
        try {
            SnailJobLog.REMOTE.info("定时任务 [重置演示环境数据] 开始执行。");
            // 检测待清理数据
            SnailJobLog.REMOTE.info("开始检测演示环境待清理数据项，请稍候...");
            Long dictItemCount = dictItemMapper.lambdaQuery().gt(DictItemDO::getId, DELETE_FLAG).count();
            this.log(dictItemCount, "字典项");
            Long dictCount = dictMapper.lambdaQuery().gt(DictDO::getId, DELETE_FLAG).count();
            this.log(dictCount, "字典");
            Long storageCount = storageMapper.lambdaQuery().gt(StorageDO::getId, DELETE_FLAG).count();
            this.log(storageCount, "存储");
            Long noticeCount = noticeMapper.lambdaQuery().gt(NoticeDO::getId, DELETE_FLAG).count();
            this.log(noticeCount, "公告");
            Long messageCount = messageMapper.lambdaQuery().count();
            this.log(messageCount, "通知");
            Long userCount = userMapper.lambdaQuery().notIn(UserDO::getId, USER_FLAG).count();
            this.log(userCount, "用户");
            Long roleCount = roleMapper.lambdaQuery().notIn(RoleDO::getId, ROLE_FLAG).count();
            this.log(roleCount, "角色");
            Long menuCount = menuMapper.lambdaQuery().gt(MenuDO::getId, DELETE_FLAG).count();
            this.log(menuCount, "菜单");
            Long deptCount = deptMapper.lambdaQuery().gt(DeptDO::getId, DEPT_FLAG).count();
            this.log(deptCount, "部门");
            Long clientCount = clientsMapper.lambdaQuery().gt(ClientDO::getId, DELETE_FLAG).count();
            this.log(clientCount, "客户端");
            InterceptorIgnoreHelper.handle(IgnoreStrategy.builder().blockAttack(true).build());
            SnailJobLog.REMOTE.info("演示环境待清理数据项检测完成，开始执行清理。");
            // 清理关联数据
            noticeLogMapper.lambdaUpdate().gt(NoticeLogDO::getNoticeId, DELETE_FLAG).remove();
            messageLogMapper.lambdaUpdate().gt(MessageLogDO::getMessageId, MESSAGE_FLAG).remove();
            userRoleMapper.lambdaUpdate().notIn(UserRoleDO::getRoleId, ROLE_FLAG).remove();
            userRoleMapper.lambdaUpdate().notIn(UserRoleDO::getUserId, USER_FLAG).remove();
            roleDeptMapper.lambdaUpdate().notIn(RoleDeptDO::getRoleId, ROLE_FLAG).remove();
            roleMenuMapper.lambdaUpdate().notIn(RoleMenuDO::getRoleId, ROLE_FLAG).remove();
            userSocialMapper.lambdaUpdate().notIn(UserSocialDO::getUserId, USER_FLAG).remove();
            // 清理具体数据
            this.clean(dictItemCount, "字典项", null, () -> dictItemMapper.lambdaUpdate()
                .gt(DictItemDO::getId, DELETE_FLAG)
                .remove());
            this.clean(dictCount, "字典", CacheConstants.DICT_KEY_PREFIX, () -> dictMapper.lambdaUpdate()
                .gt(DictDO::getId, DELETE_FLAG)
                .remove());
            this.clean(storageCount, "存储", null, () -> storageMapper.lambdaUpdate()
                .gt(StorageDO::getId, DELETE_FLAG)
                .remove());
            this.clean(noticeCount, "公告", null, () -> noticeMapper.lambdaUpdate()
                .gt(NoticeDO::getId, DELETE_FLAG)
                .remove());
            this.clean(messageCount, "通知", null, () -> messageMapper.lambdaUpdate()
                .gt(MessageDO::getId, MESSAGE_FLAG)
                .remove());
            this.clean(userCount, "用户", null, () -> userMapper.lambdaUpdate().notIn(UserDO::getId, USER_FLAG).remove());
            this.clean(roleCount, "角色", null, () -> roleMapper.lambdaUpdate().notIn(RoleDO::getId, ROLE_FLAG).remove());
            this.clean(menuCount, "菜单", CacheConstants.ROLE_MENU_KEY_PREFIX, () -> menuMapper.lambdaUpdate()
                .gt(MenuDO::getId, DELETE_FLAG)
                .remove());
            this.clean(deptCount, "部门", null, () -> deptMapper.lambdaUpdate().gt(DeptDO::getId, DEPT_FLAG).remove());
            this.clean(clientCount, "客户端", null, () -> clientsMapper.lambdaUpdate()
                .gt(ClientDO::getId, DELETE_FLAG)
                .remove());
            SnailJobLog.REMOTE.info("演示环境数据已清理完成。");
            SnailJobLog.REMOTE.info("定时任务 [重置演示环境数据] 执行结束。");
        } finally {
            InterceptorIgnoreHelper.clearIgnoreStrategy();
        }
    }

    /**
     * 输出日志
     *
     * @param count    待清理数据项数量
     * @param resource 资源名称
     */
    private void log(Long count, String resource) {
        if (count > 0) {
            SnailJobLog.REMOTE.info("检测到 [{}] 待清理数据项：{}条", resource, count);
        }
    }

    /**
     * 清理数据
     *
     * @param count    待清理数据项数量
     * @param resource 资源名称
     * @param cacheKey 缓存键
     * @param supplier 清理数据项函数
     */
    private void clean(Long count, String resource, String cacheKey, BooleanSupplier supplier) {
        if (count > 0 && supplier.getAsBoolean()) {
            SnailJobLog.REMOTE.info("[{}] 数据项清理完成。", resource);
            if (StrUtil.isNotBlank(cacheKey)) {
                RedisUtils.deleteByPattern(cacheKey + StringConstants.ASTERISK);
                SnailJobLog.REMOTE.info("[{}] 数据项缓存清理完成。", resource);
            }
        }
    }
}
