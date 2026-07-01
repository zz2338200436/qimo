package com._202510007517.platform.agent.tool;

import com._202510007517.platform.agent.model.AgentIntent;
import com._202510007517.platform.exam.api.dto.ExamDTO;
import com._202510007517.platform.exam.api.dto.TeacherExamUpsertRequestDTO;
import com._202510007517.platform.exam.api.feign.ExamFeignClient;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ExamUpdateTool implements AgentTool {

    private final ExamFeignClient examClient;

    public ExamUpdateTool(ExamFeignClient examClient) {
        this.examClient = examClient;
    }

    @Override
    public AgentIntent intent() {
        return AgentIntent.UPDATE_EXAM;
    }

    @Override
    public Map<String, Object> execute(Long userId, String userRole, Map<String, Object> request) {
        if (missing(request, "examId") || missing(request, "title") || missing(request, "courseId")
                || missing(request, "startTime") || missing(request, "endTime") || missing(request, "duration")) {
            return validationFailed("更新考试需要 examId、title、courseId、startTime、endTime、duration。");
        }

        Long examId = asLong(request.get("examId"));
        TeacherExamUpsertRequestDTO dto = new TeacherExamUpsertRequestDTO();
        dto.setTitle(String.valueOf(request.get("title")));
        dto.setDescription(asString(request.get("description")));
        dto.setCourseId(asLong(request.get("courseId")));
        dto.setStartTime(String.valueOf(request.get("startTime")));
        dto.setEndTime(String.valueOf(request.get("endTime")));
        dto.setPublishDate(asString(request.get("publishDate")));
        dto.setIsActive(asBooleanOrDefault(request.get("isActive"), true));
        dto.setIsOnline(asBooleanOrDefault(request.get("isOnline"), true));
        dto.setLocation(asString(request.get("location")));
        dto.setDuration(asLong(request.get("duration")));

        ExamDTO exam = examClient.updateTeacherExam(examId, userId, dto);
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

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }

    private static Boolean asBooleanOrDefault(Object value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        return Boolean.valueOf(String.valueOf(value));
    }
}
