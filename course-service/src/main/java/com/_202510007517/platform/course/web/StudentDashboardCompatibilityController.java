package com._202510007517.platform.course.web;

import com._202510007517.platform.common.web.ResponseResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class StudentDashboardCompatibilityController {

    private static final String MESSAGE = "当前 JWT 微服务环境暂未提供学生综合表现接口，页面将改用已接通的数据源进行统计。";

    @GetMapping("/student-performance")
    public Map<String, Object> getCurrentStudentPerformance() {
        return unsupported();
    }

    @GetMapping("/student-performance/{studentId}")
    public Map<String, Object> getStudentPerformance(@PathVariable Long studentId) {
        return unsupported();
    }

    private static Map<String, Object> unsupported() {
        return Map.of(
                "success", false,
                "code", 501,
                "unsupported", true,
                "message", MESSAGE
        );
    }
}
