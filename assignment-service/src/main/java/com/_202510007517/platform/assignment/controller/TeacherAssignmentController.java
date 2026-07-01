package com._202510007517.platform.assignment.controller;

import com._202510007517.platform.assignment.api.dto.AssignmentDTO;
import com._202510007517.platform.assignment.api.dto.AssignmentSubmissionDTO;
import com._202510007517.platform.assignment.api.dto.TeacherAssignmentUpsertRequestDTO;
import com._202510007517.platform.assignment.service.TeacherAssignmentCommandService;
import com._202510007517.platform.assignment.service.TeacherAssignmentQueryService;
import com._202510007517.platform.common.web.CommonTraceConstants;
import com._202510007517.platform.common.web.ResponseResult;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher/assignments")
@Validated
public class TeacherAssignmentController {

    private final TeacherAssignmentQueryService teacherAssignmentQueryService;
    private final TeacherAssignmentCommandService teacherAssignmentCommandService;

    public TeacherAssignmentController(TeacherAssignmentQueryService teacherAssignmentQueryService,
                                       TeacherAssignmentCommandService teacherAssignmentCommandService) {
        this.teacherAssignmentQueryService = teacherAssignmentQueryService;
        this.teacherAssignmentCommandService = teacherAssignmentCommandService;
    }

    @GetMapping
    public ResponseResult<Map<String, Object>> listAssignments(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestParam(value = "teacherId", required = false) Long teacherId,
            @RequestParam(value = "page", defaultValue = "1") @Min(1) Integer page,
            @RequestParam(value = "size", defaultValue = "10") @Min(1) @Max(100) Integer size,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "courseId", required = false) Long courseId,
            @RequestParam(value = "isActive", required = false) Boolean isActive,
            @RequestParam(value = "status", required = false) String status) {
        Long resolvedTeacherId = resolveTeacherId(userIdHeader, teacherId);
        return ResponseResult.success(
                teacherAssignmentQueryService.listAssignments(resolvedTeacherId, page, size, keyword, courseId, isActive, status),
                "获取作业列表成功",
                200);
    }

    @GetMapping("/{assignmentId}")
    public ResponseResult<AssignmentDTO> getAssignment(
            @PathVariable Long assignmentId,
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestParam(value = "teacherId", required = false) Long teacherId) {
        return ResponseResult.success(
                teacherAssignmentQueryService.getAssignmentDetail(resolveTeacherId(userIdHeader, teacherId), assignmentId),
                "获取作业详情成功",
                200);
    }

    @GetMapping("/{assignmentId}/submissions")
    public ResponseResult<List<AssignmentSubmissionDTO>> listAssignmentSubmissions(
            @PathVariable Long assignmentId,
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestParam(value = "teacherId", required = false) Long teacherId) {
        return ResponseResult.success(
                teacherAssignmentQueryService.listAssignmentSubmissions(resolveTeacherId(userIdHeader, teacherId), assignmentId),
                "获取作业提交记录成功",
                200);
    }

    @GetMapping("/submissions/{submissionId}")
    public ResponseResult<AssignmentSubmissionDTO> getAssignmentSubmission(
            @PathVariable Long submissionId,
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestParam(value = "teacherId", required = false) Long teacherId) {
        return ResponseResult.success(
                teacherAssignmentQueryService.getAssignmentSubmission(resolveTeacherId(userIdHeader, teacherId), submissionId),
                "获取作业提交记录详情成功",
                200);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResult<AssignmentDTO> createAssignment(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestParam(value = "teacherId", required = false) Long teacherId,
            @RequestBody @Valid TeacherAssignmentUpsertRequestDTO request) {
        return ResponseResult.created(
                teacherAssignmentCommandService.createAssignment(resolveTeacherId(userIdHeader, teacherId), request));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseResult<AssignmentDTO> createAssignmentWithFiles(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestParam(value = "teacherId", required = false) Long teacherId,
            @RequestPart("payload") @Valid TeacherAssignmentUpsertRequestDTO request,
            @RequestPart(value = "files", required = false) MultipartFile[] files) {
        return ResponseResult.created(
                teacherAssignmentCommandService.createAssignment(resolveTeacherId(userIdHeader, teacherId), request, files));
    }

    @PutMapping(value = "/{assignmentId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResult<AssignmentDTO> updateAssignment(
            @PathVariable Long assignmentId,
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestParam(value = "teacherId", required = false) Long teacherId,
            @RequestBody @Valid TeacherAssignmentUpsertRequestDTO request) {
        return ResponseResult.success(
                teacherAssignmentCommandService.updateAssignment(resolveTeacherId(userIdHeader, teacherId), assignmentId, request),
                "更新作业成功",
                200);
    }

    @PutMapping(value = "/{assignmentId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseResult<AssignmentDTO> updateAssignmentWithFiles(
            @PathVariable Long assignmentId,
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestParam(value = "teacherId", required = false) Long teacherId,
            @RequestPart("payload") @Valid TeacherAssignmentUpsertRequestDTO request,
            @RequestPart(value = "files", required = false) MultipartFile[] files) {
        return ResponseResult.success(
                teacherAssignmentCommandService.updateAssignment(resolveTeacherId(userIdHeader, teacherId), assignmentId, request, files),
                "更新作业成功",
                200);
    }

    @DeleteMapping("/{assignmentId}")
    public ResponseResult<Void> deleteAssignment(
            @PathVariable Long assignmentId,
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestParam(value = "teacherId", required = false) Long teacherId) {
        teacherAssignmentCommandService.deleteAssignment(resolveTeacherId(userIdHeader, teacherId), assignmentId);
        return ResponseResult.noContent();
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
