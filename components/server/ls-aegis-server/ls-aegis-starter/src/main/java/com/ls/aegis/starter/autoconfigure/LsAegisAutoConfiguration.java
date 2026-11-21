package com.ls.aegis.starter.autoconfigure;

import com.ls.aegis.common.config.TenantExtensionProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 平台核心自动装配（业务与 RBAC 由启动模块扫描 com.ls 覆盖）
 */
@AutoConfiguration
@Configuration
@EnableConfigurationProperties(TenantExtensionProperties.class)
@ComponentScan(basePackages = {
    "com.ls.aegis.common",
    "com.ls.aegis.mybatis",
    "com.ls.aegis.redis",
    "com.ls.aegis.security",
    "com.ls.aegis.crypto",
    "com.ls.aegis.starter"
})
public class LsAegisAutoConfiguration {
}
