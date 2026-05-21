package com._202510007517.platform.course.web;

import com._202510007517.platform.common.web.CommonTraceConstants;
import com._202510007517.platform.common.web.ResponseResult;
import com._202510007517.platform.course.api.dto.CourseDTO;
import com._202510007517.platform.course.service.CourseApplicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/system")
public class SystemDataCompatibilityController {

    private final CourseApplicationService courseApplicationService;

    public SystemDataCompatibilityController(CourseApplicationService courseApplicationService) {
        this.courseApplicationService = courseApplicationService;
    }

    @GetMapping("/semesters")
    public ResponseResult<List<String>> getSemesters() {
        return ResponseResult.success(courseApplicationService.listSemesters(), "获取学期列表成功", 200);
    }

    @GetMapping("/student/courses")
    public ResponseResult<List<CourseDTO>> getStudentCourses(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader) {
        return ResponseResult.success(
                courseApplicationService.listStudentCourses(resolveUserId(userIdHeader), null, null, null),
                "获取课程列表成功",
                200);
    }

    @GetMapping("/teacher/courses")
    public ResponseResult<List<CourseDTO>> getTeacherCourses(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader) {
        return ResponseResult.success(
                courseApplicationService.listTeacherCourses(resolveUserId(userIdHeader), null, null, null, null),
                "获取课程列表成功",
                200);
    }

    @GetMapping("/time-ranges")
    public ResponseResult<List<String>> getTimeRanges() {
        return ResponseResult.success(List.of("week", "month", "quarter", "semester"), "获取时间范围列表成功", 200);
    }

    private static Long resolveUserId(String userIdHeader) {
        if (userIdHeader == null || userIdHeader.isBlank()) {
            throw new IllegalArgumentException("缺少用户身份");
        }
        return Long.valueOf(userIdHeader);
    }
}
