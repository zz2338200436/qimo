package com._202510007517.platform.agent.tool;

import com._202510007517.platform.agent.model.AgentIntent;
import com._202510007517.platform.course.api.dto.TeacherClassDTO;
import com._202510007517.platform.course.api.feign.CourseFeignClient;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ClassLookupTool implements AgentTool {

    private final CourseFeignClient courseClient;

    public ClassLookupTool(CourseFeignClient courseClient) {
        this.courseClient = courseClient;
    }

    @Override
    public AgentIntent intent() {
        return AgentIntent.QUERY_CLASSES;
    }

    @Override
    public Map<String, Object> execute(Long userId, String userRole, Map<String, Object> request) {
        List<TeacherClassDTO> classes = courseClient.listTeacherClasses(
                userId,
                asString(request.get("className")),
                asString(request.get("grade")),
                asString(request.get("majorName")),
                asLongOrNull(request.get("majorId")),
                asLongOrNull(request.get("courseId"))
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "EXECUTED");
        result.put("classes", classes);
        return result;
    }

    private static String asString(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        return String.valueOf(value);
    }

    private static Long asLongOrNull(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }
}
