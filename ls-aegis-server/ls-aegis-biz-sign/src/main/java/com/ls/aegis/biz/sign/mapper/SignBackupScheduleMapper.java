package com.ls.aegis.biz.sign.mapper;

import com.ls.aegis.biz.sign.domain.SignBackupSchedule;

import java.util.List;

/**
 * 备份计划Mapper接口
 */
public interface SignBackupScheduleMapper {

    /**
     * 查询备份计划列表
     */
    List<SignBackupSchedule> selectScheduleList(SignBackupSchedule schedule);

    /**
     * 查询备份计划详情
     */
    SignBackupSchedule selectScheduleById(Long scheduleId);

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
     * 查询待执行的定时计划
     */
    List<SignBackupSchedule> selectPendingSchedules();
}
