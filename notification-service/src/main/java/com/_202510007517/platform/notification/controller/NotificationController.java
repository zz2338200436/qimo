package com._202510007517.platform.notification.controller;

import com._202510007517.platform.common.web.CommonTraceConstants;
import com._202510007517.platform.common.web.ResponseResult;
import com._202510007517.platform.notification.api.dto.TeacherSendNotificationRequestDTO;
import com._202510007517.platform.notification.repository.NotificationEntity;
import com._202510007517.platform.notification.service.NotificationCommandService;
import com._202510007517.platform.notification.service.NotificationQueryService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@Validated
public class NotificationController {

    private final NotificationQueryService notificationQueryService;
    private final NotificationCommandService notificationCommandService;

    public NotificationController(NotificationQueryService notificationQueryService,
                                  NotificationCommandService notificationCommandService) {
        this.notificationQueryService = notificationQueryService;
        this.notificationCommandService = notificationCommandService;
    }

    @GetMapping("/student")
    public ResponseResult<?> getStudentNotifications(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestParam(value = "studentId", required = false) Long studentId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "all") String filter) {
        return ResponseResult.success(
                notificationQueryService.getStudentNotifications(resolveUserId(userIdHeader, studentId, "学生"), page, size, filter));
    }

    @GetMapping("/student/all")
    public ResponseResult<?> getStudentAllNotifications(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestParam(value = "studentId", required = false) Long studentId,
            @RequestParam(defaultValue = "all") String filter) {
        return ResponseResult.success(
                notificationQueryService.getAllStudentNotifications(resolveUserId(userIdHeader, studentId, "学生"), filter));
    }

    @GetMapping("/student/unread-count")
    public ResponseResult<Integer> getUnreadCount(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestParam(value = "studentId", required = false) Long studentId) {
        return ResponseResult.success(notificationQueryService.getUnreadCount(resolveUserId(userIdHeader, studentId, "学生")));
    }

    @PutMapping("/{notificationId}/read")
    public ResponseResult<Void> markNotificationAsRead(
            @PathVariable Long notificationId,
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestParam(value = "studentId", required = false) Long studentId) {
        notificationCommandService.markNotificationAsRead(resolveUserId(userIdHeader, studentId, "学生"), notificationId);
        return ResponseResult.success();
    }

    @PutMapping("/read-all")
    public ResponseResult<Void> markAllNotificationsAsRead(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestParam(value = "studentId", required = false) Long studentId) {
        notificationCommandService.markAllNotificationsAsRead(resolveUserId(userIdHeader, studentId, "学生"));
        return ResponseResult.success();
    }

    @DeleteMapping("/{notificationId}")
    public ResponseResult<Void> deleteNotification(
            @PathVariable Long notificationId,
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestParam(value = "studentId", required = false) Long studentId) {
        notificationCommandService.deleteNotification(resolveUserId(userIdHeader, studentId, "学生"), notificationId);
        return ResponseResult.success();
    }

    @DeleteMapping("/delete-all-read")
    public ResponseResult<Void> deleteAllReadNotifications(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestParam(value = "studentId", required = false) Long studentId) {
        notificationCommandService.deleteAllReadNotifications(resolveUserId(userIdHeader, studentId, "学生"));
        return ResponseResult.success();
    }

    @PostMapping("/teacher/send")
    public ResponseResult<NotificationEntity> sendNotification(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestParam(value = "teacherId", required = false) Long teacherId,
            @RequestBody @Valid TeacherSendNotificationRequestDTO request) {
        return ResponseResult.created(
                notificationCommandService.sendNotification(resolveUserId(userIdHeader, teacherId, "教师"), request));
    }

    @PostMapping("/teacher/send-batch")
    public ResponseResult<List<NotificationEntity>> sendBatchNotification(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestParam(value = "teacherId", required = false) Long teacherId,
            @RequestBody @Valid List<TeacherSendNotificationRequestDTO> requests) {
        return ResponseResult.created(
                notificationCommandService.sendBatchNotifications(resolveUserId(userIdHeader, teacherId, "教师"), requests));
    }

    static Long resolveUserId(String userIdHeader, Long fallbackUserId, String roleLabel) {
        if (userIdHeader != null && !userIdHeader.isBlank()) {
            return Long.valueOf(userIdHeader);
        }
        if (fallbackUserId != null) {
            return fallbackUserId;
        }
        throw new IllegalArgumentException("缺少" + roleLabel + "身份");
    }
}
