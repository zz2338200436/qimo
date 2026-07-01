package com._202510007517.platform.agent.tool;

import com._202510007517.platform.agent.client.StudentExamEdgeClient;
import com._202510007517.platform.agent.model.AgentIntent;
import com._202510007517.platform.common.web.ResponseResult;
import com._202510007517.platform.exam.api.dto.StudentScoreListItemDTO;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ScoreQueryTool implements AgentTool {

    private final StudentExamEdgeClient examClient;

    public ScoreQueryTool(StudentExamEdgeClient examClient) {
        this.examClient = examClient;
    }

    @Override
    public AgentIntent intent() {
        return AgentIntent.QUERY_SCORES;
    }

    @Override
    public Map<String, Object> execute(Long userId, String userRole, Map<String, Object> request) {
        ResponseResult<List<StudentScoreListItemDTO>> response = examClient.listScores(String.valueOf(userId));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "EXECUTED");
        result.put("scores", response.getData());
        return result;
    }
}
