package com._202510007517.platform.agent.tool;

import com._202510007517.platform.agent.client.AiEdgeClient;
import com._202510007517.platform.agent.model.AgentIntent;
import com._202510007517.platform.agent.questionbank.QuestionRagService;
import com._202510007517.platform.common.web.ResponseResult;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class GenerateQuestionsTool implements AgentTool {

    private final AiEdgeClient aiClient;
    private final QuestionRagService questionRagService;

    public GenerateQuestionsTool(AiEdgeClient aiClient, QuestionRagService questionRagService) {
        this.aiClient = aiClient;
        this.questionRagService = questionRagService;
    }

    @Override
    public AgentIntent intent() {
        return AgentIntent.GENERATE_QUESTIONS;
    }

    @Override
    public Map<String, Object> execute(Long userId, String userRole, Map<String, Object> request) {
        String topic = normalizeRequestedTopic(readOptionalText(request, "topic", "courseName"));
        Integer count = asInteger(request.get("count"), 5);
        String difficulty = String.valueOf(request.getOrDefault("difficulty", "中等"));
        String type = request.get("type") == null ? null : String.valueOf(request.get("type"));

        Map<String, Object> payload = questionRagService.generateQuestions(userRole, topic, difficulty, count, type);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "EXECUTED");
        result.putAll(payload);
        result.put("message", payload.get("message"));
        return result;
    }

    private static String readOptionalText(Map<String, Object> request, String... keys) {
        for (String key : keys) {
            Object value = request.get(key);
            if (value == null) {
                continue;
            }
            String text = String.valueOf(value).trim();
            if (!text.isEmpty()) {
                return text;
            }
        }
        return null;
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

    private static String normalizeRequestedTopic(String topic) {
        if (topic == null) {
            return null;
        }
        String normalized = topic.trim()
                .replace("现在", "")
                .replace("目前", "")
                .replace("这", "")
                .replace("那", "")
                .replace("这些", "")
                .replace("那些", "")
                .replace("什么", "")
                .replace("哪些", "")
                .replace("有什么", "")
                .replace("有哪些", "")
                .replace("多少", "")
                .replace("几道", "")
                .replace("几题", "")
                .replace("基于刚才内容", "")
                .replace("基于当前内容", "")
                .replace("基于上述内容", "")
                .replace("基于前面内容", "")
                .replace("基于刚才", "")
                .replace("基于当前", "")
                .replace("基于上述", "")
                .replace("基于前面", "")
                .replace("刚才内容", "")
                .replace("当前内容", "")
                .replace("上述内容", "")
                .replace("前面内容", "")
                .replace("刚才", "")
                .replace("当前", "")
                .replace("上述", "")
                .replace("前面", "")
                .replace("基于", "")
                .replace("随机题目", "")
                .replace("随机", "")
                .replace("内容", "")
                .replace("详情", "")
                .replace("情况", "")
                .replace("课堂练习题", "")
                .replace("课堂练习", "")
                .replace("练习题", "")
                .replace("练习", "")
                .replace("题目", "")
                .replace("题", "")
                .replace("课程", "")
                .replace("综合练习", "")
                .replace("综合", "")
                .replaceAll("\\s+", "");
        normalized = normalized.replaceAll("^[\\p{Punct}“”‘’\"'`·、，。！？；：（）【】《》〈〉…—-]+$", "");
        return normalized.isBlank() ? null : normalized;
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
