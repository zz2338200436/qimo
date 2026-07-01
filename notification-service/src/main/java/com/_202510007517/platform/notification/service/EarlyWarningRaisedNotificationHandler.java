package com._202510007517.platform.notification.service;

import com._202510007517.platform.common.event.idempotency.IdempotentEventHandler;
import com._202510007517.platform.events.warning.EarlyWarningRaisedEvent;
import com._202510007517.platform.notification.repository.NotificationEntity;
import com._202510007517.platform.notification.repository.NotificationRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class EarlyWarningRaisedNotificationHandler {

    static final String CONSUMER_NAME = "notification-service.early-warning-raised";

    private final IdempotentEventHandler idempotentEventHandler;
    private final NotificationRepository notificationRepository;

    public EarlyWarningRaisedNotificationHandler(IdempotentEventHandler idempotentEventHandler,
                                                 NotificationRepository notificationRepository) {
        this.idempotentEventHandler = idempotentEventHandler;
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public boolean handle(EarlyWarningRaisedEvent event) {
        return idempotentEventHandler.handle(
                event.eventId(),
                event.getClass().getSimpleName(),
                CONSUMER_NAME,
                () -> notificationRepository.save(buildNotification(event)));
    }

    private NotificationEntity buildNotification(EarlyWarningRaisedEvent event) {
        NotificationEntity notification = new NotificationEntity();
        notification.setStudentId(event.payload().studentId());
        notification.setTeacherId(null);
        notification.setType("warning");
        notification.setTitle(event.payload().title());
        notification.setContent(event.payload().reason());
        notification.setRelatedId(event.payload().warningId());
        notification.setRead(false);
        notification.setCreatedAt(event.occurredAt());
        return notification;
    }
}
