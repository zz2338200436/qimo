package com._202510007517.platform.analysis.controller;

import com._202510007517.platform.analysis.api.dto.KnowledgeMasteryDTO;
import com._202510007517.platform.analysis.api.dto.ScoreTrendDTO;
import com._202510007517.platform.analysis.service.AnalysisQueryService;
import com._202510007517.platform.analysis.service.AnalysisTriggerCompatibilityService;
import com._202510007517.platform.analysis.service.EarlyWarningCompatibilityService;
import com._202510007517.platform.analysis.controller.dto.EarlyWarningDTO;
import com._202510007517.platform.analysis.controller.dto.EarlyWarningPageResult;
import com._202510007517.platform.analysis.controller.dto.WarningStatsDTO;
import com._202510007517.platform.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AnalysisQueryControllerTest {

    @Test
    void listTeacherScoreTrendReturnsProjectedRows() throws Exception {
        AnalysisQueryService service = mock(AnalysisQueryService.class);
        when(service.listScoreTrend(7L, 1L, 2L, "30d")).thenReturn(List.of(
                new ScoreTrendDTO(
                        42L,
                        2L,
                        1L,
                        "exam",
                        9001L,
                        9003L,
                        98,
                        100,
                        new BigDecimal("0.9800"),
                        Instant.parse("2026-05-19T05:01:39Z"))
        ));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AnalysisQueryController(service)).build();

        mockMvc.perform(get("/api/teacher/score-trend")
                        .header("X-User-Id", "7")
                        .param("classId", "1")
                        .param("courseId", "2")
                        .param("timeRange", "30d"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("获取成绩趋势数据成功"))
                .andExpect(jsonPath("$.data[0].studentId").value(42))
                .andExpect(jsonPath("$.data[0].courseId").value(2))
                .andExpect(jsonPath("$.data[0].score").value(98))
                .andExpect(jsonPath("$.data[0].scoreRate").value(0.9800));

        verify(service).listScoreTrend(7L, 1L, 2L, "30d");
    }

    @Test
    void getTeacherDashboardKeepsLegacyEnvelope() throws Exception {
        AnalysisQueryService service = mock(AnalysisQueryService.class);
        when(service.getTeacherDashboard(7L, 1L, 2L, "month")).thenReturn(Map.ofEntries(
                Map.entry("totalCourses", 2),
                Map.entry("totalStudents", 36),
                Map.entry("pendingAssignments", 4),
                Map.entry("pendingExams", 1),
                Map.entry("missingSubmissions", 3),
                Map.entry("upcomingDeadlines", 0),
                Map.entry("warningCount", 2),
                Map.entry("courseNames", List.of("Distributed Systems")),
                Map.entry("averageScores", List.of(86.5)),
                Map.entry("submissionRateDays", List.of("05-20")),
                Map.entry("submissionRates", List.of(90)),
                Map.entry("recentActivities", List.of(Map.of(
                        "activityType", "成绩记录",
                        "studentId", 42L,
                        "studentName", "学生 42",
                        "details", "学生 42 完成了 exam")))));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AnalysisQueryController(service)).build();

        mockMvc.perform(get("/api/teacher/dashboard")
                        .header("X-User-Id", "7")
                        .param("classId", "1")
                        .param("courseId", "2")
                        .param("timeRange", "month"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("获取教师仪表盘数据成功"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalCourses").value(2))
                .andExpect(jsonPath("$.data.totalStudents").value(36))
                .andExpect(jsonPath("$.data.courseNames[0]").value("Distributed Systems"))
                .andExpect(jsonPath("$.data.recentActivities[0].activityType").value("成绩记录"));

        verify(service).getTeacherDashboard(7L, 1L, 2L, "month");
    }

    @Test
    void getTeacherLearningSummaryKeepsLegacyEnvelope() throws Exception {
        AnalysisQueryService service = mock(AnalysisQueryService.class);
        when(service.getTeacherLearningSummary(7L, null, 2L, "month")).thenReturn(Map.of(
                "totalStudents", 1,
                "averageScore", 88.0,
                "totalPendingAssignments", 0,
                "overallProgress", 76.0,
                "studentPerformances", List.of(Map.of(
                        "studentId", 42L,
                        "realName", "学生 42",
                        "className", "班级 1",
                        "courseName", "课程 2",
                        "averageScore", 88,
                        "pendingAssignments", 0,
                        "overallProgress", 76,
                        "status", "良好"))));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AnalysisQueryController(service)).build();

        mockMvc.perform(get("/api/teacher/learning-summary")
                        .header("X-User-Id", "7")
                        .param("classId", "all")
                        .param("courseId", "2")
                        .param("timeRange", "month"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("获取学生学习汇总数据成功"))
                .andExpect(jsonPath("$.data.totalStudents").value(1))
                .andExpect(jsonPath("$.data.averageScore").value(88.0))
                .andExpect(jsonPath("$.data.studentPerformances[0].studentId").value(42))
                .andExpect(jsonPath("$.data.studentPerformances[0].status").value("良好"));

        verify(service).getTeacherLearningSummary(7L, null, 2L, "month");
    }

    @Test
    void teacherAnalysisTriggerUrlsKeepLegacyEnvelope() throws Exception {
        AnalysisTriggerCompatibilityService service = mock(AnalysisTriggerCompatibilityService.class);
        when(service.triggerClassAnalysis(7L, 1L, 2L)).thenReturn(3);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AnalysisTriggerCompatibilityController(service)).build();

        mockMvc.perform(post("/api/teacher/analysis/warnings/trigger")
                        .header("X-User-Id", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("学情预警分析已启动，请稍后查看结果"))
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(post("/api/teacher/analysis/knowledge-points/trigger")
                        .header("X-User-Id", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("知识点分析更新已启动，请稍后查看结果"));

        mockMvc.perform(post("/api/teacher/analysis/student/42/course/2/trigger")
                        .header("X-User-Id", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("学生学情分析已启动，请稍后查看结果"));

        mockMvc.perform(post("/api/teacher/analysis/class/1/course/2/batch-trigger")
                        .header("X-User-Id", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("班级学情分析已启动，将分析 3 名学生，请稍后查看结果"));

        verify(service).triggerWarningAnalysis(7L);
        verify(service).triggerKnowledgePointAnalysis(7L);
        verify(service).triggerStudentAnalysis(7L, 42L, 2L);
        verify(service).triggerClassAnalysis(7L, 1L, 2L);
    }

    @Test
    void getStudentKnowledgeMasteryReturnsCourseMastery() throws Exception {
        AnalysisQueryService service = mock(AnalysisQueryService.class);
        when(service.listStudentKnowledgeMastery(7L, 42L, 2L)).thenReturn(List.of(
                new KnowledgeMasteryDTO(
                        42L,
                        2L,
                        1L,
                        null,
                        new BigDecimal("0.9800"),
                        1,
                        "exam",
                        9001L,
                        "exam-finished-9003-98-c0f9620fd620",
                        Instant.parse("2026-05-19T05:01:39Z"))
        ));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AnalysisQueryController(service)).build();

        mockMvc.perform(get("/api/teacher/knowledge-points/mastery/student/42/course/2")
                        .header("X-User-Id", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("获取学生知识点掌握情况成功"))
                .andExpect(jsonPath("$.data[0].studentId").value(42))
                .andExpect(jsonPath("$.data[0].courseId").value(2))
                .andExpect(jsonPath("$.data[0].masteryScore").value(0.9800))
                .andExpect(jsonPath("$.data[0].lastSourceType").value("exam"));

        verify(service).listStudentKnowledgeMastery(7L, 42L, 2L);
    }

    @Test
    void listKnowledgePointMasteryStatsKeepsLegacyTeacherEnvelope() throws Exception {
        AnalysisQueryService service = mock(AnalysisQueryService.class);
        when(service.listKnowledgePointMasteryStats(7L, 2L)).thenReturn(List.of(Map.of(
                "knowledgePointId", 99L,
                "knowledgePointName", "函数",
                "masteryRate", 82.5,
                "studentCount", 12)));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AnalysisQueryController(service)).build();

        mockMvc.perform(get("/api/teacher/knowledge-points/stats/course/2")
                        .header("X-User-Id", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("获取知识点掌握统计成功"))
                .andExpect(jsonPath("$.data[0].knowledgePointId").value(99))
                .andExpect(jsonPath("$.data[0].knowledgePointName").value("函数"))
                .andExpect(jsonPath("$.data[0].masteryRate").value(82.5));

        verify(service).listKnowledgePointMasteryStats(7L, 2L);
    }

    @Test
    void analyzeStudentKnowledgeMasteryKeepsLegacyTeacherEnvelope() throws Exception {
        AnalysisQueryService service = mock(AnalysisQueryService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AnalysisQueryController(service)).build();

        mockMvc.perform(post("/api/teacher/knowledge-points/analyze/student/42/course/2")
                        .header("X-User-Id", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("知识点掌握情况分析完成"))
                .andExpect(jsonPath("$.code").value(200));

        verify(service).analyzeStudentKnowledgeMastery(7L, 42L, 2L);
    }

    @Test
    void getTeacherKnowledgePointAnalysisKeepsLegacyUrlContract() throws Exception {
        AnalysisQueryService service = mock(AnalysisQueryService.class);
        when(service.getTeacherKnowledgePointAnalysis(7L, 2L, 1L, 42L, 99L))
                .thenReturn(Map.of(
                        "courseName", "课程 2",
                        "knowledgePointDistribution", List.of(Map.of(
                                "knowledgePointId", 99L,
                                "knowledgePointName", "函数",
                                "masteryRate", 82.5,
                                "difficulty", "MEDIUM",
                                "orderIndex", 1)),
                        "atRiskStudents", List.of(),
                        "weakTopics", List.of(),
                        "excellentStudentAverage", List.of()));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new KnowledgePointAnalysisCompatibilityController(service))
                .build();

        mockMvc.perform(get("/api/knowledge-points/analysis/teacher/course/2")
                        .header("X-User-Id", "7")
                        .param("classId", "1")
                        .param("studentId", "42")
                        .param("knowledgePointId", "99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("获取知识点分析数据成功"))
                .andExpect(jsonPath("$.data.courseName").value("课程 2"))
                .andExpect(jsonPath("$.data.knowledgePointDistribution[0].knowledgePointId").value(99))
                .andExpect(jsonPath("$.data.knowledgePointDistribution[0].knowledgePointName").value("函数"))
                .andExpect(jsonPath("$.data.knowledgePointDistribution[0].masteryRate").value(82.5))
                .andExpect(jsonPath("$.data.atRiskStudents").isArray())
                .andExpect(jsonPath("$.data.weakTopics").isArray())
                .andExpect(jsonPath("$.data.excellentStudentAverage").isArray());

        verify(service).getTeacherKnowledgePointAnalysis(7L, 2L, 1L, 42L, 99L);
    }

    @Test
    void getTeacherKnowledgePointAnalysisAcceptsAllFilters() throws Exception {
        AnalysisQueryService service = mock(AnalysisQueryService.class);
        when(service.getTeacherKnowledgePointAnalysis(7L, null, null, null, null))
                .thenReturn(Map.of(
                        "courseName", "所有课程",
                        "knowledgePointDistribution", List.of(),
                        "atRiskStudents", List.of(),
                        "weakTopics", List.of(),
                        "excellentStudentAverage", List.of()));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new KnowledgePointAnalysisCompatibilityController(service))
                .build();

        mockMvc.perform(get("/api/knowledge-points/analysis/teacher/course/all")
                        .header("X-User-Id", "7")
                        .param("studentId", "all")
                        .param("knowledgePointId", "all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.courseName").value("所有课程"))
                .andExpect(jsonPath("$.data.knowledgePointDistribution").isArray());

        verify(service).getTeacherKnowledgePointAnalysis(7L, null, null, null, null);
    }

    @Test
    void getTeacherKnowledgePointAnalysisRejectsBadLegacyFilterId() throws Exception {
        AnalysisQueryService service = mock(AnalysisQueryService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new KnowledgePointAnalysisCompatibilityController(service))
                .build();

        mockMvc.perform(get("/api/knowledge-points/analysis/teacher/course/not-a-number")
                        .header("X-User-Id", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("无效的课程ID"));
    }

    @Test
    void getTeacherKnowledgePointAnalysisKeepsLegacyUnauthorizedEnvelope() throws Exception {
        AnalysisQueryService service = mock(AnalysisQueryService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new KnowledgePointAnalysisCompatibilityController(service))
                .build();

        mockMvc.perform(get("/api/knowledge-points/analysis/teacher/course"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("未授权，请重新登录"));
    }

    @Test
    void listStudentStudyTimeDistributionKeepsLegacyEnvelope() throws Exception {
        AnalysisQueryService service = mock(AnalysisQueryService.class);
        when(service.listStudentStudyTimeDistribution(42L, "weekly", "2025-2026-1", 2L, "month"))
                .thenReturn(List.of(Map.of(
                        "date", "第4周",
                        "label", "第4周",
                        "study_time", 3.5,
                        "hours", 3.5)));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new StudentAnalysisCompatibilityController(service))
                .build();

        mockMvc.perform(get("/api/student/study-time-distribution")
                        .header("X-User-Id", "42")
                        .param("type", "weekly")
                        .param("semester", "2025-2026-1")
                        .param("courseId", "2")
                        .param("timeRange", "month"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("获取学习时间分布成功"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].date").value("第4周"))
                .andExpect(jsonPath("$.data[0].label").value("第4周"))
                .andExpect(jsonPath("$.data[0].study_time").value(3.5))
                .andExpect(jsonPath("$.data[0].hours").value(3.5));

        verify(service).listStudentStudyTimeDistribution(42L, "weekly", "2025-2026-1", 2L, "month");
    }

    @Test
    void getStudentLearningStatsKeepsLegacyEnvelope() throws Exception {
        AnalysisQueryService service = mock(AnalysisQueryService.class);
        when(service.getStudentLearningStats(42L, "2025-2026-1", 2L, "month"))
                .thenReturn(Map.ofEntries(
                        Map.entry("studyTime", 4.0),
                        Map.entry("studyTimeChange", 0),
                        Map.entry("studyTimeDistribution", List.of(0.0, 1.0, 3.0)),
                        Map.entry("completedTasks", 2),
                        Map.entry("completedTasksChange", 0),
                        Map.entry("averageScore", 85.0),
                        Map.entry("averageScoreChange", 0.0),
                        Map.entry("knowledgeMastery", 75.0),
                        Map.entry("knowledgeMasteryChange", 0.0),
                        Map.entry("knowledgePoints", List.of(Map.of(
                                "id", 2L,
                                "courseId", 2L,
                                "name", "课程 2",
                                "mastery", 75.0,
                                "practiceCount", 2))),
                        Map.entry("studyPlan", Map.of(
                                "completionPercentage", 0,
                                "items", List.of()))));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new StudentAnalysisCompatibilityController(service))
                .build();

        mockMvc.perform(get("/api/student/stats")
                        .header("X-User-Id", "42")
                        .param("semester", "2025-2026-1")
                        .param("courseId", "2")
                        .param("timeRange", "month"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("获取学习统计成功"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.studyTime").value(4.0))
                .andExpect(jsonPath("$.data.completedTasks").value(2))
                .andExpect(jsonPath("$.data.averageScore").value(85.0))
                .andExpect(jsonPath("$.data.knowledgeMastery").value(75.0))
                .andExpect(jsonPath("$.data.knowledgePoints[0].practiceCount").value(2))
                .andExpect(jsonPath("$.data.studyPlan.completionPercentage").value(0));

        verify(service).getStudentLearningStats(42L, "2025-2026-1", 2L, "month");
    }

    @Test
    void listStudentKnowledgePointsKeepsLegacyEnvelope() throws Exception {
        AnalysisQueryService service = mock(AnalysisQueryService.class);
        when(service.listStudentKnowledgePoints(42L, "2025-2026-1", 2L, "month"))
                .thenReturn(List.of(Map.of(
                        "id", 31L,
                        "courseId", 2L,
                        "name", "一致性哈希",
                        "pointName", "一致性哈希",
                        "courseName", "Distributed Systems",
                        "mastery", 75.0,
                        "practiceCount", 3)));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new StudentAnalysisCompatibilityController(service))
                .build();

        mockMvc.perform(get("/api/student/knowledge-points")
                        .header("X-User-Id", "42")
                        .param("semester", "2025-2026-1")
                        .param("courseId", "2")
                        .param("timeRange", "month"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("获取知识点列表成功"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].id").value(31))
                .andExpect(jsonPath("$.data[0].name").value("一致性哈希"))
                .andExpect(jsonPath("$.data[0].mastery").value(75.0))
                .andExpect(jsonPath("$.data[0].practiceCount").value(3));

        verify(service).listStudentKnowledgePoints(42L, "2025-2026-1", 2L, "month");
    }

    @Test
    void getStudentKnowledgePointDetailKeepsLegacyEnvelope() throws Exception {
        AnalysisQueryService service = mock(AnalysisQueryService.class);
        when(service.getStudentKnowledgePointDetail(42L, 31L)).thenReturn(Map.of(
                "id", 31L,
                "courseId", 2L,
                "name", "一致性哈希",
                "pointName", "一致性哈希",
                "description", "虚拟节点与负载均衡",
                "difficulty", "困难",
                "courseName", "Distributed Systems",
                "mastery", 90.0,
                "practiceCount", 5));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new StudentAnalysisCompatibilityController(service))
                .build();

        mockMvc.perform(get("/api/student/knowledge-points/{knowledgePointId}", 31L)
                        .header("X-User-Id", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("获取知识点详情成功"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(31))
                .andExpect(jsonPath("$.data.description").value("虚拟节点与负载均衡"))
                .andExpect(jsonPath("$.data.courseId").value(2));

        verify(service).getStudentKnowledgePointDetail(42L, 31L);
    }

    @Test
    void getStudentKnowledgePointDetailNotFoundKeepsLegacyEnvelope() throws Exception {
        AnalysisQueryService service = mock(AnalysisQueryService.class);
        when(service.getStudentKnowledgePointDetail(42L, 999999L))
                .thenThrow(new ResourceNotFoundException("知识点不存在"));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new StudentAnalysisCompatibilityController(service))
                .build();

        mockMvc.perform(get("/api/student/knowledge-points/{knowledgePointId}", 999999L)
                        .header("X-User-Id", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("知识点不存在"));

        verify(service).getStudentKnowledgePointDetail(42L, 999999L);
    }

    @Test
    void listEarlyWarningsKeepsLegacyTeacherEnvelope() throws Exception {
        EarlyWarningCompatibilityService service = mock(EarlyWarningCompatibilityService.class);
        when(service.listWarnings(7L, 1L, 2L, "LOW_SCORE", "pending", 2, 5))
                .thenReturn(new EarlyWarningPageResult(
                        List.of(sampleWarning()),
                        2,
                        5,
                        6,
                        2,
                        false,
                        true,
                        5,
                        1,
                        false));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new EarlyWarningCompatibilityController(service)).build();

        mockMvc.perform(get("/api/early-warnings/teacher/list")
                        .header("X-User-Id", "7")
                        .param("classId", "1")
                        .param("courseId", "2")
                        .param("warningType", "LOW_SCORE")
                        .param("status", "pending")
                        .param("page", "2")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("获取预警列表成功"))
                .andExpect(jsonPath("$.data.content[0].id").value(11))
                .andExpect(jsonPath("$.data.content[0].status").value("pending"))
                .andExpect(jsonPath("$.data.pageNumber").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(6));

        verify(service).listWarnings(7L, 1L, 2L, "LOW_SCORE", "pending", 2, 5);
    }

    @Test
    void listStudentEarlyWarningsKeepsLegacyEnvelope() throws Exception {
        EarlyWarningCompatibilityService service = mock(EarlyWarningCompatibilityService.class);
        when(service.listStudentWarnings(42L)).thenReturn(List.of(sampleWarning()));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new EarlyWarningCompatibilityController(service)).build();

        mockMvc.perform(get("/api/student/early-warnings")
                        .header("X-User-Id", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("获取学情预警列表成功"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].studentId").value(42))
                .andExpect(jsonPath("$.data[0].reason").value("阶段测验低于及格线"));

        verify(service).listStudentWarnings(42L);
    }

    @Test
    void getEarlyWarningStatsKeepsLegacyTeacherEnvelope() throws Exception {
        EarlyWarningCompatibilityService service = mock(EarlyWarningCompatibilityService.class);
        when(service.getStats(7L, null, 2L)).thenReturn(new WarningStatsDTO(
                8,
                3,
                0,
                5,
                4,
                1,
                2,
                1));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new EarlyWarningCompatibilityController(service)).build();

        mockMvc.perform(get("/api/early-warnings/teacher/stats")
                        .header("X-User-Id", "7")
                        .param("courseId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("获取预警统计数据成功"))
                .andExpect(jsonPath("$.data.totalWarnings").value(8))
                .andExpect(jsonPath("$.data.pendingWarnings").value(3))
                .andExpect(jsonPath("$.data.resolvedWarnings").value(5));

        verify(service).getStats(7L, null, 2L);
    }

    @Test
    void updateEarlyWarningStatusKeepsLegacyTeacherEnvelope() throws Exception {
        EarlyWarningCompatibilityService service = mock(EarlyWarningCompatibilityService.class);
        when(service.updateStatus(7L, 11L, "resolved", "已电话沟通")).thenReturn(true);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new EarlyWarningCompatibilityController(service)).build();

        mockMvc.perform(put("/api/early-warnings/teacher/status/11")
                        .header("X-User-Id", "7")
                        .contentType("application/json")
                        .content("{\"status\":\"resolved\",\"resolvedNote\":\"已电话沟通\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("更新预警状态成功"))
                .andExpect(jsonPath("$.code").value(200));

        verify(service).updateStatus(7L, 11L, "resolved", "已电话沟通");
    }

    @Test
    void getEarlyWarningDetailKeepsLegacyTeacherEnvelope() throws Exception {
        EarlyWarningCompatibilityService service = mock(EarlyWarningCompatibilityService.class);
        when(service.getDetail(7L, 11L)).thenReturn(sampleWarning());
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new EarlyWarningCompatibilityController(service)).build();

        mockMvc.perform(get("/api/early-warnings/teacher/detail/11")
                        .header("X-User-Id", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("获取预警详情成功"))
                .andExpect(jsonPath("$.data.id").value(11))
                .andExpect(jsonPath("$.data.reason").value("阶段测验低于及格线"))
                .andExpect(jsonPath("$.data.suggestion").value("建议安排课后辅导，重点讲解薄弱知识点"));

        verify(service).getDetail(7L, 11L);
    }

    @Test
    void courseEarlyWarningsKeepLegacyTeacherEnvelope() throws Exception {
        EarlyWarningCompatibilityService service = mock(EarlyWarningCompatibilityService.class);
        when(service.listCourseWarnings(7L, 2L, "LOW_SCORE", "HIGH", false))
                .thenReturn(List.of(sampleWarning()));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new EarlyWarningCompatibilityController(service)).build();

        mockMvc.perform(get("/api/teacher/early-warnings/course/2")
                        .header("X-User-Id", "7")
                        .param("warningType", "LOW_SCORE")
                        .param("warningLevel", "HIGH")
                        .param("isResolved", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("获取课程学情预警列表成功"))
                .andExpect(jsonPath("$.data[0].courseId").value(2));

        verify(service).listCourseWarnings(7L, 2L, "LOW_SCORE", "HIGH", false);
    }

    @Test
    void createAndDeleteEarlyWarningKeepLegacyTeacherEnvelope() throws Exception {
        EarlyWarningCompatibilityService service = mock(EarlyWarningCompatibilityService.class);
        when(service.createWarning(7L, 42L, 2L, "LOW_SCORE", "HIGH", "阶段测验低于及格线", null, null))
                .thenReturn(sampleWarning());
        when(service.deleteWarning(7L, 11L)).thenReturn(true);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new EarlyWarningCompatibilityController(service)).build();

        mockMvc.perform(post("/api/early-warnings/teacher")
                        .header("X-User-Id", "7")
                        .contentType("application/json")
                        .content("""
                                {"studentId":42,"courseId":2,"warningType":"LOW_SCORE","warningLevel":"HIGH","warningMessage":"阶段测验低于及格线"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("添加预警成功"))
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data.teacherId").value(7));

        mockMvc.perform(delete("/api/early-warnings/teacher/11")
                        .header("X-User-Id", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("删除预警成功"));

        verify(service).createWarning(7L, 42L, 2L, "LOW_SCORE", "HIGH", "阶段测验低于及格线", null, null);
        verify(service).deleteWarning(7L, 11L);
    }

    @Test
    void exportEarlyWarningsKeepsLegacyExcelDownloadContract() throws Exception {
        EarlyWarningCompatibilityService service = mock(EarlyWarningCompatibilityService.class);
        when(service.listWarningsForExport(7L, null, null, null, null)).thenReturn(List.of(sampleWarning()));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new EarlyWarningCompatibilityController(service)).build();

        mockMvc.perform(get("/api/early-warnings/teacher/export")
                        .header("X-User-Id", "7"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string("Content-Disposition", containsString("early-warnings.xlsx")));

        verify(service).listWarningsForExport(7L, null, null, null, null);
    }

    private static EarlyWarningDTO sampleWarning() {
        return new EarlyWarningDTO(
                11L,
                42L,
                2L,
                7L,
                "LOW_SCORE",
                "HIGH",
                "阶段测验低于及格线",
                Instant.parse("2026-05-19T12:00:00Z"),
                false,
                null,
                null,
                null,
                "exam",
                99L,
                "学生 42",
                "课程 2",
                "pending",
                "阶段测验低于及格线",
                "建议安排课后辅导，重点讲解薄弱知识点");
    }
}
