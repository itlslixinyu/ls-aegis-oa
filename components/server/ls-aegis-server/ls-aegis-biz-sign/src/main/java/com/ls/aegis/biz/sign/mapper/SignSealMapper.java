package com.ls.aegis.biz.sign.mapper;

import com.ls.aegis.biz.sign.domain.SignSeal;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 印章管理Mapper接口
 */
public interface SignSealMapper {

    /**
     * 查询印章列表
     */
    List<SignSeal> selectSealList(SignSeal seal);

    /**
     * 查询印章详情
     */
    SignSeal selectSealById(Long sealId);

    /** Serialize seal deletion against an executing signing transaction. */
    SignSeal selectSealByIdForUpdate(Long sealId);

    /**
     * 新增印章
     */
    int insertSeal(SignSeal seal);

    /**
     * 修改印章
     */
    int updateSeal(SignSeal seal);

    /**
     * 删除印章（逻辑删除）
     */
    int deleteSealByIds(Long[] sealIds);

    /**
     * 获取当前用户可用的印章列表
     */
    List<SignSeal> selectAvailableSealList(@Param("userId") Long userId, @Param("projectId") Long projectId);

    /**
     * 查询印章编号是否存在
     */
    int existsBySealCode(String sealCode);

    /**
     * 批量查询印章信息
     */
    List<SignSeal> selectSealByIds(Long[] sealIds);

    /**
     * 自动生成下一个印章编号（YZ + 3位递增）
     * @return 下一个可用编号，如 YZ001, YZ002, ... YZ999
     */
    String selectNextSealCode();

    /**
     * 使用数据库序列生成下一个印章编号（高并发安全版）
     * @return 下一个可用编号，如 YZ001, YZ002, ... YZ999
     */
    String selectNextSealCodeWithSequence();
}
