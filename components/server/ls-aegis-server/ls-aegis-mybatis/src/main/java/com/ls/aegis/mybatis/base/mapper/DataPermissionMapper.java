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

package com.ls.aegis.mybatis.base.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import org.apache.ibatis.annotations.Param;
import top.continew.starter.data.mapper.BaseMapper;
import top.continew.starter.extension.datapermission.annotation.DataPermission;

import java.io.Serializable;
import java.util.List;

/**
 * 数据权限 Mapper 基类
 *
 * @param <T> 实体类
 * @author Charles7c
 * @since 2023/9/3 21:50
 */
public interface DataPermissionMapper<T> extends BaseMapper<T> {

    /**
     * 根据 entity 条件，查询全部记录
     *
     * @param queryWrapper 实体对象封装操作类（可以为 null）
     * @return 全部记录
     */
    @DataPermission
    @Override
    List<T> selectList(@Param(Constants.WRAPPER) Wrapper<T> queryWrapper);

    /**
     * 根据 entity 条件，查询全部记录（并翻页）
     *
     * @param page         分页查询条件
     * @param queryWrapper 实体对象封装操作类（可以为 null）
     * @return 全部记录（并翻页）
     */
    @DataPermission
    @Override
    List<T> selectList(IPage<T> page, @Param(Constants.WRAPPER) Wrapper<T> queryWrapper);

    /**
     * 根据 ID 删除
     *
     * @param id id
     * @return 删除个数
     */
    @DataPermission
    @Override
    default int deleteById(@Param("id") Serializable id) {
        // MyBatis-Plus 会为带逻辑删除/自动填充字段的实体构造主键对象，
        // 再调用实体重载。不能把此默认方法改成抽象 SQL 方法，否则参数
        // 仅有 id/param1，逻辑删除需要的 updateUser/updateTime 无法绑定。
        return BaseMapper.super.deleteById(id);
    }

    /** 实体删除同样保留数据权限；不加 @Param，沿用 MP 的实体字段绑定契约。 */
    @DataPermission
    @Override
    int deleteById(T entity);
}
