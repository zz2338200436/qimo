package com._202510007517.platform.notification.service;

import com._202510007517.platform.common.event.idempotency.IdempotentEventHandler;
import com._202510007517.platform.events.assignment.AssignmentGradedEvent;
import com._202510007517.platform.notification.repository.NotificationEntity;
import com._202510007517.platform.notification.repository.NotificationRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AssignmentGradedNotificationHandler {

    static final String CONSUMER_NAME = "notification-service.assignment-graded";

    private final IdempotentEventHandler idempotentEventHandler;
    private final NotificationRepository notificationRepository;

    public AssignmentGradedNotificationHandler(IdempotentEventHandler idempotentEventHandler,
                                               NotificationRepository notificationRepository) {
        this.idempotentEventHandler = idempotentEventHandler;
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public boolean handle(AssignmentGradedEvent event) {
        return idempotentEventHandler.handle(
                event.eventId(),
                event.getClass().getSimpleName(),
                CONSUMER_NAME,
                () -> notificationRepository.save(buildNotification(event)));
    }

    private NotificationEntity buildNotification(AssignmentGradedEvent event) {
        NotificationEntity notification = new NotificationEntity();
        notification.setStudentId(event.payload().studentId());
        notification.setTeacherId(null);
        notification.setType("assignment");
        notification.setTitle("作业已批改");
        notification.setContent(buildContent(event));
        notification.setRelatedId(event.payload().assignmentId());
        notification.setRead(false);
        notification.setCreatedAt(event.occurredAt());
        return notification;
    }

    private String buildContent(AssignmentGradedEvent event) {
        StringBuilder builder = new StringBuilder("教师已完成本次作业批改");
        if (event.payload().score() != null) {
            builder.append("，得分：").append(event.payload().score()).append("分");
        }
        if (event.payload().teacherComment() != null && !event.payload().teacherComment().isBlank()) {
            builder.append("，评语：").append(event.payload().teacherComment());
        }
        return builder.toString();
    }
}
