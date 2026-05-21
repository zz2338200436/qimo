package com._202510007517.platform.assignment.web;

import com._202510007517.platform.assignment.api.dto.AssignmentSubmissionDTO;
import com._202510007517.platform.assignment.service.TeacherAssignmentCommandService;
import com._202510007517.platform.assignment.service.TeacherAssignmentQueryService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TeacherSubmissionControllerTest {

    @Test
    void listSubmissionsReturnsPagedEnvelope() throws Exception {
        TeacherAssignmentQueryService queryService = mock(TeacherAssignmentQueryService.class);
        TeacherAssignmentCommandService commandService = mock(TeacherAssignmentCommandService.class);
        when(queryService.listSubmissions(7L, 1, 10, "id", "DESC", null, null, null)).thenReturn(Map.of(
                "content", List.of(Map.of("id", 3001L, "title", "Homework 1", "studentName", "李同学")),
                "totalElements", 1,
                "totalPages", 1
        ));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TeacherSubmissionController(queryService, commandService)).build();

        mockMvc.perform(get("/api/teacher/submissions")
                        .header("X-User-Id", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(3001))
                .andExpect(jsonPath("$.data.content[0].studentName").value("李同学"));
    }

    @Test
    void getSubmissionReturnsEnvelope() throws Exception {
        TeacherAssignmentQueryService queryService = mock(TeacherAssignmentQueryService.class);
        TeacherAssignmentCommandService commandService = mock(TeacherAssignmentCommandService.class);
        AssignmentSubmissionDTO dto = new AssignmentSubmissionDTO();
        dto.setId(3001L);
        dto.setTitle("Homework 1");
        dto.setStudentName("李同学");
        when(queryService.getAssignmentSubmission(7L, 3001L)).thenReturn(dto);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TeacherSubmissionController(queryService, commandService)).build();

        mockMvc.perform(get("/api/teacher/submissions/3001")
                        .header("X-User-Id", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(3001))
                .andExpect(jsonPath("$.data.studentName").value("李同学"));
    }

    @Test
    void gradeSubmissionReturnsEnvelope() throws Exception {
        TeacherAssignmentQueryService queryService = mock(TeacherAssignmentQueryService.class);
        TeacherAssignmentCommandService commandService = mock(TeacherAssignmentCommandService.class);
        AssignmentSubmissionDTO dto = new AssignmentSubmissionDTO();
        dto.setId(3001L);
        dto.setTitle("Homework 1");
        dto.setStudentName("李同学");
        dto.setGraded(true);
        dto.setScore(95);
        dto.setTeacherComment("做得不错");
        when(commandService.gradeSubmission(eq(7L), eq(3001L), any())).thenReturn(dto);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TeacherSubmissionController(queryService, commandService)).build();

        mockMvc.perform(put("/api/teacher/submissions/3001/grade")
                        .header("X-User-Id", "7")
                        .contentType("application/json")
                        .content("""
                                {
                                  "score": 95,
                                  "teacherComment": "做得不错",
                                  "graded": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(3001))
                .andExpect(jsonPath("$.data.graded").value(true))
                .andExpect(jsonPath("$.data.score").value(95));

        verify(commandService).gradeSubmission(eq(7L), eq(3001L), any());
    }
}
