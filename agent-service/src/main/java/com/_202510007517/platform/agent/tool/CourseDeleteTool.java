package com._202510007517.platform.agent.tool;

import com._202510007517.platform.agent.model.AgentIntent;
import com._202510007517.platform.course.api.feign.CourseFeignClient;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class CourseDeleteTool implements AgentTool {

    private final CourseFeignClient courseClient;

    public CourseDeleteTool(CourseFeignClient courseClient) {
        this.courseClient = courseClient;
    }

    @Override
    public AgentIntent intent() {
        return AgentIntent.DELETE_COURSE;
    }

    @Override
    public Map<String, Object> execute(Long userId, String userRole, Map<String, Object> request) {
        if (!"TEACHER".equalsIgnoreCase(userRole)) {
            return permissionDenied("只有教师可以删除课程。");
        }
        if (missing(request, "courseId")) {
            return validationFailed("删除课程需要 courseId。");
        }

        Long courseId = asLong(request.get("courseId"));
        courseClient.deleteCourse(courseId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "EXECUTED");
        result.put("courseId", courseId);
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

    private static Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }
}
