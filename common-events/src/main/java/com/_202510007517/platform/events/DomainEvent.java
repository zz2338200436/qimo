package com._202510007517.platform.events;

import java.time.Instant;

public interface DomainEvent<P> {

    String eventId();

    Instant occurredAt();

    EventAggregate aggregate();

    P payload();

    default String eventType() {
        return getClass().getSimpleName();
    }
}
