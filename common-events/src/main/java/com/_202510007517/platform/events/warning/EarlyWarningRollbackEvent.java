package com._202510007517.platform.events.warning;

import com._202510007517.platform.events.DomainEvent;
import com._202510007517.platform.events.EventAggregate;

import java.time.Instant;
import java.util.Objects;

public record EarlyWarningRollbackEvent(
        String eventId,
        Instant occurredAt,
        EventAggregate aggregate,
        EarlyWarningRollbackPayload payload) implements DomainEvent<EarlyWarningRollbackPayload> {

    public EarlyWarningRollbackEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(aggregate, "aggregate must not be null");
        Objects.requireNonNull(payload, "payload must not be null");
    }
}
