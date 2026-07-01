package com._202510007517.platform.common.event.outbox;

public record OutboxEventMessage(
        String bindingName,
        String eventId,
        String aggregateType,
        String aggregateId,
        String eventType,
        String payload,
        String headers) {

    public static OutboxEventMessage from(OutboxEventEntity event, String fallbackBindingName) {
        String bindingName = hasText(event.bindingName()) ? event.bindingName() : fallbackBindingName;
        return new OutboxEventMessage(bindingName, event.eventId(), event.aggregateType(), event.aggregateId(),
                event.eventType(), event.payload(), event.headers());
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
