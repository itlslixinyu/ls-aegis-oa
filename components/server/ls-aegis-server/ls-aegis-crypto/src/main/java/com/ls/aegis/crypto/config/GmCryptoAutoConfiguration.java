package com.ls.aegis.crypto.config;

import com.ls.aegis.crypto.api.IGmCrypto;
import com.ls.aegis.crypto.impl.SoftwareGmCrypto;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 国密自动装配：gm.enable=true 时自动补齐密钥并注册 {@link SoftwareGmCrypto}。
 */
@AutoConfiguration
@EnableConfigurationProperties(GmCryptoProperties.class)
public class GmCryptoAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(IGmCrypto.class)
    @ConditionalOnProperty(prefix = "gm", name = "enable", havingValue = "true")
    public IGmCrypto softwareGmCrypto(GmCryptoProperties properties) {
        GmKeyMaterialBootstrap.ensureKeys(properties);
        return new SoftwareGmCrypto(properties);
    }
}
