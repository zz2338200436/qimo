package com._202510007517.platform.notification.repository;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Repository
public class JpaNotificationRepository implements NotificationRepository {

    private final NotificationJpaRepository jpaRepository;

    public JpaNotificationRepository(NotificationJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public NotificationEntity save(NotificationEntity notification) {
        if (notification.getRead() == null) {
            notification.setRead(false);
        }
        if (notification.getCreatedAt() == null) {
            notification.setCreatedAt(Instant.now());
        }
        return jpaRepository.save(notification);
    }

    @Override
    @Transactional
    public int markAsRead(Long studentId, Long notificationId) {
        return jpaRepository.markAsRead(studentId, notificationId);
    }

    @Override
    @Transactional
    public int markAllAsRead(Long studentId) {
        return jpaRepository.markAllAsRead(studentId);
    }

    @Override
    @Transactional
    public int deleteByIdAndStudentId(Long studentId, Long notificationId) {
        return jpaRepository.deleteByIdAndStudentId(notificationId, studentId);
    }

    @Override
    @Transactional
    public int deleteAllReadByStudentId(Long studentId) {
        return jpaRepository.deleteByStudentIdAndReadTrue(studentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationEntity> findStudentNotifications(Long studentId, int offset, int limit, String filter) {
        int safeOffset = Math.max(offset, 0);
        int safeLimit = Math.max(limit, 1);
        Pageable pageable = PageRequest.of(safeOffset / safeLimit, safeLimit);
        return jpaRepository.findStudentNotifications(studentId, normalizeFilter(filter), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public long countStudentNotifications(Long studentId, String filter) {
        return jpaRepository.countStudentNotifications(studentId, normalizeFilter(filter));
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationEntity> findAllStudentNotifications(Long studentId, String filter) {
        return jpaRepository.findAllStudentNotifications(studentId, normalizeFilter(filter));
    }

    @Override
    @Transactional(readOnly = true)
    public int countUnreadByStudentId(Long studentId) {
        return jpaRepository.countByStudentIdAndReadFalse(studentId);
    }

    private String normalizeFilter(String filter) {
        return StringUtils.hasText(filter) ? filter.trim().toLowerCase(Locale.ROOT) : "all";
    }
}
