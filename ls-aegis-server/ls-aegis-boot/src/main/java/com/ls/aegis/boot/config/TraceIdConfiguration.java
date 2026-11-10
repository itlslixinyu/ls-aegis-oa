package com.ls.aegis.boot.config;

import com.yomahub.tlog.id.TLogIdGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

/**
 * Trace identities must remain available when the wall clock is corrected.
 * Business document IDs continue to use the separately configured CosID generators.
 */
@Configuration(proxyBeanMethods = false)
public class TraceIdConfiguration {

    /** Uses TLog's supported extension point; ContiNew's default bean backs off. */
    @Bean
    public TLogIdGenerator clockIndependentTraceIdGenerator() {
        return new TLogIdGenerator() {
            @Override
            public String generateTraceId() {
                return UUID.randomUUID().toString();
            }
        };
    }
}
