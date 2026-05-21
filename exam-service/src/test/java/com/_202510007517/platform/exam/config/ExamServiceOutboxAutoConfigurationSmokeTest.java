package com._202510007517.platform.exam.config;

import com._202510007517.platform.common.event.idempotency.IdempotentEventHandler;
import com._202510007517.platform.common.event.idempotency.ProcessedEventRepository;
import com._202510007517.platform.common.event.outbox.OutboxEventPublisher;
import com._202510007517.platform.common.event.outbox.OutboxEventRepository;
import com._202510007517.platform.common.event.outbox.OutboxRelayJob;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ExamServiceOutboxAutoConfigurationSmokeTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                    com._202510007517.platform.common.autoconfigure.OutboxAutoConfiguration.class))
            .withBean(JdbcTemplate.class, () -> mock(JdbcTemplate.class));

    @Test
    void examServiceOutboxAutoConfigurationProvidesOutboxInfrastructureWithoutRelayJobByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(OutboxEventRepository.class);
            assertThat(context).hasSingleBean(ProcessedEventRepository.class);
            assertThat(context).hasSingleBean(IdempotentEventHandler.class);
            assertThat(context).hasSingleBean(OutboxEventPublisher.class);
            assertThat(context).doesNotHaveBean(OutboxRelayJob.class);
        });
    }

    @Test
    void examServiceOutboxAutoConfigurationProvidesRelayJobWhenEnabled() {
        contextRunner
                .withPropertyValues("platform.outbox.relay.enabled=true")
                .run(context -> assertThat(context).hasSingleBean(OutboxRelayJob.class));
    }
}
