package com._202510007517.major_assignment.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.mock.env.MockEnvironment;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ProdConfigValidatorTest {

    @Test
    void exitsWhenProdProfileHasMissingSensitiveConfig() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.profiles.active", "prod")
                .withProperty("spring.datasource.username", "prod_user")
                .withProperty("spring.datasource.password", "")
                .withProperty("spring.data.redis.password", "redis-secret");
        environment.setActiveProfiles("prod");

        AtomicInteger exitCode = new AtomicInteger(-1);
        ProdConfigValidator validator = new ProdConfigValidator(environment, exitCode::set);

        validator.run(new DefaultApplicationArguments(new String[0]));

        assertThat(exitCode.get()).isEqualTo(1);
    }

    @Test
    void doesNotExitOutsideProdProfile() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.datasource.username", "")
                .withProperty("spring.datasource.password", "")
                .withProperty("spring.data.redis.password", "");

        AtomicInteger exitCode = new AtomicInteger(-1);
        ProdConfigValidator validator = new ProdConfigValidator(environment, exitCode::set);

        validator.run(new DefaultApplicationArguments(new String[0]));

        assertThat(exitCode.get()).isEqualTo(-1);
    }

    @Test
    void doesNotExitWhenProdProfileHasAllRequiredSensitiveConfig() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.datasource.username", "prod_user")
                .withProperty("spring.datasource.password", "prod_pwd")
                .withProperty("spring.data.redis.password", "redis-secret");
        environment.setActiveProfiles("prod");

        AtomicInteger exitCode = new AtomicInteger(-1);
        ProdConfigValidator validator = new ProdConfigValidator(environment, exitCode::set);

        validator.run(new DefaultApplicationArguments(new String[0]));

        assertThat(exitCode.get()).isEqualTo(-1);
    }
}
