package com._202510007517.platform.agent.tool;

import com._202510007517.platform.agent.model.AgentIntent;
import com._202510007517.platform.exam.api.dto.ExamDTO;
import com._202510007517.platform.exam.api.dto.ExamSubmissionDTO;
import com._202510007517.platform.exam.api.dto.ExamSubmitRequestDTO;
import com._202510007517.platform.exam.api.feign.ExamFeignClient;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ExamSubmitTool implements AgentTool {

    private final ExamFeignClient examClient;

    public ExamSubmitTool(ExamFeignClient examClient) {
        this.examClient = examClient;
    }

    @Override
    public AgentIntent intent() {
        return AgentIntent.SUBMIT_EXAM;
    }

    @Override
    public Map<String, Object> execute(Long userId, String userRole, Map<String, Object> request) {
        if (missing(request, "examId") || missing(request, "answers")) {
            return validationFailed("提交考试需要 examId 和 answers。");
        }
        Long examId = asLong(request.get("examId"));
        if (!examVisibleToStudent(userId, examId)) {
            return permissionDenied("当前学生无权提交该考试。");
        }

        ExamSubmitRequestDTO dto = new ExamSubmitRequestDTO();
        dto.setStudentId(userId);
        dto.setAnswers(asStringMap(request.get("answers")));
        if (!missing(request, "timeTaken")) {
            dto.setTimeTaken(asInteger(request.get("timeTaken")));
        }

        ExamSubmissionDTO submission = examClient.submit(examId, dto);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "EXECUTED");
        result.put("submission", submission);
        return result;
    }

    private boolean examVisibleToStudent(Long userId, Long examId) {
        List<ExamDTO> visibleExams = examClient.listByStudent(userId);
        return visibleExams.stream()
                .anyMatch(exam -> examId.equals(exam.getId()));
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

    private static Map<String, Object> permissionDenied(String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "PERMISSION_DENIED");
        result.put("message", message);
        return result;
    }

    private static Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }

    private static Integer asInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.valueOf(String.valueOf(value));
    }

    private static Map<String, String> asStringMap(Object value) {
        if (!(value instanceof Map<?, ?> rawMap)) {
            return Map.of();
        }
        Map<String, String> result = new LinkedHashMap<>();
        rawMap.forEach((key, mapValue) -> result.put(String.valueOf(key), String.valueOf(mapValue)));
        return result;
    }
}
