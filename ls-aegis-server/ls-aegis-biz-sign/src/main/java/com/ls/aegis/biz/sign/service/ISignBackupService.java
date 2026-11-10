package com.ls.aegis.biz.sign.service;

import com.ls.aegis.biz.sign.domain.SignBackupSchedule;
import com.ls.aegis.biz.sign.domain.SignBackupJob;

import java.util.List;

/**
 * 数据备份服务接口
 */
public interface ISignBackupService {

    /**
     * 查询备份任务列表
     */
    List<SignBackupJob> selectJobList(SignBackupJob job);

    /**
     * 手动执行备份
     */
    Long runManualBackup();

    /**
     * 删除备份
     */
    int deleteBackupByIds(Long[] jobIds);

    /**
     * 获取备份文件下载URL
     */
    String getDownloadUrl(Long jobId);

    /**
     * 查询备份计划列表
     */
    List<SignBackupSchedule> selectScheduleList(SignBackupSchedule schedule);

    /**
     * 新增备份计划
     */
    int insertSchedule(SignBackupSchedule schedule);

    /**
     * 修改备份计划
     */
    int updateSchedule(SignBackupSchedule schedule);

    /**
     * 删除备份计划
     */
    int deleteScheduleByIds(Long[] scheduleIds);

    /**
     * 执行定时备份
     */
    void executeScheduledBackup(Long scheduleId);
}
