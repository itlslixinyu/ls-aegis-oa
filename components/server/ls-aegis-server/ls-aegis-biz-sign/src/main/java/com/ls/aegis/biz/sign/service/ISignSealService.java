package com.ls.aegis.biz.sign.service;

import com.ls.aegis.biz.sign.domain.SignSeal;

import java.util.List;
import java.util.Map;

/**
 * 印章管理服务接口
 */
public interface ISignSealService {

    /**
     * 查询印章列表
     */
    List<SignSeal> selectSealList(SignSeal seal);

    /**
     * 查询印章详情
     */
    SignSeal selectSealById(Long sealId);

    /**
     * 新增印章
     */
    int insertSeal(SignSeal seal);

    /**
     * 修改印章
     */
    int updateSeal(SignSeal seal);

    /**
     * 删除印章
     */
    int deleteSealByIds(Long[] sealIds);

    /**
     * 获取当前用户可用印章列表
     */
    List<SignSeal> selectAvailableSealList(Long userId, Long projectId);

    /**
     * 印章授权 - 批量绑定用户权限
     */
    int bindPermissions(Long sealId, Long[] userIds);

    /**
     * 印章授权 - 批量移除用户权限
     */
    int removePermissions(Long sealId, Long[] userIds);

    /**
     * 查询印章已授权用户列表
     */
    List<Long> selectPermittedUserIds(Long sealId);

    /**
     * 判断当前用户是否可访问印章图片
     */
    boolean canAccessSealImage(Long sealId, Long userId);

    /**
     * 判断当前用户是否可访问提取记录资源
     */
    boolean canAccessExtractRecord(Long recordId, Long userId);

    /**
     * 绑定平台托管证书
     */
    Map<String, Object> bindSealCert(Long sealId);

    /**
     * 查询印章平台证书绑定信息
     */
    Map<String, Object> getSealCertInfo(Long sealId);
}
