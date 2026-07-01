package com._202510007517.platform.agent.tool;

import com._202510007517.platform.agent.client.TeacherAssignmentEdgeClient;
import com._202510007517.platform.agent.model.AgentIntent;
import com._202510007517.platform.assignment.api.dto.AssignmentDTO;
import com._202510007517.platform.assignment.api.dto.TeacherAssignmentUpsertRequestDTO;
import com._202510007517.platform.common.web.ResponseResult;
import com._202510007517.platform.course.api.feign.CourseFeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class AssignmentPublishTool implements AgentTool {

    private final TeacherAssignmentEdgeClient assignmentClient;
    private final CourseFeignClient courseClient;

    public AssignmentPublishTool(TeacherAssignmentEdgeClient assignmentClient, CourseFeignClient courseClient) {
        this.assignmentClient = assignmentClient;
        this.courseClient = courseClient;
    }

    @Override
    public AgentIntent intent() {
        return AgentIntent.PUBLISH_ASSIGNMENT;
    }

    @Override
    public Map<String, Object> execute(Long userId, String userRole, Map<String, Object> request) {
        if (missing(request, "title") || missing(request, "courseId")
                || missing(request, "dueDate") || missing(request, "maxScore")) {
            return validationFailed("发布作业需要 title、courseId、dueDate、maxScore。");
        }
        Long courseId = asLong(request.get("courseId"));
        if (!teacherOwnsCourse(userId, courseId)) {
            return permissionDenied("当前教师无权在该课程下发布作业。");
        }
        TeacherAssignmentUpsertRequestDTO dto = new TeacherAssignmentUpsertRequestDTO();
        dto.setTitle(String.valueOf(request.get("title")));
        dto.setDescription(asString(request.get("content")));
        dto.setCourseId(courseId);
        dto.setDueDate(String.valueOf(request.get("dueDate")));
        dto.setPublishDate(asString(request.get("publishDate")));
        dto.setIsActive(true);
        dto.setMaxScore(asInteger(request.get("maxScore")));

        MultipartFile[] files = AgentAttachmentMultipartSupport.toMultipartFiles(request.get("attachments"));
        ResponseResult<AssignmentDTO> response = files.length == 0
                ? assignmentClient.createAssignment(String.valueOf(userId), dto)
                : assignmentClient.createAssignmentWithFiles(String.valueOf(userId), dto, files);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "EXECUTED");
        result.put("assignment", response.getData());
        return result;
    }

    private boolean teacherOwnsCourse(Long teacherId, Long courseId) {
        return courseClient.listTeacherCourses(teacherId, null, null, null, null).stream()
                .anyMatch(course -> courseId.equals(course.getId()));
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
