package com._202510007517.platform.assignment.config;

import com._202510007517.platform.common.event.idempotency.IdempotentEventHandler;
import com._202510007517.platform.common.event.idempotency.ProcessedEventRepository;
import com._202510007517.platform.common.event.outbox.OutboxEventRepository;
import com._202510007517.platform.common.event.outbox.OutboxRelayJob;
import com._202510007517.platform.common.event.outbox.OutboxEventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AssignmentServiceOutboxAutoConfigurationSmokeTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(AssignmentEventInfrastructureConfiguration.class)
            .withBean(JdbcTemplate.class, () -> mock(JdbcTemplate.class));

    @Test
    void assignmentEventInfrastructureProvidesOutboxPublisherWithoutEagerStreamBridgeBean() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(OutboxEventRepository.class);
            assertThat(context).hasSingleBean(ProcessedEventRepository.class);
            assertThat(context).hasSingleBean(IdempotentEventHandler.class);
            assertThat(context).hasSingleBean(OutboxEventPublisher.class);
            assertThat(context).doesNotHaveBean(OutboxRelayJob.class);
        });
    }

    @Test
    void assignmentEventInfrastructureProvidesRelayJobWhenEnabled() {
        contextRunner
                .withPropertyValues("platform.outbox.relay.enabled=true")
                .run(context -> assertThat(context).hasSingleBean(OutboxRelayJob.class));
    }
}
