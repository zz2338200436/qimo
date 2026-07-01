package com._202510007517.platform.agent.tool;

import com._202510007517.platform.agent.client.AnalysisEdgeClient;
import com._202510007517.platform.agent.model.AgentIntent;
import com._202510007517.platform.common.web.ResponseResult;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class LearningStatsQueryTool implements AgentTool {

    private final AnalysisEdgeClient analysisClient;

    public LearningStatsQueryTool(AnalysisEdgeClient analysisClient) {
        this.analysisClient = analysisClient;
    }

    @Override
    public AgentIntent intent() {
        return AgentIntent.QUERY_STUDENT_STATS;
    }

    @Override
    public Map<String, Object> execute(Long userId, String userRole, Map<String, Object> request) {
        ResponseResult<Map<String, Object>> response = analysisClient.getLearningStats(String.valueOf(userId), null, null, null);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", response.isSuccess() ? "EXECUTED" : "FAILED");
        result.put("learningStats", response.getData());
        result.put("message", response.getMessage());
        return result;
    }
}
