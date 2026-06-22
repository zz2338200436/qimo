package com._202510007517.platform.agent.tool;

import com._202510007517.platform.agent.model.AgentIntent;
import com._202510007517.platform.agent.questionbank.QuestionRagService;
import com._202510007517.platform.common.web.ResponseResult;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class GenerateQuestionsTool implements AgentTool {

    private final QuestionRagService questionRagService;

    public GenerateQuestionsTool(QuestionRagService questionRagService) {
        this.questionRagService = questionRagService;
    }

    @Override
    public AgentIntent intent() {
        return AgentIntent.GENERATE_QUESTIONS;
    }

    @Override
    public Map<String, Object> execute(Long userId, String userRole, Map<String, Object> request) {
        String topic = String.valueOf(request.getOrDefault("topic", request.getOrDefault("courseName", "综合练习")));
        Integer count = asInteger(request.get("count"), 5);
        String difficulty = String.valueOf(request.getOrDefault("difficulty", "中等"));
        String type = request.get("type") == null ? null : String.valueOf(request.get("type"));

        Map<String, Object> payload = questionRagService.generateQuestions(userRole, topic, difficulty, count, type);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "EXECUTED");
        result.putAll(payload);
        result.put("aiResult", payload);
        result.put("message", payload.get("message"));
        return result;
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
        Map<String, Object> payload = response.getData();
        if (payload != null && !payload.isEmpty()) {
            result.putAll(payload);
            result.put("aiResult", payload);
        }
        String message = response.getMessage();
        Object payloadMessage = payload == null ? null : payload.get("message");
        if (payloadMessage instanceof String text && !text.isBlank()) {
            message = text;
        }
        result.put("message", message);
        return result;
    }
}
