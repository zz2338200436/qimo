package com._202510007517.platform.agent.tool;

import com._202510007517.platform.agent.client.TeacherKnowledgePointEdgeClient;
import com._202510007517.platform.agent.model.AgentIntent;
import com._202510007517.platform.common.web.ResponseResult;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class KnowledgePointQueryTool implements AgentTool {

    private final TeacherKnowledgePointEdgeClient knowledgePointClient;

    public KnowledgePointQueryTool(TeacherKnowledgePointEdgeClient knowledgePointClient) {
        this.knowledgePointClient = knowledgePointClient;
    }

    @Override
    public AgentIntent intent() {
        return AgentIntent.QUERY_KNOWLEDGE_POINTS;
    }

    @Override
    public Map<String, Object> execute(Long userId, String userRole, Map<String, Object> request) {
        Long courseId = asLong(request.get("courseId"));
        if (courseId == null) {
            return Map.of("status", "VALIDATION_FAILED", "message", "缺少课程ID。");
        }

        ResponseResult<List<Map<String, Object>>> response = knowledgePointClient.listKnowledgePoints(
                String.valueOf(userId),
                courseId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", response.isSuccess() ? "EXECUTED" : "FAILED");
        result.put("knowledgePoints", response.getData());
        result.put("message", response.getMessage());
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
}
