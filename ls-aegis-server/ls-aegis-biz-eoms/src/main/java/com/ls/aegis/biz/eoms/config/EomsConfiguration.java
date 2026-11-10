package com.ls.aegis.biz.eoms.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * EOMS 业务自动配置。
 * <p>Mapper 由全局 {@code mybatis-plus.mapper-package} 扫描，勿再 {@code @MapperScan}，
 * 否则与平台扫描双注册，Spring 6.1+ 会触发 factoryBeanObjectType 异常。</p>
 */
@AutoConfiguration
@ComponentScan("com.ls.aegis.biz.eoms")
public class EomsConfiguration {
}
