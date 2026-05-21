package com._202510007517.platform.analysis.repository;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class JdbcAnalysisRepositorySpringContextTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(JdbcTemplate.class, () -> mock(JdbcTemplate.class))
            .withUserConfiguration(RepositoryConfiguration.class);

    @Test
    void createsRepositoryBeanWithJdbcTemplateConstructor() {
        contextRunner.run(context ->
                assertThat(context).hasSingleBean(JdbcAnalysisRepository.class));
    }

    @Configuration(proxyBeanMethods = false)
    @Import(JdbcAnalysisRepository.class)
    static class RepositoryConfiguration {
    }
}
