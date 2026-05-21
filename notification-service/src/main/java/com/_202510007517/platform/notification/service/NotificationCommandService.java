package com._202510007517.platform.notification.service;

import com._202510007517.platform.notification.api.dto.TeacherSendNotificationRequestDTO;
import com._202510007517.platform.notification.repository.NotificationEntity;
import com._202510007517.platform.notification.repository.NotificationRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class NotificationCommandService {

    private final NotificationRepository notificationRepository;

    public NotificationCommandService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public NotificationEntity sendNotification(Long teacherId, TeacherSendNotificationRequestDTO request) {
        return notificationRepository.save(buildTeacherNotification(teacherId, request));
    }

    public List<NotificationEntity> sendBatchNotifications(Long teacherId, List<TeacherSendNotificationRequestDTO> requests) {
        return requests.stream()
                .map(request -> notificationRepository.save(buildTeacherNotification(teacherId, request)))
                .toList();
    }

    private NotificationEntity buildTeacherNotification(Long teacherId, TeacherSendNotificationRequestDTO request) {
        NotificationEntity entity = new NotificationEntity();
        entity.setTeacherId(teacherId);
        entity.setStudentId(request.getStudentId());
        entity.setType(request.getType());
        entity.setTitle(request.getTitle());
        entity.setContent(request.getContent());
        entity.setRelatedId(request.getRelatedId());
        entity.setRead(Boolean.TRUE.equals(request.getIsRead()));
        entity.setCreatedAt(Instant.now());
        return entity;
    }

    public void markNotificationAsRead(Long studentId, Long notificationId) {
        notificationRepository.markAsRead(studentId, notificationId);
    }

    public void markAllNotificationsAsRead(Long studentId) {
        notificationRepository.markAllAsRead(studentId);
    }

    public void deleteNotification(Long studentId, Long notificationId) {
        notificationRepository.deleteByIdAndStudentId(studentId, notificationId);
    }

    public void deleteAllReadNotifications(Long studentId) {
        notificationRepository.deleteAllReadByStudentId(studentId);
    }
}
