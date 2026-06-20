package com._202510007517.platform.agent.tool;

import com._202510007517.platform.agent.client.NotificationEdgeClient;
import com._202510007517.platform.agent.model.AgentIntent;
import com._202510007517.platform.common.web.ResponseResult;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class NotificationMarkReadTool implements AgentTool {

    private final NotificationEdgeClient notificationClient;

    public NotificationMarkReadTool(NotificationEdgeClient notificationClient) {
        this.notificationClient = notificationClient;
    }

    @Override
    public AgentIntent intent() {
        return AgentIntent.MARK_NOTIFICATION_READ;
    }

    @Override
    public Map<String, Object> execute(Long userId, String userRole, Map<String, Object> request) {
        Long notificationId = longValue(request.get("notificationId"));
        if (notificationId == null) {
            return Map.of("status", "VALIDATION_FAILED", "message", "缺少通知ID");
        }
        ResponseResult<Void> response = notificationClient.markNotificationAsRead(String.valueOf(userId), notificationId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", response.isSuccess() ? "EXECUTED" : "FAILED");
        result.put("message", response.getMessage());
        result.put("notificationId", notificationId);
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
}
