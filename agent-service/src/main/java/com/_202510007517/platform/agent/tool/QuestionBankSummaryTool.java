package com._202510007517.platform.agent.tool;

import com._202510007517.platform.agent.client.AiEdgeClient;
import com._202510007517.platform.agent.model.AgentIntent;
import com._202510007517.platform.common.web.ResponseResult;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class QuestionBankSummaryTool implements AgentTool {

    private final AiEdgeClient aiClient;

    public QuestionBankSummaryTool(AiEdgeClient aiClient) {
        this.aiClient = aiClient;
    }

    @Override
    public AgentIntent intent() {
        return AgentIntent.QUERY_QUESTION_BANK;
    }

    @Override
    public Map<String, Object> execute(Long userId, String userRole, Map<String, Object> request) {
        ResponseResult<Map<String, Object>> response = aiClient.questionBankSummary(
                String.valueOf(userId), userRole, userRole);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", response.isSuccess() ? "EXECUTED" : "FAILED");
        result.put("questionBank", response.getData());
        result.put("message", response.getMessage());
        return result;
    }
}
