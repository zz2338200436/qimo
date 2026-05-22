package com._202510007517.major_assignment.controller;

import com._202510007517.major_assignment.entity.Notification;
import com._202510007517.major_assignment.entity.dto.ResponseResult;
import com._202510007517.major_assignment.service.NotificationService;
import com._202510007517.major_assignment.utils.LogUtil;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@Validated
public class NotificationController extends BaseController {

    private static final Logger logger = LogUtil.getLogger(NotificationController.class);

    @Autowired
    private NotificationService notificationService;

    // 获取学生通知列表
    @GetMapping("/student")
    public ResponseResult<Map<String, Object>> getStudentNotifications(
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer size,
            @RequestParam(defaultValue = "all") String filter,
            HttpServletRequest requestContext) {
        try {
            Long studentId = getCurrentUserId(requestContext);
            Map<String, Object> result = notificationService.getNotificationsWithPagination(studentId, page, size, filter);
            return ResponseResult.success(result);
        } catch (Exception e) {
            logger.error("获取学生通知列表失败", e);
            return ResponseResult.failure("获取学生通知列表失败");
        }
    }
    
    // 测试用：直接根据studentId获取通知列表，不需要登录
    @GetMapping("/test/{studentId}")
    public ResponseResult<Map<String, Object>> getTestNotifications(
            @PathVariable Long studentId,
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer size,
            @RequestParam(defaultValue = "all") String filter) {
        try {
            Map<String, Object> result = notificationService.getNotificationsWithPagination(studentId, page, size, filter);
            return ResponseResult.success(result);
        } catch (Exception e) {
            logger.error("获取测试通知列表失败", e);
            return ResponseResult.failure("获取测试通知列表失败");
        }
    }

    // 获取所有通知（不分页）
    @GetMapping("/student/all")
    public ResponseResult getStudentAllNotifications(HttpServletRequest requestContext) {
        try {
            Long studentId = getCurrentUserId(requestContext);
            return ResponseResult.success(notificationService.getNotificationsByStudentId(studentId));
        } catch (Exception e) {
            logger.error("获取所有学生通知失败", e);
            return ResponseResult.failure("获取所有学生通知失败");
        }
    }

    // 获取未读通知数量
    @GetMapping("/student/unread-count")
    public ResponseResult<Integer> getUnreadNotificationCount(HttpServletRequest requestContext) {
        try {
            Long studentId = getCurrentUserId(requestContext);
            return ResponseResult.success(notificationService.getUnreadNotificationCountByStudentId(studentId));
        } catch (Exception e) {
            logger.error("获取未读通知数量失败", e);
            return ResponseResult.failure("获取未读通知数量失败");
        }
    }

    // 标记通知为已读
    @PutMapping("/{id}/read")
    public ResponseResult markAsRead(@PathVariable Long id, HttpServletRequest requestContext) {
        try {
            notificationService.markAsRead(id);
            return ResponseResult.success("通知已标记为已读");
        } catch (Exception e) {
            logger.error("标记通知为已读失败", e);
            return ResponseResult.failure("标记通知为已读失败");
        }
    }

    // 标记所有通知为已读
    @PutMapping("/read-all")
    public ResponseResult markAllAsRead(HttpServletRequest requestContext) {
        try {
            Long studentId = getCurrentUserId(requestContext);
            notificationService.markAllAsRead(studentId);
            return ResponseResult.success("所有通知已标记为已读");
        } catch (Exception e) {
            logger.error("标记所有通知为已读失败", e);
            return ResponseResult.failure("标记所有通知为已读失败");
        }
    }

    // 删除通知
    @DeleteMapping("/{id}")
    public ResponseResult deleteNotification(@PathVariable Long id, HttpServletRequest requestContext) {
        try {
            notificationService.delete(id);
            return ResponseResult.success("通知已删除");
        } catch (Exception e) {
            logger.error("删除通知失败", e);
            return ResponseResult.failure("删除通知失败");
        }
    }

    // 删除所有已读通知
    @DeleteMapping("/delete-all-read")
    public ResponseResult deleteAllRead(HttpServletRequest requestContext) {
        try {
            Long studentId = getCurrentUserId(requestContext);
            notificationService.deleteAllRead(studentId);
            return ResponseResult.success("所有已读通知已删除");
        } catch (Exception e) {
            logger.error("删除所有已读通知失败", e);
            return ResponseResult.failure("删除所有已读通知失败");
        }
    }
    
    // 老师发送通知
    @PostMapping("/teacher/send")
    public ResponseResult sendNotification(@RequestBody Notification notification, HttpServletRequest requestContext) {
        try {
            Long teacherId = getCurrentUserId(requestContext);
            notification.setTeacherId(teacherId);
            notificationService.create(notification);
            return ResponseResult.success("通知发送成功");
        } catch (Exception e) {
            logger.error("发送通知失败", e);
            return ResponseResult.failure("发送通知失败");
        }
    }
    
    // 老师批量发送通知
    @PostMapping("/teacher/send-batch")
    public ResponseResult sendBatchNotification(@RequestBody List<Notification> notifications, HttpServletRequest requestContext) {
        try {
            Long teacherId = getCurrentUserId(requestContext);
            notifications.forEach(notification -> {
                notification.setTeacherId(teacherId);
            });
            notificationService.createBatch(notifications);
            return ResponseResult.success("通知批量发送成功");
        } catch (Exception e) {
            logger.error("批量发送通知失败", e);
            return ResponseResult.failure("批量发送通知失败");
        }
    }
}
