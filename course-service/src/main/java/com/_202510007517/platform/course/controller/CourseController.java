package com._202510007517.platform.course.controller;

import com._202510007517.platform.common.web.ResponseResult;
import com._202510007517.platform.common.web.CommonTraceConstants;
import com._202510007517.platform.course.api.dto.CourseDTO;
import com._202510007517.platform.course.api.dto.CourseUpsertRequestDTO;
import com._202510007517.platform.course.api.dto.PageResultDTO;
import com._202510007517.platform.course.service.CourseApplicationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher/courses")
@Validated
public class CourseController {

    private final CourseApplicationService courseApplicationService;

    public CourseController(CourseApplicationService courseApplicationService) {
        this.courseApplicationService = courseApplicationService;
    }

    @GetMapping
    public ResponseResult<PageResultDTO<CourseDTO>> listTeacherCourses(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestParam(value = "teacherId", required = false) Long teacherId,
            @RequestParam(value = "page", defaultValue = "1") @Min(1) Integer page,
            @RequestParam(value = "size", defaultValue = "10") @Min(1) @Max(100) Integer size,
            @RequestParam(value = "courseName", required = false) String courseName,
            @RequestParam(value = "courseStatus", required = false) String courseStatus,
            @RequestParam(value = "courseCode", required = false) String courseCode,
            @RequestParam(value = "category", required = false) String category) {
        List<CourseDTO> courses = courseApplicationService.listTeacherCourses(resolveTeacherId(userIdHeader, teacherId), courseName, courseCode, category, courseStatus);
        return ResponseResult.success(toPage(courses, page, size), "获取课程列表成功", 200);
    }

    @PostMapping
    public ResponseResult<CourseDTO> createCourse(@RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
                                                  @RequestParam(value = "teacherId", required = false) Long teacherId,
                                                  @RequestBody @Valid CourseUpsertRequestDTO request) {
        return ResponseResult.created(courseApplicationService.createCourse(resolveTeacherId(userIdHeader, teacherId), request));
    }

    @PutMapping("/{courseId}")
    public ResponseResult<CourseDTO> updateCourse(@PathVariable Long courseId,
                                                  @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
                                                  @RequestParam(value = "teacherId", required = false) Long teacherId,
                                                  @RequestBody @Valid CourseUpsertRequestDTO request) {
        return ResponseResult.success(courseApplicationService.updateCourse(courseId, resolveTeacherId(userIdHeader, teacherId), request), "操作成功", 200);
    }

    @DeleteMapping("/{courseId}")
    public ResponseResult<Void> deleteCourse(@PathVariable Long courseId) {
        courseApplicationService.deleteCourse(courseId);
        return ResponseResult.noContent();
    }

    @GetMapping("/{courseId}")
    public ResponseResult<CourseDTO> getCourse(@PathVariable Long courseId) {
        return ResponseResult.success(courseApplicationService.getCourse(courseId));
    }

    @GetMapping("/{courseId}/students")
    public ResponseResult<List<Map<String, Object>>> listCourseStudents(@PathVariable Long courseId) {
        return ResponseResult.success(courseApplicationService.listCourseStudents(courseId), "获取学生列表成功", 200);
    }

    private static <T> PageResultDTO<T> toPage(List<T> content, Integer page, Integer size) {
        PageResultDTO<T> pageResult = new PageResultDTO<>();
        pageResult.setContent(content);
        pageResult.setPageNumber(page);
        pageResult.setPageSize(size);
        pageResult.setTotalElements((long) content.size());
        pageResult.setTotalPages((int) Math.ceil((double) content.size() / size));
        pageResult.setFirst(page == 1);
        pageResult.setLast(page >= pageResult.getTotalPages());
        pageResult.setOffset((long) (page - 1) * size);
        pageResult.setNumberOfElements(content.size());
        pageResult.setEmpty(content.isEmpty());
        return pageResult;
    }

    static Long resolveTeacherId(String userIdHeader, Long teacherId) {
        if (userIdHeader != null && !userIdHeader.isBlank()) {
            return Long.valueOf(userIdHeader);
        }
        if (teacherId != null) {
            return teacherId;
        }
        throw new IllegalArgumentException("缺少教师身份");
    }
}
