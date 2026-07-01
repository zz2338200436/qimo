package com._202510007517.platform.assignment.controller;

import com._202510007517.platform.assignment.api.dto.AssignmentSubmissionDTO;
import com._202510007517.platform.assignment.api.dto.TeacherAssignmentGradeRequestDTO;
import com._202510007517.platform.assignment.service.TeacherAssignmentCommandService;
import com._202510007517.platform.assignment.service.TeacherAssignmentQueryService;
import com._202510007517.platform.common.web.CommonTraceConstants;
import com._202510007517.platform.common.web.ResponseResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/teacher/submissions")
@Validated
public class TeacherSubmissionController {

    private final TeacherAssignmentQueryService teacherAssignmentQueryService;
    private final TeacherAssignmentCommandService teacherAssignmentCommandService;

    public TeacherSubmissionController(TeacherAssignmentQueryService teacherAssignmentQueryService,
                                       TeacherAssignmentCommandService teacherAssignmentCommandService) {
        this.teacherAssignmentQueryService = teacherAssignmentQueryService;
        this.teacherAssignmentCommandService = teacherAssignmentCommandService;
    }

    @GetMapping
    public ResponseResult<Map<String, Object>> listSubmissions(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestParam(value = "teacherId", required = false) Long teacherId,
            @RequestParam(value = "page", defaultValue = "1") @Min(1) Integer page,
            @RequestParam(value = "size", defaultValue = "10") @Min(1) @Max(100) Integer size,
            @RequestParam(value = "sortBy", defaultValue = "id") String sortBy,
            @RequestParam(value = "order", defaultValue = "DESC") String order,
            @RequestParam(value = "assignmentId", required = false) Long assignmentId,
            @RequestParam(value = "studentId", required = false) Long studentId,
            @RequestParam(value = "graded", required = false) Boolean graded) {
        return ResponseResult.success(
                teacherAssignmentQueryService.listSubmissions(
                        TeacherAssignmentController.resolveTeacherId(userIdHeader, teacherId),
                        page,
                        size,
                        sortBy,
                        order,
                        assignmentId,
                        studentId,
                        graded),
                "获取作业提交记录成功",
                200);
    }

    @GetMapping("/{submissionId}")
    public ResponseResult<AssignmentSubmissionDTO> getSubmission(
            @PathVariable Long submissionId,
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestParam(value = "teacherId", required = false) Long teacherId) {
        return ResponseResult.success(
                teacherAssignmentQueryService.getAssignmentSubmission(
                        TeacherAssignmentController.resolveTeacherId(userIdHeader, teacherId),
                        submissionId),
                "获取作业提交记录详情成功",
                200);
    }

    @PutMapping("/{submissionId}/grade")
    public ResponseResult<AssignmentSubmissionDTO> gradeSubmission(
            @PathVariable Long submissionId,
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestParam(value = "teacherId", required = false) Long teacherId,
            @RequestBody @Valid TeacherAssignmentGradeRequestDTO request) {
        return ResponseResult.success(
                teacherAssignmentCommandService.gradeSubmission(
                        TeacherAssignmentController.resolveTeacherId(userIdHeader, teacherId),
                        submissionId,
                        request),
                "作业批改成功",
                200);
    }
}
