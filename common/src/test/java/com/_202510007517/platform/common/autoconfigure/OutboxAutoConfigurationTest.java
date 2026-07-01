package com._202510007517.platform.common.autoconfigure;

import com._202510007517.platform.common.event.idempotency.IdempotentEventHandler;
import com._202510007517.platform.common.event.idempotency.ProcessedEventRepository;
import com._202510007517.platform.common.event.outbox.OutboxEventPublisher;
import com._202510007517.platform.common.event.outbox.OutboxEventRepository;
import com._202510007517.platform.common.event.outbox.OutboxRelayJob;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class OutboxAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(OutboxAutoConfiguration.class))
            .withUserConfiguration(JdbcTemplateConfiguration.class);

    @Test
    void auto_configures_jdbc_repositories_and_idempotent_handler() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(OutboxEventRepository.class);
            assertThat(context).hasSingleBean(ProcessedEventRepository.class);
            assertThat(context).hasSingleBean(IdempotentEventHandler.class);
            assertThat(context).doesNotHaveBean(OutboxRelayJob.class);
        });
    }

    @Test
    void auto_configures_relay_job_when_enabled_and_publisher_exists() {
        contextRunner
                .withBean(OutboxEventPublisher.class, () -> message -> true)
                .withPropertyValues("platform.outbox.relay.enabled=true")
                .run(context -> assertThat(context).hasSingleBean(OutboxRelayJob.class));
    }

    @Test
    void auto_configures_repositories_after_boot_creates_jdbc_template() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        OutboxAutoConfiguration.class,
                        JdbcTemplateAutoConfiguration.class))
                .withBean(DataSource.class, () -> mock(DataSource.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(JdbcTemplate.class);
                    assertThat(context).hasSingleBean(OutboxEventRepository.class);
                    assertThat(context).hasSingleBean(ProcessedEventRepository.class);
                    assertThat(context).hasSingleBean(IdempotentEventHandler.class);
                });
    }

    @Test
    void backs_off_when_jdbc_template_is_missing() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(OutboxAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).doesNotHaveBean(OutboxEventRepository.class);
                    assertThat(context).doesNotHaveBean(ProcessedEventRepository.class);
                    assertThat(context).doesNotHaveBean(IdempotentEventHandler.class);
                    assertThat(context).doesNotHaveBean(OutboxRelayJob.class);
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class JdbcTemplateConfiguration {

        @Bean
        JdbcTemplate jdbcTemplate() {
            return mock(JdbcTemplate.class);
        }
    }
}
