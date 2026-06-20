package com._202510007517.platform.agent.tool;

import com._202510007517.platform.agent.client.NotificationEdgeClient;
import com._202510007517.platform.agent.model.AgentIntent;
import com._202510007517.platform.common.web.ResponseResult;
import com._202510007517.platform.notification.api.dto.TeacherSendNotificationRequestDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class TeacherSendBatchNotificationTool implements AgentTool {

    static final int MAX_RECIPIENTS = 100;

    private final NotificationEdgeClient notificationClient;

    public TeacherSendBatchNotificationTool(NotificationEdgeClient notificationClient) {
        this.notificationClient = notificationClient;
    }

    @Override
    public AgentIntent intent() {
        return AgentIntent.SEND_BATCH_NOTIFICATION;
    }

    @Override
    public Map<String, Object> execute(Long userId, String userRole, Map<String, Object> request) {
        List<Long> studentIds = longListValue(request.get("studentIds"));
        String title = stringValue(request.get("title"));
        String content = stringValue(request.get("content"));
        String type = stringValue(request.get("type"));
        if (studentIds.isEmpty() || title == null || content == null || type == null) {
            return Map.of("status", "VALIDATION_FAILED", "message", "缺少学生ID列表、标题、内容或通知类型");
        }
        if (studentIds.size() > MAX_RECIPIENTS) {
            return Map.of("status", "VALIDATION_FAILED", "message", "批量通知一次最多支持100名学生");
        }

        Long relatedId = longValue(request.get("relatedId"));
        List<TeacherSendNotificationRequestDTO> payload = studentIds.stream()
                .map(studentId -> buildRequest(studentId, title, content, type, relatedId))
                .toList();

        ResponseResult<Object> response = notificationClient.sendBatchNotifications(String.valueOf(userId), payload);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", response.isSuccess() ? "EXECUTED" : "FAILED");
        result.put("message", response.getMessage());
        result.put("notificationCount", payload.size());
        result.put("notifications", response.getData());
        return result;
    }

    private TeacherSendNotificationRequestDTO buildRequest(Long studentId, String title, String content, String type,
                                                           Long relatedId) {
        TeacherSendNotificationRequestDTO payload = new TeacherSendNotificationRequestDTO();
        payload.setStudentId(studentId);
        payload.setTitle(title);
        payload.setContent(content);
        payload.setType(type);
        payload.setRelatedId(relatedId);
        return payload;
    }

    private List<Long> longListValue(Object value) {
        if (value instanceof Iterable<?> iterable) {
            List<Long> result = new ArrayList<>();
            for (Object item : iterable) {
                Long parsed = longValue(item);
                if (parsed != null && !result.contains(parsed)) {
                    result.add(parsed);
                }
            }
            return result;
        }
        Long single = longValue(value);
        return single == null ? List.of() : List.of(single);
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
