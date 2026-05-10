package com._202510007517.major_assignment.service;

import com._202510007517.major_assignment.entity.Notification;

import java.util.List;
import java.util.Map;

public interface NotificationService {
    // 根据学生ID获取所有通知
    List<Notification> getNotificationsByStudentId(Long studentId);

    // 根据学生ID获取未读通知数量
    Integer getUnreadNotificationCountByStudentId(Long studentId);

    // 根据ID获取通知
    Notification getNotificationById(Long id);

    // 创建通知
    void create(Notification notification);

    // 批量创建通知
    void createBatch(List<Notification> notifications);

    // 将通知标记为已读
    void markAsRead(Long id);

    // 将所有通知标记为已读
    void markAllAsRead(Long studentId);

    // 删除通知
    void delete(Long id);

    // 删除所有已读通知
    void deleteAllRead(Long studentId);

    // 获取通知列表（支持分页和筛选）
    Map<String, Object> getNotificationsWithPagination(Long studentId, Integer page, Integer size, String filter);
}