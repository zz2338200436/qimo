package com._202510007517.platform.notification.config;

import com._202510007517.platform.common.event.idempotency.IdempotentEventHandler;
import com._202510007517.platform.common.event.idempotency.JdbcProcessedEventRepository;
import com._202510007517.platform.common.event.idempotency.ProcessedEventRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration(proxyBeanMethods = false)
public class NotificationEventInfrastructureConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ProcessedEventRepository processedEventRepository(JdbcTemplate jdbcTemplate) {
        return new JdbcProcessedEventRepository(jdbcTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    public IdempotentEventHandler idempotentEventHandler(ProcessedEventRepository repository) {
        return new IdempotentEventHandler(repository);
    }
}
