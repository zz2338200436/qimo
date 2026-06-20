package com._202510007517.platform.agent.tool;

import com._202510007517.platform.agent.model.AgentIntent;
import com._202510007517.platform.course.api.feign.CourseFeignClient;
import com._202510007517.platform.exam.api.dto.ExamDTO;
import com._202510007517.platform.exam.api.dto.TeacherExamUpsertRequestDTO;
import com._202510007517.platform.exam.api.feign.ExamFeignClient;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ExamPublishTool implements AgentTool {

    private final ExamFeignClient examClient;
    private final CourseFeignClient courseClient;

    public ExamPublishTool(ExamFeignClient examClient, CourseFeignClient courseClient) {
        this.examClient = examClient;
        this.courseClient = courseClient;
    }

    @Override
    public AgentIntent intent() {
        return AgentIntent.PUBLISH_EXAM;
    }

    @Override
    public Map<String, Object> execute(Long userId, String userRole, Map<String, Object> request) {
        if (missing(request, "title") || missing(request, "courseId") || missing(request, "startTime")
                || missing(request, "endTime") || missing(request, "duration")) {
            return validationFailed("发布考试需要 title、courseId、startTime、endTime、duration。");
        }
        Long courseId = asLong(request.get("courseId"));
        if (!teacherOwnsCourse(userId, courseId)) {
            return permissionDenied("当前教师无权在该课程下发布考试。");
        }

        TeacherExamUpsertRequestDTO dto = new TeacherExamUpsertRequestDTO();
        dto.setTitle(String.valueOf(request.get("title")));
        dto.setDescription(asString(request.get("description")));
        dto.setCourseId(courseId);
        dto.setStartTime(String.valueOf(request.get("startTime")));
        dto.setEndTime(String.valueOf(request.get("endTime")));
        dto.setPublishDate(asString(request.get("publishDate")));
        dto.setIsActive(asBooleanOrDefault(request.get("isActive"), true));
        dto.setIsOnline(asBooleanOrDefault(request.get("isOnline"), true));
        dto.setLocation(asString(request.get("location")));
        dto.setDuration(asLong(request.get("duration")));

        ExamDTO exam = examClient.createTeacherExam(userId, dto);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "EXECUTED");
        result.put("exam", exam);
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
