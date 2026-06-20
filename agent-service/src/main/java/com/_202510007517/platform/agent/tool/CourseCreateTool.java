package com._202510007517.platform.agent.tool;

import com._202510007517.platform.agent.model.AgentIntent;
import com._202510007517.platform.course.api.dto.CourseDTO;
import com._202510007517.platform.course.api.dto.CourseUpsertRequestDTO;
import com._202510007517.platform.course.api.feign.CourseFeignClient;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class CourseCreateTool implements AgentTool {

    private final CourseFeignClient courseClient;

    public CourseCreateTool(CourseFeignClient courseClient) {
        this.courseClient = courseClient;
    }

    @Override
    public AgentIntent intent() {
        return AgentIntent.CREATE_COURSE;
    }

    @Override
    public Map<String, Object> execute(Long userId, String userRole, Map<String, Object> request) {
        if (!"TEACHER".equalsIgnoreCase(userRole)) {
            return permissionDenied("只有教师可以创建课程。");
        }
        if (missing(request, "courseName") || missing(request, "courseCode")
                || missing(request, "credit") || missing(request, "totalHours")) {
            return validationFailed("创建课程需要 courseName、courseCode、credit、totalHours。");
        }

        CourseUpsertRequestDTO dto = new CourseUpsertRequestDTO();
        dto.setCourseName(asString(request.get("courseName")));
        dto.setCourseCode(asString(request.get("courseCode")));
        dto.setDescription(asString(request.get("description")));
        dto.setCredit(asInteger(request.get("credit")));
        dto.setCourseCategory(asString(request.get("courseCategory")));
        dto.setTotalHours(asInteger(request.get("totalHours")));
        dto.setCourseDirector(asLong(request.get("courseDirector")));
        dto.setAssessmentMethod(asString(request.get("assessmentMethod")));
        dto.setCourseStatus(asString(request.get("courseStatus")));
        dto.setSemester(asString(request.get("semester")));
        dto.setStartDate(asString(request.get("startDate")));
        dto.setEndDate(asString(request.get("endDate")));
        dto.setMaxStudents(asInteger(request.get("maxStudents")));

        CourseDTO course = courseClient.createCourse(userId, dto);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "EXECUTED");
        result.put("course", course);
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

    private static Map<String, Object> permissionDenied(String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "PERMISSION_DENIED");
        result.put("message", message);
        return result;
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static Integer asInteger(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.valueOf(String.valueOf(value));
    }

    private static Long asLong(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }
}
