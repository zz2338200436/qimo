package com._202510007517.platform.common.event.outbox;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OutboxRelayJobTest {

    private static final Instant NOW = Instant.parse("2026-05-13T00:00:00Z");

    @Test
    void relay_once_publishes_pending_event_and_marks_it_published() {
        InMemoryOutboxEventRepository repository = new InMemoryOutboxEventRepository(List.of(sampleEvent()));
        CapturingOutboxEventPublisher publisher = new CapturingOutboxEventPublisher(true);
        OutboxRelayJob job = new OutboxRelayJob(repository, publisher, new OutboxRelayProperties(), fixedClock());

        int published = job.relayOnce();

        assertEquals(1, published);
        assertEquals("domainEvents-out-0", publisher.message.bindingName());
        assertEquals("evt-1", publisher.message.eventId());
        assertEquals(1L, repository.publishedEventId);
        assertEquals(NOW, repository.publishedAt);
    }

    @Test
    void relay_once_marks_event_failed_when_publisher_returns_false() {
        InMemoryOutboxEventRepository repository = new InMemoryOutboxEventRepository(List.of(sampleEvent()));
        CapturingOutboxEventPublisher publisher = new CapturingOutboxEventPublisher(false);
        OutboxRelayProperties properties = new OutboxRelayProperties();
        properties.setMaxRetries(3);
        OutboxRelayJob job = new OutboxRelayJob(repository, publisher, properties, fixedClock());

        int published = job.relayOnce();

        assertEquals(0, published);
        assertEquals(1L, repository.failedEventId);
        assertEquals(1, repository.failedRetryCount);
    }

    private OutboxEventEntity sampleEvent() {
        return new OutboxEventEntity(1L, "evt-1", "Assignment", "assignment-42",
                "AssignmentSubmittedEvent", "domainEvents-out-0", "{\"score\":95}",
                "{\"traceId\":\"trace-1\"}", 0, 0, NOW, null, NOW, NOW, null);
    }

    private Clock fixedClock() {
        return Clock.fixed(NOW, ZoneOffset.UTC);
    }

    private static final class InMemoryOutboxEventRepository implements OutboxEventRepository {
        private final List<OutboxEventEntity> events;
        private Long publishedEventId;
        private Instant publishedAt;
        private Long failedEventId;
        private Integer failedRetryCount;

        private InMemoryOutboxEventRepository(List<OutboxEventEntity> events) {
            this.events = new ArrayList<>(events);
        }

        @Override
        public List<OutboxEventEntity> findDuePendingEvents(int limit, Instant now) {
            return events.stream().limit(limit).toList();
        }

        @Override
        public void save(OutboxEventEntity event) {
            events.add(event);
        }

        @Override
        public void markPublished(long id, Instant publishedAt) {
            this.publishedEventId = id;
            this.publishedAt = publishedAt;
        }

        @Override
        public void markFailed(long id, int retryCount, Instant nextRetryAt, String lastError, int maxRetries) {
            this.failedEventId = id;
            this.failedRetryCount = retryCount;
        }
    }

    private static final class CapturingOutboxEventPublisher implements OutboxEventPublisher {
        private final boolean result;
        private OutboxEventMessage message;

        private CapturingOutboxEventPublisher(boolean result) {
            this.result = result;
        }

        @Override
        public boolean publish(OutboxEventMessage message) {
            this.message = message;
            return result;
        }
    }
}
