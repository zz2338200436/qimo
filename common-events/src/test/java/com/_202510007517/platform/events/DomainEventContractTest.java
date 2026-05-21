package com._202510007517.platform.events;

import com._202510007517.platform.events.assignment.AssignmentSubmittedEvent;
import com._202510007517.platform.events.assignment.AssignmentSubmittedPayload;
import com._202510007517.platform.events.exam.ExamFinishedEvent;
import com._202510007517.platform.events.exam.ExamFinishedPayload;
import com._202510007517.platform.events.notification.NotificationPushedEvent;
import com._202510007517.platform.events.notification.NotificationPushedPayload;
import com._202510007517.platform.events.warning.EarlyWarningRaisedEvent;
import com._202510007517.platform.events.warning.EarlyWarningRaisedPayload;
import com._202510007517.platform.events.warning.EarlyWarningRollbackEvent;
import com._202510007517.platform.events.warning.EarlyWarningRollbackPayload;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DomainEventContractTest {

    private static final Instant OCCURRED_AT = Instant.parse("2026-05-13T00:00:00Z");

    @Test
    void core_events_share_event_envelope_fields() {
        List<DomainEvent<?>> events = List.of(
                new AssignmentSubmittedEvent("evt-assignment-1", OCCURRED_AT,
                        new EventAggregate("assignment", "assignment-42"),
                        new AssignmentSubmittedPayload(42L, 7L, 1001L, 5L, 9L, false, OCCURRED_AT)),
                new ExamFinishedEvent("evt-exam-1", OCCURRED_AT,
                        new EventAggregate("exam", "exam-8"),
                        new ExamFinishedPayload(8L, 18L, 1001L, 5L, 9L, 92, 100, OCCURRED_AT)),
                new EarlyWarningRaisedEvent("evt-warning-1", OCCURRED_AT,
                        new EventAggregate("early_warning", "warning-3"),
                        new EarlyWarningRaisedPayload(3L, 1001L, 5L, "LOW_SCORE", "HIGH", "成绩预警", "考试低于阈值")),
                new EarlyWarningRollbackEvent("evt-warning-rollback-1", OCCURRED_AT,
                        new EventAggregate("early_warning", "warning-3"),
                        new EarlyWarningRollbackPayload(3L, "evt-exam-1", "考试提交回滚")),
                new NotificationPushedEvent("evt-notification-1", OCCURRED_AT,
                        new EventAggregate("notification", "notification-88"),
                        new NotificationPushedPayload(88L, 1001L, "IN_APP", "新通知", "exam", 8L, OCCURRED_AT))
        );

        for (DomainEvent<?> event : events) {
            assertNotNull(event.eventId());
            assertEquals(OCCURRED_AT, event.occurredAt());
            assertNotNull(event.aggregate());
            assertNotNull(event.payload());
            assertEquals(event.getClass().getSimpleName(), event.eventType());
        }
    }
}
