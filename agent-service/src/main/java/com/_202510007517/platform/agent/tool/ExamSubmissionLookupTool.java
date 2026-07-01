package com._202510007517.platform.agent.tool;

import com._202510007517.platform.agent.client.TeacherExamEdgeClient;
import com._202510007517.platform.agent.model.AgentIntent;
import com._202510007517.platform.common.web.ResponseResult;
import com._202510007517.platform.exam.api.dto.ExamSubmissionDTO;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ExamSubmissionLookupTool implements AgentTool {

    private final TeacherExamEdgeClient examClient;

    public ExamSubmissionLookupTool(TeacherExamEdgeClient examClient) {
        this.examClient = examClient;
    }

    @Override
    public AgentIntent intent() {
        return AgentIntent.QUERY_EXAM_SUBMISSIONS;
    }

    @Override
    public Map<String, Object> execute(Long userId, String userRole, Map<String, Object> request) {
        if (missing(request, "examId")) {
            return validationFailed("查询考试提交记录需要 examId。");
        }

        Long examId = asLong(request.get("examId"));
        ResponseResult<List<ExamSubmissionDTO>> response =
                examClient.listExamSubmissions(String.valueOf(userId), examId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "EXECUTED");
        result.put("examId", examId);
        result.put("submissions", response.getData());
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
