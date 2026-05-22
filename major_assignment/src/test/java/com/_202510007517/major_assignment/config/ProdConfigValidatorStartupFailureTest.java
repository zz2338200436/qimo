package com._202510007517.major_assignment.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationHook;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(OutputCaptureExtension.class)
class ProdConfigValidatorStartupFailureTest {

    @Test
    void failsStartupWithClearMessageWhenProdProfileMissesDbPassword(CapturedOutput output) {
        assertThatThrownBy(this::runProdContextWithoutDbPassword)
                .isInstanceOf(ExitInterceptedException.class)
                .hasMessage("Intercepted System.exit(1)");

        assertThat(output)
                .contains("Prod 启动校验失败：以下敏感配置缺失或为空，无法启动。")
                .contains("spring.datasource.password (env: DB_PASSWORD)")
                .contains("请检查对应环境变量是否已在运行环境中注入。");
    }

    private void runProdContextWithoutDbPassword() {
        SpringApplication application = new SpringApplicationBuilder(ProdStartupFailureTestApplication.class)
                .profiles("prod")
                .properties(
                        "spring.main.web-application-type=none",
                        "spring.main.banner-mode=off",
                        "spring.datasource.username=prod_user",
                        "spring.data.redis.password=redis-secret")
                .build();

        try (ConfigurableApplicationContext ignored = application.run()) {
            // startup is expected to fail before context becomes usable
        }
    }

    @SpringBootConfiguration
    static class ProdStartupFailureTestApplication {

        @Bean
        ProdConfigValidator prodConfigValidator(Environment environment) {
            return new ProdConfigValidator(environment, code -> {
                throw new ExitInterceptedException(code);
            });
        }
    }

    static final class ExitInterceptedException extends RuntimeException {

        ExitInterceptedException(int exitCode) {
            super("Intercepted System.exit(" + exitCode + ")");
        }
    }
}
