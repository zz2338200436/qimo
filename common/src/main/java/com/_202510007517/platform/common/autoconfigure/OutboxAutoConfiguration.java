package com._202510007517.platform.common.autoconfigure;

import com._202510007517.platform.common.event.idempotency.IdempotentEventHandler;
import com._202510007517.platform.common.event.idempotency.JdbcProcessedEventRepository;
import com._202510007517.platform.common.event.idempotency.ProcessedEventRepository;
import com._202510007517.platform.common.event.outbox.JdbcOutboxEventRepository;
import com._202510007517.platform.common.event.outbox.OutboxEventPublisher;
import com._202510007517.platform.common.event.outbox.OutboxEventRepository;
import com._202510007517.platform.common.event.outbox.OutboxRelayJob;
import com._202510007517.platform.common.event.outbox.OutboxRelayProperties;
import com._202510007517.platform.common.event.outbox.StreamBridgeOutboxEventPublisher;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;

@AutoConfiguration(after = JdbcTemplateAutoConfiguration.class)
@EnableScheduling
@EnableConfigurationProperties(OutboxRelayProperties.class)
@ConditionalOnClass(JdbcTemplate.class)
public class OutboxAutoConfiguration {

    @Bean
    @ConditionalOnBean(JdbcTemplate.class)
    @ConditionalOnMissingBean
    public OutboxEventRepository outboxEventRepository(JdbcTemplate jdbcTemplate) {
        return new JdbcOutboxEventRepository(jdbcTemplate);
    }

    @Bean
    @ConditionalOnBean(JdbcTemplate.class)
    @ConditionalOnMissingBean
    public ProcessedEventRepository processedEventRepository(JdbcTemplate jdbcTemplate) {
        return new JdbcProcessedEventRepository(jdbcTemplate);
    }

    @Bean
    @ConditionalOnBean(ProcessedEventRepository.class)
    @ConditionalOnMissingBean
    public IdempotentEventHandler idempotentEventHandler(ProcessedEventRepository repository) {
        return new IdempotentEventHandler(repository);
    }

    @Bean
    @ConditionalOnClass(StreamBridge.class)
    @ConditionalOnMissingBean
    public OutboxEventPublisher streamBridgeOutboxEventPublisher(ObjectProvider<StreamBridge> streamBridgeProvider) {
        return new StreamBridgeOutboxEventPublisher(streamBridgeProvider);
    }

    @Bean
    @ConditionalOnBean({OutboxEventRepository.class, OutboxEventPublisher.class})
    @ConditionalOnProperty(prefix = "platform.outbox.relay", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean
    public OutboxRelayJob outboxRelayJob(OutboxEventRepository repository,
                                         OutboxEventPublisher publisher,
                                         OutboxRelayProperties properties,
                                         ObjectProvider<Clock> clockProvider) {
        return new OutboxRelayJob(repository, publisher, properties, clockProvider.getIfAvailable(Clock::systemUTC));
    }
}
