package com._202510007517.platform.agent.tool;

import com._202510007517.platform.agent.client.TeacherAssignmentEdgeClient;
import com._202510007517.platform.agent.model.AgentIntent;
import com._202510007517.platform.assignment.api.dto.AssignmentSubmissionDTO;
import com._202510007517.platform.common.web.ResponseResult;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class AssignmentSubmissionLookupTool implements AgentTool {

    private final TeacherAssignmentEdgeClient assignmentClient;

    public AssignmentSubmissionLookupTool(TeacherAssignmentEdgeClient assignmentClient) {
        this.assignmentClient = assignmentClient;
    }

    @Override
    public AgentIntent intent() {
        return AgentIntent.QUERY_ASSIGNMENT_SUBMISSIONS;
    }

    @Override
    public Map<String, Object> execute(Long userId, String userRole, Map<String, Object> request) {
        if (missing(request, "assignmentId")) {
            return validationFailed("查询作业提交记录需要 assignmentId。");
        }

        Long assignmentId = asLong(request.get("assignmentId"));
        ResponseResult<List<AssignmentSubmissionDTO>> response =
                assignmentClient.listAssignmentSubmissions(String.valueOf(userId), assignmentId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "EXECUTED");
        result.put("assignmentId", assignmentId);
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
