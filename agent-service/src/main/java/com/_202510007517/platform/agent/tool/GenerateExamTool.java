package com._202510007517.platform.agent.tool;

import com._202510007517.platform.agent.client.AiEdgeClient;
import com._202510007517.platform.agent.model.AgentIntent;
import com._202510007517.platform.ai.api.dto.GenerateExamRequestDTO;
import com._202510007517.platform.common.web.ResponseResult;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class GenerateExamTool implements AgentTool {

    private final AiEdgeClient aiClient;

    public GenerateExamTool(AiEdgeClient aiClient) {
        this.aiClient = aiClient;
    }

    @Override
    public AgentIntent intent() {
        return AgentIntent.GENERATE_EXAM;
    }

    @Override
    public Map<String, Object> execute(Long userId, String userRole, Map<String, Object> request) {
        GenerateExamRequestDTO dto = new GenerateExamRequestDTO();
        dto.setCourseName(String.valueOf(request.getOrDefault("courseName", "综合课程")));
        dto.setTotalScore(asInteger(request.get("totalScore"), 100));
        dto.setDuration(asInteger(request.get("duration"), 120));
        dto.setDifficulty(String.valueOf(request.getOrDefault("difficulty", "中等")));
        ResponseResult<Map<String, Object>> response = aiClient.generateExam(
                String.valueOf(userId), userRole, userRole, dto);
        return GenerateQuestionsTool.aiResult(response);
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
}
