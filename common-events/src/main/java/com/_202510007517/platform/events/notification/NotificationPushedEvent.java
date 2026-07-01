package com._202510007517.platform.events.notification;

import com._202510007517.platform.events.DomainEvent;
import com._202510007517.platform.events.EventAggregate;

import java.time.Instant;
import java.util.Objects;

public record NotificationPushedEvent(
        String eventId,
        Instant occurredAt,
        EventAggregate aggregate,
        NotificationPushedPayload payload) implements DomainEvent<NotificationPushedPayload> {

    public NotificationPushedEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(aggregate, "aggregate must not be null");
        Objects.requireNonNull(payload, "payload must not be null");
    }
}
