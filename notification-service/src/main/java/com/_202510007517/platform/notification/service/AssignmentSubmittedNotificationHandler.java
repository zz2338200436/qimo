package com._202510007517.platform.notification.service;

import com._202510007517.platform.common.event.idempotency.IdempotentEventHandler;
import com._202510007517.platform.events.assignment.AssignmentSubmittedEvent;
import com._202510007517.platform.notification.repository.NotificationEntity;
import com._202510007517.platform.notification.repository.NotificationRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AssignmentSubmittedNotificationHandler {

    static final String CONSUMER_NAME = "notification-service.assignment-submitted";

    private final IdempotentEventHandler idempotentEventHandler;
    private final NotificationRepository notificationRepository;

    public AssignmentSubmittedNotificationHandler(IdempotentEventHandler idempotentEventHandler,
                                                  NotificationRepository notificationRepository) {
        this.idempotentEventHandler = idempotentEventHandler;
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public boolean handle(AssignmentSubmittedEvent event) {
        return idempotentEventHandler.handle(
                event.eventId(),
                event.getClass().getSimpleName(),
                CONSUMER_NAME,
                () -> notificationRepository.save(buildNotification(event)));
    }

    private NotificationEntity buildNotification(AssignmentSubmittedEvent event) {
        NotificationEntity notification = new NotificationEntity();
        notification.setStudentId(event.payload().studentId());
        notification.setTeacherId(null);
        notification.setType("assignment");
        notification.setTitle("作业提交成功");
        notification.setContent("您提交的作业已收到，请等待教师批改");
        notification.setRelatedId(event.payload().assignmentId());
        notification.setRead(false);
        notification.setCreatedAt(event.occurredAt());
        return notification;
    }
}
