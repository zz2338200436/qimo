package com._202510007517.platform.agent.tool;

import com._202510007517.platform.agent.client.AiEdgeClient;
import com._202510007517.platform.agent.model.AgentIntent;
import com._202510007517.platform.ai.api.dto.LearningSuggestionRequestDTO;
import com._202510007517.platform.common.web.ResponseResult;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class GenerateLearningSuggestionsTool implements AgentTool {

    private final AiEdgeClient aiClient;

    public GenerateLearningSuggestionsTool(AiEdgeClient aiClient) {
        this.aiClient = aiClient;
    }

    @Override
    public AgentIntent intent() {
        return AgentIntent.GENERATE_LEARNING_SUGGESTIONS;
    }

    @Override
    public Map<String, Object> execute(Long userId, String userRole, Map<String, Object> request) {
        LearningSuggestionRequestDTO dto = new LearningSuggestionRequestDTO();
        dto.setStudentId(userId);
        ResponseResult<Map<String, Object>> response = aiClient.learningSuggestions(
                String.valueOf(userId), userRole, userRole, dto);
        return GenerateQuestionsTool.aiResult(response);
    }
}
