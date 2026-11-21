package com.ls.aegis.biz.schedule.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;
import com.ls.aegis.biz.schedule.annotation.ConditionalOnEnabledScheduleJob;

/**
 * 仅在启用 Snail Job 时开启 Feign 客户端扫描
 */
@Configuration
@ConditionalOnEnabledScheduleJob
@EnableFeignClients(basePackages = "com.ls.aegis.biz.schedule.api")
public class ScheduleFeignEnableConfiguration {
}
