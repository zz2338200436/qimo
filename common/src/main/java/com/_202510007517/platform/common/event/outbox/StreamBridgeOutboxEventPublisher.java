package com._202510007517.platform.common.event.outbox;

import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.MimeTypeUtils;

public class StreamBridgeOutboxEventPublisher implements OutboxEventPublisher {

    private final ObjectProvider<StreamBridge> streamBridgeProvider;

    public StreamBridgeOutboxEventPublisher(ObjectProvider<StreamBridge> streamBridgeProvider) {
        this.streamBridgeProvider = streamBridgeProvider;
    }

    @Override
    public boolean publish(OutboxEventMessage message) {
        StreamBridge streamBridge = streamBridgeProvider.getIfAvailable();
        if (streamBridge == null) {
            throw new IllegalStateException("StreamBridge bean is not available");
        }
        Message<String> springMessage = MessageBuilder.withPayload(message.payload())
                .setHeader("eventId", message.eventId())
                .setHeader("eventType", message.eventType())
                .setHeader("aggregateType", message.aggregateType())
                .setHeader("aggregateId", message.aggregateId())
                .setHeader("eventHeaders", message.headers())
                .setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON)
                .build();
        return streamBridge.send(message.bindingName(), springMessage);
    }
}
