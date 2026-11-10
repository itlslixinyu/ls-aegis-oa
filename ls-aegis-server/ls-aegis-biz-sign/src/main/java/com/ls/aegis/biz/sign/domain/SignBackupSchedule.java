package com.ls.aegis.biz.sign.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ls.aegis.mybatis.base.model.entity.XmlMapperDO;

import java.util.Date;

/**
 * 备份计划表 sign_backup_schedule
 */
public class SignBackupSchedule extends XmlMapperDO {

    private static final long serialVersionUID = 1L;

    private Long scheduleId;
    private String name;
    /** 周期(daily/weekly/monthly) */
    private String cycle;
    /** 执行时间(HH:MM) */
    private String runTime;
    /** 状态(0启用 1暂停) */
    private String status;
    private String triggerOs;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastRunAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date nextRunAt;

    public Long getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(Long scheduleId) {
        this.scheduleId = scheduleId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCycle() {
        return cycle;
    }

    public void setCycle(String cycle) {
        this.cycle = cycle;
    }

    public String getRunTime() {
        return runTime;
    }

    public void setRunTime(String runTime) {
        this.runTime = runTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTriggerOs() {
        return triggerOs;
    }

    public void setTriggerOs(String triggerOs) {
        this.triggerOs = triggerOs;
    }

    public Date getLastRunAt() {
        return lastRunAt;
    }

    public void setLastRunAt(Date lastRunAt) {
        this.lastRunAt = lastRunAt;
    }

    public Date getNextRunAt() {
        return nextRunAt;
    }

    public void setNextRunAt(Date nextRunAt) {
        this.nextRunAt = nextRunAt;
    }
}
