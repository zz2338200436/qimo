package com._202510007517.platform.agent.tool;

import com._202510007517.platform.agent.model.AgentIntent;
import com._202510007517.platform.exam.api.dto.ExamDTO;
import com._202510007517.platform.exam.api.feign.ExamFeignClient;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ExamDetailLookupTool implements AgentTool {

    private final ExamFeignClient examClient;

    public ExamDetailLookupTool(ExamFeignClient examClient) {
        this.examClient = examClient;
    }

    @Override
    public AgentIntent intent() {
        return AgentIntent.QUERY_EXAM_DETAIL;
    }

    @Override
    public Map<String, Object> execute(Long userId, String userRole, Map<String, Object> request) {
        if (missing(request, "examId")) {
            return validationFailed("查询考试详情需要 examId。");
        }

        Long examId = asLong(request.get("examId"));
        ExamDTO exam = examClient.getTeacherExam(examId, userId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "EXECUTED");
        result.put("exam", exam);
        return result;
    }

    private static boolean missing(Map<String, Object> request, String key) {
        Object value = request.get(key);
        return value == null || String.valueOf(value).isBlank();
    }

    private static Map<String, Object> validationFailed(String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "VALIDATION_FAILED");
        result.put("message", message);
        return result;
    }

    private static Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }
}
