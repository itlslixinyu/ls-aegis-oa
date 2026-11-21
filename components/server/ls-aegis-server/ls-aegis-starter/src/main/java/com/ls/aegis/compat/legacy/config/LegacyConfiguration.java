package com.ls.aegis.compat.legacy.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * 遗留过渡层自动配置：扫描 common/framework/system 下的组件（ServerConfig、RedisCache、桩服务等）
 */
@AutoConfiguration
@ComponentScan({
    "com.ls.aegis.compat.common",
    "com.ls.aegis.compat.framework",
    "com.ls.aegis.compat.system",
    "com.ls.aegis.compat.legacy"
})
public class LegacyConfiguration {
}
