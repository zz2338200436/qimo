package com._202510007517.platform.common.event.outbox;

import org.springframework.scheduling.annotation.Scheduled;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

public class OutboxRelayJob {

    private final OutboxEventRepository repository;
    private final OutboxEventPublisher publisher;
    private final OutboxRelayProperties properties;
    private final Clock clock;

    public OutboxRelayJob(OutboxEventRepository repository,
                          OutboxEventPublisher publisher,
                          OutboxRelayProperties properties,
                          Clock clock) {
        this.repository = repository;
        this.publisher = publisher;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${platform.outbox.relay.fixed-delay:5000}")
    public void run() {
        relayOnce();
    }

    public int relayOnce() {
        Instant now = clock.instant();
        List<OutboxEventEntity> events = repository.findDuePendingEvents(properties.getBatchSize(), now);
        int published = 0;
        for (OutboxEventEntity event : events) {
            if (publish(event, now)) {
                published++;
            }
        }
        return published;
    }

    private boolean publish(OutboxEventEntity event, Instant now) {
        try {
            OutboxEventMessage message = OutboxEventMessage.from(event, properties.getBindingName());
            if (publisher.publish(message)) {
                repository.markPublished(event.id(), now);
                return true;
            }
            markFailed(event, now, "StreamBridge returned false");
            return false;
        } catch (RuntimeException ex) {
            markFailed(event, now, abbreviate(ex.getMessage()));
            return false;
        }
    }

    private void markFailed(OutboxEventEntity event, Instant now, String reason) {
        int nextRetryCount = event.retryCount() + 1;
        Instant nextRetryAt = now.plus(properties.getInitialBackoff().multipliedBy(Math.max(1, nextRetryCount)));
        repository.markFailed(event.id(), nextRetryCount, nextRetryAt, reason, properties.getMaxRetries());
    }

    private String abbreviate(String value) {
        if (value == null || value.isBlank()) {
            return "unknown publish error";
        }
        return value.length() <= 1000 ? value : value.substring(0, 1000);
    }
}
