package com._202510007517.platform.agent.tool;

import com._202510007517.platform.agent.client.StudentCourseEdgeClient;
import com._202510007517.platform.agent.model.AgentIntent;
import com._202510007517.platform.common.web.ResponseResult;
import com._202510007517.platform.course.api.dto.CourseDTO;
import com._202510007517.platform.course.api.feign.CourseFeignClient;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Component
public class CourseDetailLookupTool implements AgentTool {

    private final CourseFeignClient teacherCourseClient;
    private final StudentCourseEdgeClient studentCourseClient;

    public CourseDetailLookupTool(CourseFeignClient teacherCourseClient,
                                  StudentCourseEdgeClient studentCourseClient) {
        this.teacherCourseClient = teacherCourseClient;
        this.studentCourseClient = studentCourseClient;
    }

    @Override
    public AgentIntent intent() {
        return AgentIntent.QUERY_COURSE_DETAIL;
    }

    @Override
    public Map<String, Object> execute(Long userId, String userRole, Map<String, Object> request) {
        if (missing(request, "courseId")) {
            return validationFailed("查询课程详情需要 courseId。");
        }

        Long courseId = asLong(request.get("courseId"));
        CourseDTO course;
        if ("STUDENT".equals(normalizeRole(userRole))) {
            ResponseResult<CourseDTO> response = studentCourseClient.getStudentCourse(String.valueOf(userId), courseId);
            course = response.getData();
        } else {
            course = teacherCourseClient.getCourse(courseId);
        }

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

    private static Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }

    private static String normalizeRole(String userRole) {
        if (userRole == null || userRole.isBlank()) {
            return "";
        }
        String role = userRole.trim().toUpperCase(Locale.ROOT);
        if (role.startsWith("ROLE_")) {
            role = role.substring("ROLE_".length());
        }
        return role;
    }
}
