package com.ls.aegis.biz.sign.config;

import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 签章模块配置
 * <p>勿单独声明 {@code ObjectMapper} Bean，否则会覆盖 Spring Boot 自带的
 * JavaTimeModule，导致全局 LocalDateTime 序列化失败。</p>
 */
@Configuration
public class SignConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer signObjectMapperCustomizer() {
        return builder -> builder.featuresToDisable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    }
}
