package com._202510007517.platform.agent.tool;

import com._202510007517.platform.agent.client.AiEdgeClient;
import com._202510007517.platform.agent.model.AgentIntent;
import com._202510007517.platform.ai.api.dto.GenerateQuestionsRequestDTO;
import com._202510007517.platform.common.web.ResponseResult;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class GenerateQuestionsTool implements AgentTool {

    private final AiEdgeClient aiClient;

    public GenerateQuestionsTool(AiEdgeClient aiClient) {
        this.aiClient = aiClient;
    }

    @Override
    public AgentIntent intent() {
        return AgentIntent.GENERATE_QUESTIONS;
    }

    @Override
    public Map<String, Object> execute(Long userId, String userRole, Map<String, Object> request) {
        GenerateQuestionsRequestDTO dto = new GenerateQuestionsRequestDTO();
        dto.setTopic(String.valueOf(request.getOrDefault("topic", request.getOrDefault("courseName", "综合练习"))));
        dto.setCount(asInteger(request.get("count"), 5));
        dto.setDifficulty(String.valueOf(request.getOrDefault("difficulty", "中等")));
        ResponseResult<Map<String, Object>> response = aiClient.generateQuestions(
                String.valueOf(userId), userRole, userRole, dto);
        return aiResult(response);
    }

    private static Integer asInteger(Object value, Integer fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Integer.parseInt(text);
        }
        return fallback;
    }

    static Map<String, Object> aiResult(ResponseResult<Map<String, Object>> response) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", response.isSuccess() ? "EXECUTED" : "FAILED");
        result.put("aiResult", response.getData());
        result.put("message", response.getMessage());
        return result;
    }
}
