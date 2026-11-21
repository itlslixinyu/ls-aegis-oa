package com.ls.aegis.biz.sign.task;

import com.ls.aegis.biz.sign.service.ISignProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 签章项目状态刷新（兼容旧 invoke 入口）。
 * <p>正式调度见 {@code com.ls.aegis.boot.job.SignProjectStatusJob}；
 * 列表/详情/签章校验也会即时刷新。</p>
 */
@Component("signTask")
public class SignTask {

    private static final Logger log = LoggerFactory.getLogger(SignTask.class);

    @Autowired
    private ISignProjectService projectService;

    /**
     * 刷新到期项目状态：未开始→进行中；进行中/未开始且已过结束→已完成
     */
    public void refreshExpiredProjects() {
        log.info("签章任务：开始刷新项目状态");
        try {
            int count = projectService.refreshExpiredProjects();
            log.info("签章任务：刷新完成，更新了 {} 个项目", count);
        } catch (Exception e) {
            log.error("签章任务：刷新项目状态失败", e);
        }
    }
}
