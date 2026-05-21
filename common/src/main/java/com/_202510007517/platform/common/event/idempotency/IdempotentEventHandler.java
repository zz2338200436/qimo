package com._202510007517.platform.common.event.idempotency;

import org.springframework.util.Assert;

import java.time.Clock;
import java.time.Instant;

public class IdempotentEventHandler {

    private final ProcessedEventRepository repository;
    private final Clock clock;

    public IdempotentEventHandler(ProcessedEventRepository repository) {
        this(repository, Clock.systemUTC());
    }

    public IdempotentEventHandler(ProcessedEventRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public boolean handle(String eventId, String eventType, String consumerName, Runnable action) {
        Assert.hasText(eventId, "eventId must not be blank");
        Assert.hasText(eventType, "eventType must not be blank");
        Assert.hasText(consumerName, "consumerName must not be blank");
        Assert.notNull(action, "action must not be null");

        ProcessedEventEntity event = new ProcessedEventEntity(null, eventId, eventType, consumerName,
                Instant.now(clock));
        if (!repository.insertIfAbsent(event)) {
            return false;
        }
        try {
            action.run();
            return true;
        } catch (RuntimeException | Error ex) {
            repository.delete(eventId, consumerName);
            throw ex;
        }
    }
}
