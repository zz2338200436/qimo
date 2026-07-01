package com._202510007517.platform.agent.tool;

import com._202510007517.platform.agent.client.NotificationEdgeClient;
import com._202510007517.platform.agent.model.AgentIntent;
import com._202510007517.platform.common.web.ResponseResult;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class NotificationQueryTool implements AgentTool {

    private final NotificationEdgeClient notificationClient;

    public NotificationQueryTool(NotificationEdgeClient notificationClient) {
        this.notificationClient = notificationClient;
    }

    @Override
    public AgentIntent intent() {
        return AgentIntent.QUERY_NOTIFICATIONS;
    }

    @Override
    public Map<String, Object> execute(Long userId, String userRole, Map<String, Object> request) {
        ResponseResult<Object> response = notificationClient.getStudentNotifications(String.valueOf(userId), 1, 10, "all");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", status(response));
        result.put("notifications", response.getData());
        result.put("message", response.getMessage());
        return result;
    }

    private static String status(ResponseResult<?> response) {
        return response.isSuccess() ? "EXECUTED" : "FAILED";
    }
}
