package com._202510007517.platform.exam.controller;

import com._202510007517.platform.common.web.CommonTraceConstants;
import com._202510007517.platform.exam.api.dto.ExamSubmissionDTO;
import com._202510007517.platform.exam.api.dto.StudentScoreListItemDTO;
import com._202510007517.platform.exam.service.ExamApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StudentExamController.class)
@Import(com._202510007517.platform.exam.config.ExamServiceExceptionHandler.class)
class StudentExamControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExamApplicationService examApplicationService;

    @Test
    void listExamsUsesGatewayInjectedUserIdHeader() throws Exception {
        Map<String, Object> page = new LinkedHashMap<>();
        page.put("content", List.of());
        when(examApplicationService.listStudentExams(42L, 1, 10, "startTime", "DESC", null, null, null))
                .thenReturn(page);

        mockMvc.perform(get("/api/student/exams")
                        .header(CommonTraceConstants.USER_ID_HEADER, "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("获取考试列表成功"));

        verify(examApplicationService).listStudentExams(42L, 1, 10, "startTime", "DESC", null, null, null);
    }

    @Test
    void submitExamReturnsExpectedResponseShape() throws Exception {
        ExamSubmissionDTO submission = new ExamSubmissionDTO();
        submission.setId(9004L);
        submission.setSubmissionDate("2026-05-17 09:00:00");
        submission.setTimeTaken(48);
        submission.setGraded(false);
        when(examApplicationService.submit(eq(9001L), org.mockito.ArgumentMatchers.any())).thenReturn(submission);

        mockMvc.perform(post("/api/student/exams/9001/submit")
                        .header(CommonTraceConstants.USER_ID_HEADER, "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "timeTaken": 48,
                                  "answers": {
                                    "q1": "A"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("考试提交成功"))
                .andExpect(jsonPath("$.data.id").value(9004))
                .andExpect(jsonPath("$.data.timeTaken").value(48))
                .andExpect(jsonPath("$.data.graded").value(false));
    }

    @Test
    void submitExamAcceptsMultipartAttachments() throws Exception {
        ExamSubmissionDTO submission = new ExamSubmissionDTO();
        submission.setId(9004L);
        submission.setSubmissionDate("2026-05-17 09:00:00");
        submission.setTimeTaken(48);
        submission.setGraded(false);
        submission.setAttachments(List.of(Map.of(
                "id", 8001L,
                "name", "考试附件.pdf",
                "downloadUrl", "/api/attachments/exam/8001/download"
        )));
        when(examApplicationService.submit(eq(9001L), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(submission);

        mockMvc.perform(multipart("/api/student/exams/9001/submit")
                        .file(new org.springframework.mock.web.MockMultipartFile(
                                "payload",
                                "",
                                "application/json",
                                """
                                        {
                                          "timeTaken": 48,
                                          "answers": {
                                            "q1": "A"
                                          }
                                        }
                                        """.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                        .file(new org.springframework.mock.web.MockMultipartFile(
                                "files",
                                "考试附件.pdf",
                                "application/pdf",
                                "file content".getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                        .header(CommonTraceConstants.USER_ID_HEADER, "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("考试提交成功"))
                .andExpect(jsonPath("$.data.id").value(9004))
                .andExpect(jsonPath("$.data.attachments[0].name").value("考试附件.pdf"))
                .andExpect(jsonPath("$.data.attachments[0].downloadUrl").value("/api/attachments/exam/8001/download"));

        verify(examApplicationService).submit(eq(9001L), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void listScoresReturnsFlatArrayPayload() throws Exception {
        StudentScoreListItemDTO dto = new StudentScoreListItemDTO();
        dto.setId(2001L);
        dto.setType("assignment");
        dto.setCourseName("分布式框架技术");
        dto.setTitle("Homework 1");
        dto.setSubmitDate("2026-09-01 10:00:00");
        dto.setScore(95);
        when(examApplicationService.listStudentScores(42L)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/student/scores")
                        .header(CommonTraceConstants.USER_ID_HEADER, "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("获取成绩列表成功"))
                .andExpect(jsonPath("$.data[0].type").value("assignment"))
                .andExpect(jsonPath("$.data[0].courseName").value("分布式框架技术"))
                .andExpect(jsonPath("$.data[0].submitDate").value("2026-09-01 10:00:00"));
    }
}
