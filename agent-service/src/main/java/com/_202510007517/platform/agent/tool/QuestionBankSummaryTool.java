package com._202510007517.platform.agent.tool;

import com._202510007517.platform.agent.model.AgentIntent;
import com._202510007517.platform.agent.questionbank.QuestionBankSummaryService;
import com._202510007517.platform.agent.questionbank.QuestionRagService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class QuestionBankSummaryTool implements AgentTool {

    private final QuestionBankSummaryService summaryService;
    private final QuestionRagService questionRagService;

    public QuestionBankSummaryTool(QuestionBankSummaryService summaryService,
                                   QuestionRagService questionRagService) {
        this.summaryService = summaryService;
        this.questionRagService = questionRagService;
    }

    @Override
    public AgentIntent intent() {
        return AgentIntent.QUERY_QUESTION_BANK;
    }

    @Override
    public Map<String, Object> execute(Long userId, String userRole, Map<String, Object> request) {
        String topic = normalizeRequestedTopic(readOptionalText(request, "topic", "courseName"));
        String difficulty = String.valueOf(request.getOrDefault("difficulty", "中等"));
        String type = request.get("type") == null ? null : String.valueOf(request.get("type"));
        Integer count = asInteger(request.get("count"), 5);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "EXECUTED");

        if (topic != null || request.containsKey("difficulty") || request.containsKey("type") || request.containsKey("count")) {
            Map<String, Object> payload = questionRagService.generateQuestions(userRole, topic, difficulty, count, type);
            result.put("questionBank", payload);
            result.put("questions", payload.get("questions"));
            result.put("message", payload.get("message") == null ? "题库查询完成。" : payload.get("message"));
            return result;
        }

        Map<String, Object> summary = summaryService.summary();
        result.put("questionBank", summary);
        result.put("message", summary.getOrDefault("message", "题库查询完成。"));
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
                .replace("随机题目", "")
                .replace("随机", "")
                .replace("课堂练习题", "")
                .replace("课堂练习", "")
                .replace("练习题", "")
                .replace("练习", "")
                .replace("题库", "")
                .replace("题目", "")
                .replace("题", "")
                .replace("课程", "")
                .replace("综合练习", "")
                .replace("综合", "")
                .replace("有什么", "")
                .replace("有哪些", "")
                .replace("查看", "")
                .replace("查询", "")
                .replace("列出", "")
                .replace("给我", "")
                .replace("来点", "")
                .replace("看看", "")
                .replaceAll("\\s+", "");
        return normalized.isBlank() ? null : topic.trim();
    }
}
