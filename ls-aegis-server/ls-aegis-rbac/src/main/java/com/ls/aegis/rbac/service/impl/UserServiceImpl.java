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

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.EnumUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.validation.ValidationUtil;
import cn.hutool.http.ContentType;
import cn.hutool.json.JSONUtil;
import cn.idev.excel.EasyExcel;
import com.alicp.jetcache.anno.CacheInvalidate;
import com.alicp.jetcache.anno.CacheUpdate;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.ahoo.cosid.IdGenerator;
import me.ahoo.cosid.provider.DefaultIdGeneratorProvider;
import net.dreamlu.mica.core.result.R;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.ls.aegis.rbac.auth.service.OnlineUserService;
import com.ls.aegis.mybatis.base.service.BaseServiceImpl;
import com.ls.aegis.redis.constant.CacheConstants;
import com.ls.aegis.common.context.UserContext;
import com.ls.aegis.common.context.UserContextHolder;
import com.ls.aegis.common.enums.DisEnableStatusEnum;
import com.ls.aegis.common.enums.GenderEnum;
import com.ls.aegis.common.constant.UserConstants;
import com.ls.aegis.common.util.PasswordUtils;
import com.ls.aegis.common.util.SecureUtils;
import com.ls.aegis.rbac.enums.OptionCategoryEnum;
import com.ls.aegis.rbac.mapper.user.UserMapper;
import com.ls.aegis.rbac.model.entity.DeptDO;
import com.ls.aegis.rbac.model.entity.RoleDO;
import com.ls.aegis.rbac.model.entity.UserRoleDO;
import com.ls.aegis.rbac.model.entity.user.UserDO;
import com.ls.aegis.rbac.model.query.UserQuery;
import com.ls.aegis.rbac.model.req.user.UserBasicInfoUpdateReq;
import com.ls.aegis.rbac.model.req.user.UserImportReq;
import com.ls.aegis.rbac.model.req.user.UserImportRowReq;
import com.ls.aegis.rbac.model.req.user.UserPasswordResetReq;
import com.ls.aegis.rbac.model.req.user.UserReq;
import com.ls.aegis.rbac.model.req.user.UserRoleUpdateReq;
import com.ls.aegis.rbac.model.resp.user.UserCreateResp;
import com.ls.aegis.rbac.model.resp.user.UserDetailResp;
import com.ls.aegis.rbac.model.resp.user.UserImportParseResp;
import com.ls.aegis.rbac.model.resp.user.UserImportResp;
import com.ls.aegis.rbac.model.resp.user.UserResp;
import com.ls.aegis.biz.file.service.FileService;
import com.ls.aegis.rbac.service.DeptService;
import com.ls.aegis.rbac.service.OptionService;
import com.ls.aegis.rbac.service.RoleService;
import com.ls.aegis.rbac.service.UserPasswordHistoryService;
import com.ls.aegis.rbac.service.UserRoleService;
import com.ls.aegis.rbac.service.UserService;
import com.ls.aegis.rbac.service.UserSocialService;
import top.continew.starter.cache.redisson.util.RedisUtils;
import top.continew.starter.core.constant.StringConstants;
import top.continew.starter.core.exception.BusinessException;
import top.continew.starter.core.util.validation.ValidationUtils;
import top.continew.starter.core.util.CollUtils;
import top.continew.starter.core.util.FileUploadUtils;
import top.continew.starter.core.util.validation.CheckUtils;
import top.continew.starter.encrypt.field.annotation.FieldEncrypt;
import top.continew.starter.encrypt.field.util.EncryptHelper;
import top.continew.starter.extension.crud.model.query.PageQuery;
import top.continew.starter.extension.crud.model.query.SortQuery;
import top.continew.starter.extension.crud.model.resp.PageResp;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.ls.aegis.rbac.enums.ImportPolicyEnum.EXIT;
import static com.ls.aegis.rbac.enums.ImportPolicyEnum.SKIP;
import static com.ls.aegis.rbac.enums.ImportPolicyEnum.UPDATE;
import static com.ls.aegis.rbac.enums.PasswordPolicyEnum.PASSWORD_ALLOW_CONTAIN_USERNAME;
import static com.ls.aegis.rbac.enums.PasswordPolicyEnum.PASSWORD_MIN_LENGTH;
import static com.ls.aegis.rbac.enums.PasswordPolicyEnum.PASSWORD_REPETITION_TIMES;
import static com.ls.aegis.rbac.enums.PasswordPolicyEnum.PASSWORD_REQUIRE_SYMBOLS;

/**
 * 用户业务实现
 *
 * @author Charles7c
 * @since 2022/12/21 21:49
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends BaseServiceImpl<UserMapper, UserDO, UserResp, UserDetailResp, UserQuery, UserReq> implements UserService {

    private final PasswordEncoder passwordEncoder;
    private final UserPasswordHistoryService userPasswordHistoryService;
    private final UserSocialService userSocialService;
    private final com.ls.aegis.rbac.service.UserPreferenceService userPreferenceService;
    private final UserRoleService userRoleService;
    private final OptionService optionService;
    private final RoleService roleService;
    private final OnlineUserService onlineUserService;
    private final FileService fileService;
    private final FileStorageService fileStorageService;

    @Resource
    private DeptService deptService;
    @Value("${avatar.support-suffix}")
    private String[] avatarSupportSuffix;
    @Value("${avatar.path}")
    private String avatarPath;

    /** 创建过程中暂存自动生成的明文初始密码（仅当前线程、创建响应一次） */
    private static final ThreadLocal<String> GENERATED_INITIAL_PASSWORD = new ThreadLocal<>();

    @Override
    public PageResp<UserResp> page(UserQuery query, PageQuery pageQuery) {
        QueryWrapper<UserDO> queryWrapper = this.buildQueryWrapper(query);
        super.sort(queryWrapper, pageQuery);
        IPage<UserDetailResp> page = baseMapper.selectUserPage(new Page<>(pageQuery.getPage(), pageQuery
            .getSize()), queryWrapper);
        PageResp<UserResp> pageResp = PageResp.build(page, super.getListClass());
        pageResp.getList().forEach(this::fill);
        return pageResp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserCreateResp createUser(UserReq req) {
        try {
            Long id = this.create(req);
            String initialPassword = GENERATED_INITIAL_PASSWORD.get();
            return new UserCreateResp(id, initialPassword, StrUtil.isNotBlank(initialPassword));
        } finally {
            GENERATED_INITIAL_PASSWORD.remove();
        }
    }

    @Override
    public void beforeCreate(UserReq req) {
        if (StrUtil.isBlank(req.getPassword())) {
            String generated = PasswordUtils.generateRandomPassword();
            req.setPassword(generated);
            GENERATED_INITIAL_PASSWORD.set(generated);
        } else {
            String password = SecureUtils.decryptPasswordByRsaPrivateKey(req.getPassword(), "密码解密失败", true);
            req.setPassword(password);
        }
        this.checkUsernameRepeat(req.getUsername(), null);
        this.checkEmailRepeat(req.getEmail(), null, "邮箱为 [{}] 的用户已存在");
        this.checkPhoneRepeat(req.getPhone(), null, "手机号为 [{}] 的用户已存在");
    }

    @Override
    public void afterCreate(UserReq req, UserDO user) {
        Long userId = user.getId();
        // 写入初始口令哨兵时间，强制首次登录改密
        baseMapper.lambdaUpdate()
            .set(UserDO::getPwdResetTime, UserConstants.INITIAL_PASSWORD_RESET_TIME)
            .eq(UserDO::getId, userId)
            .update();
        // 保存用户和角色关联
        userRoleService.assignRolesToUser(req.getRoleIds(), userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheUpdate(key = "#id", value = "#req.nickname", name = CacheConstants.USER_KEY_PREFIX)
    public void update(UserReq req, Long id) {
        this.checkUsernameRepeat(req.getUsername(), id);
        this.checkEmailRepeat(req.getEmail(), id, "邮箱为 [{}] 的用户已存在");
        this.checkPhoneRepeat(req.getPhone(), id, "手机号为 [{}] 的用户已存在");
        DisEnableStatusEnum newStatus = req.getStatus();
        CheckUtils.throwIf(DisEnableStatusEnum.DISABLE.equals(newStatus) && ObjectUtil.equal(id, UserContextHolder
            .getUserId()), "不允许禁用当前用户");
        UserDO oldUser = this.getById(id);
        List<Long> roleIds = req.getRoleIds();
        if (Boolean.TRUE.equals(oldUser.getIsSystem())) {
            CheckUtils.throwIfEqual(DisEnableStatusEnum.DISABLE, newStatus, "[{}] 是系统内置用户，不允许禁用", oldUser
                .getNickname());
            // 系统内置用户不允许变更角色：强制沿用原角色，避免前端类型差异导致误判拦截资料修改
            roleIds = userRoleService.listRoleIdByUserId(id);
            req.setRoleIds(roleIds);
        }
        // 更新信息
        UserDO newUser = BeanUtil.toBean(req, UserDO.class);
        newUser.setId(id);
        baseMapper.updateById(newUser);
        // 系统内置用户不变更角色；其他用户才同步角色关联
        boolean isSaveUserRoleSuccess = false;
        if (!Boolean.TRUE.equals(oldUser.getIsSystem())) {
            isSaveUserRoleSuccess = userRoleService.assignRolesToUser(roleIds, id);
        }
        // 如果禁用用户，则踢出在线用户
        if (DisEnableStatusEnum.DISABLE.equals(newStatus)) {
            onlineUserService.kickOut(id);
            return;
        }
        // 如果角色有变更，则更新在线用户权限信息
        if (isSaveUserRoleSuccess) {
            this.updateContext(id);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheInvalidate(key = "#ids", name = CacheConstants.USER_KEY_PREFIX, multi = true)
    public void delete(List<Long> ids) {
        CheckUtils.throwIf(CollUtil.contains(ids, UserContextHolder.getUserId()), "不允许删除当前用户");
        List<UserDO> list = baseMapper.lambdaQuery()
            .select(UserDO::getId, UserDO::getNickname, UserDO::getIsSystem)
            .in(UserDO::getId, ids)
            .list();
        List<Long> idList = CollUtils.mapToList(list, UserDO::getId);
        Collection<Long> subtractIds = CollUtil.subtract(ids, idList);
        CheckUtils.throwIfNotEmpty(subtractIds, "所选用户 [{}] 不存在", CollUtil.join(subtractIds, StringConstants.COMMA));
        Optional<UserDO> isSystemData = list.stream().filter(UserDO::getIsSystem).findFirst();
        CheckUtils.throwIf(isSystemData::isPresent, "所选用户 [{}] 是系统内置用户，不允许删除", isSystemData.orElseGet(UserDO::new)
            .getNickname());
        // 删除用户和角色关联
        userRoleService.deleteByUserIds(ids);
        // 删除历史密码
        userPasswordHistoryService.deleteByUserIds(ids);
        // 删除用户绑定的第三方账号信息
        userSocialService.deleteByUserIds(ids);
          userPreferenceService.deleteByUserIds(ids);
        // 删除用户
        super.delete(ids);
        // 踢出在线用户
        ids.forEach(onlineUserService::kickOut);
    }

    @Override
    public void downloadImportTemplate(HttpServletResponse response) throws IOException {
        try {
            FileUploadUtils.download(response, ResourceUtil.getStream("templates/import/user.xlsx"), "用户导入模板.xlsx");
        } catch (Exception e) {
            log.error("下载用户导入模板失败：{}", e.getMessage(), e);
            response.setCharacterEncoding(CharsetUtil.UTF_8);
            response.setContentType(ContentType.JSON.toString());
            response.getWriter().write(JSONUtil.toJsonStr(R.fail("下载用户导入模板失败")));
        }
    }

    @Override
    public UserImportParseResp parseImport(MultipartFile file) {
        UserImportParseResp userImportResp = new UserImportParseResp();
        List<UserImportRowReq> importRowList;
        // 读取表格数据
        try {
            importRowList = EasyExcel.read(file.getInputStream())
                .head(UserImportRowReq.class)
                .sheet()
                .headRowNumber(1)
                .doReadSync();
        } catch (Exception e) {
            log.error("用户导入数据文件解析异常：{}", e.getMessage(), e);
            throw new BusinessException("数据文件解析异常");
        }
        // 总计行数
        userImportResp.setTotalRows(importRowList.size());
        CheckUtils.throwIfEmpty(importRowList, "数据文件格式不正确");
        // 有效行数：过滤无效数据
        List<UserImportRowReq> validRowList = this.filterImportData(importRowList);
        userImportResp.setValidRows(validRowList.size());
        CheckUtils.throwIfEmpty(validRowList, "数据文件格式不正确");

        // 检测表格内数据是否合法
        Set<String> seenEmails = new HashSet<>();
        boolean hasDuplicateEmail = validRowList.stream()
            .map(UserImportRowReq::getEmail)
            .anyMatch(email -> email != null && !seenEmails.add(email));
        CheckUtils.throwIf(hasDuplicateEmail, "存在重复邮箱，请检测数据");
        Set<String> seenPhones = new HashSet<>();
        boolean hasDuplicatePhone = validRowList.stream()
            .map(UserImportRowReq::getPhone)
            .anyMatch(phone -> phone != null && !seenPhones.add(phone));
        CheckUtils.throwIf(hasDuplicatePhone, "存在重复手机，请检测数据");

        // 校验是否存在无效角色
        List<String> roleNames = validRowList.stream().map(UserImportRowReq::getRoleName).distinct().toList();
        int existRoleCount = roleService.countByNames(roleNames);
        CheckUtils.throwIf(existRoleCount < roleNames.size(), "存在无效角色，请检查数据");
        // 校验是否存在无效部门（支持多级部门解析）
        Set<String> deptNames = CollUtils.mapToSet(validRowList, UserImportRowReq::getDeptName);
        int existDeptCount = countValidMultiLevelDepts(deptNames);
        CheckUtils.throwIf(existDeptCount < deptNames.size(), "存在无效部门，请检查部门名称或部门层级是否正确");

        // 查询重复用户
        userImportResp
            .setDuplicateUserRows(countExistByField(validRowList, UserImportRowReq::getUsername, UserDO::getUsername));
        // 查询重复邮箱
        userImportResp.setDuplicateEmailRows(countExistByField(validRowList, row -> EncryptHelper.encrypt(row
            .getEmail()), UserDO::getEmail));
        // 查询重复手机
        userImportResp.setDuplicatePhoneRows(countExistByField(validRowList, row -> EncryptHelper.encrypt(row
            .getPhone()), UserDO::getPhone));

        // 设置导入会话并缓存数据，有效期10分钟
        String importKey = UUID.fastUUID().toString(true);
        RedisUtils.set(CacheConstants.DATA_IMPORT_KEY + importKey, JSONUtil.toJsonStr(validRowList), Duration
            .ofMinutes(10));
        userImportResp.setImportKey(importKey);
        return userImportResp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserImportResp importUser(UserImportReq req) {
        // 校验导入会话是否过期
        List<UserImportRowReq> importUserList;
        try {
            String data = RedisUtils.get(CacheConstants.DATA_IMPORT_KEY + req.getImportKey());
            importUserList = JSONUtil.toList(data, UserImportRowReq.class);
            CheckUtils.throwIf(CollUtil.isEmpty(importUserList), "导入已过期，请重新上传");
        } catch (Exception e) {
            log.error("导入异常:", e);
            throw new BusinessException("导入已过期，请重新上传");
        }
        // 已存在数据查询
        List<String> existEmails = listExistByField(importUserList, row -> EncryptHelper.encrypt(row
            .getEmail()), UserDO::getEmail);
        List<String> existPhones = listExistByField(importUserList, row -> EncryptHelper.encrypt(row
            .getPhone()), UserDO::getPhone);
        List<UserDO> existUserList = listByUsernames(CollUtils
            .mapToList(importUserList, UserImportRowReq::getUsername));
        List<String> existUsernames = CollUtils.mapToList(existUserList, UserDO::getUsername);
        CheckUtils
            .throwIf(isExitImportUser(req, importUserList, existUsernames, existEmails, existPhones), "数据不符合导入策略，已退出导入");

        // 基础数据准备
        Map<String, Long> userMap = existUserList.stream()
            .collect(Collectors.toMap(UserDO::getUsername, UserDO::getId));
        List<RoleDO> roleList = roleService.listByNames(importUserList.stream()
            .map(UserImportRowReq::getRoleName)
            .distinct()
            .toList());
        Map<String, Long> roleMap = roleList.stream().collect(Collectors.toMap(RoleDO::getName, RoleDO::getId));
        // 获取多级部门映射
        Map<String, Long> deptMap = buildMultiLevelDeptMapping(importUserList.stream()
            .map(UserImportRowReq::getDeptName)
            .distinct()
            .toList());

        // 批量操作数据库集合
        List<UserDO> insertList = new ArrayList<>();
        List<UserDO> updateList = new ArrayList<>();
        List<UserRoleDO> userRoleDOList = new ArrayList<>();
        // ID生成器
        IdGenerator idGenerator = DefaultIdGeneratorProvider.INSTANCE.getShare();
        for (UserImportRowReq row : importUserList) {
            if (isSkipUserImport(req, row, existUsernames, existPhones, existEmails)) {
                // 按规则跳过该行
                continue;
            }
            UserDO userDO = BeanUtil.toBeanIgnoreError(row, UserDO.class);
            userDO.setStatus(req.getDefaultStatus());
            // 导入新建同样强制首登改密；更新行保持后续自行改密时间逻辑（此处仍写哨兵，避免导入重置口令后不改密）
            userDO.setPwdResetTime(UserConstants.INITIAL_PASSWORD_RESET_TIME);
            userDO.setGender(EnumUtil.getBy(GenderEnum::getDescription, row.getGender(), GenderEnum.UNKNOWN));
            userDO.setDeptId(deptMap.get(row.getDeptName()));
            // 修改 or 新增
            if (UPDATE.validate(req.getDuplicateUser(), row.getUsername(), existUsernames)) {
                userDO.setId(userMap.get(row.getUsername()));
                updateList.add(userDO);
            } else {
                userDO.setId(idGenerator.generate());
                userDO.setIsSystem(false);
                insertList.add(userDO);
            }
            userRoleDOList.add(new UserRoleDO(userDO.getId(), roleMap.get(row.getRoleName())));
        }
        doImportUser(insertList, updateList, userRoleDOList);
        RedisUtils.delete(CacheConstants.DATA_IMPORT_KEY + req.getImportKey());
        return new UserImportResp(insertList.size() + updateList.size(), insertList.size(), updateList.size());
    }

    @Override
    public void resetPassword(UserPasswordResetReq req, Long id) {
        this.getById(id);
        baseMapper.lambdaUpdate()
            .set(UserDO::getPassword, req.getNewPassword())
            .set(UserDO::getPwdResetTime, UserConstants.INITIAL_PASSWORD_RESET_TIME)
            .eq(UserDO::getId, id)
            .update();
    }

    @Override
    public void updateRole(UserRoleUpdateReq updateReq, Long id) {
        this.getById(id);
        List<Long> roleIds = updateReq.getRoleIds();
        // 保存用户和角色关联
        userRoleService.assignRolesToUser(roleIds, id);
        // 更新用户上下文
        this.updateContext(id);
    }

    @Override
    public String updateAvatar(MultipartFile avatarFile, Long id) throws IOException {
        String avatarImageType = FileNameUtil.extName(avatarFile.getOriginalFilename());
        CheckUtils.throwIf(!StrUtil.equalsAnyIgnoreCase(avatarImageType, avatarSupportSuffix), "头像仅支持 {} 格式的图片", String
            .join(StringConstants.COMMA, avatarSupportSuffix));
        // 上传新头像
        UserDO user = super.getById(id);
        FileInfo fileInfo = fileService.upload(avatarFile, avatarPath);
        // 更新用户头像
        String newAvatar = fileInfo.getUrl();
        baseMapper.lambdaUpdate().set(UserDO::getAvatar, newAvatar).eq(UserDO::getId, id).update();
        // 删除原头像
        String oldAvatar = user.getAvatar();
        if (StrUtil.isNotBlank(oldAvatar)) {
            fileStorageService.delete(oldAvatar);
        }
        return newAvatar;
    }

    @Override
    @CacheUpdate(key = "#id", value = "#req.nickname", name = CacheConstants.USER_KEY_PREFIX)
    public void updateBasicInfo(UserBasicInfoUpdateReq req, Long id) {
        super.getById(id);
        baseMapper.lambdaUpdate()
            .set(UserDO::getNickname, req.getNickname())
            .set(UserDO::getGender, req.getGender())
            .eq(UserDO::getId, id)
            .update();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePassword(String oldPassword, String newPassword, Long id) {
        CheckUtils.throwIfEqual(newPassword, oldPassword, "新密码不能与当前密码相同");
        UserDO user = super.getById(id);
        String password = user.getPassword();
        if (StrUtil.isNotBlank(password)) {
            CheckUtils.throwIf(!passwordEncoder.matches(oldPassword, password), "当前密码不正确");
        }
        // 校验密码合法性
        int passwordRepetitionTimes = this.checkPassword(newPassword, user);
        // 更新密码和密码重置时间
        baseMapper.lambdaUpdate()
            .set(UserDO::getPassword, newPassword)
            .set(UserDO::getPwdResetTime, LocalDateTime.now())
            .eq(UserDO::getId, id)
            .update();
        // 保存历史密码
        userPasswordHistoryService.add(id, password, passwordRepetitionTimes);
        // 修改后登出
        StpUtil.logout();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void forgotPassword(String email, String captcha, String newPassword) {
        String captchaKey = CacheConstants.CAPTCHA_KEY_PREFIX + email;
        String captchaCached = RedisUtils.get(captchaKey);
        ValidationUtils.throwIfBlank(captchaCached, "验证码已失效");
        ValidationUtils.throwIfNotEqualIgnoreCase(captcha, captchaCached, "验证码不正确");
        RedisUtils.delete(captchaKey);

        UserDO user = this.getByEmail(email);
        ValidationUtils.throwIfNull(user, "此邮箱未绑定本系统账号");
        CheckUtils.throwIfEqual(DisEnableStatusEnum.DISABLE, user.getStatus(), "此账号已被禁用，如有疑问，请联系管理员");

        String oldPassword = user.getPassword();
        if (StrUtil.isNotBlank(oldPassword)) {
            CheckUtils.throwIf(passwordEncoder.matches(newPassword, oldPassword), "新密码不能与当前密码相同");
        }
        int passwordRepetitionTimes = this.checkPassword(newPassword, user);
        baseMapper.lambdaUpdate()
            .set(UserDO::getPassword, newPassword)
            .set(UserDO::getPwdResetTime, LocalDateTime.now())
            .eq(UserDO::getId, user.getId())
            .update();
        userPasswordHistoryService.add(user.getId(), oldPassword, passwordRepetitionTimes);
        // 重置后踢出该账号全部在线会话
        StpUtil.logout(user.getId());
    }

    @Override
    public void updatePhone(String newPhone, String oldPassword, Long id) {
        UserDO user = super.getById(id);
        CheckUtils.throwIf(!passwordEncoder.matches(oldPassword, user.getPassword()), "当前密码不正确");
        this.checkPhoneRepeat(newPhone, id, "手机号已绑定其他账号，请更换其他手机号");
        CheckUtils.throwIfEqual(newPhone, user.getPhone(), "新手机号不能与当前手机号相同");
        // 更新手机号
        baseMapper.lambdaUpdate().set(UserDO::getPhone, newPhone).eq(UserDO::getId, id).update();
    }

    @Override
    public void updateEmail(String newEmail, String oldPassword, Long id) {
        UserDO user = super.getById(id);
        CheckUtils.throwIf(!passwordEncoder.matches(oldPassword, user.getPassword()), "当前密码不正确");
        this.checkEmailRepeat(newEmail, id, "邮箱已绑定其他账号，请更换其他邮箱");
        CheckUtils.throwIfEqual(newEmail, user.getEmail(), "新邮箱不能与当前邮箱相同");
        // 更新邮箱
        baseMapper.lambdaUpdate().set(UserDO::getEmail, newEmail).eq(UserDO::getId, id).update();
    }

    @Override
    public UserDO getByUsername(String username) {
        return baseMapper.selectByUsername(username);
    }

    @Override
    public UserDO getByEmail(String email) {
        if (StrUtil.isBlank(email)) {
            return null;
        }
        UserDO user = baseMapper.selectByEmail(email);
        if (user != null) {
            return user;
        }
        // 兼容：显式按字段注解加密后用「密文直查」（避免条件再次被拦截器加密）
        try {
            FieldEncrypt fieldEncrypt = UserDO.class.getDeclaredField("email").getAnnotation(FieldEncrypt.class);
            String cipher = EncryptHelper.encrypt(email, fieldEncrypt);
            if (StrUtil.isNotBlank(cipher) && !StrUtil.equals(cipher, email)) {
                user = baseMapper.selectByEmailCipher(cipher);
                if (user != null) {
                    return user;
                }
            }
            // 兼容存量 AES 密文（EncryptHelper 默认算法）
            String aesCipher = EncryptHelper.encrypt(email);
            if (StrUtil.isNotBlank(aesCipher) && !StrUtil.equals(aesCipher, email) && !StrUtil.equals(aesCipher, cipher)) {
                return baseMapper.selectByEmailCipher(aesCipher);
            }
        } catch (Exception ignored) {
            // 视为未绑定
        }
        return null;
    }

    @Override
    public UserDO getByPhone(String phone) {
        if (StrUtil.isBlank(phone)) {
            return null;
        }
        UserDO user = baseMapper.selectByPhone(phone);
        if (user != null) {
            return user;
        }
        try {
            FieldEncrypt fieldEncrypt = UserDO.class.getDeclaredField("phone").getAnnotation(FieldEncrypt.class);
            String cipher = EncryptHelper.encrypt(phone, fieldEncrypt);
            if (StrUtil.isNotBlank(cipher) && !StrUtil.equals(cipher, phone)) {
                user = baseMapper.selectByPhoneCipher(cipher);
                if (user != null) {
                    return user;
                }
            }
            String aesCipher = EncryptHelper.encrypt(phone);
            if (StrUtil.isNotBlank(aesCipher) && !StrUtil.equals(aesCipher, phone) && !StrUtil.equals(aesCipher, cipher)) {
                return baseMapper.selectByPhoneCipher(aesCipher);
            }
        } catch (Exception ignored) {
            // ignore
        }
        return null;
    }

    @Override
    public Long countByDeptIds(List<Long> deptIds) {
        if (CollUtil.isEmpty(deptIds)) {
            return 0L;
        }
        return baseMapper.lambdaQuery().in(UserDO::getDeptId, deptIds).count();
    }

    @Override
    protected <E> List<E> list(UserQuery query, SortQuery sortQuery, Class<E> targetClass) {
        QueryWrapper<UserDO> queryWrapper = this.buildQueryWrapper(query);
        // 设置排序
        super.sort(queryWrapper, sortQuery);
        List<UserDetailResp> entityList = baseMapper.selectUserList(queryWrapper);
        if (this.getEntityClass() == targetClass) {
            return (List<E>)entityList;
        }
        return BeanUtil.copyToList(entityList, targetClass);
    }

    @Override
    protected QueryWrapper<UserDO> buildQueryWrapper(UserQuery query) {
        String description = query.getDescription();
        DisEnableStatusEnum status = query.getStatus();
        List<LocalDateTime> createTimeList = query.getCreateTime();
        Long deptId = query.getDeptId();
        List<Long> userIdList = query.getUserIds();
        // 获取排除用户 ID 列表
        List<Long> excludeUserIdList = null;
        if (query.getRoleId() != null) {
            excludeUserIdList = userRoleService.listUserIdByRoleId(query.getRoleId());
        }
        return new QueryWrapper<UserDO>().and(StrUtil.isNotBlank(description), q -> q.like("t1.username", description)
            .or()
            .like("t1.nickname", description)
            .or()
            .like("t1.description", description))
            .eq(status != null, "t1.status", status)
            .between(CollUtil.isNotEmpty(createTimeList), "t1.create_time", CollUtil.getFirst(createTimeList), CollUtil
                .getLast(createTimeList))
            .and(deptId != null, q -> {
                List<Long> deptIdList = CollUtils.mapToList(deptService.listChildren(deptId), DeptDO::getId);
                deptIdList.add(deptId);
                q.in("t1.dept_id", deptIdList);
            })
            .in(CollUtil.isNotEmpty(userIdList), "t1.id", userIdList)
            .notIn(CollUtil.isNotEmpty(excludeUserIdList), "t1.id", excludeUserIdList)
            .eq("t1.deleted", 0L);
    }

    /**
     * 导入用户
     *
     * @param insertList     新增用户
     * @param updateList     修改用户
     * @param userRoleDOList 用户角色关联
     */
    private void doImportUser(List<UserDO> insertList, List<UserDO> updateList, List<UserRoleDO> userRoleDOList) {
        if (CollUtil.isNotEmpty(insertList)) {
            baseMapper.insert(insertList);
        }
        if (CollUtil.isNotEmpty(updateList)) {
            baseMapper.updateBatchById(updateList);
            userRoleService.deleteByUserIds(CollUtils.mapToList(updateList, UserDO::getId));
        }
        if (CollUtil.isNotEmpty(userRoleDOList)) {
            userRoleService.saveBatch(userRoleDOList);
        }
    }

    /**
     * 判断是否跳过导入
     *
     * @param req            导入参数
     * @param row            导入数据
     * @param existUsernames 导入数据中已存在的用户名
     * @param existEmails    导入数据中已存在的邮箱
     * @param existPhones    导入数据中已存在的手机号
     * @return 是否跳过
     */
    private boolean isSkipUserImport(UserImportReq req,
                                     UserImportRowReq row,
                                     List<String> existUsernames,
                                     List<String> existPhones,
                                     List<String> existEmails) {
        return SKIP.validate(req.getDuplicateUser(), row.getUsername(), existUsernames) || SKIP.validate(req
            .getDuplicateEmail(), row.getEmail(), existEmails) || SKIP.validate(req.getDuplicatePhone(), row
                .getPhone(), existPhones);
    }

    /**
     * 判断是否退出导入
     *
     * @param req            导入参数
     * @param list           导入数据
     * @param existUsernames 导入数据中已存在的用户名
     * @param existEmails    导入数据中已存在的邮箱
     * @param existPhones    导入数据中已存在的手机号
     * @return 是否退出
     */
    private boolean isExitImportUser(UserImportReq req,
                                     List<UserImportRowReq> list,
                                     List<String> existUsernames,
                                     List<String> existEmails,
                                     List<String> existPhones) {
        return list.stream()
            .anyMatch(row -> EXIT.validate(req.getDuplicateUser(), row.getUsername(), existUsernames) || EXIT
                .validate(req.getDuplicateEmail(), row.getEmail(), existEmails) || EXIT.validate(req
                    .getDuplicatePhone(), row.getPhone(), existPhones));
    }

    /**
     * 按指定数据集获取数据库已存在的数量
     *
     * @param userRowList 导入的数据源
     * @param rowField    导入数据的字段
     * @param dbField     对比数据库的字段
     * @return 存在的数量
     */
    private int countExistByField(List<UserImportRowReq> userRowList,
                                  Function<UserImportRowReq, String> rowField,
                                  SFunction<UserDO, ?> dbField) {
        List<String> fieldValues = CollUtils.mapToList(userRowList, rowField);
        if (fieldValues.isEmpty()) {
            return 0;
        }
        return Math.toIntExact(baseMapper.lambdaQuery().in(dbField, fieldValues).count());
    }

    /**
     * 按指定数据集获取数据库已存在内容
     *
     * @param userRowList 导入的数据源
     * @param rowField    导入数据的字段
     * @param dbField     对比数据库的字段
     * @return 存在的内容
     */
    private List<String> listExistByField(List<UserImportRowReq> userRowList,
                                          Function<UserImportRowReq, String> rowField,
                                          SFunction<UserDO, String> dbField) {
        List<String> fieldValues = CollUtils.mapToList(userRowList, rowField);
        if (fieldValues.isEmpty()) {
            return Collections.emptyList();
        }
        List<UserDO> userList = baseMapper.lambdaQuery().select(dbField).in(dbField, fieldValues).list();
        return CollUtils.mapToList(userList, dbField);
    }

    /**
     * 过滤无效的导入用户数据（批量导入不严格校验数据）
     *
     * @param importRowList 导入数据
     */
    private List<UserImportRowReq> filterImportData(List<UserImportRowReq> importRowList) {
        // 校验过滤
        List<UserImportRowReq> list = importRowList.stream()
            .filter(row -> ValidationUtil.validate(row).isEmpty())
            .toList();
        // 用户名去重
        return list.stream()
            .collect(Collectors.toMap(UserImportRowReq::getUsername, user -> user, (existing, replacement) -> existing))
            .values()
            .stream()
            .toList();
    }

    /**
     * 检测密码合法性
     *
     * @param password 密码
     * @param user     用户信息
     * @return 密码允许重复使用次数
     */
    private int checkPassword(String password, UserDO user) {
        Map<String, String> passwordPolicy = optionService.getByCategory(OptionCategoryEnum.PASSWORD);
        // 密码最小长度
        PASSWORD_MIN_LENGTH.validate(password, MapUtil.getInt(passwordPolicy, PASSWORD_MIN_LENGTH.name()), user);
        // 密码是否必须包含特殊字符
        PASSWORD_REQUIRE_SYMBOLS.validate(password, MapUtil.getInt(passwordPolicy, PASSWORD_REQUIRE_SYMBOLS
            .name()), user);
        // 密码是否允许包含正反序账号名
        PASSWORD_ALLOW_CONTAIN_USERNAME.validate(password, MapUtil
            .getInt(passwordPolicy, PASSWORD_ALLOW_CONTAIN_USERNAME.name()), user);
        // 密码重复使用次数
        int passwordRepetitionTimes = MapUtil.getInt(passwordPolicy, PASSWORD_REPETITION_TIMES.name());
        PASSWORD_REPETITION_TIMES.validate(password, passwordRepetitionTimes, user);
        return passwordRepetitionTimes;
    }

    /**
     * 检查用户名是否重复
     *
     * @param username 用户名
     * @param id       ID
     */
    private void checkUsernameRepeat(String username, Long id) {
        CheckUtils.throwIf(baseMapper.lambdaQuery()
            .eq(UserDO::getUsername, username)
            .ne(id != null, UserDO::getId, id)
            .exists(), "用户名为 [{}] 的用户已存在", username);
    }

    /**
     * 检查邮箱是否重复
     *
     * @param email    邮箱
     * @param id       ID
     * @param template 提示模板
     */
    private void checkEmailRepeat(String email, Long id, String template) {
        CheckUtils.throwIf(StrUtil.isNotBlank(email) && baseMapper.lambdaQuery()
            .eq(UserDO::getEmail, EncryptHelper.encrypt(email))
            .ne(ObjectUtil.isNotNull(id), UserDO::getId, id)
            .exists(), template, email);
    }

    /**
     * 检查手机号码是否重复
     *
     * @param phone    手机号码
     * @param id       ID
     * @param template 提示模板
     */
    private void checkPhoneRepeat(String phone, Long id, String template) {
        CheckUtils.throwIf(StrUtil.isNotBlank(phone) && baseMapper.lambdaQuery()
            .eq(UserDO::getPhone, EncryptHelper.encrypt(phone))
            .ne(ObjectUtil.isNotNull(id), UserDO::getId, id)
            .exists(), template, phone);
    }

    /**
     * 根据用户名获取用户列表
     *
     * @param usernames 用户名列表
     * @return 用户列表
     */
    private List<UserDO> listByUsernames(List<String> usernames) {
        return this.list(Wrappers.<UserDO>lambdaQuery()
            .in(UserDO::getUsername, usernames)
            .select(UserDO::getId, UserDO::getUsername));
    }

    /**
     * 更新用户上下文信息
     *
     * @param id ID
     */
    private void updateContext(Long id) {
        UserContext userContext = UserContextHolder.getContext(id);
        if (userContext != null) {
            userContext.setRoles(roleService.listByUserId(id));
            userContext.setPermissions(roleService.listPermissionByUserId(id));
            UserContextHolder.setContext(userContext);
        }
    }

    /**
     * 根据 ID 获取用户信息（数据权限）
     *
     * @param id ID
     * @return 用户信息
     */
    private UserDO getById(Long id) {
        UserDO user = baseMapper.lambdaQuery().eq(UserDO::getId, id).one();
        CheckUtils.throwIfNull(user, "用户不存在");
        return user;
    }

    /**
     * 统计有效的多级部门数量
     * <p>
     * 支持多级部门路径解析，使用冒号(:)作为层级分隔符
     * 例如：公司A:研发部:前端组 或 研发部
     * </p>
     *
     * @param deptNames 部门名称集合
     * @return 有效部门数量
     */
    private int countValidMultiLevelDepts(Set<String> deptNames) {
        CheckUtils.throwIfEmpty(deptNames, "部门名称集合不能为空");

        int validCount = 0;
        List<String> invalidDepts = new ArrayList<>();

        for (String deptName : deptNames) {
            try {
                findDeptByHierarchicalPath(deptName);
                validCount++;
            } catch (Exception e) {
                invalidDepts.add(deptName);
            }
        }

        CheckUtils.throwIf(CollUtil.isNotEmpty(invalidDepts), "以下部门无效或存在歧义：{}", String.join(", ", invalidDepts));

        return validCount;
    }

    /**
     * 构建多级部门映射关系
     * <p>
     * 将部门名称列表转换为部门名称到ID的映射，支持多级部门路径解析
     * </p>
     *
     * @param deptNames 部门名称列表
     * @return 部门名称到ID的映射
     */
    private Map<String, Long> buildMultiLevelDeptMapping(List<String> deptNames) {
        CheckUtils.throwIfEmpty(deptNames, "部门名称列表不能为空");

        Map<String, Long> deptMap = new HashMap<>();
        for (String deptName : deptNames) {
            DeptDO dept = findDeptByHierarchicalPath(deptName);
            CheckUtils.throwIfNull(dept, "部门 [{}] 不存在或存在歧义", deptName);
            deptMap.put(deptName, dept.getId());
        }
        return deptMap;
    }

    /**
     * 根据层级路径查找部门
     * <p>
     * 支持两种格式：
     * <ul>
     * <li>多级部门：公司A/研发部/前端组</li>
     * <li>单级部门：研发部</li>
     * </ul>
     * 使用左斜杠/作为层级分隔符，会逐级查找对应的部门
     * </p>
     *
     * @param deptPath 部门路径
     * @return 部门信息，未找到时返回null
     */
    private DeptDO findDeptByHierarchicalPath(String deptPath) {
        CheckUtils.throwIfBlank(deptPath, "部门路径不能为空");
        return deptPath.contains(StringConstants.SLASH)
            ? findMultiLevelDept(deptPath)
            : findSingleLevelDept(deptPath.trim());
    }

    /**
     * 查找多级部门
     * <p>
     * 从根部门开始逐级查找，确保部门层级关系正确
     * </p>
     *
     * @param deptPath 多级部门路径
     * @return 部门信息，未找到时返回null
     */
    private DeptDO findMultiLevelDept(String deptPath) {
        String[] pathParts = deptPath.split(StringConstants.SLASH);
        CheckUtils.throwIf(pathParts.length == 0, "部门路径格式错误：{}", deptPath);

        // 从根部门开始逐级查找
        DeptDO currentDept = null;
        Long parentId = 0L; // 根部门的parentId为null

        for (String part : pathParts) {
            String trimmedPart = part.trim();
            CheckUtils.throwIfBlank(trimmedPart, "部门路径包含空名称：{}", deptPath);

            // 查找当前层级下指定名称的部门
            currentDept = deptService.lambdaQuery()
                .eq(DeptDO::getName, trimmedPart)
                .eq(DeptDO::getParentId, parentId)
                .one();

            CheckUtils.throwIfNull(currentDept, "找不到部门 [{}] 在路径 [{}] 中", trimmedPart, deptPath);
            parentId = currentDept.getId(); // 更新父级ID为当前部门ID
        }

        return currentDept;
    }

    /**
     * 查找单级部门
     * <p>
     * 当只提供部门名称时，检查是否存在多个同名部门
     * 如果存在多个同名部门，则要求用户提供完整的层级路径
     * </p>
     *
     * @param deptName 部门名称
     * @return 部门信息，未找到或存在歧义时返回null
     */
    private DeptDO findSingleLevelDept(String deptName) {
        // 查找所有同名部门
        List<DeptDO> deptList = deptService.lambdaQuery().eq(DeptDO::getName, deptName).list();

        CheckUtils.throwIfEmpty(deptList, "部门 [{}] 不存在", deptName);
        CheckUtils.throwIf(deptList.size() > 1, "存在多个同名部门 [{}]，请使用完整层级路径，如：公司名:{}", deptName, deptName);

        return deptList.get(0);
    }
}
