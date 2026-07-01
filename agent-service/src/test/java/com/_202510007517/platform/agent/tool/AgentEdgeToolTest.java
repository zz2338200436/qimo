package com._202510007517.platform.agent.tool;

import com._202510007517.platform.agent.AgentServiceApplication;
import com._202510007517.platform.agent.api.dto.AgentChatResponseDTO;
import com._202510007517.platform.agent.api.dto.AgentExecutionResultDTO;
import com._202510007517.platform.agent.client.AiEdgeClient;
import com._202510007517.platform.agent.client.AnalysisEdgeClient;
import com._202510007517.platform.agent.client.NotificationEdgeClient;
import com._202510007517.platform.agent.client.TeacherAnalysisEdgeClient;
import com._202510007517.platform.agent.client.TeacherKnowledgeAnalysisEdgeClient;
import com._202510007517.platform.agent.client.TeacherWarningEdgeClient;
import com._202510007517.platform.agent.domain.AgentAuditLogEntity;
import com._202510007517.platform.agent.questionbank.QuestionRagService;
import com._202510007517.platform.agent.repository.AgentActionRepository;
import com._202510007517.platform.agent.repository.AgentAuditLogRepository;
import com._202510007517.platform.agent.repository.AgentSessionRepository;
import com._202510007517.platform.agent.service.AgentOrchestrator;
import com._202510007517.platform.ai.api.dto.GenerateExamRequestDTO;
import com._202510007517.platform.ai.api.dto.GenerateQuestionsRequestDTO;
import com._202510007517.platform.ai.api.dto.LearningSuggestionRequestDTO;
import com._202510007517.platform.analysis.api.dto.KnowledgeMasteryDTO;
import com._202510007517.platform.analysis.api.dto.ScoreTrendDTO;
import com._202510007517.platform.common.web.ResponseResult;
import com._202510007517.platform.notification.api.dto.TeacherSendNotificationRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Map;
import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentCaptor.forClass;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SpringBootTest(
        classes = AgentServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.profiles.active=test",
                "eureka.client.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "spring.cloud.config.enabled=false",
                "spring.datasource.url=jdbc:h2:mem:agent-edge-tools;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "agent.question-bank.enabled=true",
                "agent.question-bank.document-paths[0]=../docs/question-bank/java/java-basic-sample.md",
                "agent.question-bank.min-score=0.0"
        })
class AgentEdgeToolTest {

    @Autowired
    private AgentOrchestrator orchestrator;

    @Autowired
    private AgentActionRepository actionRepository;

    @Autowired
    private AgentAuditLogRepository auditLogRepository;

    @Autowired
    private AgentSessionRepository sessionRepository;

    @MockitoBean
    private NotificationEdgeClient notificationEdgeClient;

    @MockitoBean
    private AnalysisEdgeClient analysisEdgeClient;

    @MockitoBean
    private TeacherAnalysisEdgeClient teacherAnalysisEdgeClient;

    @MockitoBean
    private TeacherKnowledgeAnalysisEdgeClient teacherKnowledgeAnalysisEdgeClient;

    @MockitoBean
    private TeacherWarningEdgeClient teacherWarningEdgeClient;

    @MockitoBean
    private AiEdgeClient aiEdgeClient;

    @MockitoBean(name = "questionBankEmbeddingClient")
    private QuestionRagService.EmbeddingClient questionBankEmbeddingClient;

    @BeforeEach
    void clearData() {
        auditLogRepository.deleteAll();
        actionRepository.deleteAll();
        sessionRepository.deleteAll();
        when(questionBankEmbeddingClient.embed(any())).thenReturn(List.of(1.0, 0.0));
    }

    @Test
    void notificationQueryExecutesThroughNotificationService() {
        when(notificationEdgeClient.getStudentNotifications("7", 1, 10, "all"))
                .thenReturn(ResponseResult.success(Map.of("items", List.of("系统通知"))));

        AgentChatResponseDTO response = orchestrator.chat(7L, "STUDENT", null, "查看我的通知");

        assertThat(response.getResponseType()).isEqualTo("DATA");
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.getData();
        assertThat(data).containsEntry("status", "EXECUTED").containsKey("notifications");
        assertThat(actionRepository.findAll()).isEmpty();
    }

    @Test
    void notificationUnreadCountExecutesThroughNotificationService() {
        when(notificationEdgeClient.getUnreadCount("7"))
                .thenReturn(ResponseResult.success(3));

        AgentChatResponseDTO response = orchestrator.chat(7L, "STUDENT", null, "我有多少未读通知");

        assertThat(response.getResponseType()).isEqualTo("DATA");
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.getData();
        assertThat(data).containsEntry("status", "EXECUTED").containsEntry("unreadCount", 3);
        verify(notificationEdgeClient).getUnreadCount("7");
        assertThat(actionRepository.findAll()).isEmpty();
        assertThat(auditLogRepository.findAll()).isEmpty();
    }

    @Test
    void markAllNotificationsReadRequiresConfirmationThenExecutesThroughNotificationService() {
        when(notificationEdgeClient.markAllNotificationsAsRead("7"))
                .thenReturn(ResponseResult.success());

        AgentChatResponseDTO preview = orchestrator.chat(7L, "STUDENT", null, "把我的所有通知标为已读");

        assertThat(preview.getResponseType()).isEqualTo("ACTION_PREVIEW");
        assertThat(preview.getActionPreview().getIntent()).isEqualTo("MARK_ALL_NOTIFICATIONS_READ");
        assertThat(preview.getActionPreview().getRiskLevel()).isEqualTo("MEDIUM");

        AgentExecutionResultDTO result = orchestrator.confirm(
                7L,
                "STUDENT",
                preview.getActionPreview().getActionId(),
                preview.getActionPreview().getIdempotencyKey(),
                preview.getActionPreview().getSecondConfirmationPhrase());

        assertThat(result.getStatus()).isEqualTo("EXECUTED");
        assertThat(result.getResult()).containsEntry("status", "EXECUTED");
        verify(notificationEdgeClient).markAllNotificationsAsRead("7");
        assertThat(auditLogRepository.findAll())
                .hasSize(1)
                .first()
                .extracting(
                        AgentAuditLogEntity::getOperation,
                        AgentAuditLogEntity::getTargetService,
                        AgentAuditLogEntity::getSuccess)
                .containsExactly("MARK_ALL_NOTIFICATIONS_READ", "notification-service", true);
    }

    @Test
    void markNotificationReadRequiresConfirmationThenExecutesThroughNotificationService() {
        when(notificationEdgeClient.markNotificationAsRead("7", 9L))
                .thenReturn(ResponseResult.success());

        AgentChatResponseDTO preview = orchestrator.chat(7L, "STUDENT", null, "把通知ID 9标为已读");

        assertThat(preview.getResponseType()).isEqualTo("ACTION_PREVIEW");
        assertThat(preview.getActionPreview().getIntent()).isEqualTo("MARK_NOTIFICATION_READ");
        assertThat(preview.getActionPreview().getRiskLevel()).isEqualTo("MEDIUM");

        AgentExecutionResultDTO result = orchestrator.confirm(
                7L,
                "STUDENT",
                preview.getActionPreview().getActionId(),
                preview.getActionPreview().getIdempotencyKey(),
                preview.getActionPreview().getSecondConfirmationPhrase());

        assertThat(result.getStatus()).isEqualTo("EXECUTED");
        assertThat(result.getResult()).containsEntry("status", "EXECUTED");
        verify(notificationEdgeClient).markNotificationAsRead("7", 9L);
        assertThat(auditLogRepository.findAll())
                .hasSize(1)
                .first()
                .extracting(
                        AgentAuditLogEntity::getOperation,
                        AgentAuditLogEntity::getTargetService,
                        AgentAuditLogEntity::getSuccess)
                .containsExactly("MARK_NOTIFICATION_READ", "notification-service", true);
    }

    @Test
    void deleteNotificationRequiresSecondConfirmationStageThenExecutesThroughNotificationService() {
        when(notificationEdgeClient.deleteNotification("7", 9L))
                .thenReturn(ResponseResult.success());

        AgentChatResponseDTO preview = orchestrator.chat(7L, "STUDENT", null, "删除通知ID 9");

        assertThat(preview.getResponseType()).isEqualTo("ACTION_PREVIEW");
        assertThat(preview.getActionPreview().getIntent()).isEqualTo("DELETE_NOTIFICATION");
        assertThat(preview.getActionPreview().getRiskLevel()).isEqualTo("CRITICAL");

        AgentExecutionResultDTO staged = orchestrator.confirm(
                7L,
                "STUDENT",
                preview.getActionPreview().getActionId(),
                preview.getActionPreview().getIdempotencyKey());

        assertThat(staged.getStatus()).isEqualTo("PENDING_SECOND_CONFIRMATION");
        assertThat(staged.getMessage()).contains("二级确认");
        assertThat(staged.getResult())
                .containsEntry("status", "PENDING_SECOND_CONFIRMATION")
                .containsEntry("secondConfirmationPhrase", "确认执行");
        verifyNoInteractions(notificationEdgeClient);
        assertThat(auditLogRepository.findAll()).isEmpty();

        AgentExecutionResultDTO result = orchestrator.confirm(
                7L,
                "STUDENT",
                preview.getActionPreview().getActionId(),
                preview.getActionPreview().getIdempotencyKey(),
                preview.getActionPreview().getSecondConfirmationPhrase());
        assertThat(result.getStatus()).isEqualTo("EXECUTED");
        assertThat(result.getResult()).containsEntry("status", "EXECUTED");
        assertThat(Number.class.cast(result.getResult().get("notificationId")).longValue()).isEqualTo(9L);
        verify(notificationEdgeClient).deleteNotification("7", 9L);
        assertThat(auditLogRepository.findAll())
                .hasSize(1)
                .first()
                .extracting(
                        AgentAuditLogEntity::getOperation,
                        AgentAuditLogEntity::getTargetService,
                        AgentAuditLogEntity::getSuccess)
                .containsExactly("DELETE_NOTIFICATION", "notification-service", true);
    }

    @Test
    void deleteAllReadNotificationsRequiresSecondConfirmationStageThenExecutesThroughNotificationService() {
        when(notificationEdgeClient.deleteAllReadNotifications("7"))
                .thenReturn(ResponseResult.success());

        AgentChatResponseDTO preview = orchestrator.chat(7L, "STUDENT", null, "删除所有已读通知");

        assertThat(preview.getResponseType()).isEqualTo("ACTION_PREVIEW");
        assertThat(preview.getActionPreview().getIntent()).isEqualTo("DELETE_ALL_READ_NOTIFICATIONS");
        assertThat(preview.getActionPreview().getRiskLevel()).isEqualTo("CRITICAL");

        AgentExecutionResultDTO staged = orchestrator.confirm(
                7L,
                "STUDENT",
                preview.getActionPreview().getActionId(),
                preview.getActionPreview().getIdempotencyKey());

        assertThat(staged.getStatus()).isEqualTo("PENDING_SECOND_CONFIRMATION");
        assertThat(staged.getResult()).containsEntry("secondConfirmationPhrase", "确认执行");
        verifyNoInteractions(notificationEdgeClient);

        AgentExecutionResultDTO result = orchestrator.confirm(
                7L,
                "STUDENT",
                preview.getActionPreview().getActionId(),
                preview.getActionPreview().getIdempotencyKey(),
                preview.getActionPreview().getSecondConfirmationPhrase());
        assertThat(result.getStatus()).isEqualTo("EXECUTED");
        assertThat(result.getResult()).containsEntry("status", "EXECUTED");
        verify(notificationEdgeClient).deleteAllReadNotifications("7");
        assertThat(auditLogRepository.findAll())
                .hasSize(1)
                .first()
                .extracting(
                        AgentAuditLogEntity::getOperation,
                        AgentAuditLogEntity::getTargetService,
                        AgentAuditLogEntity::getSuccess)
                .containsExactly("DELETE_ALL_READ_NOTIFICATIONS", "notification-service", true);
    }

    @Test
    void sendNotificationRequiresConfirmationThenExecutesThroughNotificationService() {
        when(notificationEdgeClient.sendNotification(eq("7"), any(TeacherSendNotificationRequestDTO.class)))
                .thenReturn(ResponseResult.success(Map.of("id", 77L)));

        AgentChatResponseDTO preview = orchestrator.chat(
                7L,
                "TEACHER",
                null,
                "给学生ID 42发送通知，标题是开课通知，内容是请按时上课，类型是course");

        assertThat(preview.getResponseType()).isEqualTo("ACTION_PREVIEW");
        assertThat(preview.getActionPreview().getIntent()).isEqualTo("SEND_NOTIFICATION");
        assertThat(preview.getActionPreview().getRiskLevel()).isEqualTo("MEDIUM");

        AgentExecutionResultDTO result = orchestrator.confirm(
                7L,
                "TEACHER",
                preview.getActionPreview().getActionId(),
                preview.getActionPreview().getIdempotencyKey(),
                preview.getActionPreview().getSecondConfirmationPhrase());

        assertThat(result.getStatus()).isEqualTo("EXECUTED");
        assertThat(result.getResult()).containsEntry("status", "EXECUTED").containsKey("notification");
        verify(notificationEdgeClient).sendNotification(eq("7"), any(TeacherSendNotificationRequestDTO.class));
        assertThat(auditLogRepository.findAll())
                .hasSize(1)
                .first()
                .extracting(
                        AgentAuditLogEntity::getOperation,
                        AgentAuditLogEntity::getTargetService,
                        AgentAuditLogEntity::getSuccess)
                .containsExactly("SEND_NOTIFICATION", "notification-service", true);
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendBatchNotificationRequiresSecondConfirmationStageThenExecutesThroughNotificationService() {
        when(notificationEdgeClient.sendBatchNotifications(eq("7"), any(List.class)))
                .thenReturn(ResponseResult.success(List.of(Map.of("id", 77L), Map.of("id", 78L))));

        AgentChatResponseDTO preview = orchestrator.chat(
                7L,
                "TEACHER",
                null,
                "给学生ID 42,43批量发送通知，标题是开课通知，内容是请按时上课，类型是course");

        assertThat(preview.getResponseType()).isEqualTo("ACTION_PREVIEW");
        assertThat(preview.getActionPreview().getIntent()).isEqualTo("SEND_BATCH_NOTIFICATION");
        assertThat(preview.getActionPreview().getRiskLevel()).isEqualTo("CRITICAL");

        AgentExecutionResultDTO staged = orchestrator.confirm(
                7L,
                "TEACHER",
                preview.getActionPreview().getActionId(),
                preview.getActionPreview().getIdempotencyKey());

        assertThat(staged.getStatus()).isEqualTo("PENDING_SECOND_CONFIRMATION");
        assertThat(staged.getResult())
                .containsEntry("status", "PENDING_SECOND_CONFIRMATION")
                .containsEntry("secondConfirmationPhrase", "确认执行")
                .containsEntry("recipientCount", 2);
        assertThat(auditLogRepository.findAll()).isEmpty();

        AgentExecutionResultDTO result = orchestrator.confirm(
                7L,
                "TEACHER",
                preview.getActionPreview().getActionId(),
                preview.getActionPreview().getIdempotencyKey(),
                preview.getActionPreview().getSecondConfirmationPhrase());
        assertThat(result.getStatus()).isEqualTo("EXECUTED");
        assertThat(result.getResult()).containsEntry("status", "EXECUTED").containsEntry("notificationCount", 2);
        verify(notificationEdgeClient).sendBatchNotifications(eq("7"), any(List.class));
        assertThat(auditLogRepository.findAll())
                .hasSize(1)
                .first()
                .extracting(
                        AgentAuditLogEntity::getOperation,
                        AgentAuditLogEntity::getTargetService,
                        AgentAuditLogEntity::getSuccess)
                .containsExactly("SEND_BATCH_NOTIFICATION", "notification-service", true);
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendBatchNotificationDeduplicatesRecipientsBeforeRemoteCall() {
        when(notificationEdgeClient.sendBatchNotifications(eq("7"), any(List.class)))
                .thenReturn(ResponseResult.success(List.of(Map.of("id", 77L), Map.of("id", 78L))));

        AgentChatResponseDTO preview = orchestrator.chat(
                7L,
                "TEACHER",
                null,
                "给学生ID 42,42,43批量发送通知，标题是开课通知，内容是请按时上课，类型是course");

        AgentExecutionResultDTO staged = orchestrator.confirm(
                7L,
                "TEACHER",
                preview.getActionPreview().getActionId(),
                preview.getActionPreview().getIdempotencyKey());
        assertThat(staged.getResult()).containsEntry("recipientCount", 2);

        AgentExecutionResultDTO result = orchestrator.confirm(
                7L,
                "TEACHER",
                preview.getActionPreview().getActionId(),
                preview.getActionPreview().getIdempotencyKey(),
                preview.getActionPreview().getSecondConfirmationPhrase());

        ArgumentCaptor<List<TeacherSendNotificationRequestDTO>> captor = forClass(List.class);
        verify(notificationEdgeClient).sendBatchNotifications(eq("7"), captor.capture());
        assertThat(captor.getValue()).extracting(TeacherSendNotificationRequestDTO::getStudentId)
                .containsExactly(42L, 43L);
        assertThat(result.getResult()).containsEntry("notificationCount", 2);
    }

    @Test
    void criticalNotificationDeleteRejectsWrongSecondConfirmationAfterStage() {
        when(notificationEdgeClient.deleteNotification("7", 9L))
                .thenReturn(ResponseResult.success());

        AgentChatResponseDTO preview = orchestrator.chat(7L, "STUDENT", null, "删除通知ID 9");

        AgentExecutionResultDTO staged = orchestrator.confirm(
                7L,
                "STUDENT",
                preview.getActionPreview().getActionId(),
                preview.getActionPreview().getIdempotencyKey());

        assertThat(staged.getStatus()).isEqualTo("PENDING_SECOND_CONFIRMATION");
        assertThatThrownBy(() -> orchestrator.confirm(
                7L,
                "STUDENT",
                preview.getActionPreview().getActionId(),
                preview.getActionPreview().getIdempotencyKey(),
                "不确认"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("二级确认");
        verifyNoInteractions(notificationEdgeClient);
    }

    @Test
    void sendBatchNotificationRejectsOversizedRecipientListBeforeRemoteCall() {
        TeacherSendBatchNotificationTool tool = new TeacherSendBatchNotificationTool(notificationEdgeClient);
        List<Long> studentIds = java.util.stream.LongStream.rangeClosed(1, 101).boxed().toList();

        Map<String, Object> result = tool.execute(7L, "TEACHER", Map.of(
                "studentIds", studentIds,
                "title", "开课通知",
                "content", "请按时上课",
                "type", "course"
        ));

        assertThat(result).containsEntry("status", "VALIDATION_FAILED")
                .containsEntry("message", "批量通知一次最多支持100名学生");
    }

    @Test
    void learningStatsQueryExecutesThroughAnalysisService() {
        when(analysisEdgeClient.getLearningStats("7", null, null, null))
                .thenReturn(ResponseResult.success(Map.of("completedAssignments", 3)));

        AgentChatResponseDTO response = orchestrator.chat(7L, "STUDENT", null, "查看学习统计");

        assertThat(response.getResponseType()).isEqualTo("DATA");
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.getData();
        assertThat(data).containsEntry("status", "EXECUTED").containsKey("learningStats");
        verify(analysisEdgeClient).getLearningStats("7", null, null, null);
        assertThat(actionRepository.findAll()).isEmpty();
        assertThat(auditLogRepository.findAll()).isEmpty();
    }

    @Test
    void studyTimeDistributionQueryExecutesThroughAnalysisService() {
        when(analysisEdgeClient.listStudyTimeDistribution("7", "daily", null, null, null))
                .thenReturn(ResponseResult.success(List.of(Map.of("date", "2026-06-13", "minutes", 45))));

        AgentChatResponseDTO response = orchestrator.chat(7L, "STUDENT", null, "查看学习时间分布");

        assertThat(response.getResponseType()).isEqualTo("DATA");
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.getData();
        assertThat(data).containsEntry("status", "EXECUTED").containsKey("studyTimeDistribution");
        verify(analysisEdgeClient).listStudyTimeDistribution("7", "daily", null, null, null);
        assertThat(actionRepository.findAll()).isEmpty();
        assertThat(auditLogRepository.findAll()).isEmpty();
    }

    @Test
    void teacherDashboardQueryExecutesThroughAnalysisService() {
        when(teacherAnalysisEdgeClient.getTeacherDashboard("7", null, null, null))
                .thenReturn(ResponseResult.success(Map.of("studentCount", 12)));

        AgentChatResponseDTO response = orchestrator.chat(7L, "TEACHER", null, "查看教师仪表盘");

        assertThat(response.getResponseType()).isEqualTo("DATA");
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.getData();
        assertThat(data).containsEntry("status", "EXECUTED").containsKey("teacherDashboard");
        verify(teacherAnalysisEdgeClient).getTeacherDashboard("7", null, null, null);
        assertThat(actionRepository.findAll()).isEmpty();
        assertThat(auditLogRepository.findAll()).isEmpty();
    }

    @Test
    void learningSummaryQueryExecutesThroughAnalysisService() {
        when(teacherAnalysisEdgeClient.getLearningSummary("7", null, null, null))
                .thenReturn(ResponseResult.success(Map.of("totalStudents", 36)));

        AgentChatResponseDTO response = orchestrator.chat(7L, "TEACHER", null, "查看学生学习汇总");

        assertThat(response.getResponseType()).isEqualTo("DATA");
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.getData();
        assertThat(data).containsEntry("status", "EXECUTED").containsKey("learningSummary");
        verify(teacherAnalysisEdgeClient).getLearningSummary("7", null, null, null);
        assertThat(actionRepository.findAll()).isEmpty();
        assertThat(auditLogRepository.findAll()).isEmpty();
    }

    @Test
    void scoreTrendQueryExecutesThroughAnalysisService() {
        ScoreTrendDTO trend = new ScoreTrendDTO(
                21L,
                3L,
                2L,
                "ASSIGNMENT",
                1001L,
                2001L,
                88,
                100,
                new BigDecimal("0.88"),
                Instant.parse("2026-06-13T08:00:00Z"));
        when(teacherAnalysisEdgeClient.listScoreTrend("7", null, null, null))
                .thenReturn(ResponseResult.success(List.of(trend)));

        AgentChatResponseDTO response = orchestrator.chat(7L, "TEACHER", null, "查看班级成绩趋势");

        assertThat(response.getResponseType()).isEqualTo("DATA");
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.getData();
        assertThat(data).containsEntry("status", "EXECUTED").containsKey("scoreTrend");
        verify(teacherAnalysisEdgeClient).listScoreTrend("7", null, null, null);
        assertThat(actionRepository.findAll()).isEmpty();
        assertThat(auditLogRepository.findAll()).isEmpty();
    }

    @Test
    void knowledgeMasteryQueryExecutesThroughAnalysisService() {
        KnowledgeMasteryDTO mastery = new KnowledgeMasteryDTO(
                21L,
                3L,
                2L,
                5L,
                new BigDecimal("0.82"),
                4,
                "ASSIGNMENT",
                1001L,
                "evt-1",
                Instant.parse("2026-06-13T08:00:00Z"));
        when(teacherAnalysisEdgeClient.listStudentKnowledgeMastery("7", 21L, 3L))
                .thenReturn(ResponseResult.success(List.of(mastery)));

        AgentChatResponseDTO response = orchestrator.chat(7L, "TEACHER", null, "查看学生21课程3知识点掌握情况");

        assertThat(response.getResponseType()).isEqualTo("DATA");
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.getData();
        assertThat(data).containsEntry("status", "EXECUTED").containsKey("knowledgeMastery");
        verify(teacherAnalysisEdgeClient).listStudentKnowledgeMastery("7", 21L, 3L);
        assertThat(actionRepository.findAll()).isEmpty();
        assertThat(auditLogRepository.findAll()).isEmpty();
    }

    @Test
    void knowledgeMasteryAnalysisForAllStudentsUsesTeacherAggregateEndpoint() {
        when(teacherKnowledgeAnalysisEdgeClient.getKnowledgePointAnalysis("7", null, null, null, null))
                .thenReturn(ResponseResult.success(Map.of(
                        "courseName", "所有课程",
                        "knowledgePointDistribution", List.of(Map.of(
                                "knowledgePointId", 99L,
                                "knowledgePointName", "函数",
                                "masteryRate", 82.5)),
                        "atRiskStudents", List.of(),
                        "weakTopics", List.of(),
                        "excellentStudentAverage", List.of())));

        AgentChatResponseDTO response = orchestrator.chat(7L, "TEACHER", null, "分析全部学生的知识点掌握情况");

        assertThat(response.getResponseType()).isEqualTo("DATA");
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.getData();
        assertThat(data)
                .containsEntry("status", "EXECUTED")
                .containsKey("knowledgeAnalysis")
                .containsEntry("message", "操作成功");
        verify(teacherKnowledgeAnalysisEdgeClient).getKnowledgePointAnalysis("7", null, null, null, null);
        assertThat(actionRepository.findAll()).isEmpty();
        assertThat(auditLogRepository.findAll()).isEmpty();
    }

    @Test
    void earlyWarningsQueryExecutesThroughTeacherWarningService() {
        when(teacherWarningEdgeClient.getWarningStats("7", null, null))
                .thenReturn(ResponseResult.success(Map.of(
                        "totalWarnings", 8,
                        "pendingWarnings", 3,
                        "resolvedWarnings", 5)));
        when(teacherWarningEdgeClient.listWarnings("7", null, null, null, null, 1, 10))
                .thenReturn(ResponseResult.success(Map.of(
                        "content", List.of(sampleWarning()),
                        "pageNumber", 1,
                        "pageSize", 10,
                        "totalElements", 1)));

        AgentChatResponseDTO response = orchestrator.chat(7L, "TEACHER", null, "学情预警");

        assertThat(response.getResponseType()).isEqualTo("DATA");
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.getData();
        assertThat(data)
                .containsEntry("status", "EXECUTED")
                .containsKey("warningStats")
                .containsKey("earlyWarnings")
                .containsEntry("message", "操作成功");
        verify(teacherWarningEdgeClient).getWarningStats("7", null, null);
        verify(teacherWarningEdgeClient).listWarnings("7", null, null, null, null, 1, 10);
        assertThat(actionRepository.findAll()).isEmpty();
        assertThat(auditLogRepository.findAll()).isEmpty();
    }

    @Test
    void generateQuestionsExecutesThroughLocalQuestionBankWithoutConfirmation() {
        AgentChatResponseDTO response = orchestrator.chat(7L, "TEACHER", null, "生成五道Java基础中等题");

        assertThat(response.getResponseType()).isEqualTo("DATA");
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.getData();
        assertThat(data)
                .containsEntry("status", "EXECUTED")
                .containsEntry("topic", "Java基础")
                .containsEntry("difficulty", "中等")
                .containsKey("questions");
        verify(aiEdgeClient, never()).generateQuestions(any(), any(), any(), any());
    }

    @Test
    void questionBankSummaryUsesLocalDocumentSummary() {
        AgentChatResponseDTO response = orchestrator.chat(7L, "TEACHER", null, "现在题库有什么题目");

        assertThat(response.getResponseType()).isEqualTo("DATA");
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.getData();
        assertThat(data).containsEntry("status", "EXECUTED").containsKey("questionBank");
        @SuppressWarnings("unchecked")
        Map<String, Object> questionBank = (Map<String, Object>) data.get("questionBank");
        assertThat(questionBank).containsKey("questions");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> questions = (List<Map<String, Object>>) questionBank.get("questions");
        assertThat(questions).isNotEmpty();
        assertThat(questions.get(0)).containsKeys("id", "content", "difficulty", "type", "answer", "analysis");
        verify(aiEdgeClient, never()).questionBankSummary(any(), any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void questionBankOverviewQueryUsesLocalSummaryInsteadOfTopicSearch() {
        AgentChatResponseDTO response = orchestrator.chat(7L, "TEACHER", null, "查询题库概览");

        assertThat(response.getResponseType()).isEqualTo("DATA");
        Map<String, Object> data = (Map<String, Object>) response.getData();
        assertThat(data)
                .containsEntry("status", "EXECUTED")
                .containsEntry("message", "题库查询完成。")
                .containsKey("questionBank");
        Map<String, Object> questionBank = (Map<String, Object>) data.get("questionBank");
        assertThat(questionBank).containsKey("questions");
        assertThat(questionBank).doesNotContainKeys("count", "actualCount", "partial");
        verify(aiEdgeClient, never()).questionBankSummary(any(), any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void questionBankQueryWithoutExplicitQuestionBankKeywordUsesLocalRetrieval() {
        AgentChatResponseDTO response = orchestrator.chat(7L, "TEACHER", null, "Java基础有哪些选择题");

        assertThat(response.getResponseType()).isEqualTo("DATA");
        Map<String, Object> data = (Map<String, Object>) response.getData();
        assertThat(data).containsEntry("status", "EXECUTED").containsKey("questionBank");
        Map<String, Object> questionBank = (Map<String, Object>) data.get("questionBank");
        List<Map<String, Object>> questions = (List<Map<String, Object>>) questionBank.get("questions");
        assertThat(questions).isNotEmpty();
        assertThat(questions)
                .extracting(question -> question.get("type"))
                .containsOnly("选择题");
        verify(aiEdgeClient, never()).questionBankSummary(any(), any(), any());
    }

    @Test
    void questionBankSummaryReturnsLocalEmptySummaryWhenLocalQuestionBankIsEmpty() {
        QuestionBankSummaryTool tool = new QuestionBankSummaryTool(
                new com._202510007517.platform.agent.questionbank.QuestionBankSummaryService(List.of()),
                disabledQuestionRagService());

        Map<String, Object> result = tool.execute(7L, "TEACHER", Map.of());

        assertThat(result)
                .containsEntry("status", "EXECUTED")
                .containsEntry("message", "题库暂无可查询数据")
                .containsKey("questionBank");
        verify(aiEdgeClient, never()).questionBankSummary(any(), any(), any());
    }

    @Test
    void generateRandomQuestionsWithoutTopicExecutesThroughLocalQuestionBank() {
        AgentChatResponseDTO response = orchestrator.chat(7L, "TEACHER", null, "生成随机题目十道");

        assertThat(response.getResponseType()).isEqualTo("DATA");
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.getData();
        assertThat(data)
                .containsEntry("status", "EXECUTED")
                .containsEntry("topic", null)
                .containsEntry("count", 10)
                .containsKey("questions");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> questions = (List<Map<String, Object>>) data.get("questions");
        assertThat(questions).hasSize(2);
        verify(aiEdgeClient, never()).generateQuestions(any(), any(), any(), any());
    }

    @Test
    void generateQuestionsReturnsLocalDisabledMessageWhenQuestionBankDisabled() {
        GenerateQuestionsTool tool = new GenerateQuestionsTool(aiEdgeClient, disabledQuestionRagService());

        Map<String, Object> result = tool.execute(7L, "TEACHER", Map.of(
                "count", 10,
                "difficulty", "中等"
        ));

        assertThat(result)
                .containsEntry("status", "EXECUTED")
                .containsEntry("topic", null)
                .containsEntry("count", 10)
                .containsEntry("actualCount", 0)
                .containsEntry("partial", false)
                .containsEntry("message", "题库题目生成功能未启用。")
                .containsKey("questions");
        verify(aiEdgeClient, never()).generateQuestions(eq("7"), eq("TEACHER"), eq("TEACHER"), any(GenerateQuestionsRequestDTO.class));
    }

    private QuestionRagService disabledQuestionRagService() {
        var properties = new com._202510007517.platform.agent.config.QuestionBankProperties();
        properties.setEnabled(false);
        return new QuestionRagService(properties, text -> List.of(), new com._202510007517.platform.agent.questionbank.QuestionBankIndex());
    }

    @Test
    void generateLearningSuggestionsExecutesThroughAiServiceWithoutConfirmation() {
        when(aiEdgeClient.learningSuggestions(eq("7"), eq("STUDENT"), eq("STUDENT"), any(LearningSuggestionRequestDTO.class)))
                .thenReturn(ResponseResult.success(Map.of("suggestions", List.of("复习 Spring"))));

        AgentChatResponseDTO response = orchestrator.chat(7L, "STUDENT", null, "生成我的学习建议");

        assertThat(response.getResponseType()).isEqualTo("DATA");
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.getData();
        assertThat(data).containsEntry("status", "EXECUTED").containsKey("aiResult");
    }

    @Test
    void generateExamRequiresConfirmationThenExecutesThroughAiService() {
        when(aiEdgeClient.generateExam(eq("7"), eq("TEACHER"), eq("TEACHER"), any(GenerateExamRequestDTO.class)))
                .thenReturn(ResponseResult.success(Map.of(
                        "title", "Java 模拟试卷",
                        "courseName", "Java",
                        "questions", List.of(Map.of("content", "题目1")),
                        "source", "question-bank"
                )));

        AgentChatResponseDTO preview = orchestrator.chat(7L, "TEACHER", null, "帮我生成一份Java模拟试卷");

        assertThat(preview.getResponseType()).isEqualTo("ACTION_PREVIEW");
        AgentExecutionResultDTO result = orchestrator.confirm(
                7L,
                "TEACHER",
                preview.getActionPreview().getActionId(),
                preview.getActionPreview().getIdempotencyKey());

        assertThat(result.getStatus()).isEqualTo("EXECUTED");
        assertThat(result.getResult())
                .containsEntry("title", "Java 模拟试卷")
                .containsEntry("courseName", "Java")
                .containsEntry("source", "question-bank")
                .containsKey("questions")
                .containsKey("aiResult");
    }

    private static Map<String, Object> sampleWarning() {
        return Map.ofEntries(
                Map.entry("id", 11L),
                Map.entry("studentId", 42L),
                Map.entry("courseId", 2L),
                Map.entry("teacherId", 7L),
                Map.entry("warningType", "LOW_SCORE"),
                Map.entry("warningLevel", "HIGH"),
                Map.entry("warningMessage", "阶段测验低于及格线"),
                Map.entry("triggerDate", Instant.parse("2026-05-19T12:00:00Z").toString()),
                Map.entry("studentName", "学生 42"),
                Map.entry("courseName", "课程 2"),
                Map.entry("status", "pending"),
                Map.entry("reason", "阶段测验低于及格线"),
                Map.entry("suggestion", "建议安排课后辅导，重点讲解薄弱知识点"));
    }
}
