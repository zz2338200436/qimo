package com._202510007517.platform.agent.client;

import com._202510007517.platform.common.web.CommonTraceConstants;
import com._202510007517.platform.common.web.ResponseResult;
import com._202510007517.platform.notification.api.dto.TeacherSendNotificationRequestDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(contextId = "agentNotificationEdgeClient", name = "notification-service", path = "/api/notifications")
public interface NotificationEdgeClient {

    @GetMapping("/student")
    ResponseResult<Object> getStudentNotifications(
            @RequestHeader(CommonTraceConstants.USER_ID_HEADER) String userId,
            @RequestParam("page") Integer page,
            @RequestParam("size") Integer size,
            @RequestParam("filter") String filter);

    @GetMapping("/student/unread-count")
    ResponseResult<Integer> getUnreadCount(
            @RequestHeader(CommonTraceConstants.USER_ID_HEADER) String userId);

    @PutMapping("/read-all")
    ResponseResult<Void> markAllNotificationsAsRead(
            @RequestHeader(CommonTraceConstants.USER_ID_HEADER) String userId);

    @PutMapping("/{notificationId}/read")
    ResponseResult<Void> markNotificationAsRead(
            @RequestHeader(CommonTraceConstants.USER_ID_HEADER) String userId,
            @PathVariable("notificationId") Long notificationId);

    @DeleteMapping("/{notificationId}")
    ResponseResult<Void> deleteNotification(
            @RequestHeader(CommonTraceConstants.USER_ID_HEADER) String userId,
            @PathVariable("notificationId") Long notificationId);

    @DeleteMapping("/delete-all-read")
    ResponseResult<Void> deleteAllReadNotifications(
            @RequestHeader(CommonTraceConstants.USER_ID_HEADER) String userId);

    @PostMapping("/teacher/send")
    ResponseResult<Object> sendNotification(
            @RequestHeader(CommonTraceConstants.USER_ID_HEADER) String userId,
            @RequestBody TeacherSendNotificationRequestDTO request);

    @PostMapping("/teacher/send-batch")
    ResponseResult<Object> sendBatchNotifications(
            @RequestHeader(CommonTraceConstants.USER_ID_HEADER) String userId,
            @RequestBody List<TeacherSendNotificationRequestDTO> requests);
}
