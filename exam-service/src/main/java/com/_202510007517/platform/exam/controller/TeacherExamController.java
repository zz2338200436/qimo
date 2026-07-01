package com._202510007517.platform.exam.controller;

import com._202510007517.platform.common.web.CommonTraceConstants;
import com._202510007517.platform.common.web.ResponseResult;
import com._202510007517.platform.exam.api.dto.TeacherExamGradeRequestDTO;
import com._202510007517.platform.exam.domain.ExamSubmissionRecord;
import com._202510007517.platform.exam.api.dto.TeacherExamUpsertRequestDTO;
import com._202510007517.platform.exam.domain.ExamRecord;
import com._202510007517.platform.exam.service.ExamApplicationService;
import jakarta.validation.Valid;
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
@RequestMapping("/api/teacher/exams")
@Validated
public class TeacherExamController {

    private final ExamApplicationService examApplicationService;

    public TeacherExamController(ExamApplicationService examApplicationService) {
        this.examApplicationService = examApplicationService;
    }

    @GetMapping
    public ResponseResult<Map<String, Object>> listExams(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "sortBy", defaultValue = "id") String sortBy,
            @RequestParam(value = "order", defaultValue = "DESC") String order,
            @RequestParam(value = "courseId", required = false) Long courseId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "isOnline", required = false) Boolean isOnline) {
        return ResponseResult.success(
                examApplicationService.listTeacherExams(resolveTeacherId(userIdHeader), page, size, sortBy, order, courseId, status, isOnline),
                "获取考试列表成功",
                200
        );
    }

    @GetMapping("/{examId}")
    public ResponseResult<Map<String, Object>> getExamDetail(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @PathVariable Long examId) {
        return ResponseResult.success(
                examApplicationService.getTeacherExamDetail(resolveTeacherId(userIdHeader), examId),
                "获取考试详情成功",
                200
        );
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResult<ExamRecord> createExam(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestBody @Valid TeacherExamUpsertRequestDTO request) {
        return ResponseResult.created(
                examApplicationService.createTeacherExam(resolveTeacherId(userIdHeader), request)
        );
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseResult<ExamRecord> createExamWithFiles(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestPart("payload") @Valid TeacherExamUpsertRequestDTO request,
            @RequestPart(value = "files", required = false) MultipartFile[] files) {
        return ResponseResult.created(
                examApplicationService.createTeacherExam(resolveTeacherId(userIdHeader), request, files)
        );
    }

    @PutMapping(value = "/{examId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResult<ExamRecord> updateExam(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @PathVariable Long examId,
            @RequestBody @Valid TeacherExamUpsertRequestDTO request) {
        return ResponseResult.success(
                examApplicationService.updateTeacherExam(resolveTeacherId(userIdHeader), examId, request),
                "操作成功",
                200
        );
    }

    @PutMapping(value = "/{examId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseResult<ExamRecord> updateExamWithFiles(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @PathVariable Long examId,
            @RequestPart("payload") @Valid TeacherExamUpsertRequestDTO request,
            @RequestPart(value = "files", required = false) MultipartFile[] files) {
        return ResponseResult.success(
                examApplicationService.updateTeacherExam(resolveTeacherId(userIdHeader), examId, request, files),
                "操作成功",
                200
        );
    }

    @DeleteMapping("/{examId}")
    public ResponseResult<Void> deleteExam(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @PathVariable Long examId) {
        examApplicationService.deleteTeacherExam(resolveTeacherId(userIdHeader), examId);
        return ResponseResult.noContent();
    }

    @GetMapping("/{examId}/submissions")
    public ResponseResult<List<ExamSubmissionRecord>> listExamSubmissions(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @PathVariable Long examId) {
        return ResponseResult.success(
                examApplicationService.listTeacherExamSubmissions(resolveTeacherId(userIdHeader), examId),
                "获取考试提交列表成功",
                200
        );
    }

    @GetMapping("/submissions")
    public ResponseResult<Map<String, Object>> listAllExamSubmissions(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "sortBy", defaultValue = "id") String sortBy,
            @RequestParam(value = "order", defaultValue = "DESC") String order,
            @RequestParam(value = "examId", required = false) Long examId,
            @RequestParam(value = "studentId", required = false) Long studentId,
            @RequestParam(value = "graded", required = false) Boolean graded) {
        return ResponseResult.success(
                examApplicationService.listTeacherExamSubmissions(
                        resolveTeacherId(userIdHeader),
                        page,
                        size,
                        sortBy,
                        order,
                        examId,
                        studentId,
                        graded),
                "获取考试提交记录成功",
                200
        );
    }

    @GetMapping("/submissions/{submissionId}")
    public ResponseResult<ExamSubmissionRecord> getSubmissionDetail(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @PathVariable Long submissionId) {
        return ResponseResult.success(
                examApplicationService.getTeacherExamSubmissionDetail(resolveTeacherId(userIdHeader), submissionId),
                "获取考试提交记录详情成功",
                200
        );
    }

    @PutMapping("/submissions/{submissionId}")
    public ResponseResult<ExamSubmissionRecord> updateSubmission(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @PathVariable Long submissionId,
            @RequestBody ExamSubmissionRecord request) {
        return ResponseResult.success(
                examApplicationService.updateTeacherExamSubmission(resolveTeacherId(userIdHeader), submissionId, request),
                "更新考试提交记录成功",
                200
        );
    }

    @DeleteMapping("/submissions/{submissionId}")
    public ResponseResult<Void> deleteSubmission(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @PathVariable Long submissionId) {
        examApplicationService.deleteTeacherExamSubmission(resolveTeacherId(userIdHeader), submissionId);
        return ResponseResult.noContent();
    }

    @PutMapping("/grade/{submissionId}")
    public ResponseResult<ExamSubmissionRecord> gradeSubmission(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @PathVariable Long submissionId,
            @RequestBody @Valid TeacherExamGradeRequestDTO request) {
        return ResponseResult.success(
                examApplicationService.gradeTeacherExamSubmission(resolveTeacherId(userIdHeader), submissionId, request),
                "考试批改成功",
                200
        );
    }

    private static Long resolveTeacherId(String userIdHeader) {
        if (userIdHeader == null || userIdHeader.isBlank()) {
            throw new IllegalArgumentException("缺少教师身份");
        }
        return Long.valueOf(userIdHeader);
    }
}
