package com._202510007517.platform.assignment.controller;

import com._202510007517.platform.assignment.api.dto.AssignmentSubmitRequestDTO;
import com._202510007517.platform.assignment.api.dto.AssignmentSubmissionDTO;
import com._202510007517.platform.assignment.service.AssignmentApplicationService;
import com._202510007517.platform.common.web.CommonTraceConstants;
import com._202510007517.platform.common.web.ResponseResult;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student")
@Validated
public class StudentAssignmentController {

    private final AssignmentApplicationService assignmentApplicationService;
    private final Validator validator;

    public StudentAssignmentController(AssignmentApplicationService assignmentApplicationService,
                                       Validator validator) {
        this.assignmentApplicationService = assignmentApplicationService;
        this.validator = validator;
    }

    @GetMapping("/assignments")
    public ResponseResult<Map<String, Object>> listAssignments(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestParam(value = "studentId", required = false) Long studentId,
            @RequestParam(value = "page", defaultValue = "1") @Min(1) Integer page,
            @RequestParam(value = "size", defaultValue = "10") @Min(1) @Max(100) Integer size,
            @RequestParam(value = "sortBy", defaultValue = "dueDate") String sortBy,
            @RequestParam(value = "order", defaultValue = "DESC") String order,
            @RequestParam(value = "courseId", required = false) Long courseId,
            @RequestParam(value = "submitted", required = false) Boolean submitted,
            @RequestParam(value = "isActive", required = false) Boolean isActive) {
        Long resolvedStudentId = resolveStudentId(userIdHeader, studentId);
        return ResponseResult.success(
                assignmentApplicationService.listStudentAssignments(
                        resolvedStudentId, page, size, sortBy, order, courseId, submitted, isActive),
                "获取作业列表成功",
                200);
    }

    @GetMapping("/assignments/{assignmentId}")
    public ResponseResult<Map<String, Object>> getAssignmentDetail(
            @PathVariable Long assignmentId,
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestParam(value = "studentId", required = false) Long studentId) {
        return ResponseResult.success(
                assignmentApplicationService.getStudentAssignmentDetail(resolveStudentId(userIdHeader, studentId), assignmentId),
                "获取作业详情成功",
                200);
    }

    @GetMapping("/assignment-submissions")
    public ResponseResult<List<Map<String, Object>>> listAssignmentSubmissions(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestParam(value = "studentId", required = false) Long studentId) {
        return ResponseResult.success(
                assignmentApplicationService.listStudentSubmissions(resolveStudentId(userIdHeader, studentId)),
                "获取作业提交记录成功",
                200);
    }

    @PostMapping(value = "/assignments/{assignmentId}/submit", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResult<Map<String, Object>> submitAssignment(
            @PathVariable Long assignmentId,
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestParam(value = "studentId", required = false) Long studentId,
            @RequestBody AssignmentSubmitRequestDTO request) {
        request.setStudentId(resolveStudentId(userIdHeader, studentId));
        validate(request);
        AssignmentSubmissionDTO submission = assignmentApplicationService.submit(assignmentId, request);
        return ResponseResult.success(toSubmissionResponse(submission), "作业提交成功", 200);
    }

    @PostMapping(value = "/assignments/{assignmentId}/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseResult<Map<String, Object>> submitAssignmentWithFiles(
            @PathVariable Long assignmentId,
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestParam(value = "studentId", required = false) Long studentId,
            @RequestPart("payload") AssignmentSubmitRequestDTO request,
            @RequestPart(value = "files", required = false) MultipartFile[] files) {
        request.setStudentId(resolveStudentId(userIdHeader, studentId));
        validate(request);
        AssignmentSubmissionDTO submission = assignmentApplicationService.submit(assignmentId, request, files);
        return ResponseResult.success(toSubmissionResponse(submission), "作业提交成功", 200);
    }

    static Long resolveStudentId(String userIdHeader, Long studentId) {
        if (userIdHeader != null && !userIdHeader.isBlank()) {
            return Long.valueOf(userIdHeader);
        }
        if (studentId != null) {
            return studentId;
        }
        throw new IllegalArgumentException("缺少学生身份");
    }

    private static Map<String, Object> toSubmissionResponse(AssignmentSubmissionDTO submission) {
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("id", submission.getId());
        result.put("submissionDate", submission.getSubmissionDate());
        result.put("isLate", Boolean.TRUE.equals(submission.getIsLate()));
        result.put("latePenalty", submission.getLatePenalty());
        result.put("graded", Boolean.TRUE.equals(submission.getGraded()));
        result.put("attachments", submission.getAttachments() == null ? List.of() : submission.getAttachments());
        return result;
    }

    private void validate(AssignmentSubmitRequestDTO request) {
        for (ConstraintViolation<AssignmentSubmitRequestDTO> violation : validator.validate(request)) {
            throw new IllegalArgumentException(violation.getMessage());
        }
    }
}
