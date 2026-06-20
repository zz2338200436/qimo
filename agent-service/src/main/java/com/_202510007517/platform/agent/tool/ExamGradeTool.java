package com._202510007517.platform.agent.tool;

import com._202510007517.platform.agent.client.TeacherExamEdgeClient;
import com._202510007517.platform.agent.model.AgentIntent;
import com._202510007517.platform.common.web.ResponseResult;
import com._202510007517.platform.exam.api.dto.ExamSubmissionDTO;
import com._202510007517.platform.exam.api.dto.TeacherExamGradeRequestDTO;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ExamGradeTool implements AgentTool {

    private final TeacherExamEdgeClient examClient;

    public ExamGradeTool(TeacherExamEdgeClient examClient) {
        this.examClient = examClient;
    }

    @Override
    public AgentIntent intent() {
        return AgentIntent.GRADE_EXAM;
    }

    @Override
    public Map<String, Object> execute(Long userId, String userRole, Map<String, Object> request) {
        if (missing(request, "submissionId") || (missing(request, "score") && missing(request, "maxScore"))) {
            return validationFailed("批改考试需要 submissionId、score。");
        }

        Long submissionId = asLong(request.get("submissionId"));
        TeacherExamGradeRequestDTO dto = new TeacherExamGradeRequestDTO();
        dto.setScore(asInteger(request.containsKey("score") ? request.get("score") : request.get("maxScore")));
        dto.setTeacherComment(asString(request.get("teacherComment")));
        dto.setGraded(true);

        ResponseResult<ExamSubmissionDTO> response =
                examClient.gradeSubmission(String.valueOf(userId), submissionId, dto);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "EXECUTED");
        result.put("submission", response.getData());
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

    private static Integer asInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.valueOf(String.valueOf(value));
    }
}
