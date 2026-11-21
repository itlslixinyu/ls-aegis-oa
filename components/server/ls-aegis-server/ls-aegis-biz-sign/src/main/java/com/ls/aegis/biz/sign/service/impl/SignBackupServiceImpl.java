package com.ls.aegis.biz.sign.service.impl;

import com.ls.aegis.compat.common.exception.ServiceException;
import com.ls.aegis.biz.sign.domain.SignBackupJob;
import com.ls.aegis.common.context.UserContextHolder;
import com.ls.aegis.biz.sign.domain.SignBackupSchedule;
import com.ls.aegis.biz.sign.mapper.SignBackupJobMapper;
import com.ls.aegis.biz.sign.mapper.SignBackupScheduleMapper;
import com.ls.aegis.biz.sign.service.ISignBackupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * 数据备份服务实现
 */
@Service
public class SignBackupServiceImpl implements ISignBackupService {

    @Autowired
    private SignBackupJobMapper jobMapper;

    @Autowired
    private SignBackupScheduleMapper scheduleMapper;

    @Override
    public List<SignBackupJob> selectJobList(SignBackupJob job) {
        return jobMapper.selectJobList(job);
    }

    @Override
    @Transactional
    public Long runManualBackup() {
        Long operatorId = UserContextHolder.getUserId();

        // 创建备份任务记录
        SignBackupJob job = new SignBackupJob();
        job.setJobCode("BK" + System.currentTimeMillis());
        job.setTriggerType("manual");
        job.setOperatorId(operatorId);
        job.setStatus("running");
        job.setStartedAt(new Date());
        job.setCreateTime(LocalDateTime.now());

        jobMapper.insertJob(job);

        // TODO: 执行实际备份逻辑
        // 1. 导出 MySQL 数据库
        // 2. 打包上传文件目录
        // 3. 上传到 MinIO
        // 4. 更新任务状态

        // 模拟完成
        job.setStatus("success");
        job.setFinishedAt(new Date());
        job.setMessage("备份成功");
        job.setFilePath("/backup/" + job.getJobCode() + ".zip");
        job.setFileSize(0L);
        jobMapper.updateJob(job);

        // TODO: 清理超出保留数的旧备份

        return job.getJobId();
    }

    @Override
    @Transactional
    public int deleteBackupByIds(Long[] jobIds) {
        // TODO: 删除备份文件（MinIO / 本地存储）
        return jobMapper.deleteJobByIds(jobIds);
    }

    @Override
    public String getDownloadUrl(Long jobId) {
        SignBackupJob job = jobMapper.selectJobById(jobId);
        if (job == null) {
            throw new ServiceException("备份任务不存在");
        }
        return "/sign/backup/download/" + jobId;
    }

    @Override
    public List<SignBackupSchedule> selectScheduleList(SignBackupSchedule schedule) {
        return scheduleMapper.selectScheduleList(schedule);
    }

    @Override
    @Transactional
    public int insertSchedule(SignBackupSchedule schedule) {
        schedule.setCreateBy(UserContextHolder.getUsername());
        schedule.setCreateTime(LocalDateTime.now());
        return scheduleMapper.insertSchedule(schedule);
    }

    @Override
    @Transactional
    public int updateSchedule(SignBackupSchedule schedule) {
        schedule.setUpdateBy(UserContextHolder.getUsername());
        schedule.setUpdateTime(LocalDateTime.now());
        return scheduleMapper.updateSchedule(schedule);
    }

    @Override
    @Transactional
    public int deleteScheduleByIds(Long[] scheduleIds) {
        return scheduleMapper.deleteScheduleByIds(scheduleIds);
    }

    @Override
    public void executeScheduledBackup(Long scheduleId) {
        // TODO: 由 Quartz 定时任务调用，执行计划备份
        runManualBackup();
    }
}
