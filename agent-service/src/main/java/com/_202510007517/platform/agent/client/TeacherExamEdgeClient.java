package com._202510007517.platform.agent.client;

import com._202510007517.platform.common.web.CommonTraceConstants;
import com._202510007517.platform.common.web.ResponseResult;
import com._202510007517.platform.exam.api.dto.ExamSubmissionDTO;
import com._202510007517.platform.exam.api.dto.TeacherExamGradeRequestDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@FeignClient(contextId = "teacherExamEdgeClient", name = "exam-service", path = "/api/teacher/exams")
public interface TeacherExamEdgeClient {

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
