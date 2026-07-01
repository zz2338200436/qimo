package com._202510007517.platform.assignment.config;

import org.springframework.cloud.stream.binder.ProducerProperties;
import org.springframework.cloud.stream.binder.rabbit.properties.RabbitProducerProperties;
import org.springframework.cloud.stream.binding.NewDestinationBindingCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.MessageChannel;

@Configuration(proxyBeanMethods = false)
public class AssignmentSubmittedStreamConfiguration {

    static final String ASSIGNMENT_SUBMITTED_BINDING = "assignment.submitted";
    static final String ASSIGNMENT_SUBMITTED_INSPECTOR_GROUP = "assignment-submitted-inspector";

    @Bean
    public NewDestinationBindingCallback<RabbitProducerProperties> assignmentSubmittedBindingCallback() {
        return (String destinationName,
                MessageChannel channel,
                ProducerProperties producer,
                RabbitProducerProperties rabbit) -> {
            if (!ASSIGNMENT_SUBMITTED_BINDING.equals(destinationName)) {
                return;
            }
            producer.setRequiredGroups(ASSIGNMENT_SUBMITTED_INSPECTOR_GROUP);
            rabbit.setAutoBindDlq(true);
        };
    }
}
