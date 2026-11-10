package com.ls.aegis.compat.common.utils;

import java.time.ZoneId;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.util.PatternMatchUtils;
import cn.hutool.extra.spring.SpringUtil;
import com.ls.aegis.compat.common.constant.Constants;
import com.ls.aegis.compat.common.constant.HttpStatus;
import com.ls.aegis.compat.common.core.domain.entity.SysRole;
import com.ls.aegis.compat.common.core.domain.entity.SysUser;
import com.ls.aegis.compat.common.core.domain.model.LoginUser;
import com.ls.aegis.compat.common.exception.ServiceException;
import com.ls.aegis.common.context.UserContext;
import com.ls.aegis.common.context.UserContextHolder;
import com.ls.aegis.common.context.UserExtraContext;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 安全服务工具类（桥接 Sa-Token / UserContextHolder，替代原 Spring Security）
 *
 * @author ItLs
 */
public class SecurityUtils
{

    /**
     * 用户ID
     **/
    public static Long getUserId()
    {
        try
        {
            Long userId = UserContextHolder.getUserId();
            if (userId == null)
            {
                throw new ServiceException("获取用户ID异常", HttpStatus.UNAUTHORIZED);
            }
            return userId;
        }
        catch (ServiceException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new ServiceException("获取用户ID异常", HttpStatus.UNAUTHORIZED);
        }
    }

    /**
     * 获取部门ID
     **/
    public static Long getDeptId()
    {
        try
        {
            UserContext ctx = UserContextHolder.getContext();
            if (ctx == null || ctx.getDeptId() == null)
            {
                throw new ServiceException("获取部门ID异常", HttpStatus.UNAUTHORIZED);
            }
            return ctx.getDeptId();
        }
        catch (ServiceException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new ServiceException("获取部门ID异常", HttpStatus.UNAUTHORIZED);
        }
    }

    /**
     * 获取用户账户
     **/
    public static String getUsername()
    {
        try
        {
            String username = UserContextHolder.getUsername();
            if (StringUtils.isEmpty(username))
            {
                throw new ServiceException("获取用户账户异常", HttpStatus.UNAUTHORIZED);
            }
            return username;
        }
        catch (ServiceException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new ServiceException("获取用户账户异常", HttpStatus.UNAUTHORIZED);
        }
    }

    /**
     * 获取登录用户（由 UserContextHolder 组装）
     **/
    public static LoginUser getLoginUser()
    {
        try
        {
            UserContext ctx = UserContextHolder.getContext();
            if (ctx == null || ctx.getId() == null)
            {
                throw new ServiceException("获取用户信息异常", HttpStatus.UNAUTHORIZED);
            }
            LoginUser loginUser = new LoginUser();
            loginUser.setUserId(ctx.getId());
            loginUser.setDeptId(ctx.getDeptId());
            loginUser.setPermissions(ctx.getPermissions() == null ? Collections.emptySet() : ctx.getPermissions());

            UserExtraContext extra = null;
            try
            {
                extra = UserContextHolder.getExtraContext();
            }
            catch (Exception ignored)
            {
                // 无额外上下文时忽略
            }
            if (extra != null)
            {
                loginUser.setIpaddr(extra.getIp());
                loginUser.setLoginLocation(extra.getAddress());
                loginUser.setBrowser(extra.getBrowser());
                loginUser.setOs(extra.getOs());
                if (extra.getLoginTime() != null)
                {
                    loginUser.setLoginTime(extra.getLoginTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
                }
            }

            SysUser user = new SysUser();
            user.setUserId(ctx.getId());
            user.setUserName(ctx.getUsername());
            user.setNickName(UserContextHolder.getNickname());
            user.setDeptId(ctx.getDeptId());
            if (ctx.getRoleCodes() != null && !ctx.getRoleCodes().isEmpty())
            {
                List<SysRole> roles = ctx.getRoleCodes().stream().map(code -> {
                    SysRole role = new SysRole();
                    role.setRoleKey(code);
                    return role;
                }).collect(Collectors.toList());
                user.setRoles(roles);
            }
            loginUser.setUser(user);
            return loginUser;
        }
        catch (ServiceException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new ServiceException("获取用户信息异常", HttpStatus.UNAUTHORIZED);
        }
    }

    /**
     * 生成密码（委托平台 PasswordEncoder，国密开启时为 SM3）
     *
     * @param password 密码
     * @return 加密字符串
     */
    public static String encryptPassword(String password)
    {
        return SpringUtil.getBean(PasswordEncoder.class).encode(password);
    }

    /**
     * 判断密码是否相同
     *
     * @param rawPassword 真实密码
     * @param encodedPassword 加密后字符
     * @return 结果
     */
    public static boolean matchesPassword(String rawPassword, String encodedPassword)
    {
        return SpringUtil.getBean(PasswordEncoder.class).matches(rawPassword, encodedPassword);
    }

    /**
     * 校验密码复杂度
     * 要求：至少包含大小写字母、数字、特殊字符中的三种
     *
     * @param password 密码
     * @return 是否符合复杂度要求
     */
    public static boolean checkPasswordComplexity(String password)
    {
        if (StringUtils.isEmpty(password))
        {
            return false;
        }
        int types = 0;
        if (password.matches(".*[a-z].*")) types++;
        if (password.matches(".*[A-Z].*")) types++;
        if (password.matches(".*\\d.*")) types++;
        if (password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) types++;
        return types >= 3;
    }

    /**
     * 是否为管理员
     *
     * @return 结果
     */
    public static boolean isAdmin()
    {
        try
        {
            return UserContextHolder.isSuperAdmin();
        }
        catch (Exception e)
        {
            return isAdmin(getUserId());
        }
    }

    /**
     * 是否为管理员（兼容旧调用：优先角色，回退 userId==1）
     *
     * @param userId 用户ID
     * @return 结果
     */
    public static boolean isAdmin(Long userId)
    {
        try
        {
            UserContext ctx = UserContextHolder.getContext();
            if (ctx != null && userId != null && userId.equals(ctx.getId()))
            {
                return ctx.isSuperAdmin();
            }
        }
        catch (Exception ignored)
        {
            // 无上下文时回退
        }
        return userId != null && 1L == userId;
    }

    /**
     * 验证用户是否具备某权限
     *
     * @param permission 权限字符串
     * @return 用户是否具备某权限
     */
    public static boolean hasPermi(String permission)
    {
        return hasPermi(getLoginUser().getPermissions(), permission);
    }

    /**
     * 判断是否包含权限
     *
     * @param authorities 权限列表
     * @param permission 权限字符串
     * @return 用户是否具备某权限
     */
    public static boolean hasPermi(Collection<String> authorities, String permission)
    {
        if (authorities == null || authorities.isEmpty())
        {
            return false;
        }
        return authorities.stream().filter(StringUtils::hasText)
                .anyMatch(x -> Constants.ALL_PERMISSION.equals(x) || PatternMatchUtils.simpleMatch(x, permission));
    }

    /**
     * 验证用户是否拥有某个角色
     *
     * @param role 角色标识
     * @return 用户是否具备某角色
     */
    public static boolean hasRole(String role)
    {
        try
        {
            Set<String> roleCodes = UserContextHolder.getContext().getRoleCodes();
            if (roleCodes != null && !roleCodes.isEmpty())
            {
                return hasRole(roleCodes, role);
            }
        }
        catch (Exception ignored)
        {
            // 回退到 LoginUser.roles
        }
        List<SysRole> roleList = getLoginUser().getUser().getRoles();
        if (roleList == null || roleList.isEmpty())
        {
            return false;
        }
        Collection<String> roles = roleList.stream().map(SysRole::getRoleKey).collect(Collectors.toSet());
        return hasRole(roles, role);
    }

    /**
     * 判断是否包含角色
     *
     * @param roles 角色列表
     * @param role 角色
     * @return 用户是否具备某角色权限
     */
    public static boolean hasRole(Collection<String> roles, String role)
    {
        if (roles == null || roles.isEmpty())
        {
            return false;
        }
        return roles.stream().filter(StringUtils::hasText)
                .anyMatch(x -> Constants.SUPER_ADMIN.equals(x) || PatternMatchUtils.simpleMatch(x, role));
    }

}
