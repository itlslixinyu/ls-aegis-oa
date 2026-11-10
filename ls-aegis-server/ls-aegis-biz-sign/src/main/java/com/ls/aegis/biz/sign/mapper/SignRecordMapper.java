package com.ls.aegis.biz.sign.mapper;

import com.ls.aegis.biz.sign.domain.SignRecord;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 签章记录Mapper接口
 * <p>
 * 【合规强制】签章记录属于电子签名法规定的「不可篡改审计日志」，
 * 任何业务接口都不允许调用删除方法（仅数据清理脚本可离线归档，需DBA审批）。
 */
public interface SignRecordMapper {

    /**
     * 查询签章记录列表
     */
    List<SignRecord> selectRecordList(SignRecord record);

    /**
     * 查询签章记录详情
     */
    SignRecord selectRecordById(String recordId);

    /**
     * 根据签后文件ID查询最近一条签章记录
     */
    SignRecord selectBySignedFileId(@Param("signedFileId") Long signedFileId);

    /**
     * 新增签章记录
     */
    int insertRecord(SignRecord record);

    /**
     * 修改签章记录（仅允许更新 status / 验签 等非核心字段）
     */
    int updateRecord(SignRecord record);

    /**
     * 【禁止调用】删除签章记录
     * @deprecated 签章记录是电子签名核心审计依据，严禁物理/逻辑删除。
     *             调用此方法将抛出 UnsupportedOperationException。
     */
    @Deprecated
    int deleteRecordByIds(String[] recordIds);

    /**
     * 查询最近签章记录
     */
    List<SignRecord> selectRecentRecords(@Param("limit") int limit);

    /**
     * 统计本月签章数量
     */
    Long countMonthSign();

    /**
     * 统计一组印章关联的签章记录条数（用于印章删除前的「关联校验」）
     * @param sealIds 印章ID数组
     * @return 关联记录数
     */
    long countBySealIds(@Param("sealIds") Long[] sealIds);

    /** Current read after locking seal anchors; never rely on an older RR snapshot. */
    List<String> selectRecordIdsBySealIdsForUpdate(@Param("sealIds") Long[] sealIds);
}
