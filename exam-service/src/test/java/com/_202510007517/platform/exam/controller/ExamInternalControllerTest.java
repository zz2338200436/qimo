package com._202510007517.platform.exam.controller;

import com._202510007517.platform.exam.api.dto.ExamDTO;
import com._202510007517.platform.exam.api.dto.ExamSubmissionDTO;
import com._202510007517.platform.exam.api.dto.StudentScoreDTO;
import com._202510007517.platform.exam.api.dto.TeacherExamUpsertRequestDTO;
import com._202510007517.platform.exam.domain.ExamRecord;
import com._202510007517.platform.exam.service.ExamApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExamInternalController.class)
@Import(com._202510007517.platform.exam.config.ExamServiceExceptionHandler.class)
class ExamInternalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExamApplicationService examApplicationService;

    @Test
    void listStudentExamsReturnsInternalDtos() throws Exception {
        when(examApplicationService.listStudentExams(42L, 1, 100, "startTime", "DESC", null, null, true))
                .thenReturn(Map.of("content", List.of(Map.of(
                        "id", 9001L,
                        "title", "Java期末考试",
                        "courseId", 2L,
                        "courseName", "Java企业开发",
                        "duration", 90,
                        "totalScore", 100,
                        "isActive", true,
                        "isOnline", true
                ))));

        mockMvc.perform(get("/internal/exams")
                        .param("studentId", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(9001))
                .andExpect(jsonPath("$[0].title").value("Java期末考试"))
                .andExpect(jsonPath("$[0].courseName").value("Java企业开发"));

        verify(examApplicationService).listStudentExams(42L, 1, 100, "startTime", "DESC", null, null, true);
    }

    @Test
    void getStudentExamReturnsInternalDto() throws Exception {
        when(examApplicationService.getStudentExamDetail(42L, 9001L))
                .thenReturn(Map.of(
                        "id", 9001L,
                        "title", "Java期末考试",
                        "courseId", 2L,
                        "courseName", "Java企业开发",
                        "duration", 90,
                        "isActive", true,
                        "isOnline", true
                ));

        mockMvc.perform(get("/internal/exams/9001")
                        .param("studentId", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(9001))
                .andExpect(jsonPath("$.title").value("Java期末考试"))
                .andExpect(jsonPath("$.courseName").value("Java企业开发"));

        verify(examApplicationService).getStudentExamDetail(42L, 9001L);
    }

    @Test
    void submitStudentExamReturnsInternalSubmission() throws Exception {
        ExamSubmissionDTO submission = new ExamSubmissionDTO();
        submission.setId(3001L);
        submission.setExamId(9001L);
        submission.setStudentId(42L);
        submission.setTimeTaken(45);
        when(examApplicationService.submit(org.mockito.ArgumentMatchers.eq(9001L),
                org.mockito.ArgumentMatchers.any())).thenReturn(submission);

        mockMvc.perform(post("/internal/exams/9001/submissions")
                        .contentType("application/json")
                        .content("""
                                {
                                  "studentId": 42,
                                  "timeTaken": 45,
                                  "answers": {
                                    "1": "A",
                                    "2": "B"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3001))
                .andExpect(jsonPath("$.examId").value(9001))
                .andExpect(jsonPath("$.studentId").value(42))
                .andExpect(jsonPath("$.timeTaken").value(45));

        verify(examApplicationService).submit(org.mockito.ArgumentMatchers.eq(9001L),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void listStudentScoresReturnsInternalDtos() throws Exception {
        StudentScoreDTO score = new StudentScoreDTO();
        score.setType("exam");
        score.setRelatedId(9001L);
        score.setTitle("Java期末考试");
        score.setScore(95);
        when(examApplicationService.listStudentExamScores(42L)).thenReturn(List.of(score));

        mockMvc.perform(get("/internal/exams/scores")
                        .param("studentId", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].relatedId").value(9001))
                .andExpect(jsonPath("$[0].title").value("Java期末考试"))
                .andExpect(jsonPath("$[0].score").value(95));

        verify(examApplicationService).listStudentExamScores(42L);
    }

    @Test
    void getTeacherExamReturnsInternalDto() throws Exception {
        ExamDTO exam = new ExamDTO();
        exam.setId(9001L);
        exam.setTitle("期中考试");
        exam.setCourseId(2L);
        when(examApplicationService.getTeacherExam(7L, 9001L)).thenReturn(exam);

        mockMvc.perform(get("/internal/exams/9001/teacher")
                        .param("teacherId", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(9001))
                .andExpect(jsonPath("$.title").value("期中考试"))
                .andExpect(jsonPath("$.courseId").value(2));

        verify(examApplicationService).getTeacherExam(7L, 9001L);
    }

    @Test
    void createTeacherExamReturnsInternalRecord() throws Exception {
        ExamRecord created = new ExamRecord();
        created.setId(9002L);
        created.setTitle("期末考试");
        created.setCourseId(2L);
        when(examApplicationService.createTeacherExam(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.any(TeacherExamUpsertRequestDTO.class))).thenReturn(created);

        mockMvc.perform(post("/internal/exams/teacher")
                        .param("teacherId", "7")
                        .contentType("application/json")
                        .content("""
                                {
                                  "title": "期末考试",
                                  "description": "闭卷考试",
                                  "courseId": 2,
                                  "startTime": "2026-06-20T09:00",
                                  "endTime": "2026-06-20T10:30",
                                  "duration": 90,
                                  "isActive": true,
                                  "isOnline": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(9002))
                .andExpect(jsonPath("$.title").value("期末考试"))
                .andExpect(jsonPath("$.courseId").value(2));

        verify(examApplicationService).createTeacherExam(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.any(TeacherExamUpsertRequestDTO.class));
    }

    @Test
    void updateTeacherExamReturnsInternalRecord() throws Exception {
        ExamRecord updated = new ExamRecord();
        updated.setId(9002L);
        updated.setTitle("期末考试-更新");
        updated.setCourseId(2L);
        when(examApplicationService.updateTeacherExam(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.eq(9002L),
                org.mockito.ArgumentMatchers.any(TeacherExamUpsertRequestDTO.class))).thenReturn(updated);

        mockMvc.perform(put("/internal/exams/9002/teacher")
                        .param("teacherId", "7")
                        .contentType("application/json")
                        .content("""
                                {
                                  "title": "期末考试-更新",
                                  "description": "闭卷考试",
                                  "courseId": 2,
                                  "startTime": "2026-06-20T09:00",
                                  "endTime": "2026-06-20T10:30",
                                  "duration": 90,
                                  "isActive": true,
                                  "isOnline": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(9002))
                .andExpect(jsonPath("$.title").value("期末考试-更新"))
                .andExpect(jsonPath("$.courseId").value(2));

        verify(examApplicationService).updateTeacherExam(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.eq(9002L),
                org.mockito.ArgumentMatchers.any(TeacherExamUpsertRequestDTO.class));
    }

    @Test
    void deleteTeacherExamReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/internal/exams/9002/teacher")
                        .param("teacherId", "7"))
                .andExpect(status().isNoContent());

        verify(examApplicationService).deleteTeacherExam(7L, 9002L);
    }

    @Test
    void listKnowledgePointIdsReturnsInternalArray() throws Exception {
        when(examApplicationService.listKnowledgePointIds(9001L)).thenReturn(List.of(701L, 702L));

        mockMvc.perform(get("/internal/exams/9001/knowledge-point-ids"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value(701))
                .andExpect(jsonPath("$[1]").value(702));

        verify(examApplicationService).listKnowledgePointIds(9001L);
    }
}
