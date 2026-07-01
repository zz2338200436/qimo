package com._202510007517.platform.analysis.config;

import com._202510007517.platform.analysis.service.AssignmentSubmittedAnalysisHandler;
import com._202510007517.platform.analysis.service.ExamFinishedAnalysisHandler;
import com._202510007517.platform.events.assignment.AssignmentSubmittedEvent;
import com._202510007517.platform.events.exam.ExamFinishedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration(proxyBeanMethods = false)
public class AnalysisConsumerConfiguration {

    @Bean
    public Consumer<Object> assignmentSubmittedAnalysisConsumer(
            AssignmentSubmittedAnalysisHandler handler,
            ObjectMapper objectMapper) {
        return new JsonBackedDomainEventConsumer<>(AssignmentSubmittedEvent.class, handler::handle, objectMapper);
    }

    @Bean
    public Consumer<Object> examFinishedAnalysisConsumer(
            ExamFinishedAnalysisHandler handler,
            ObjectMapper objectMapper) {
        return new JsonBackedDomainEventConsumer<>(ExamFinishedEvent.class, handler::handle, objectMapper);
    }
}
