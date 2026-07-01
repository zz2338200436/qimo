package com._202510007517.platform.agent.tool;

import com._202510007517.platform.agent.client.TeacherWarningEdgeClient;
import com._202510007517.platform.agent.model.AgentIntent;
import com._202510007517.platform.common.web.ResponseResult;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class EarlyWarningQueryTool implements AgentTool {

    private final TeacherWarningEdgeClient warningClient;

    public EarlyWarningQueryTool(TeacherWarningEdgeClient warningClient) {
        this.warningClient = warningClient;
    }

    @Override
    public AgentIntent intent() {
        return AgentIntent.QUERY_EARLY_WARNINGS;
    }

    @Override
    public Map<String, Object> execute(Long userId, String userRole, Map<String, Object> request) {
        Long classId = asLong(request.get("classId"));
        Long courseId = asLong(request.get("courseId"));
        String warningType = asString(request.get("warningType"));
        String status = asString(request.get("status"));

        ResponseResult<Map<String, Object>> statsResponse = warningClient.getWarningStats(
                String.valueOf(userId), classId, courseId);
        ResponseResult<Map<String, Object>> listResponse = warningClient.listWarnings(
                String.valueOf(userId), classId, courseId, warningType, status, 1, 10);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", statsResponse.isSuccess() && listResponse.isSuccess() ? "EXECUTED" : "FAILED");
        result.put("warningStats", statsResponse.getData());
        result.put("earlyWarnings", listResponse.getData());
        result.put("message", listResponse.getMessage());
        return result;
    }

    private static Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank() && !"all".equalsIgnoreCase(text)) {
            return Long.valueOf(text);
        }
        return null;
    }

    private static String asString(Object value) {
        if (value instanceof String text && !text.isBlank()) {
            return text;
        }
        return null;
    }
}
