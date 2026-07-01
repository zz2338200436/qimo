package com._202510007517.platform.notification.service;

import com._202510007517.platform.notification.repository.NotificationEntity;
import com._202510007517.platform.notification.repository.NotificationRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class NotificationQueryService {

    private final NotificationRepository notificationRepository;

    public NotificationQueryService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public Map<String, Object> getStudentNotifications(Long studentId, int page, int size, String filter) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);
        int offset = (safePage - 1) * safeSize;
        Map<String, Object> result = new LinkedHashMap<>();
        long total = notificationRepository.countStudentNotifications(studentId, filter);
        result.put("notifications", notificationRepository.findStudentNotifications(studentId, offset, safeSize, filter));
        result.put("total", total);
        result.put("page", safePage);
        result.put("size", safeSize);
        result.put("totalPages", (int) Math.ceil((double) total / safeSize));
        return result;
    }

    public List<NotificationEntity> getAllStudentNotifications(Long studentId, String filter) {
        return notificationRepository.findAllStudentNotifications(studentId, filter);
    }

    public int getUnreadCount(Long studentId) {
        return notificationRepository.countUnreadByStudentId(studentId);
    }

    public Map<String, Object> getTeacherSentNotifications(Long teacherId, int page, int size, String filter) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);
        int offset = (safePage - 1) * safeSize;
        Map<String, Object> result = new LinkedHashMap<>();
        long total = notificationRepository.countTeacherSentNotifications(teacherId, filter);
        result.put("notifications", notificationRepository.findTeacherSentNotifications(teacherId, offset, safeSize, filter));
        result.put("total", total);
        result.put("page", safePage);
        result.put("size", safeSize);
        result.put("totalPages", (int) Math.ceil((double) total / safeSize));
        return result;
    }
}
