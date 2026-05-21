package com._202510007517.platform.exam.web;

import com._202510007517.platform.common.web.CommonTraceConstants;
import com._202510007517.platform.exam.domain.ExamSubmissionRecord;
import com._202510007517.platform.exam.domain.ExamRecord;
import com._202510007517.platform.exam.service.ExamApplicationService;
import com._202510007517.platform.exam.service.ExamKnowledgePointService;
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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TeacherExamController.class)
@Import(com._202510007517.platform.exam.config.ExamServiceExceptionHandler.class)
class TeacherExamControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExamApplicationService examApplicationService;

    @Test
    void listTeacherExamsReturnsPagedPayload() throws Exception {
        Map<String, Object> page = new LinkedHashMap<>();
        page.put("content", List.of(Map.of(
                "id", 9001L,
                "title", "StudentExamSmoke-Open",
                "courseName", "CourseSmokeA"
        )));
        page.put("totalPages", 1);
        page.put("totalElements", 1);
        when(examApplicationService.listTeacherExams(7L, 1, 10, "id", "DESC", null, null, null))
                .thenReturn(page);

        mockMvc.perform(get("/api/teacher/exams")
                        .header(CommonTraceConstants.USER_ID_HEADER, "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("获取考试列表成功"))
                .andExpect(jsonPath("$.data.content[0].title").value("StudentExamSmoke-Open"))
                .andExpect(jsonPath("$.data.content[0].courseName").value("CourseSmokeA"));

        verify(examApplicationService).listTeacherExams(7L, 1, 10, "id", "DESC", null, null, null);
    }

    @Test
    void getTeacherExamDetailReturnsPayload() throws Exception {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("id", 9001L);
        detail.put("title", "StudentExamSmoke-Open");
        detail.put("courseName", "CourseSmokeA");
        when(examApplicationService.getTeacherExamDetail(7L, 9001L)).thenReturn(detail);

        mockMvc.perform(get("/api/teacher/exams/9001")
                        .header(CommonTraceConstants.USER_ID_HEADER, "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("获取考试详情成功"))
                .andExpect(jsonPath("$.data.title").value("StudentExamSmoke-Open"));

        verify(examApplicationService).getTeacherExamDetail(7L, 9001L);
    }

    @Test
    void createTeacherExamReturnsCreatedRecord() throws Exception {
        ExamRecord created = new ExamRecord();
        created.setId(9005L);
        created.setTitle("TeacherExamCrudSmoke");
        created.setCourseId(2L);
        when(examApplicationService.createTeacherExam(org.mockito.ArgumentMatchers.eq(7L), org.mockito.ArgumentMatchers.any()))
                .thenReturn(created);

        mockMvc.perform(post("/api/teacher/exams")
                        .header(CommonTraceConstants.USER_ID_HEADER, "7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "TeacherExamCrudSmoke",
                                  "description": "teacher exam smoke",
                                  "courseId": 2,
                                  "startTime": "2026-05-20T01:00:00.000Z",
                                  "endTime": "2026-05-20T02:30:00.000Z",
                                  "publishDate": "2026-05-19T01:00:00.000Z",
                                  "duration": 90,
                                  "isActive": true,
                                  "isOnline": true,
                                  "location": ""
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("创建成功"))
                .andExpect(jsonPath("$.data.id").value(9005))
                .andExpect(jsonPath("$.data.title").value("TeacherExamCrudSmoke"));
    }

    @Test
    void updateTeacherExamReturnsUpdatedRecord() throws Exception {
        ExamRecord updated = new ExamRecord();
        updated.setId(9005L);
        updated.setTitle("TeacherExamCrudSmoke-Edited");
        updated.setCourseId(2L);
        when(examApplicationService.updateTeacherExam(org.mockito.ArgumentMatchers.eq(7L), org.mockito.ArgumentMatchers.eq(9005L), org.mockito.ArgumentMatchers.any()))
                .thenReturn(updated);

        mockMvc.perform(put("/api/teacher/exams/9005")
                        .header(CommonTraceConstants.USER_ID_HEADER, "7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "TeacherExamCrudSmoke-Edited",
                                  "description": "teacher exam smoke updated",
                                  "courseId": 2,
                                  "startTime": "2026-05-20T01:00:00.000Z",
                                  "endTime": "2026-05-20T02:30:00.000Z",
                                  "publishDate": "2026-05-19T01:00:00.000Z",
                                  "duration": 90,
                                  "isActive": true,
                                  "isOnline": true,
                                  "location": ""
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data.title").value("TeacherExamCrudSmoke-Edited"));
    }

    @Test
    void deleteTeacherExamReturnsNoContentEnvelope() throws Exception {
        mockMvc.perform(delete("/api/teacher/exams/9005")
                        .header(CommonTraceConstants.USER_ID_HEADER, "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.code").value(204));

        verify(examApplicationService).deleteTeacherExam(7L, 9005L);
    }

    @Test
    void listTeacherExamSubmissionsReturnsArrayPayload() throws Exception {
        ExamSubmissionRecord submission = new ExamSubmissionRecord();
        submission.setId(9101L);
        submission.setExamId(9001L);
        submission.setStudentId(42L);
        submission.setStudentName("Student Forty Two");
        submission.setSubmissionDate("2026-05-17 08:00:00");
        submission.setContent("answer");
        submission.setTimeTaken(35);
        submission.setGraded(false);

        when(examApplicationService.listTeacherExamSubmissions(7L, 9001L)).thenReturn(List.of(submission));

        mockMvc.perform(get("/api/teacher/exams/9001/submissions")
                        .header(CommonTraceConstants.USER_ID_HEADER, "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("获取考试提交列表成功"))
                .andExpect(jsonPath("$.data[0].studentName").value("Student Forty Two"));
    }

    @Test
    void listAllTeacherExamSubmissionsKeepsLegacyPagedEnvelope() throws Exception {
        Map<String, Object> page = new LinkedHashMap<>();
        page.put("submissions", List.of(Map.of(
                "id", 9101L,
                "examId", 9001L,
                "studentId", 42L,
                "studentName", "Student Forty Two",
                "examTitle", "StudentExamSmoke-Open",
                "graded", false
        )));
        page.put("total", 1);
        page.put("page", 1);
        page.put("size", 10);
        page.put("pages", 1);
        when(examApplicationService.listTeacherExamSubmissions(
                7L,
                1,
                10,
                "id",
                "DESC",
                9001L,
                42L,
                false)).thenReturn(page);

        mockMvc.perform(get("/api/teacher/exams/submissions")
                        .header(CommonTraceConstants.USER_ID_HEADER, "7")
                        .param("examId", "9001")
                        .param("studentId", "42")
                        .param("graded", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("获取考试提交记录成功"))
                .andExpect(jsonPath("$.data.submissions[0].studentName").value("Student Forty Two"))
                .andExpect(jsonPath("$.data.submissions[0].examTitle").value("StudentExamSmoke-Open"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.pages").value(1));

        verify(examApplicationService).listTeacherExamSubmissions(
                7L,
                1,
                10,
                "id",
                "DESC",
                9001L,
                42L,
                false);
    }

    @Test
    void getTeacherExamSubmissionDetailReturnsPayload() throws Exception {
        ExamSubmissionRecord submission = new ExamSubmissionRecord();
        submission.setId(9101L);
        submission.setExamId(9001L);
        submission.setStudentId(42L);
        submission.setContent("answer");
        submission.setScore(88);
        submission.setTeacherComment("well done");

        when(examApplicationService.getTeacherExamSubmissionDetail(7L, 9101L)).thenReturn(submission);

        mockMvc.perform(get("/api/teacher/exams/submissions/9101")
                        .header(CommonTraceConstants.USER_ID_HEADER, "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("获取考试提交记录详情成功"))
                .andExpect(jsonPath("$.data.score").value(88));
    }

    @Test
    void updateTeacherExamSubmissionReturnsUpdatedPayload() throws Exception {
        ExamSubmissionRecord submission = new ExamSubmissionRecord();
        submission.setId(9101L);
        submission.setExamId(9001L);
        submission.setStudentId(42L);
        submission.setContent("{\"q1\":\"B\"}");
        submission.setTimeTaken(41);
        submission.setScore(88);
        submission.setTeacherComment("manual adjustment");
        submission.setGraded(true);

        when(examApplicationService.updateTeacherExamSubmission(org.mockito.ArgumentMatchers.eq(7L), org.mockito.ArgumentMatchers.eq(9101L), org.mockito.ArgumentMatchers.any()))
                .thenReturn(submission);

        mockMvc.perform(put("/api/teacher/exams/submissions/9101")
                        .header(CommonTraceConstants.USER_ID_HEADER, "7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "examId": 9001,
                                  "studentId": 42,
                                  "content": "{\\"q1\\":\\"B\\"}",
                                  "timeTaken": 41,
                                  "score": 88,
                                  "teacherComment": "manual adjustment",
                                  "graded": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("更新考试提交记录成功"))
                .andExpect(jsonPath("$.data.score").value(88))
                .andExpect(jsonPath("$.data.graded").value(true));
    }

    @Test
    void deleteTeacherExamSubmissionKeepsLegacyNoContentEnvelope() throws Exception {
        mockMvc.perform(delete("/api/teacher/exams/submissions/9101")
                        .header(CommonTraceConstants.USER_ID_HEADER, "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.code").value(204));

        verify(examApplicationService).deleteTeacherExamSubmission(7L, 9101L);
    }

    @Test
    void gradeTeacherExamSubmissionReturnsUpdatedSubmission() throws Exception {
        ExamSubmissionRecord submission = new ExamSubmissionRecord();
        submission.setId(9101L);
        submission.setExamId(9001L);
        submission.setStudentId(42L);
        submission.setScore(91);
        submission.setTeacherComment("graded");
        submission.setGraded(true);

        when(examApplicationService.gradeTeacherExamSubmission(org.mockito.ArgumentMatchers.eq(7L), org.mockito.ArgumentMatchers.eq(9101L), org.mockito.ArgumentMatchers.any()))
                .thenReturn(submission);

        mockMvc.perform(put("/api/teacher/exams/grade/9101")
                        .header(CommonTraceConstants.USER_ID_HEADER, "7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "score": 91,
                                  "teacherComment": "graded"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("考试批改成功"))
                .andExpect(jsonPath("$.data.score").value(91))
                .andExpect(jsonPath("$.data.graded").value(true));
    }

    @Test
    void listExamKnowledgePointsKeepsLegacyEnvelope() throws Exception {
        ExamKnowledgePointService service = org.mockito.Mockito.mock(ExamKnowledgePointService.class);
        when(service.listKnowledgePoints(7L, 9001L)).thenReturn(List.of(Map.of(
                "id", 99L,
                "knowledgePointId", 99L,
                "pointName", "函数",
                "name", "函数"
        )));
        MockMvc mockMvc = org.springframework.test.web.servlet.setup.MockMvcBuilders
                .standaloneSetup(new ExamKnowledgePointCompatibilityController(service))
                .build();

        mockMvc.perform(get("/api/teacher/knowledge-points/exam/9001")
                        .header(CommonTraceConstants.USER_ID_HEADER, "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("获取考试知识点成功"))
                .andExpect(jsonPath("$.data[0].knowledgePointId").value(99))
                .andExpect(jsonPath("$.data[0].pointName").value("函数"));

        verify(service).listKnowledgePoints(7L, 9001L);
    }

    @Test
    void replaceExamKnowledgePointsKeepsLegacyEnvelope() throws Exception {
        ExamKnowledgePointService service = org.mockito.Mockito.mock(ExamKnowledgePointService.class);
        MockMvc mockMvc = org.springframework.test.web.servlet.setup.MockMvcBuilders
                .standaloneSetup(new ExamKnowledgePointCompatibilityController(service))
                .build();

        mockMvc.perform(post("/api/teacher/knowledge-points/exam/9001")
                        .header(CommonTraceConstants.USER_ID_HEADER, "7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "knowledgePointIds": [99, 100]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("设置考试知识点成功"))
                .andExpect(jsonPath("$.code").value(200));

        verify(service).replaceKnowledgePoints(7L, 9001L, List.of(99L, 100L));
    }
}
