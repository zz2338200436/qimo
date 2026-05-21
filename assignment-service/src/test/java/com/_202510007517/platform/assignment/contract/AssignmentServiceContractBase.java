package com._202510007517.platform.assignment.contract;

import com._202510007517.platform.assignment.api.dto.AssignmentDTO;
import com._202510007517.platform.assignment.service.AssignmentApplicationService;
import com._202510007517.platform.assignment.web.AssignmentInternalController;
import com._202510007517.platform.assignment.web.StudentAssignmentController;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class AssignmentServiceContractBase {

    private AssignmentApplicationService assignmentApplicationService;
    private Validator validator;

    @BeforeEach
    void setup() {
        assignmentApplicationService = mock(AssignmentApplicationService.class);
        validator = Validation.buildDefaultValidatorFactory().getValidator();
        stubContracts();
        RestAssuredMockMvc.standaloneSetup(
                new StudentAssignmentController(assignmentApplicationService, validator),
                new AssignmentInternalController(assignmentApplicationService)
        );
    }

    private void stubContracts() {
        Map<String, Object> studentAssignment = new LinkedHashMap<>();
        studentAssignment.put("id", 2001L);
        studentAssignment.put("title", "Lab Report");
        studentAssignment.put("description", "Analyze a distributed framework case study");
        studentAssignment.put("courseId", 101L);
        studentAssignment.put("courseName", "Distributed Systems");
        studentAssignment.put("dueDate", "2026-06-01");
        studentAssignment.put("publishDate", "2026-05-20");
        studentAssignment.put("teacherId", 7L);
        studentAssignment.put("teacherName", "Dr. Chen");
        studentAssignment.put("isActive", true);
        studentAssignment.put("submission", null);

        Map<String, Object> page = new LinkedHashMap<>();
        page.put("content", List.of(studentAssignment));
        page.put("totalPages", 1);
        page.put("totalElements", 1);
        page.put("size", 10);
        page.put("number", 0);
        page.put("first", true);
        page.put("last", true);
        page.put("numberOfElements", 1);
        page.put("empty", false);
        when(assignmentApplicationService.listStudentAssignments(42L, 1, 10, "dueDate", "DESC", null, null, null))
                .thenReturn(page);

        AssignmentDTO assignment = new AssignmentDTO();
        assignment.setId(2001L);
        assignment.setTitle("Lab Report");
        assignment.setDescription("Analyze a distributed framework case study");
        assignment.setCourseId(101L);
        assignment.setTeacherId(7L);
        assignment.setDueDate("2026-06-01");
        assignment.setPublishDate("2026-05-20");
        assignment.setMaxScore(100);
        assignment.setIsActive(true);
        assignment.setStatus("ACTIVE");
        assignment.setSubmissionCount(0);
        assignment.setSubmittedCount(0);
        assignment.setGradedCount(0);
        assignment.setTotalStudents(36);
        when(assignmentApplicationService.getAssignment(2001L)).thenReturn(assignment);
        when(assignmentApplicationService.listKnowledgePointIds(2001L)).thenReturn(List.of(501L, 502L));
    }
}
