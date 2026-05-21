package com._202510007517.platform.notification.config;

import com._202510007517.platform.events.assignment.AssignmentGradedEvent;
import com._202510007517.platform.events.assignment.AssignmentSubmittedEvent;
import com._202510007517.platform.events.exam.ExamFinishedEvent;
import com._202510007517.platform.events.warning.EarlyWarningRaisedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com._202510007517.platform.notification.service.AssignmentGradedNotificationHandler;
import com._202510007517.platform.notification.service.AssignmentSubmittedNotificationHandler;
import com._202510007517.platform.notification.service.EarlyWarningRaisedNotificationHandler;
import com._202510007517.platform.notification.service.ExamFinishedNotificationHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration(proxyBeanMethods = false)
public class AssignmentSubmittedConsumerConfiguration {

    @Bean
    public Consumer<Object> assignmentSubmittedConsumer(
            AssignmentSubmittedNotificationHandler handler,
            ObjectMapper objectMapper) {
        return new JsonBackedDomainEventConsumer<>(AssignmentSubmittedEvent.class, handler::handle, objectMapper);
    }

    @Bean
    public Consumer<Object> assignmentGradedConsumer(
            AssignmentGradedNotificationHandler handler,
            ObjectMapper objectMapper) {
        return new JsonBackedDomainEventConsumer<>(AssignmentGradedEvent.class, handler::handle, objectMapper);
    }

    @Bean
    public Consumer<Object> examFinishedConsumer(
            ExamFinishedNotificationHandler handler,
            ObjectMapper objectMapper) {
        return new JsonBackedDomainEventConsumer<>(ExamFinishedEvent.class, handler::handle, objectMapper);
    }

    @Bean
    public Consumer<Object> earlyWarningRaisedConsumer(
            EarlyWarningRaisedNotificationHandler handler,
            ObjectMapper objectMapper) {
        return new JsonBackedDomainEventConsumer<>(EarlyWarningRaisedEvent.class, handler::handle, objectMapper);
    }
}
