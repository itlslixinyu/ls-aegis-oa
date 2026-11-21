package com.ls.aegis.crypto.config;

import com.ls.aegis.crypto.api.IGmCrypto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 国密自动装配开关与密钥自生成测试。
 */
class GmCryptoAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(GmCryptoAutoConfiguration.class));

    @TempDir
    Path tempDir;

    @Test
    void disabledWhenGmEnableFalse() {
        contextRunner.withPropertyValues("gm.enable=false")
            .run(context -> assertThat(context).doesNotHaveBean(IGmCrypto.class));
    }

    @Test
    void enabledAndAutoGenerateKeys() {
        Path store = tempDir.resolve("keys.properties");
        contextRunner
            .withPropertyValues(
                "gm.enable=true",
                "gm.auto-generate=true",
                "gm.key-store-path=" + store.toAbsolutePath(),
                "gm.sm2.public-key=",
                "gm.sm2.private-key=",
                "gm.sm4.key=",
                "gm.sm4.iv=")
            .run(context -> {
                assertThat(context).hasSingleBean(IGmCrypto.class);
                IGmCrypto crypto = context.getBean(IGmCrypto.class);
                assertThat(crypto.isEnabled()).isTrue();
                assertThat(store).exists();
                byte[] data = "auto-key".getBytes();
                assertThat(crypto.sm2Verify(data, crypto.sm2Sign(data))).isTrue();
            });
    }
}
