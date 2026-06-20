package com._202510007517.platform.agent.tool;

import com._202510007517.platform.agent.client.NotificationEdgeClient;
import com._202510007517.platform.agent.model.AgentIntent;
import com._202510007517.platform.common.web.ResponseResult;
import com._202510007517.platform.notification.api.dto.TeacherSendNotificationRequestDTO;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class TeacherSendNotificationTool implements AgentTool {

    private final NotificationEdgeClient notificationClient;

    public TeacherSendNotificationTool(NotificationEdgeClient notificationClient) {
        this.notificationClient = notificationClient;
    }

    @Override
    public AgentIntent intent() {
        return AgentIntent.SEND_NOTIFICATION;
    }

    @Override
    public Map<String, Object> execute(Long userId, String userRole, Map<String, Object> request) {
        Long studentId = longValue(request.get("studentId"));
        String title = stringValue(request.get("title"));
        String content = stringValue(request.get("content"));
        String type = stringValue(request.get("type"));
        if (studentId == null || title == null || content == null || type == null) {
            return Map.of("status", "VALIDATION_FAILED", "message", "缺少学生ID、标题、内容或通知类型");
        }

        TeacherSendNotificationRequestDTO payload = new TeacherSendNotificationRequestDTO();
        payload.setStudentId(studentId);
        payload.setTitle(title);
        payload.setContent(content);
        payload.setType(type);
        payload.setRelatedId(longValue(request.get("relatedId")));

        ResponseResult<Object> response = notificationClient.sendNotification(String.valueOf(userId), payload);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", response.isSuccess() ? "EXECUTED" : "FAILED");
        result.put("message", response.getMessage());
        result.put("notification", response.getData());
        return result;
    }

    private Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        return Long.valueOf(String.valueOf(value).trim());
    }

    private String stringValue(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        return String.valueOf(value).trim();
    }
}
