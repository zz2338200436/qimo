package com._202510007517.platform.course.web;

import com._202510007517.platform.common.web.CommonTraceConstants;
import com._202510007517.platform.common.web.ResponseResult;
import com._202510007517.platform.course.api.dto.CourseDTO;
import com._202510007517.platform.course.api.dto.PageResultDTO;
import com._202510007517.platform.course.service.CourseApplicationService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/student/courses")
@Validated
public class StudentCourseController {

    private final CourseApplicationService courseApplicationService;

    public StudentCourseController(CourseApplicationService courseApplicationService) {
        this.courseApplicationService = courseApplicationService;
    }

    @GetMapping
    public ResponseResult<PageResultDTO<CourseDTO>> listStudentCourses(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestParam(value = "studentId", required = false) Long studentId,
            @RequestParam(value = "page", defaultValue = "1") @Min(1) Integer page,
            @RequestParam(value = "size", defaultValue = "10") @Min(1) @Max(100) Integer size,
            @RequestParam(value = "searchQuery", required = false) String searchQuery,
            @RequestParam(value = "courseCategory", required = false) String courseCategory,
            @RequestParam(value = "courseStatus", required = false) String courseStatus) {
        List<CourseDTO> courses = courseApplicationService.listStudentCourses(resolveStudentId(userIdHeader, studentId), searchQuery, courseCategory, courseStatus);
        return ResponseResult.success(toPage(courses, page, size), "获取课程列表成功", 200);
    }

    @GetMapping("/{courseId}")
    public ResponseResult<CourseDTO> getStudentCourse(@PathVariable Long courseId,
                                                      @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
                                                      @RequestParam(value = "studentId", required = false) Long studentId) {
        return ResponseResult.success(courseApplicationService.getStudentCourse(resolveStudentId(userIdHeader, studentId), courseId), "获取课程详情成功", 200);
    }

    private static Long resolveStudentId(String userIdHeader, Long studentId) {
        if (userIdHeader != null && !userIdHeader.isBlank()) {
            return Long.valueOf(userIdHeader);
        }
        if (studentId != null) {
            return studentId;
        }
        throw new IllegalArgumentException("缺少学生身份");
    }

    private static <T> PageResultDTO<T> toPage(List<T> content, Integer page, Integer size) {
        PageResultDTO<T> pageResult = new PageResultDTO<>();
        pageResult.setContent(content);
        pageResult.setPageNumber(page);
        pageResult.setPageSize(size);
        pageResult.setTotalElements((long) content.size());
        int totalPages = size <= 0 ? 1 : (int) Math.ceil((double) content.size() / size);
        pageResult.setTotalPages(Math.max(totalPages, 1));
        pageResult.setFirst(page == 1);
        pageResult.setLast(page >= pageResult.getTotalPages());
        pageResult.setOffset((long) (page - 1) * size);
        pageResult.setNumberOfElements(content.size());
        pageResult.setEmpty(content.isEmpty());
        return pageResult;
    }
}
