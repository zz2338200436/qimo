package com._202510007517.platform.assignment.controller;

import com._202510007517.platform.assignment.api.dto.AssignmentSubmissionDTO;
import com._202510007517.platform.assignment.service.AssignmentApplicationService;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StudentAssignmentControllerTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void listAssignmentsReturnsPagedEnvelope() throws Exception {
        AssignmentApplicationService service = mock(AssignmentApplicationService.class);
        when(service.listStudentAssignments(42L, 1, 10, "dueDate", "DESC", null, null, null)).thenReturn(Map.of(
                "content", List.of(Map.of("id", 2001L, "title", "Homework 1", "courseName", "分布式框架技术")),
                "totalElements", 1,
                "totalPages", 1
        ));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new StudentAssignmentController(service, validator)).build();

        mockMvc.perform(get("/api/student/assignments")
                        .header("X-User-Id", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(2001))
                .andExpect(jsonPath("$.data.content[0].courseName").value("分布式框架技术"));
    }

    @Test
    void getAssignmentDetailReturnsSubmissionEnvelope() throws Exception {
        AssignmentApplicationService service = mock(AssignmentApplicationService.class);
        when(service.getStudentAssignmentDetail(42L, 2001L)).thenReturn(Map.of(
                "id", 2001L,
                "title", "Homework 1",
                "submission", Map.of("id", 3001L, "graded", false)
        ));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new StudentAssignmentController(service, validator)).build();

        mockMvc.perform(get("/api/student/assignments/2001")
                        .header("X-User-Id", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(2001))
                .andExpect(jsonPath("$.data.submission.id").value(3001));
    }

    @Test
    void listAssignmentSubmissionsReturnsEnvelope() throws Exception {
        AssignmentApplicationService service = mock(AssignmentApplicationService.class);
        when(service.listStudentSubmissions(42L)).thenReturn(List.of(Map.of(
                "id", 3001L,
                "title", "Homework 1",
                "courseName", "分布式框架技术",
                "graded", false
        )));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new StudentAssignmentController(service, validator)).build();

        mockMvc.perform(get("/api/student/assignment-submissions")
                        .header("X-User-Id", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(3001))
                .andExpect(jsonPath("$.data[0].title").value("Homework 1"));
    }

    @Test
    void submitAssignmentInjectsStudentIdAndReturnsCompactEnvelope() throws Exception {
        AssignmentApplicationService service = mock(AssignmentApplicationService.class);
        AssignmentSubmissionDTO dto = new AssignmentSubmissionDTO();
        dto.setId(3001L);
        dto.setAssignmentId(2001L);
        dto.setStudentId(42L);
        dto.setSubmissionDate("2026-09-01 10:00:00");
        dto.setIsLate(false);
        dto.setLatePenalty(null);
        dto.setGraded(false);
        when(service.submit(eq(2001L), any())).thenReturn(dto);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new StudentAssignmentController(service, validator)).build();

        mockMvc.perform(post("/api/student/assignments/2001/submit")
                        .header("X-User-Id", "42")
                        .contentType("application/json")
                        .content("""
                                {
                                  "content": "my answer"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(3001))
                .andExpect(jsonPath("$.data.submissionDate").value("2026-09-01 10:00:00"))
                .andExpect(jsonPath("$.data.isLate").value(false))
                .andExpect(jsonPath("$.data.graded").value(false));

        verify(service).submit(eq(2001L), any());
    }
}
