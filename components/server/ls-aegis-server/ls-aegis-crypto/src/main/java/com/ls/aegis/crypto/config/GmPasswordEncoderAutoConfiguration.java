package com.ls.aegis.crypto.config;

import com.ls.aegis.crypto.password.Sm3PasswordEncoder;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.Map;

/**
 * 国密口令编码器：默认编码 SM3，校验兼容存量 {@code {bcrypt}} / {@code $2a$}。
 */
@AutoConfiguration
@ConditionalOnClass(PasswordEncoder.class)
@ConditionalOnProperty(prefix = "gm", name = "enable", havingValue = "true")
public class GmPasswordEncoderAutoConfiguration {

    @Bean
    @Primary
    public PasswordEncoder gmPasswordEncoder() {
        Map<String, PasswordEncoder> encoders = new HashMap<>(4);
        BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();
        encoders.put("bcrypt", bcrypt);
        encoders.put("sm3", new Sm3PasswordEncoder());
        DelegatingPasswordEncoder encoder = new DelegatingPasswordEncoder("sm3", encoders);
        // 兼容历史无 {id} 前缀的 $2a$ BCrypt 密文
        encoder.setDefaultPasswordEncoderForMatches(bcrypt);
        return encoder;
    }
}
