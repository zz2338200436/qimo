package com._202510007517.platform.agent.tool;

import com._202510007517.platform.agent.client.TeacherAnalysisEdgeClient;
import com._202510007517.platform.agent.model.AgentIntent;
import com._202510007517.platform.analysis.api.dto.ScoreTrendDTO;
import com._202510007517.platform.common.web.ResponseResult;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ScoreTrendQueryTool implements AgentTool {

    private final TeacherAnalysisEdgeClient analysisClient;

    public ScoreTrendQueryTool(TeacherAnalysisEdgeClient analysisClient) {
        this.analysisClient = analysisClient;
    }

    @Override
    public AgentIntent intent() {
        return AgentIntent.QUERY_SCORE_TREND;
    }

    @Override
    public Map<String, Object> execute(Long userId, String userRole, Map<String, Object> request) {
        ResponseResult<List<ScoreTrendDTO>> response = analysisClient.listScoreTrend(
                String.valueOf(userId),
                asLong(request.get("classId")),
                asLong(request.get("courseId")),
                asString(request.get("timeRange")));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", response.isSuccess() ? "EXECUTED" : "FAILED");
        result.put("scoreTrend", response.getData());
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

    private static String asString(Object value) {
        if (value instanceof String text && !text.isBlank()) {
            return text;
        }
        return null;
    }
}
