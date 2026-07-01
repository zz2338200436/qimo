package com._202510007517.platform.agent.tool;

import com._202510007517.platform.agent.client.TeacherAssignmentEdgeClient;
import com._202510007517.platform.agent.model.AgentIntent;
import com._202510007517.platform.assignment.api.dto.AssignmentDTO;
import com._202510007517.platform.assignment.api.dto.TeacherAssignmentUpsertRequestDTO;
import com._202510007517.platform.common.web.ResponseResult;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class AssignmentUpdateTool implements AgentTool {

    private final TeacherAssignmentEdgeClient assignmentClient;

    public AssignmentUpdateTool(TeacherAssignmentEdgeClient assignmentClient) {
        this.assignmentClient = assignmentClient;
    }

    @Override
    public AgentIntent intent() {
        return AgentIntent.UPDATE_ASSIGNMENT;
    }

    @Override
    public Map<String, Object> execute(Long userId, String userRole, Map<String, Object> request) {
        if (missing(request, "assignmentId") || missing(request, "title") || missing(request, "courseId")
                || missing(request, "dueDate") || missing(request, "maxScore")) {
            return validationFailed("更新作业需要 assignmentId、title、courseId、dueDate、maxScore。");
        }

        Long assignmentId = asLong(request.get("assignmentId"));
        TeacherAssignmentUpsertRequestDTO dto = new TeacherAssignmentUpsertRequestDTO();
        dto.setTitle(String.valueOf(request.get("title")));
        dto.setDescription(asString(request.get("content")));
        dto.setCourseId(asLong(request.get("courseId")));
        dto.setDueDate(String.valueOf(request.get("dueDate")));
        dto.setPublishDate(asString(request.get("publishDate")));
        dto.setIsActive(asBooleanOrDefault(request.get("isActive"), true));
        dto.setMaxScore(asInteger(request.get("maxScore")));

        ResponseResult<AssignmentDTO> response = assignmentClient.updateAssignment(String.valueOf(userId), assignmentId, dto);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "EXECUTED");
        result.put("assignment", response.getData());
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
