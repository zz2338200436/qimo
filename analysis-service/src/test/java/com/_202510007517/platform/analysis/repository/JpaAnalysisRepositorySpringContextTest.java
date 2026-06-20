package com._202510007517.platform.analysis.repository;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class JpaAnalysisRepositorySpringContextTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(ScoreTrendJpaRepository.class, () -> mock(ScoreTrendJpaRepository.class))
            .withBean(KnowledgeMasteryJpaRepository.class, () -> mock(KnowledgeMasteryJpaRepository.class))
            .withUserConfiguration(RepositoryConfiguration.class);

    @Test
    void createsRepositoryBeanWithJpaRepositoryConstructor() {
        contextRunner.run(context ->
                assertThat(context).hasSingleBean(JpaAnalysisRepository.class));
    }

    @Configuration(proxyBeanMethods = false)
    @Import(JpaAnalysisRepository.class)
    static class RepositoryConfiguration {
    }
}
