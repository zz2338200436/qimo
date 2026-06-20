package com._202510007517.platform.agent.client;

import com._202510007517.platform.assignment.api.dto.AssignmentSubmissionDTO;
import com._202510007517.platform.assignment.api.dto.TeacherAssignmentGradeRequestDTO;
import com._202510007517.platform.common.web.CommonTraceConstants;
import com._202510007517.platform.common.web.ResponseResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(contextId = "teacherSubmissionEdgeClient", name = "assignment-service", path = "/api/teacher/submissions")
public interface TeacherSubmissionEdgeClient {

    @PutMapping("/{submissionId}/grade")
    ResponseResult<AssignmentSubmissionDTO> gradeSubmission(
            @RequestHeader(CommonTraceConstants.USER_ID_HEADER) String userId,
            @PathVariable("submissionId") Long submissionId,
            @RequestBody TeacherAssignmentGradeRequestDTO request);
}
