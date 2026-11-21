package com.ls.aegis.boot.job;

import cn.hutool.extra.spring.SpringUtil;
import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import com.aizuda.snailjob.common.log.SnailJobLog;
import com.ls.aegis.biz.schedule.annotation.ConditionalOnEnabledScheduleJob;
import com.ls.aegis.biz.sign.service.ISignProjectService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import top.continew.starter.core.constant.PropertiesConstants;
import top.continew.starter.extension.tenant.annotation.TenantIgnore;

/**
 * 签章项目状态自动流转：
 * 审核通过后的「未开始」到达开始时间→进行中；到达结束时间→已完成。
 */
@Slf4j
@Component
public class SignProjectStatusJob {

    /**
     * 未启用 Snail Job 时使用 Spring 定时任务（默认每分钟）
     */
    @Component
    @ConditionalOnProperty(prefix = "snail-job", name = PropertiesConstants.ENABLED, havingValue = "false")
    public static class Scheduler {

        @TenantIgnore
        @Scheduled(cron = "0 * * * * ?")
        public void refreshWithSchedule() {
            log.info("定时任务 [签章项目状态刷新] 开始执行。");
            refresh();
            log.info("定时任务 [签章项目状态刷新] 执行结束。");
        }
    }

    /**
     * 启用 Snail Job 时由调度中心触发（执行器名：SignProjectStatusJob）
     */
    @Component
    @ConditionalOnEnabledScheduleJob
    public static class ScheduleJob {

        @TenantIgnore
        @JobExecutor(name = "SignProjectStatusJob")
        public void refreshWithScheduleJob() {
            SnailJobLog.REMOTE.info("定时任务 [签章项目状态刷新] 开始执行。");
            refresh();
            SnailJobLog.REMOTE.info("定时任务 [签章项目状态刷新] 执行结束。");
        }
    }

    private static void refresh() {
        ISignProjectService projectService = SpringUtil.getBean(ISignProjectService.class);
        int count = projectService.refreshExpiredProjects();
        if (count > 0) {
            log.info("签章项目状态刷新：更新了 {} 条", count);
        }
    }
}
