package com._202510007517.platform.agent.tool;

import com._202510007517.platform.agent.client.AnalysisEdgeClient;
import com._202510007517.platform.agent.model.AgentIntent;
import com._202510007517.platform.common.web.ResponseResult;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class StudyTimeDistributionQueryTool implements AgentTool {

    private final AnalysisEdgeClient analysisClient;

    public StudyTimeDistributionQueryTool(AnalysisEdgeClient analysisClient) {
        this.analysisClient = analysisClient;
    }

    @Override
    public AgentIntent intent() {
        return AgentIntent.QUERY_STUDY_TIME_DISTRIBUTION;
    }

    @Override
    public Map<String, Object> execute(Long userId, String userRole, Map<String, Object> request) {
        ResponseResult<List<Map<String, Object>>> response = analysisClient.listStudyTimeDistribution(
                String.valueOf(userId),
                "daily",
                null,
                null,
                null);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", response.isSuccess() ? "EXECUTED" : "FAILED");
        result.put("studyTimeDistribution", response.getData());
        result.put("message", response.getMessage());
        return result;
    }
}
