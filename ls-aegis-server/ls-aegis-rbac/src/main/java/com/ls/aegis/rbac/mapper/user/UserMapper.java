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

package com.ls.aegis.rbac.mapper.user;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import com.ls.aegis.mybatis.base.mapper.DataPermissionMapper;
import com.ls.aegis.rbac.model.entity.user.UserDO;
import com.ls.aegis.rbac.model.resp.user.UserDetailResp;
import top.continew.starter.extension.datapermission.annotation.DataPermission;
import top.continew.starter.encrypt.field.annotation.FieldEncrypt;

import java.util.List;

/**
 * 用户 Mapper
 *
 * @author Charles7c
 * @since 2022/12/22 21:47
 */
@Mapper
public interface UserMapper extends DataPermissionMapper<UserDO> {

    /**
     * 分页查询列表
     *
     * @param page         分页条件
     * @param queryWrapper 查询条件
     * @return 分页列表信息
     */
    @DataPermission(tableAlias = "t1")
    IPage<UserDetailResp> selectUserPage(@Param("page") IPage<UserDO> page,
                                         @Param(Constants.WRAPPER) QueryWrapper<UserDO> queryWrapper);

    /**
     * 查询列表
     *
     * @param queryWrapper 查询条件
     * @return 列表信息
     */
    @DataPermission(tableAlias = "t1")
    List<UserDetailResp> selectUserList(@Param(Constants.WRAPPER) QueryWrapper<UserDO> queryWrapper);

    /**
     * 根据用户名查询
     *
     * @param username 用户名
     * @return 用户信息
     */
    @Select("SELECT * FROM sys_user WHERE username = #{username} AND deleted = 0")
    UserDO selectByUsername(@Param("username") String username);

    /**
     * 根据手机号查询
     *
     * @param phone 手机号
     * @return 用户信息
     */
    @Select("SELECT * FROM sys_user WHERE phone = #{phone} AND deleted = 0")
    UserDO selectByPhone(@FieldEncrypt(encryptor = com.ls.aegis.crypto.encryptor.Sm4Encryptor.class) @Param("phone") String phone);

    /**
     * 根据邮箱查询
     *
     * @param email 邮箱
     * @return 用户信息
     */
    @Select("SELECT * FROM sys_user WHERE email = #{email} AND deleted = 0")
    UserDO selectByEmail(@FieldEncrypt(encryptor = com.ls.aegis.crypto.encryptor.Sm4Encryptor.class) @Param("email") String email);

    /**
     * 根据邮箱密文查询（入参已是密文，不再二次加密）
     *
     * @param emailCipher 邮箱密文
     * @return 用户信息
     */
    @Select("SELECT * FROM sys_user WHERE email = #{emailCipher} AND deleted = 0")
    UserDO selectByEmailCipher(@Param("emailCipher") String emailCipher);

    /**
     * 根据手机号密文查询（入参已是密文，不再二次加密）
     *
     * @param phoneCipher 手机号密文
     * @return 用户信息
     */
    @Select("SELECT * FROM sys_user WHERE phone = #{phoneCipher} AND deleted = 0")
    UserDO selectByPhoneCipher(@Param("phoneCipher") String phoneCipher);

    /**
     * 根据 ID 查询昵称
     *
     * @param id ID
     * @return 昵称
     */
    @Select("SELECT nickname FROM sys_user WHERE id = #{id} AND deleted = 0")
    String selectNicknameById(@Param("id") Long id);
}
