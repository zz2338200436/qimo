package com._202510007517.platform.notification.service;

import com._202510007517.platform.common.event.idempotency.IdempotentEventHandler;
import com._202510007517.platform.common.event.idempotency.ProcessedEventEntity;
import com._202510007517.platform.common.event.idempotency.ProcessedEventRepository;
import com._202510007517.platform.events.EventAggregate;
import com._202510007517.platform.events.assignment.AssignmentGradedEvent;
import com._202510007517.platform.events.assignment.AssignmentGradedPayload;
import com._202510007517.platform.events.assignment.AssignmentSubmittedEvent;
import com._202510007517.platform.events.assignment.AssignmentSubmittedPayload;
import com._202510007517.platform.events.exam.ExamFinishedEvent;
import com._202510007517.platform.events.exam.ExamFinishedPayload;
import com._202510007517.platform.notification.config.AssignmentSubmittedConsumerConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com._202510007517.platform.notification.repository.NotificationEntity;
import com._202510007517.platform.notification.repository.NotificationRepository;
import com._202510007517.platform.events.warning.EarlyWarningRaisedEvent;
import com._202510007517.platform.events.warning.EarlyWarningRaisedPayload;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

class AssignmentSubmittedNotificationHandlerTest {

    @Test
    void handleCreatesOneStudentNotificationForNewEvent() {
        InMemoryProcessedEventRepository processedEvents = new InMemoryProcessedEventRepository();
        InMemoryNotificationRepository notifications = new InMemoryNotificationRepository();
        AssignmentSubmittedNotificationHandler handler = new AssignmentSubmittedNotificationHandler(
                new IdempotentEventHandler(processedEvents),
                notifications);

        handler.handle(event("evt-1", 42L, 2001L, 3001L));

        assertThat(notifications.saved).hasSize(1);
        NotificationEntity notification = notifications.saved.get(0);
        assertThat(notification.getStudentId()).isEqualTo(42L);
        assertThat(notification.getType()).isEqualTo("assignment");
        assertThat(notification.getTitle()).isEqualTo("作业提交成功");
        assertThat(notification.getContent()).contains("您提交的作业已收到");
        assertThat(notification.getRelatedId()).isEqualTo(2001L);
        assertThat(notification.getRead()).isFalse();
    }

    @Test
    void handleIgnoresDuplicateEventId() {
        InMemoryProcessedEventRepository processedEvents = new InMemoryProcessedEventRepository();
        InMemoryNotificationRepository notifications = new InMemoryNotificationRepository();
        AssignmentSubmittedNotificationHandler handler = new AssignmentSubmittedNotificationHandler(
                new IdempotentEventHandler(processedEvents),
                notifications);

        AssignmentSubmittedEvent event = event("evt-dup", 42L, 2001L, 3001L);
        handler.handle(event);
        handler.handle(event);

        assertThat(notifications.saved).hasSize(1);
    }

    @Test
    void examFinishedCreatesStudentExamNotification() {
        InMemoryProcessedEventRepository processedEvents = new InMemoryProcessedEventRepository();
        InMemoryNotificationRepository notifications = new InMemoryNotificationRepository();
        ExamFinishedNotificationHandler handler = new ExamFinishedNotificationHandler(
                new IdempotentEventHandler(processedEvents),
                notifications);

        handler.handle(new ExamFinishedEvent(
                "exam-finished-1",
                Instant.parse("2026-05-15T11:00:00Z"),
                new EventAggregate("exam_submission", "9001"),
                new ExamFinishedPayload(77L, 9001L, 42L, 5001L, 6001L, 85, 100,
                        Instant.parse("2026-05-15T10:59:00Z"))));

        assertThat(notifications.saved).hasSize(1);
        NotificationEntity notification = notifications.saved.get(0);
        assertThat(notification.getStudentId()).isEqualTo(42L);
        assertThat(notification.getType()).isEqualTo("exam");
        assertThat(notification.getTitle()).isEqualTo("考试已完成");
        assertThat(notification.getContent()).contains("成绩");
        assertThat(notification.getRelatedId()).isEqualTo(77L);
    }

    @Test
    void earlyWarningRaisedCreatesStudentWarningNotification() {
        InMemoryProcessedEventRepository processedEvents = new InMemoryProcessedEventRepository();
        InMemoryNotificationRepository notifications = new InMemoryNotificationRepository();
        EarlyWarningRaisedNotificationHandler handler = new EarlyWarningRaisedNotificationHandler(
                new IdempotentEventHandler(processedEvents),
                notifications);

        handler.handle(new EarlyWarningRaisedEvent(
                "warning-raised-1",
                Instant.parse("2026-05-15T11:10:00Z"),
                new EventAggregate("early_warning", "7001"),
                new EarlyWarningRaisedPayload(
                        7001L,
                        42L,
                        5001L,
                        "low_score",
                        "HIGH",
                        "学情预警",
                        "学生近期成绩低于及格线")));

        assertThat(notifications.saved).hasSize(1);
        NotificationEntity notification = notifications.saved.get(0);
        assertThat(notification.getStudentId()).isEqualTo(42L);
        assertThat(notification.getTeacherId()).isNull();
        assertThat(notification.getType()).isEqualTo("warning");
        assertThat(notification.getTitle()).isEqualTo("学情预警");
        assertThat(notification.getContent()).contains("低于及格线");
        assertThat(notification.getRelatedId()).isEqualTo(7001L);
    }

    @Test
    void assignmentGradedCreatesStudentAssignmentNotification() {
        InMemoryProcessedEventRepository processedEvents = new InMemoryProcessedEventRepository();
        InMemoryNotificationRepository notifications = new InMemoryNotificationRepository();
        AssignmentGradedNotificationHandler handler = new AssignmentGradedNotificationHandler(
                new IdempotentEventHandler(processedEvents),
                notifications);

        handler.handle(new AssignmentGradedEvent(
                "assignment-graded-1",
                Instant.parse("2026-05-16T15:10:00Z"),
                new EventAggregate("assignment_submission", "3001"),
                new AssignmentGradedPayload(
                        2001L,
                        3001L,
                        42L,
                        5001L,
                        96,
                        "graded in e2e smoke",
                        Instant.parse("2026-05-16T15:09:58Z"))));

        assertThat(notifications.saved).hasSize(1);
        NotificationEntity notification = notifications.saved.get(0);
        assertThat(notification.getStudentId()).isEqualTo(42L);
        assertThat(notification.getType()).isEqualTo("assignment");
        assertThat(notification.getTitle()).isEqualTo("作业已批改");
        assertThat(notification.getContent()).contains("96");
        assertThat(notification.getContent()).contains("graded in e2e smoke");
        assertThat(notification.getRelatedId()).isEqualTo(2001L);
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void assignmentSubmittedConsumerAcceptsRawJsonPayload() {
        InMemoryProcessedEventRepository processedEvents = new InMemoryProcessedEventRepository();
        InMemoryNotificationRepository notifications = new InMemoryNotificationRepository();
        AssignmentSubmittedNotificationHandler handler = new AssignmentSubmittedNotificationHandler(
                new IdempotentEventHandler(processedEvents),
                notifications);

        AssignmentSubmittedConsumerConfiguration configuration = new AssignmentSubmittedConsumerConfiguration();
        Consumer consumer = configuration.assignmentSubmittedConsumer(handler, new ObjectMapper().findAndRegisterModules());

        consumer.accept(assignmentSubmittedJson().getBytes(StandardCharsets.UTF_8));

        assertThat(notifications.saved).hasSize(1);
        assertThat(notifications.saved.get(0).getType()).isEqualTo("assignment");
        assertThat(notifications.saved.get(0).getStudentId()).isEqualTo(42L);
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void examFinishedConsumerAcceptsRawJsonPayload() {
        InMemoryProcessedEventRepository processedEvents = new InMemoryProcessedEventRepository();
        InMemoryNotificationRepository notifications = new InMemoryNotificationRepository();
        ExamFinishedNotificationHandler handler = new ExamFinishedNotificationHandler(
                new IdempotentEventHandler(processedEvents),
                notifications);

        AssignmentSubmittedConsumerConfiguration configuration = new AssignmentSubmittedConsumerConfiguration();
        Consumer consumer = configuration.examFinishedConsumer(handler, new ObjectMapper().findAndRegisterModules());

        consumer.accept(examFinishedJson().getBytes(StandardCharsets.UTF_8));

        assertThat(notifications.saved).hasSize(1);
        assertThat(notifications.saved.get(0).getType()).isEqualTo("exam");
        assertThat(notifications.saved.get(0).getStudentId()).isEqualTo(42L);
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void earlyWarningRaisedConsumerAcceptsRawJsonPayload() {
        InMemoryProcessedEventRepository processedEvents = new InMemoryProcessedEventRepository();
        InMemoryNotificationRepository notifications = new InMemoryNotificationRepository();
        EarlyWarningRaisedNotificationHandler handler = new EarlyWarningRaisedNotificationHandler(
                new IdempotentEventHandler(processedEvents),
                notifications);

        AssignmentSubmittedConsumerConfiguration configuration = new AssignmentSubmittedConsumerConfiguration();
        Consumer consumer = configuration.earlyWarningRaisedConsumer(handler, new ObjectMapper().findAndRegisterModules());

        consumer.accept(earlyWarningRaisedJson().getBytes(StandardCharsets.UTF_8));

        assertThat(notifications.saved).hasSize(1);
        assertThat(notifications.saved.get(0).getType()).isEqualTo("warning");
        assertThat(notifications.saved.get(0).getStudentId()).isEqualTo(42L);
        assertThat(notifications.saved.get(0).getRelatedId()).isEqualTo(7001L);
    }

    private AssignmentSubmittedEvent event(String eventId, Long studentId, Long assignmentId, Long submissionId) {
        return new AssignmentSubmittedEvent(
                eventId,
                Instant.parse("2026-05-15T10:15:30Z"),
                new EventAggregate("assignment", String.valueOf(assignmentId)),
                new AssignmentSubmittedPayload(
                        assignmentId,
                        submissionId,
                        studentId,
                        5001L,
                        6001L,
                        false,
                        Instant.parse("2026-05-15T10:15:00Z")));
    }

    private String assignmentSubmittedJson() {
        return """
                {"eventId":"assignment-consumer-1","occurredAt":"2026-05-15T10:15:30Z","aggregate":{"type":"assignment_submission","id":"2001"},"payload":{"assignmentId":2001,"submissionId":3001,"studentId":42,"courseId":5001,"classId":6001,"late":false,"submittedAt":"2026-05-15T10:15:00Z"}}
                """;
    }

    private String examFinishedJson() {
        return """
                {"eventId":"exam-consumer-1","occurredAt":"2026-05-15T11:00:00Z","aggregate":{"type":"exam_submission","id":"9001"},"payload":{"examId":77,"submissionId":9001,"studentId":42,"courseId":5001,"classId":6001,"score":85,"maxScore":100,"finishedAt":"2026-05-15T10:59:00Z"}}
                """;
    }

    private String earlyWarningRaisedJson() {
        return """
                {"eventId":"warning-consumer-1","occurredAt":"2026-05-15T11:10:00Z","aggregate":{"type":"early_warning","id":"7001"},"payload":{"warningId":7001,"studentId":42,"courseId":5001,"warningType":"low_score","level":"HIGH","title":"学情预警","reason":"学生近期成绩低于及格线"}}
                """;
    }

    private static final class InMemoryProcessedEventRepository implements ProcessedEventRepository {

        private final Set<String> seenKeys = new HashSet<>();

        @Override
        public boolean insertIfAbsent(ProcessedEventEntity event) {
            return seenKeys.add(event.eventId() + "::" + event.consumerName());
        }

        @Override
        public void delete(String eventId, String consumerName) {
            seenKeys.remove(eventId + "::" + consumerName);
        }
    }

    private static final class InMemoryNotificationRepository implements NotificationRepository {

        private final List<NotificationEntity> saved = new ArrayList<>();

        @Override
        public NotificationEntity save(NotificationEntity notification) {
            saved.add(notification);
            return notification;
        }

        @Override
        public int markAsRead(Long studentId, Long notificationId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int markAllAsRead(Long studentId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int deleteByIdAndStudentId(Long studentId, Long notificationId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int deleteAllReadByStudentId(Long studentId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<NotificationEntity> findStudentNotifications(Long studentId, int offset, int limit, String filter) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long countStudentNotifications(Long studentId, String filter) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<NotificationEntity> findAllStudentNotifications(Long studentId, String filter) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int countUnreadByStudentId(Long studentId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<NotificationEntity> findTeacherSentNotifications(Long teacherId, int offset, int limit, String filter) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long countTeacherSentNotifications(Long teacherId, String filter) {
            throw new UnsupportedOperationException();
        }
    }
}
