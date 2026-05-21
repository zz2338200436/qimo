package com._202510007517.platform.assignment.config;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.stream.binder.ProducerProperties;
import org.springframework.cloud.stream.binding.NewDestinationBindingCallback;
import org.springframework.cloud.stream.binder.rabbit.properties.RabbitProducerProperties;
import org.springframework.messaging.MessageChannel;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AssignmentSubmittedStreamConfigurationTest {

    @Test
    void assignmentSubmittedDestinationGetsInspectableRabbitQueueConfiguration() throws Exception {
        NewDestinationBindingCallback<RabbitProducerProperties> callback = loadCallback();
        ProducerProperties producer = new ProducerProperties();
        RabbitProducerProperties rabbit = new RabbitProducerProperties();

        callback.configure("assignment.submitted", mock(MessageChannel.class), producer, rabbit);

        assertThat(producer.getRequiredGroups()).containsExactly("assignment-submitted-inspector");
        assertThat(rabbit.isAutoBindDlq()).isTrue();
    }

    @Test
    void unrelatedDestinationKeepsDefaultRabbitConfiguration() throws Exception {
        NewDestinationBindingCallback<RabbitProducerProperties> callback = loadCallback();
        ProducerProperties producer = new ProducerProperties();
        RabbitProducerProperties rabbit = new RabbitProducerProperties();

        callback.configure("other.destination", mock(MessageChannel.class), producer, rabbit);

        assertThat(producer.getRequiredGroups()).isNullOrEmpty();
        assertThat(rabbit.isAutoBindDlq()).isFalse();
    }

    @SuppressWarnings("unchecked")
    private NewDestinationBindingCallback<RabbitProducerProperties> loadCallback() throws Exception {
        Class<?> configurationType = Class.forName(
                "com._202510007517.platform.assignment.config.AssignmentSubmittedStreamConfiguration");
        Object configuration = configurationType.getDeclaredConstructor().newInstance();
        Method callbackFactory = configurationType.getDeclaredMethod("assignmentSubmittedBindingCallback");
        return (NewDestinationBindingCallback<RabbitProducerProperties>) callbackFactory.invoke(configuration);
    }
}
