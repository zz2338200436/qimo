package com._202510007517.platform.common.event.idempotency;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdempotentEventHandlerTest {

    @Test
    void handle_runs_action_and_records_event_once() {
        InMemoryProcessedEventRepository repository = new InMemoryProcessedEventRepository();
        IdempotentEventHandler handler = new IdempotentEventHandler(repository);
        AtomicInteger calls = new AtomicInteger();

        boolean handled = handler.handle("evt-1", "AssignmentSubmittedEvent", "analysis-service", calls::incrementAndGet);

        assertTrue(handled);
        assertEquals(1, calls.get());
        assertTrue(repository.contains("evt-1", "analysis-service"));
    }

    @Test
    void handle_skips_duplicate_event_without_running_action() {
        InMemoryProcessedEventRepository repository = new InMemoryProcessedEventRepository();
        repository.insertIfAbsent(new ProcessedEventEntity(null, "evt-1", "AssignmentSubmittedEvent",
                "analysis-service", Instant.EPOCH));
        IdempotentEventHandler handler = new IdempotentEventHandler(repository);
        AtomicInteger calls = new AtomicInteger();

        boolean handled = handler.handle("evt-1", "AssignmentSubmittedEvent", "analysis-service", calls::incrementAndGet);

        assertFalse(handled);
        assertEquals(0, calls.get());
    }

    @Test
    void handle_removes_processing_marker_when_action_fails() {
        InMemoryProcessedEventRepository repository = new InMemoryProcessedEventRepository();
        IdempotentEventHandler handler = new IdempotentEventHandler(repository);

        assertThrows(IllegalStateException.class, () -> handler.handle("evt-1", "AssignmentSubmittedEvent",
                "analysis-service", () -> {
                    throw new IllegalStateException("boom");
                }));

        assertFalse(repository.contains("evt-1", "analysis-service"));
    }

    @Test
    void handle_is_idempotent_for_generated_duplicate_sequences() {
        Random random = new Random(26L);

        for (int example = 0; example < 1000; example++) {
            InMemoryProcessedEventRepository repository = new InMemoryProcessedEventRepository();
            IdempotentEventHandler handler = new IdempotentEventHandler(repository);
            int uniqueEventConsumers = 1 + random.nextInt(8);
            List<EventAttempt> attempts = new ArrayList<>();
            Map<String, AtomicInteger> sideEffectsByKey = new LinkedHashMap<>();

            for (int index = 0; index < uniqueEventConsumers; index++) {
                EventAttempt attempt = new EventAttempt(
                        "event-" + example + "-" + (index / 2),
                        switch (index % 3) {
                            case 0 -> "AssignmentSubmittedEvent";
                            case 1 -> "ExamFinishedEvent";
                            default -> "EarlyWarningRaisedEvent";
                        },
                        "consumer-" + (index % 2));
                sideEffectsByKey.put(attempt.key(), new AtomicInteger());

                int duplicateCount = 1 + random.nextInt(20);
                for (int duplicate = 0; duplicate < duplicateCount; duplicate++) {
                    attempts.add(attempt);
                }
            }

            Collections.shuffle(attempts, random);

            int handledCount = 0;
            for (EventAttempt attempt : attempts) {
                AtomicInteger sideEffects = sideEffectsByKey.get(attempt.key());
                boolean handled = handler.handle(attempt.eventId(), attempt.eventType(), attempt.consumerName(),
                        sideEffects::incrementAndGet);
                if (handled) {
                    handledCount++;
                }
            }

            assertEquals(uniqueEventConsumers, handledCount);
            assertEquals(uniqueEventConsumers, repository.size());
            sideEffectsByKey.values().forEach(sideEffects -> assertEquals(1, sideEffects.get()));
        }
    }

    private record EventAttempt(String eventId, String eventType, String consumerName) {
        private String key() {
            return eventId + "::" + consumerName;
        }
    }

    private static final class InMemoryProcessedEventRepository implements ProcessedEventRepository {
        private final Set<String> records = new LinkedHashSet<>();

        @Override
        public boolean insertIfAbsent(ProcessedEventEntity event) {
            return records.add(key(event.eventId(), event.consumerName()));
        }

        @Override
        public void delete(String eventId, String consumerName) {
            records.remove(key(eventId, consumerName));
        }

        boolean contains(String eventId, String consumerName) {
            return records.contains(key(eventId, consumerName));
        }

        int size() {
            return records.size();
        }

        private String key(String eventId, String consumerName) {
            return eventId + "::" + consumerName;
        }
    }
}
