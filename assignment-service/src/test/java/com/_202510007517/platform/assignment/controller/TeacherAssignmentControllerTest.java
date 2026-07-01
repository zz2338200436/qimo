package com._202510007517.platform.assignment.controller;

import com._202510007517.platform.assignment.api.dto.AssignmentDTO;
import com._202510007517.platform.assignment.service.AssignmentKnowledgePointService;
import com._202510007517.platform.assignment.service.TeacherAssignmentCommandService;
import com._202510007517.platform.assignment.service.TeacherAssignmentQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TeacherAssignmentControllerTest {

    @Test
    void listAssignmentsReturnsResponseEnvelope() throws Exception {
        TeacherAssignmentQueryService service = mock(TeacherAssignmentQueryService.class);
        TeacherAssignmentCommandService commandService = mock(TeacherAssignmentCommandService.class);
        AssignmentDTO dto = new AssignmentDTO();
        dto.setId(2001L);
        dto.setTitle("Homework 1");
        when(service.listAssignments(7L, 1, 10, null, null, null, null)).thenReturn(Map.of(
                "content", List.of(dto),
                "totalPages", 1,
                "totalElements", 1,
                "size", 10
        ));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TeacherAssignmentController(service, commandService)).build();

        mockMvc.perform(get("/api/teacher/assignments")
                        .header("X-User-Id", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(2001))
                .andExpect(jsonPath("$.data.content[0].title").value("Homework 1"));
    }

    @Test
    void createAssignmentReturnsCreatedEnvelope() throws Exception {
        TeacherAssignmentQueryService queryService = mock(TeacherAssignmentQueryService.class);
        TeacherAssignmentCommandService commandService = mock(TeacherAssignmentCommandService.class);
        AssignmentDTO dto = new AssignmentDTO();
        dto.setId(2002L);
        dto.setTitle("Homework 2");
        when(commandService.createAssignment(any(), any())).thenReturn(dto);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TeacherAssignmentController(queryService, commandService)).build();

        mockMvc.perform(post("/api/teacher/assignments")
                        .header("X-User-Id", "7")
                        .contentType("application/json")
                        .content("""
                                {
                                  "title": "Homework 2",
                                  "description": "chapter 2",
                                  "courseId": 101,
                                  "publishDate": "2026-09-01 08:00:00",
                                  "dueDate": "2026-09-15 23:59:59",
                                  "isActive": true,
                                  "maxScore": 100
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data.id").value(2002))
                .andExpect(jsonPath("$.data.title").value("Homework 2"));
    }

    @Test
    void createAssignmentAcceptsMultipartAttachments() throws Exception {
        TeacherAssignmentQueryService queryService = mock(TeacherAssignmentQueryService.class);
        TeacherAssignmentCommandService commandService = mock(TeacherAssignmentCommandService.class);
        AssignmentDTO dto = new AssignmentDTO();
        dto.setId(2002L);
        dto.setTitle("Homework 2");
        when(commandService.createAssignment(any(), any(), any())).thenReturn(dto);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TeacherAssignmentController(queryService, commandService)).build();

        mockMvc.perform(multipart("/api/teacher/assignments")
                        .file(new org.springframework.mock.web.MockMultipartFile(
                                "payload",
                                "",
                                "application/json",
                                """
                                        {
                                          "title": "Homework 2",
                                          "description": "chapter 2",
                                          "courseId": 101,
                                          "publishDate": "2026-09-01 08:00:00",
                                          "dueDate": "2026-09-15 23:59:59",
                                          "isActive": true,
                                          "maxScore": 100
                                        }
                                        """.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                        .file(new org.springframework.mock.web.MockMultipartFile(
                                "files",
                                "实验说明.docx",
                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                "content".getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                        .header("X-User-Id", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data.id").value(2002));

        verify(commandService).createAssignment(
                org.mockito.ArgumentMatchers.eq(7L),
                any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void updateAssignmentReturnsUpdatedEnvelope() throws Exception {
        TeacherAssignmentQueryService queryService = mock(TeacherAssignmentQueryService.class);
        TeacherAssignmentCommandService commandService = mock(TeacherAssignmentCommandService.class);
        AssignmentDTO dto = new AssignmentDTO();
        dto.setId(2001L);
        dto.setTitle("Homework 1 revised");
        when(commandService.updateAssignment(any(), any(), any())).thenReturn(dto);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TeacherAssignmentController(queryService, commandService)).build();

        mockMvc.perform(put("/api/teacher/assignments/2001")
                        .header("X-User-Id", "7")
                        .contentType("application/json")
                        .content("""
                                {
                                  "title": "Homework 1 revised",
                                  "description": "chapter 3",
                                  "courseId": 101,
                                  "publishDate": "2026-09-01T08:00:00.000Z",
                                  "dueDate": "2026-09-20T23:59:59.000Z",
                                  "isActive": true,
                                  "maxScore": 120
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(2001))
                .andExpect(jsonPath("$.data.title").value("Homework 1 revised"));
    }

    @Test
    void updateAssignmentAcceptsMultipartAttachments() throws Exception {
        TeacherAssignmentQueryService queryService = mock(TeacherAssignmentQueryService.class);
        TeacherAssignmentCommandService commandService = mock(TeacherAssignmentCommandService.class);
        AssignmentDTO dto = new AssignmentDTO();
        dto.setId(2001L);
        dto.setTitle("Homework 1 revised");
        when(commandService.updateAssignment(any(), any(), any(), any())).thenReturn(dto);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TeacherAssignmentController(queryService, commandService)).build();

        mockMvc.perform(multipart("/api/teacher/assignments/2001")
                        .file(new org.springframework.mock.web.MockMultipartFile(
                                "payload",
                                "",
                                "application/json",
                                """
                                        {
                                          "title": "Homework 1 revised",
                                          "description": "chapter 3",
                                          "courseId": 101,
                                          "publishDate": "2026-09-01 08:00:00",
                                          "dueDate": "2026-09-20 23:59:59",
                                          "isActive": true,
                                          "maxScore": 120
                                        }
                                        """.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                        .file(new org.springframework.mock.web.MockMultipartFile(
                                "files",
                                "实验补充说明.docx",
                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                "content".getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .header("X-User-Id", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(2001));

        verify(commandService).updateAssignment(
                org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.eq(2001L),
                any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deleteAssignmentReturnsNoContentEnvelope() throws Exception {
        TeacherAssignmentQueryService queryService = mock(TeacherAssignmentQueryService.class);
        TeacherAssignmentCommandService commandService = mock(TeacherAssignmentCommandService.class);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TeacherAssignmentController(queryService, commandService)).build();

        mockMvc.perform(delete("/api/teacher/assignments/2001")
                        .header("X-User-Id", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(204));
    }

    @Test
    void listAssignmentKnowledgePointsKeepsLegacyEnvelope() throws Exception {
        AssignmentKnowledgePointService service = mock(AssignmentKnowledgePointService.class);
        when(service.listKnowledgePoints(7L, 2001L)).thenReturn(List.of(Map.of(
                "id", 99L,
                "knowledgePointId", 99L,
                "pointName", "函数",
                "name", "函数"
        )));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AssignmentKnowledgePointCompatibilityController(service)).build();

        mockMvc.perform(get("/api/teacher/knowledge-points/assignment/2001")
                        .header("X-User-Id", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("获取作业知识点成功"))
                .andExpect(jsonPath("$.data[0].knowledgePointId").value(99))
                .andExpect(jsonPath("$.data[0].pointName").value("函数"));

        verify(service).listKnowledgePoints(7L, 2001L);
    }

    @Test
    void replaceAssignmentKnowledgePointsKeepsLegacyEnvelope() throws Exception {
        AssignmentKnowledgePointService service = mock(AssignmentKnowledgePointService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AssignmentKnowledgePointCompatibilityController(service)).build();

        mockMvc.perform(post("/api/teacher/knowledge-points/assignment/2001")
                        .header("X-User-Id", "7")
                        .contentType("application/json")
                        .content("""
                                {
                                  "knowledgePointIds": [99, 100]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("设置作业知识点成功"))
                .andExpect(jsonPath("$.code").value(200));

        verify(service).replaceKnowledgePoints(7L, 2001L, List.of(99L, 100L));
    }
}
