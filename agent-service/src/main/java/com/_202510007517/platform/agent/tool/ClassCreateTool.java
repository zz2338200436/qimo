package com._202510007517.platform.agent.tool;

import com._202510007517.platform.agent.model.AgentIntent;
import com._202510007517.platform.course.api.dto.ClassUpsertRequestDTO;
import com._202510007517.platform.course.api.feign.CourseFeignClient;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ClassCreateTool implements AgentTool {

    private final CourseFeignClient courseClient;

    public ClassCreateTool(CourseFeignClient courseClient) {
        this.courseClient = courseClient;
    }

    @Override
    public AgentIntent intent() {
        return AgentIntent.CREATE_CLASS;
    }

    @Override
    public Map<String, Object> execute(Long userId, String userRole, Map<String, Object> request) {
        if (!"TEACHER".equalsIgnoreCase(userRole)) {
            return permissionDenied("只有教师可以创建班级。");
        }
        if (missing(request, "className") || missing(request, "year") || missing(request, "capacity")) {
            return validationFailed("创建班级需要 className、year、capacity。");
        }

        ClassUpsertRequestDTO dto = new ClassUpsertRequestDTO();
        dto.setClassName(asString(request.get("className")));
        dto.setYear(asString(request.get("year")));
        dto.setCapacity(asInteger(request.get("capacity")));
        dto.setCourseId(asLong(request.get("courseId")));
        dto.setMajorId(asLong(request.get("majorId")));
        dto.setClassTime(asString(request.get("classTime")));
        dto.setClassLocation(asString(request.get("classLocation")));

        Long classId = courseClient.createClass(userId, dto);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "EXECUTED");
        result.put("classId", classId);
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
