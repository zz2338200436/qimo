package com._202510007517.platform.agent.client;

import com._202510007517.platform.common.web.CommonTraceConstants;
import com._202510007517.platform.common.web.ResponseResult;
import com._202510007517.platform.exam.api.dto.ExamDTO;
import com._202510007517.platform.exam.api.dto.ExamSubmissionDTO;
import com._202510007517.platform.exam.api.dto.TeacherExamGradeRequestDTO;
import com._202510007517.platform.exam.api.dto.TeacherExamUpsertRequestDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@FeignClient(contextId = "teacherExamEdgeClient", name = "exam-service", path = "/api/teacher/exams")
public interface TeacherExamEdgeClient {

    @GetMapping
    ResponseResult<Map<String, Object>> listExams(
            @RequestHeader(CommonTraceConstants.USER_ID_HEADER) String userId,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "sortBy", defaultValue = "id") String sortBy,
            @RequestParam(value = "order", defaultValue = "DESC") String order,
            @RequestParam(value = "courseId", required = false) Long courseId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "isOnline", required = false) Boolean isOnline);

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseResult<ExamDTO> createExamWithFiles(
            @RequestHeader(CommonTraceConstants.USER_ID_HEADER) String userId,
            @RequestPart("payload") TeacherExamUpsertRequestDTO request,
            @RequestPart(value = "files", required = false) MultipartFile[] files);

    @GetMapping("/{examId}/submissions")
    ResponseResult<List<ExamSubmissionDTO>> listExamSubmissions(
            @RequestHeader(CommonTraceConstants.USER_ID_HEADER) String userId,
            @PathVariable("examId") Long examId);

    @PutMapping("/grade/{submissionId}")
    ResponseResult<ExamSubmissionDTO> gradeSubmission(
            @RequestHeader(CommonTraceConstants.USER_ID_HEADER) String userId,
            @PathVariable("submissionId") Long submissionId,
            @RequestBody TeacherExamGradeRequestDTO request);
}
