package com._202510007517.platform.notification.service;

import com._202510007517.platform.common.event.idempotency.IdempotentEventHandler;
import com._202510007517.platform.events.exam.ExamFinishedEvent;
import com._202510007517.platform.notification.repository.NotificationEntity;
import com._202510007517.platform.notification.repository.NotificationRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ExamFinishedNotificationHandler {

    static final String CONSUMER_NAME = "notification-service.exam-finished";

    private final IdempotentEventHandler idempotentEventHandler;
    private final NotificationRepository notificationRepository;

    public ExamFinishedNotificationHandler(IdempotentEventHandler idempotentEventHandler,
                                           NotificationRepository notificationRepository) {
        this.idempotentEventHandler = idempotentEventHandler;
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public boolean handle(ExamFinishedEvent event) {
        return idempotentEventHandler.handle(
                event.eventId(),
                event.getClass().getSimpleName(),
                CONSUMER_NAME,
                () -> notificationRepository.save(buildNotification(event)));
    }

    private NotificationEntity buildNotification(ExamFinishedEvent event) {
        NotificationEntity notification = new NotificationEntity();
        notification.setStudentId(event.payload().studentId());
        notification.setTeacherId(null);
        notification.setType("exam");
        notification.setTitle("考试已完成");
        notification.setContent("您已完成本次考试，当前成绩：" + event.payload().score() + "/" + event.payload().maxScore());
        notification.setRelatedId(event.payload().examId());
        notification.setRead(false);
        notification.setCreatedAt(event.occurredAt());
        return notification;
    }
}
