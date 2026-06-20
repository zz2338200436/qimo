package com._202510007517.platform.agent.tool;

import com._202510007517.platform.agent.client.NotificationEdgeClient;
import com._202510007517.platform.agent.model.AgentIntent;
import com._202510007517.platform.common.web.ResponseResult;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class NotificationMarkAllReadTool implements AgentTool {

    private final NotificationEdgeClient notificationClient;

    public NotificationMarkAllReadTool(NotificationEdgeClient notificationClient) {
        this.notificationClient = notificationClient;
    }

    @Override
    public AgentIntent intent() {
        return AgentIntent.MARK_ALL_NOTIFICATIONS_READ;
    }

    @Override
    public Map<String, Object> execute(Long userId, String userRole, Map<String, Object> request) {
        ResponseResult<Void> response = notificationClient.markAllNotificationsAsRead(String.valueOf(userId));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", response.isSuccess() ? "EXECUTED" : "FAILED");
        result.put("message", response.getMessage());
        return result;
    }
}
