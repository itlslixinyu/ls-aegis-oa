package com.ls.aegis.biz.sign.mapper;

import com.ls.aegis.biz.sign.domain.SignBackupJob;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 备份任务Mapper接口
 */
public interface SignBackupJobMapper {

    /**
     * 查询备份任务列表
     */
    List<SignBackupJob> selectJobList(SignBackupJob job);

    /**
     * 查询备份任务详情
     */
    SignBackupJob selectJobById(Long jobId);

    /**
     * 新增备份任务
     */
    int insertJob(SignBackupJob job);

    /**
     * 修改备份任务
     */
    int updateJob(SignBackupJob job);

    /**
     * 删除备份任务
     */
    int deleteJobByIds(Long[] jobIds);

    /**
     * 查询超出保留数的旧备份
     */
    List<SignBackupJob> selectExcessJobs(@Param("retainCount") int retainCount);

    /**
     * 更新任务状态
     */
    int updateJobStatus(@Param("jobId") Long jobId, @Param("status") String status, @Param("message") String message);
}
