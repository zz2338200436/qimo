package com._202510007517.platform.notification.repository;

import java.util.List;

public interface NotificationRepository {

    NotificationEntity save(NotificationEntity notification);

    int markAsRead(Long studentId, Long notificationId);

    int markAllAsRead(Long studentId);

    int deleteByIdAndStudentId(Long studentId, Long notificationId);

    int deleteAllReadByStudentId(Long studentId);

    List<NotificationEntity> findStudentNotifications(Long studentId, int offset, int limit, String filter);

    long countStudentNotifications(Long studentId, String filter);

    List<NotificationEntity> findAllStudentNotifications(Long studentId, String filter);

    int countUnreadByStudentId(Long studentId);
}
