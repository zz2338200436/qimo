package com._202510007517.platform.assignment.controller;

import com._202510007517.platform.assignment.api.dto.AssignmentDTO;
import com._202510007517.platform.assignment.api.dto.AssignmentStudentScoreDTO;
import com._202510007517.platform.assignment.api.dto.AssignmentSubmissionDTO;
import com._202510007517.platform.assignment.service.AssignmentApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AssignmentInternalControllerTest {

    @Test
    void getAssignmentReturnsDto() throws Exception {
        AssignmentApplicationService service = mock(AssignmentApplicationService.class);
        AssignmentDTO dto = new AssignmentDTO();
        dto.setId(2001L);
        dto.setTitle("Homework 1");
        when(service.getAssignment(2001L)).thenReturn(dto);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AssignmentInternalController(service)).build();

        mockMvc.perform(get("/internal/assignments/2001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2001))
                .andExpect(jsonPath("$.title").value("Homework 1"));
    }

    @Test
    void listByCourseReturnsDtos() throws Exception {
        AssignmentApplicationService service = mock(AssignmentApplicationService.class);
        AssignmentDTO dto = new AssignmentDTO();
        dto.setId(2001L);
        dto.setCourseId(101L);
        when(service.listByCourse(101L)).thenReturn(List.of(dto));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AssignmentInternalController(service)).build();

        mockMvc.perform(get("/internal/assignments/course/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2001))
                .andExpect(jsonPath("$[0].courseId").value(101));
    }

    @Test
    void listKnowledgePointIdsReturnsInternalArray() throws Exception {
        AssignmentApplicationService service = mock(AssignmentApplicationService.class);
        when(service.listKnowledgePointIds(2001L)).thenReturn(List.of(501L, 502L));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AssignmentInternalController(service)).build();

        mockMvc.perform(get("/internal/assignments/2001/knowledge-point-ids"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value(501))
                .andExpect(jsonPath("$[1]").value(502));
    }

    @Test
    void submitDelegatesToService() throws Exception {
        AssignmentApplicationService service = mock(AssignmentApplicationService.class);
        AssignmentSubmissionDTO dto = new AssignmentSubmissionDTO();
        dto.setId(3001L);
        dto.setAssignmentId(2001L);
        when(service.submit(eq(2001L), any())).thenReturn(dto);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AssignmentInternalController(service)).build();

        mockMvc.perform(post("/internal/assignments/2001/submissions")
                        .contentType("application/json")
                        .content("{\"studentId\":42,\"content\":\"my answer\",\"submissionDate\":\"2026-09-01 10:00:00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3001))
                .andExpect(jsonPath("$.assignmentId").value(2001));
    }

    @Test
    void listStudentAssignmentsReturnsPagedContent() throws Exception {
        AssignmentApplicationService service = mock(AssignmentApplicationService.class);
        when(service.listStudentAssignments(42L, 1, 10, "dueDate", "DESC", null, null, null)).thenReturn(Map.of(
                "content", List.of(Map.of("id", 2001L, "title", "Homework 1", "courseName", "分布式框架技术")),
                "totalElements", 1,
                "totalPages", 1
        ));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AssignmentInternalController(service)).build();

        mockMvc.perform(get("/internal/assignments/student/42")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(2001))
                .andExpect(jsonPath("$.content[0].courseName").value("分布式框架技术"));
    }

    @Test
    void getStudentAssignmentDetailReturnsSubmissionBlock() throws Exception {
        AssignmentApplicationService service = mock(AssignmentApplicationService.class);
        when(service.getStudentAssignmentDetail(42L, 2001L)).thenReturn(Map.of(
                "id", 2001L,
                "title", "Homework 1",
                "submission", Map.of("id", 3001L, "graded", false)
        ));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AssignmentInternalController(service)).build();

        mockMvc.perform(get("/internal/assignments/student/42/2001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2001))
                .andExpect(jsonPath("$.submission.id").value(3001))
                .andExpect(jsonPath("$.submission.graded").value(false));
    }

    @Test
    void listStudentSubmissionsReturnsSubmissionRows() throws Exception {
        AssignmentApplicationService service = mock(AssignmentApplicationService.class);
        when(service.listStudentSubmissions(42L)).thenReturn(List.of(Map.of(
                "id", 3001L,
                "assignmentId", 2001L,
                "title", "Homework 1",
                "courseName", "分布式框架技术",
                "graded", false
        )));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AssignmentInternalController(service)).build();

        mockMvc.perform(get("/internal/assignments/student/42/submissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(3001))
                .andExpect(jsonPath("$[0].title").value("Homework 1"))
                .andExpect(jsonPath("$[0].courseName").value("分布式框架技术"));
    }

    @Test
    void listStudentScoresReturnsScoreRows() throws Exception {
        AssignmentApplicationService service = mock(AssignmentApplicationService.class);
        AssignmentStudentScoreDTO dto = new AssignmentStudentScoreDTO();
        dto.setType("assignment");
        dto.setRelatedId(2001L);
        dto.setTitle("Homework 1");
        dto.setCourseName("分布式框架技术");
        dto.setCompletedAt("2026-09-01 10:00:00");
        dto.setSubmitDate("2026-09-01 10:00:00");
        dto.setScore(95);
        dto.setTotalScore(100);
        when(service.listStudentScores(42L)).thenReturn(List.of(dto));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AssignmentInternalController(service)).build();

        mockMvc.perform(get("/internal/assignments/student/42/scores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("assignment"))
                .andExpect(jsonPath("$[0].relatedId").value(2001))
                .andExpect(jsonPath("$[0].courseName").value("分布式框架技术"))
                .andExpect(jsonPath("$[0].score").value(95));
    }
}
