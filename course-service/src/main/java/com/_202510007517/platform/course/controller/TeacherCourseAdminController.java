package com._202510007517.platform.course.controller;

import com._202510007517.platform.common.web.CommonTraceConstants;
import com._202510007517.platform.common.web.ResponseResult;
import com._202510007517.platform.course.api.dto.ClassUpsertRequestDTO;
import com._202510007517.platform.course.api.dto.CourseAssignmentDTO;
import com._202510007517.platform.course.api.dto.CourseAssignmentRequestDTO;
import com._202510007517.platform.course.api.dto.MajorDTO;
import com._202510007517.platform.course.api.dto.PageResultDTO;
import com._202510007517.platform.course.api.dto.TeacherClassDTO;
import com._202510007517.platform.course.service.CourseApplicationService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher")
@Validated
public class TeacherCourseAdminController {

    private final CourseApplicationService courseApplicationService;

    public TeacherCourseAdminController(CourseApplicationService courseApplicationService) {
        this.courseApplicationService = courseApplicationService;
    }

    @GetMapping("/classes")
    public ResponseResult<List<TeacherClassDTO>> listTeacherClasses(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestParam(value = "teacherId", required = false) Long teacherId,
            @RequestParam(value = "className", required = false) String className,
            @RequestParam(value = "grade", required = false) String grade,
            @RequestParam(value = "majorName", required = false) String majorName,
            @RequestParam(value = "majorId", required = false) Long majorId,
            @RequestParam(value = "courseId", required = false) Long courseId) {
        Long resolvedTeacherId = CourseController.resolveTeacherId(userIdHeader, teacherId);
        return ResponseResult.success(
                courseApplicationService.listTeacherClasses(resolvedTeacherId, className, grade, majorName, majorId, courseId),
                "获取班级列表成功",
                200);
    }

    @PostMapping("/classes")
    public ResponseResult<Map<String, Object>> createClass(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestParam(value = "teacherId", required = false) Long teacherId,
            @RequestBody ClassUpsertRequestDTO request) {
        Long id = courseApplicationService.createClass(CourseController.resolveTeacherId(userIdHeader, teacherId), request);
        return ResponseResult.success(Map.of("id", id), "班级创建成功", 201);
    }

    @PutMapping("/classes/{classId}")
    public ResponseResult<Void> updateClass(
            @PathVariable Long classId,
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestParam(value = "teacherId", required = false) Long teacherId,
            @RequestBody ClassUpsertRequestDTO request) {
        courseApplicationService.updateClass(CourseController.resolveTeacherId(userIdHeader, teacherId), classId, request);
        return ResponseResult.success(null, "班级更新成功", 200);
    }

    @DeleteMapping("/classes/{classId}")
    public ResponseResult<Void> deleteClass(
            @PathVariable Long classId,
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestParam(value = "teacherId", required = false) Long teacherId) {
        courseApplicationService.deleteClass(CourseController.resolveTeacherId(userIdHeader, teacherId), classId);
        return ResponseResult.success(null, "班级删除成功", 204);
    }

    @GetMapping("/classes/{classId}")
    public ResponseResult<Map<String, Object>> getClass(
            @PathVariable Long classId,
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestParam(value = "teacherId", required = false) Long teacherId) {
        return ResponseResult.success(
                courseApplicationService.getClass(CourseController.resolveTeacherId(userIdHeader, teacherId), classId),
                "获取班级详情成功",
                200);
    }

    @GetMapping("/classes/{classId}/students")
    public ResponseResult<List<Map<String, Object>>> listClassStudents(
            @PathVariable Long classId,
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestParam(value = "teacherId", required = false) Long teacherId) {
        return ResponseResult.success(
                courseApplicationService.listClassStudents(CourseController.resolveTeacherId(userIdHeader, teacherId), classId),
                "获取班级学生列表成功",
                200);
    }

    @GetMapping("/check-class-name")
    public ResponseResult<Map<String, Boolean>> checkClassName(
            @RequestParam String className,
            @RequestParam(value = "classId", required = false) Long classId) {
        return ResponseResult.success(
                Map.of("exists", courseApplicationService.classNameExists(className, classId)),
                "班级名称检查成功",
                200);
    }

    @GetMapping("/students/{studentId}")
    public ResponseResult<Map<String, Object>> getTeacherStudent(
            @PathVariable Long studentId,
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestParam(value = "teacherId", required = false) Long teacherId) {
        return ResponseResult.success(
                courseApplicationService.getTeacherStudent(
                        CourseController.resolveTeacherId(userIdHeader, teacherId),
                        studentId),
                "获取学生详情成功",
                200);
    }

    @PutMapping("/students/{studentId}")
    public ResponseResult<Map<String, Object>> updateTeacherStudent(
            @PathVariable Long studentId,
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestParam(value = "teacherId", required = false) Long teacherId,
            @RequestBody Map<String, Object> request) {
        return ResponseResult.success(
                courseApplicationService.updateTeacherStudent(
                        CourseController.resolveTeacherId(userIdHeader, teacherId),
                        studentId,
                        request),
                "学生信息更新成功",
                200);
    }

    @GetMapping("/course-assignments")
    public ResponseResult<PageResultDTO<CourseAssignmentDTO>> listCourseAssignments(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestParam(value = "teacherId", required = false) Long teacherId,
            @RequestParam(value = "page", defaultValue = "1") @Min(1) Integer page,
            @RequestParam(value = "size", defaultValue = "10") @Min(1) @Max(100) Integer size,
            @RequestParam(value = "courseId", required = false) Long courseId,
            @RequestParam(value = "classId", required = false) Long classId) {
        Long resolvedTeacherId = CourseController.resolveTeacherId(userIdHeader, teacherId);
        List<CourseAssignmentDTO> assignments = courseApplicationService.listCourseAssignments(resolvedTeacherId, courseId, classId);
        return ResponseResult.success(toPage(assignments, page, size), "获取课程分配列表成功", 200);
    }

    @PostMapping("/course-assignments")
    public ResponseResult<Map<String, Object>> assignCourse(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestParam(value = "teacherId", required = false) Long teacherId,
            @RequestBody CourseAssignmentRequestDTO request) {
        Long id = courseApplicationService.assignCourse(CourseController.resolveTeacherId(userIdHeader, teacherId), request);
        return ResponseResult.success(Map.of("assignmentId", id), "课程分配成功", 201);
    }

    @DeleteMapping("/course-assignments/{assignmentId}")
    public ResponseResult<Void> unassignCourse(
            @PathVariable Long assignmentId,
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestParam(value = "teacherId", required = false) Long teacherId) {
        courseApplicationService.unassignCourse(CourseController.resolveTeacherId(userIdHeader, teacherId), assignmentId);
        return ResponseResult.success(null, "课程分配已取消", 204);
    }

    @DeleteMapping("/class-courses/unassign")
    public ResponseResult<Void> unassignClassCourse(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestParam(value = "teacherId", required = false) Long teacherId,
            @RequestParam Long classId,
            @RequestParam Long courseId) {
        courseApplicationService.unassignClassCourse(CourseController.resolveTeacherId(userIdHeader, teacherId), classId, courseId);
        return ResponseResult.success(null, "课程分配已取消", 204);
    }

    @GetMapping("/majors")
    public ResponseResult<List<MajorDTO>> listMajors() {
        return ResponseResult.success(courseApplicationService.listMajors(), "获取专业列表成功", 200);
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
}
