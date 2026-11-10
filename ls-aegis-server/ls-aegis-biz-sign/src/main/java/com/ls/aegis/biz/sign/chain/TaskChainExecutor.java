package com.ls.aegis.biz.sign.chain;

import com.ls.aegis.compat.common.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TaskChainExecutor {

    private static final Logger log = LoggerFactory.getLogger(TaskChainExecutor.class);

    public SignChainContext executeSequential(List<SignTask> tasks, SignChainContext context) {
        context.setChainStatus("processing");
        context.setTotalTasks(tasks.size());
        context.setCompletedTasks(0);

        log.info("任务链开始执行，共{}个任务", tasks.size());

        for (SignTask task : tasks) {
            if (task.canSkip(context)) {
                log.info("跳过任务：{}", task.getTaskName());
                continue;
            }

            long start = System.currentTimeMillis();
            try {
                log.info("开始执行任务：{}", task.getTaskName());
                task.execute(context);
                context.setCompletedTasks(context.getCompletedTasks() + 1);
                log.info("任务执行完成：{}，耗时{}ms", task.getTaskName(), (System.currentTimeMillis() - start));
            } catch (Exception e) {
                context.setChainStatus("failed");
                context.setErrorMessage(task.getTaskName() + "失败: " + e.getMessage());
                log.error("任务执行失败：{}", task.getTaskName(), e);
                throw new ServiceException("签章任务执行失败：" + task.getTaskName() + " - " + e.getMessage());
            }
        }

        context.setChainStatus("completed");
        log.info("任务链执行完成，共{}个任务", tasks.size());
        return context;
    }
}
