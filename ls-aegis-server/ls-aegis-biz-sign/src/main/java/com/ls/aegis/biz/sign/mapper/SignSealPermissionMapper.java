package com.ls.aegis.biz.sign.mapper;

import com.ls.aegis.biz.sign.domain.SignSealPermission;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 印章授权Mapper接口
 */
public interface SignSealPermissionMapper {

    /**
     * 查询印章授权用户列表
     */
    List<SignSealPermission> selectPermissionList(SignSealPermission permission);

    /**
     * 查询印章的已授权用户ID列表
     */
    List<Long> selectUserIdsBySealId(Long sealId);

    /**
     * 批量绑定授权
     */
    int batchInsert(@Param("sealId") Long sealId, @Param("userIds") Long[] userIds);

    /**
     * 批量移除授权
     */
    int deleteBySealIdAndUserIds(@Param("sealId") Long sealId, @Param("userIds") Long[] userIds);

    /**
     * 删除印章的所有授权
     */
    int deleteBySealId(Long sealId);

    /**
     * 检查用户是否有印章使用权限
     */
    int existsPermission(@Param("sealId") Long sealId, @Param("userId") Long userId);
}
