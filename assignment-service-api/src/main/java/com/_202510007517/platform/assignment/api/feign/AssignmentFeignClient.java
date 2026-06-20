package com._202510007517.platform.assignment.api.feign;

import com._202510007517.platform.assignment.api.dto.AssignmentDTO;
import com._202510007517.platform.assignment.api.dto.AssignmentStudentScoreDTO;
import com._202510007517.platform.assignment.api.dto.AssignmentSubmissionDTO;
import com._202510007517.platform.assignment.api.dto.AssignmentSubmitRequestDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "assignment-service", path = "/internal/assignments")
public interface AssignmentFeignClient {

    @GetMapping("/{assignmentId}")
    AssignmentDTO getAssignment(@PathVariable("assignmentId") Long assignmentId);

    @GetMapping("/course/{courseId}")
    List<AssignmentDTO> listByCourse(@PathVariable("courseId") Long courseId);

    @GetMapping("/{assignmentId}/knowledge-point-ids")
    List<Long> listKnowledgePointIds(@PathVariable("assignmentId") Long assignmentId);

    @GetMapping("/student/{studentId}/scores")
    List<AssignmentStudentScoreDTO> listStudentScores(@PathVariable("studentId") Long studentId);

    @GetMapping("/student/{studentId}")
    Map<String, Object> listStudentAssignments(@PathVariable("studentId") Long studentId,
                                               @RequestParam("page") Integer page,
                                               @RequestParam("size") Integer size,
                                               @RequestParam("sortBy") String sortBy,
                                               @RequestParam("order") String order,
                                               @RequestParam(value = "courseId", required = false) Long courseId,
                                               @RequestParam(value = "submitted", required = false) Boolean submitted,
                                               @RequestParam(value = "isActive", required = false) Boolean isActive);

    @PostMapping("/{assignmentId}/submissions")
    AssignmentSubmissionDTO submit(@PathVariable("assignmentId") Long assignmentId,
                                   @RequestBody AssignmentSubmitRequestDTO request);
}
