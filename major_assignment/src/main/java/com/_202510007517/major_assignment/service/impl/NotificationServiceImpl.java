package com._202510007517.major_assignment.service.impl;

import com._202510007517.major_assignment.entity.Notification;
import com._202510007517.major_assignment.mapper.NotificationMapper;
import com._202510007517.major_assignment.service.NotificationService;
import com._202510007517.major_assignment.utils.PageUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private NotificationMapper notificationMapper;

    @Override
    public List<Notification> getNotificationsByStudentId(Long studentId) {
        List<Notification> notifications = notificationMapper.getNotificationsByStudentId(studentId);
        return notifications;
    }

    @Override
    public Integer getUnreadNotificationCountByStudentId(Long studentId) {
        return notificationMapper.getUnreadNotificationCountByStudentId(studentId);
    }

    @Override
    public Notification getNotificationById(Long id) {
        return notificationMapper.getNotificationById(id);
    }

    @Override
    public void create(Notification notification) {
        notification.setIsRead(false);
        notificationMapper.insert(notification);
    }

    @Override
    public void createBatch(List<Notification> notifications) {
        notifications.forEach(notification -> {
            notification.setIsRead(false);
        });
        // 这里可以优化为批量插入，目前Mapper中已经定义了insertBatch方法
        notifications.forEach(notificationMapper::insert);
    }

    @Override
    public void markAsRead(Long id) {
        notificationMapper.markAsRead(id);
    }

    @Override
    public void markAllAsRead(Long studentId) {
        notificationMapper.markAllAsRead(studentId);
    }

    @Override
    public void delete(Long id) {
        notificationMapper.delete(id);
    }

    @Override
    public void deleteAllRead(Long studentId) {
        notificationMapper.deleteAllRead(studentId);
    }

    @Override
    public Map<String, Object> getNotificationsWithPagination(Long studentId, Integer page, Integer size, String filter) {
        List<Notification> allNotifications = notificationMapper.getNotificationsByStudentId(studentId);
        
        // 筛选
        List<Notification> filteredNotifications = allNotifications;
        if (filter != null && !filter.isEmpty() && !"all".equals(filter)) {
            if ("unread".equals(filter)) {
                filteredNotifications = allNotifications.stream()
                        .filter(notification -> !notification.getIsRead())
                        .collect(Collectors.toList());
            } else {
                filteredNotifications = allNotifications.stream()
                        .filter(notification -> filter.equals(notification.getType()))
                        .collect(Collectors.toList());
            }
        }
        
        // 分页
        int total = filteredNotifications.size();
        PageUtils.PageWindow window = PageUtils.resolvePageWindow(
                page == null ? 1 : page,
                size == null ? PageUtils.DEFAULT_PAGE_SIZE : size,
                total);
        List<Notification> paginatedNotifications = PageUtils.paginate(filteredNotifications, window.page(), window.size());
        
        Map<String, Object> result = new HashMap<>();
        result.put("notifications", paginatedNotifications);
        result.put("total", total);
        result.put("page", window.page());
        result.put("size", window.size());
        result.put("totalPages", window.totalPages());
        
        return result;
    }
}
