package com._202510007517.platform.exam.controller;

import com._202510007517.platform.common.web.CommonTraceConstants;
import com._202510007517.platform.common.web.ResponseResult;
import com._202510007517.platform.exam.api.dto.ExamSubmissionDTO;
import com._202510007517.platform.exam.api.dto.ExamSubmitRequestDTO;
import com._202510007517.platform.exam.api.dto.StudentScoreListItemDTO;
import com._202510007517.platform.exam.service.ExamApplicationService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student")
@Validated
public class StudentExamController {

    private final ExamApplicationService examApplicationService;
    private final Validator validator;

    public StudentExamController(ExamApplicationService examApplicationService,
                                 Validator validator) {
        this.examApplicationService = examApplicationService;
        this.validator = validator;
    }

    @GetMapping("/exams")
    public ResponseResult<Map<String, Object>> listExams(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestParam(value = "studentId", required = false) Long studentId,
            @RequestParam(value = "page", defaultValue = "1") @Min(1) Integer page,
            @RequestParam(value = "size", defaultValue = "10") @Min(1) @Max(100) Integer size,
            @RequestParam(value = "sortBy", defaultValue = "startTime") String sortBy,
            @RequestParam(value = "order", defaultValue = "DESC") String order,
            @RequestParam(value = "courseId", required = false) Long courseId,
            @RequestParam(value = "isActive", required = false) Boolean isActive,
            @RequestParam(value = "submitted", required = false) Boolean submitted) {
        return ResponseResult.success(
                examApplicationService.listStudentExams(resolveStudentId(userIdHeader, studentId), page, size, sortBy, order, courseId, submitted, isActive),
                "获取考试列表成功",
                200);
    }

    @GetMapping("/exams/{examId}")
    public ResponseResult<Map<String, Object>> getExamDetail(
            @PathVariable Long examId,
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestParam(value = "studentId", required = false) Long studentId) {
        return ResponseResult.success(
                examApplicationService.getStudentExamDetail(resolveStudentId(userIdHeader, studentId), examId),
                "获取考试详情成功",
                200);
    }

    @PostMapping(value = "/exams/{examId}/submit", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResult<Map<String, Object>> submitExam(
            @PathVariable Long examId,
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestParam(value = "studentId", required = false) Long studentId,
            @RequestBody ExamSubmitRequestDTO request) {
        request.setStudentId(resolveStudentId(userIdHeader, studentId));
        validate(request);
        ExamSubmissionDTO submission = examApplicationService.submit(examId, request);
        return ResponseResult.success(toSubmissionResponse(submission), "考试提交成功", 200);
    }

    @PostMapping(value = "/exams/{examId}/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseResult<Map<String, Object>> submitExamWithFiles(
            @PathVariable Long examId,
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestParam(value = "studentId", required = false) Long studentId,
            @RequestPart("payload") ExamSubmitRequestDTO request,
            @RequestPart(value = "files", required = false) MultipartFile[] files) {
        request.setStudentId(resolveStudentId(userIdHeader, studentId));
        validate(request);
        ExamSubmissionDTO submission = examApplicationService.submit(examId, request, files);
        return ResponseResult.success(toSubmissionResponse(submission), "考试提交成功", 200);
    }

    @GetMapping("/scores")
    public ResponseResult<List<StudentScoreListItemDTO>> listScores(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestParam(value = "studentId", required = false) Long studentId) {
        return ResponseResult.success(
                examApplicationService.listStudentScores(resolveStudentId(userIdHeader, studentId)),
                "获取成绩列表成功",
                200);
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

    private static Map<String, Object> toSubmissionResponse(ExamSubmissionDTO submission) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", submission.getId());
        result.put("submissionDate", submission.getSubmissionDate());
        result.put("timeTaken", submission.getTimeTaken());
        result.put("graded", Boolean.TRUE.equals(submission.getGraded()));
        result.put("attachments", submission.getAttachments() == null ? List.of() : submission.getAttachments());
        return result;
    }

    private void validate(ExamSubmitRequestDTO request) {
        for (ConstraintViolation<ExamSubmitRequestDTO> violation : validator.validate(request)) {
            throw new IllegalArgumentException(violation.getMessage());
        }
    }
}
