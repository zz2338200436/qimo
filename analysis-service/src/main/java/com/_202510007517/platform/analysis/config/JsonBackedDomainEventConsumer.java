package com._202510007517.platform.analysis.config;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

final class JsonBackedDomainEventConsumer<T> implements Consumer<Object> {

    private final Class<T> eventType;
    private final Consumer<T> delegate;
    private final ObjectMapper objectMapper;

    JsonBackedDomainEventConsumer(Class<T> eventType, Consumer<T> delegate, ObjectMapper objectMapper) {
        this.eventType = eventType;
        this.delegate = delegate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void accept(Object payload) {
        delegate.accept(convert(payload));
    }

    private T convert(Object payload) {
        if (eventType.isInstance(payload)) {
            return eventType.cast(payload);
        }
        if (payload instanceof byte[] bytes) {
            return readValue(bytes);
        }
        if (payload instanceof String text) {
            return readValue(text.getBytes(StandardCharsets.UTF_8));
        }
        throw new IllegalArgumentException("Unsupported event payload type: " + payload.getClass().getName());
    }

    private T readValue(byte[] payload) {
        try {
            return objectMapper.readValue(payload, eventType);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Failed to deserialize event payload as " + eventType.getSimpleName(), ex);
        }
    }
}
