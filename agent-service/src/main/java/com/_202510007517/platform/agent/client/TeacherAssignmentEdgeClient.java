package com._202510007517.platform.agent.client;

import com._202510007517.platform.assignment.api.dto.AssignmentDTO;
import com._202510007517.platform.assignment.api.dto.AssignmentSubmissionDTO;
import com._202510007517.platform.assignment.api.dto.TeacherAssignmentUpsertRequestDTO;
import com._202510007517.platform.common.web.CommonTraceConstants;
import com._202510007517.platform.common.web.ResponseResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@FeignClient(contextId = "teacherAssignmentEdgeClient", name = "assignment-service", path = "/api/teacher/assignments")
public interface TeacherAssignmentEdgeClient {

    @PostMapping
    ResponseResult<AssignmentDTO> createAssignment(
            @RequestHeader(CommonTraceConstants.USER_ID_HEADER) String userId,
            @RequestBody TeacherAssignmentUpsertRequestDTO request);

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseResult<AssignmentDTO> createAssignmentWithFiles(
            @RequestHeader(CommonTraceConstants.USER_ID_HEADER) String userId,
            @RequestPart("payload") TeacherAssignmentUpsertRequestDTO request,
            @RequestPart(value = "files", required = false) MultipartFile[] files);

    @PutMapping("/{assignmentId}")
    ResponseResult<AssignmentDTO> updateAssignment(
            @RequestHeader(CommonTraceConstants.USER_ID_HEADER) String userId,
            @PathVariable("assignmentId") Long assignmentId,
            @RequestBody TeacherAssignmentUpsertRequestDTO request);

    @DeleteMapping("/{assignmentId}")
    ResponseResult<Void> deleteAssignment(
            @RequestHeader(CommonTraceConstants.USER_ID_HEADER) String userId,
            @PathVariable("assignmentId") Long assignmentId);

    @GetMapping("/{assignmentId}")
    ResponseResult<AssignmentDTO> getAssignment(
            @RequestHeader(CommonTraceConstants.USER_ID_HEADER) String userId,
            @PathVariable("assignmentId") Long assignmentId);

    @GetMapping("/{assignmentId}/submissions")
    ResponseResult<List<AssignmentSubmissionDTO>> listAssignmentSubmissions(
            @RequestHeader(CommonTraceConstants.USER_ID_HEADER) String userId,
            @PathVariable("assignmentId") Long assignmentId);
}
