package com.ls.aegis.biz.sign.mapper;

import com.ls.aegis.biz.sign.domain.SignSealExtract;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 印章提取记录Mapper接口
 */
public interface SignSealExtractMapper {

    /**
     * 根据ID查询提取记录
     */
    SignSealExtract selectExtractById(Long id);

    /**
     * 查询提取记录列表
     */
    List<SignSealExtract> selectExtractList(SignSealExtract extract);

    /**
     * 根据用户ID查询提取记录列表（Map格式）
     */
    List<Map<String, Object>> selectExtractListByUserId(@Param("userId") Long userId);

    /**
     * 查询提取记录列表（Map格式）
     */
    List<Map<String, Object>> selectExtractListMap();

    /**
     * 新增提取记录
     */
    int insertExtract(SignSealExtract extract);

    /**
     * 根据 ID 更新 extract_file_name 字段
     */
    int updateExtractFileName(SignSealExtract record);

    /**
     * 删除提取记录
     */
    int deleteExtractByIds(Long[] ids);

    /**
     * 清空所有提取记录
     */
    int clearAll();
}
