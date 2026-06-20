package com._202510007517.platform.agent.tool;

import com._202510007517.platform.agent.model.AgentIntent;
import com._202510007517.platform.assignment.api.dto.AssignmentStudentScoreDTO;
import com._202510007517.platform.assignment.api.dto.AssignmentSubmissionDTO;
import com._202510007517.platform.assignment.api.dto.AssignmentSubmitRequestDTO;
import com._202510007517.platform.assignment.api.feign.AssignmentFeignClient;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class AssignmentSubmitTool implements AgentTool {

    private final AssignmentFeignClient assignmentClient;

    public AssignmentSubmitTool(AssignmentFeignClient assignmentClient) {
        this.assignmentClient = assignmentClient;
    }

    @Override
    public AgentIntent intent() {
        return AgentIntent.SUBMIT_ASSIGNMENT;
    }

    @Override
    public Map<String, Object> execute(Long userId, String userRole, Map<String, Object> request) {
        if (missing(request, "assignmentId") || missing(request, "content")) {
            return validationFailed("提交作业需要 assignmentId 和 content。");
        }
        Long assignmentId = asLong(request.get("assignmentId"));
        if (!assignmentVisibleToStudent(userId, assignmentId)) {
            return permissionDenied("当前学生无权提交该作业。");
        }

        AssignmentSubmitRequestDTO dto = new AssignmentSubmitRequestDTO();
        dto.setStudentId(userId);
        dto.setContent(String.valueOf(request.get("content")));
        dto.setSubmissionDate(LocalDate.now().toString());

        AssignmentSubmissionDTO submission = assignmentClient.submit(assignmentId, dto);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "EXECUTED");
        result.put("submission", submission);
        return result;
    }

    private boolean assignmentVisibleToStudent(Long userId, Long assignmentId) {
        Map<String, Object> pendingAssignments = assignmentClient.listStudentAssignments(
                userId, 1, 100, "dueDate", "DESC", null, false, true);
        if (containsAssignmentId(pendingAssignments, assignmentId)) {
            return true;
        }
        List<AssignmentStudentScoreDTO> visibleAssignments = assignmentClient.listStudentScores(userId);
        return visibleAssignments.stream()
                .anyMatch(assignment -> assignmentId.equals(assignment.getRelatedId()));
    }

    private boolean containsAssignmentId(Map<String, Object> page, Long assignmentId) {
        if (page == null || !(page.get("content") instanceof Iterable<?> assignments)) {
            return false;
        }
        for (Object assignment : assignments) {
            if (assignment instanceof Map<?, ?> assignmentMap
                    && assignmentId.equals(asLongOrNull(assignmentMap.get("id")))) {
                return true;
            }
        }
        return false;
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

    private static Long asLongOrNull(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        return asLong(value);
    }
}
