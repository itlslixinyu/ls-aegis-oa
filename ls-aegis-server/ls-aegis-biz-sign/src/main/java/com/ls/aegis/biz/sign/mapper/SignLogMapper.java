package com.ls.aegis.biz.sign.mapper;

import com.ls.aegis.biz.sign.domain.SignLog;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 签章日志Mapper接口
 * <p>
 * 【合规强制】签章日志属于《电子签名法》《网络安全法》法定留痕记录，
 * 禁止任何业务接口通过代码方式删除。数据清理须走合规流程（离线脚本+审计）。
 */
public interface SignLogMapper {

    /**
     * 查询签章日志列表
     */
    List<SignLog> selectLogList(SignLog log);

    /**
     * 查询签章日志详情
     */
    SignLog selectLogById(Long logId);

    /**
     * 根据签后文件ID查询最近一条签章日志
     */
    SignLog selectLatestByOutputFileId(@Param("outputFileId") Long outputFileId);

    /**
     * 新增签章日志
     */
    int insertLog(SignLog log);

    /**
     * 【禁止调用】删除签章日志
     * @deprecated 签章日志严禁删除。调用即抛 UnsupportedOperationException。
     */
    @Deprecated
    int deleteLogByIds(Long[] logIds);

    /**
     * 【禁止调用】清空所有签章日志
     * @deprecated 严禁清空审计日志。调用即抛 UnsupportedOperationException。
     */
    @Deprecated
    int clearAll();
}
