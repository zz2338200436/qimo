package com._202510007517.platform.agent.tool;

import com._202510007517.platform.agent.client.TeacherExamEdgeClient;
import com._202510007517.platform.agent.model.AgentIntent;
import com._202510007517.platform.common.web.ResponseResult;
import com._202510007517.platform.exam.api.feign.ExamFeignClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Component
public class ExamQueryTool implements AgentTool {

    private final ExamFeignClient examClient;
    private final TeacherExamEdgeClient teacherExamEdgeClient;

    public ExamQueryTool(ExamFeignClient examClient, TeacherExamEdgeClient teacherExamEdgeClient) {
        this.examClient = examClient;
        this.teacherExamEdgeClient = teacherExamEdgeClient;
    }

    @Override
    public AgentIntent intent() {
        return AgentIntent.QUERY_EXAMS;
    }

    @Override
    public Map<String, Object> execute(Long userId, String userRole, Map<String, Object> request) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (isTeacher(userRole)) {
            ResponseResult<Map<String, Object>> response = teacherExamEdgeClient.listExams(
                    String.valueOf(userId), 1, 10, "id", "DESC", null, null, null);
            Map<String, Object> page = response.getData();
            Object exams = page == null ? List.of() : page.getOrDefault("content", List.of());
            result.put("status", response.isSuccess() ? "EXECUTED" : "FAILED");
            result.put("exams", exams);
            result.put("teacherExamPage", page);
            result.put("message", response.getMessage());
            return result;
        }

        result.put("status", "EXECUTED");
        result.put("exams", examClient.listByStudent(userId));
        return result;
    }

    private static boolean isTeacher(String userRole) {
        if (userRole == null || userRole.isBlank()) {
            return false;
        }
        String normalizedRole = userRole.trim().toUpperCase(Locale.ROOT);
        if (normalizedRole.startsWith("ROLE_")) {
            normalizedRole = normalizedRole.substring("ROLE_".length());
        }
        return "TEACHER".equals(normalizedRole);
    }
}
