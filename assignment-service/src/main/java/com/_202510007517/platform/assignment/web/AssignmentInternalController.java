package com._202510007517.platform.assignment.web;

import com._202510007517.platform.assignment.api.dto.AssignmentDTO;
import com._202510007517.platform.assignment.api.dto.AssignmentStudentScoreDTO;
import com._202510007517.platform.assignment.api.dto.AssignmentSubmissionDTO;
import com._202510007517.platform.assignment.api.dto.AssignmentSubmitRequestDTO;
import com._202510007517.platform.assignment.service.AssignmentApplicationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal/assignments")
public class AssignmentInternalController {

    private final AssignmentApplicationService assignmentApplicationService;

    public AssignmentInternalController(AssignmentApplicationService assignmentApplicationService) {
        this.assignmentApplicationService = assignmentApplicationService;
    }

    @GetMapping("/{assignmentId}")
    public AssignmentDTO getAssignment(@PathVariable Long assignmentId) {
        return assignmentApplicationService.getAssignment(assignmentId);
    }

    @GetMapping("/course/{courseId}")
    public List<AssignmentDTO> listByCourse(@PathVariable Long courseId) {
        return assignmentApplicationService.listByCourse(courseId);
    }

    @GetMapping("/{assignmentId}/knowledge-point-ids")
    public List<Long> listKnowledgePointIds(@PathVariable Long assignmentId) {
        return assignmentApplicationService.listKnowledgePointIds(assignmentId);
    }

    @GetMapping("/student/{studentId}")
    public Map<String, Object> listStudentAssignments(@PathVariable Long studentId,
                                                      @RequestParam(value = "page", defaultValue = "1") Integer page,
                                                      @RequestParam(value = "size", defaultValue = "10") Integer size,
                                                      @RequestParam(value = "sortBy", defaultValue = "dueDate") String sortBy,
                                                      @RequestParam(value = "order", defaultValue = "DESC") String order,
                                                      @RequestParam(value = "courseId", required = false) Long courseId,
                                                      @RequestParam(value = "submitted", required = false) Boolean submitted,
                                                      @RequestParam(value = "isActive", required = false) Boolean isActive) {
        return assignmentApplicationService.listStudentAssignments(
                studentId, page, size, sortBy, order, courseId, submitted, isActive);
    }

    @GetMapping("/student/{studentId}/{assignmentId}")
    public Map<String, Object> getStudentAssignmentDetail(@PathVariable Long studentId,
                                                          @PathVariable Long assignmentId) {
        return assignmentApplicationService.getStudentAssignmentDetail(studentId, assignmentId);
    }

    @GetMapping("/student/{studentId}/submissions")
    public List<Map<String, Object>> listStudentSubmissions(@PathVariable Long studentId) {
        return assignmentApplicationService.listStudentSubmissions(studentId);
    }

    @GetMapping("/student/{studentId}/scores")
    public List<AssignmentStudentScoreDTO> listStudentScores(@PathVariable Long studentId) {
        return assignmentApplicationService.listStudentScores(studentId);
    }

    @PostMapping("/{assignmentId}/submissions")
    public AssignmentSubmissionDTO submit(@PathVariable Long assignmentId,
                                          @RequestBody @Valid AssignmentSubmitRequestDTO request) {
        return assignmentApplicationService.submit(assignmentId, request);
    }
}
