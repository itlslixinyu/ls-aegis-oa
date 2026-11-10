package com.ls.aegis.biz.quote.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * 报价业务自动配置。
 * <p>Mapper 由全局 {@code mybatis-plus.mapper-package} 扫描，勿再 {@code @MapperScan}。</p>
 */
@AutoConfiguration
@ComponentScan("com.ls.aegis.biz.quote")
public class QuoteConfiguration {
}
