package com._202510007517.platform.agent.service;

import com._202510007517.platform.agent.AgentServiceApplication;
import com._202510007517.platform.agent.api.dto.AgentActionDTO;
import com._202510007517.platform.agent.api.dto.AgentChatResponseDTO;
import com._202510007517.platform.agent.api.dto.AgentMessageDTO;
import com._202510007517.platform.agent.api.dto.AgentSessionDTO;
import com._202510007517.platform.agent.domain.AgentActionEntity;
import com._202510007517.platform.agent.domain.AgentSessionEntity;
import com._202510007517.platform.agent.model.AgentIntent;
import com._202510007517.platform.agent.repository.AgentActionRepository;
import com._202510007517.platform.agent.repository.AgentAuditLogRepository;
import com._202510007517.platform.agent.repository.AgentMessageRepository;
import com._202510007517.platform.agent.repository.AgentSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(
        classes = AgentServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.profiles.active=test",
                "eureka.client.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "spring.cloud.config.enabled=false",
                "spring.datasource.url=jdbc:h2:mem:agent-permission-policy;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.datasource.driver-class-name=org.h2.Driver"
        })
class AgentPermissionPolicyTest {

    @Autowired
    private AgentOrchestrator orchestrator;

    @Autowired
    private AgentActionRepository actionRepository;

    @Autowired
    private AgentAuditLogRepository auditLogRepository;

    @Autowired
    private AgentSessionRepository sessionRepository;

    @Autowired
    private AgentMessageRepository messageRepository;

    @BeforeEach
    void clearData() {
        auditLogRepository.deleteAll();
        actionRepository.deleteAll();
        messageRepository.deleteAll();
        sessionRepository.deleteAll();
    }

    @Test
    void studentCannotCreateTeacherWriteActionPreview() {
        assertThatThrownBy(() -> orchestrator.chat(7L, "STUDENT", null,
                "给Java课程发布作业，标题是Spring Cloud实验，满分100"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("当前角色无权执行");

        assertThat(actionRepository.findAll()).isEmpty();
    }

    @Test
    void teacherAndStudentCanUseRagKnowledgeQuery() {
        AgentPermissionPolicy policy = new AgentPermissionPolicy();

        policy.assertAllowed(7L, "TEACHER", AgentIntent.QUERY_RAG_KNOWLEDGE);
        policy.assertAllowed(8L, "STUDENT", AgentIntent.QUERY_RAG_KNOWLEDGE);
        policy.assertAllowed(9L, "ADMIN", AgentIntent.QUERY_RAG_KNOWLEDGE);
    }

    @Test
    void teacherCannotExecuteStudentOnlyReadAction() {
        assertThatThrownBy(() -> orchestrator.chat(7L, "TEACHER", null, "查看我的通知"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("当前角色无权执行");

        assertThat(actionRepository.findAll()).isEmpty();
        assertThat(messageRepository.findAll()).isEmpty();
    }

    @Test
    void studentCanCreateDeleteNotificationPreview() {
        AgentChatResponseDTO preview = orchestrator.chat(7L, "STUDENT", null, "删除通知ID 9");

        assertThat(preview.getResponseType()).isEqualTo("ACTION_PREVIEW");
        assertThat(preview.getActionPreview().getIntent()).isEqualTo("DELETE_NOTIFICATION");
        assertThat(preview.getActionPreview().getRiskLevel()).isEqualTo("CRITICAL");
        assertThat(actionRepository.findAll()).hasSize(1);
    }

    @Test
    void teacherCannotCreateDeleteNotificationPreview() {
        assertThatThrownBy(() -> orchestrator.chat(7L, "TEACHER", null, "删除通知ID 9"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("当前角色无权执行");

        assertThat(actionRepository.findAll()).isEmpty();
    }

    @Test
    void studentCanCreateDeleteAllReadNotificationsPreview() {
        AgentChatResponseDTO preview = orchestrator.chat(7L, "STUDENT", null, "删除所有已读通知");

        assertThat(preview.getResponseType()).isEqualTo("ACTION_PREVIEW");
        assertThat(preview.getActionPreview().getIntent()).isEqualTo("DELETE_ALL_READ_NOTIFICATIONS");
        assertThat(preview.getActionPreview().getRiskLevel()).isEqualTo("CRITICAL");
        assertThat(actionRepository.findAll()).hasSize(1);
    }

    @Test
    void teacherCannotCreateDeleteAllReadNotificationsPreview() {
        assertThatThrownBy(() -> orchestrator.chat(7L, "TEACHER", null, "删除所有已读通知"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("当前角色无权执行");

        assertThat(actionRepository.findAll()).isEmpty();
    }

    @Test
    void teacherCanCreateSendBatchNotificationPreview() {
        AgentChatResponseDTO preview = orchestrator.chat(7L, "TEACHER", null,
                "给学生ID 42,43批量发送通知，标题是开课通知，内容是请按时上课，类型是course");

        assertThat(preview.getResponseType()).isEqualTo("ACTION_PREVIEW");
        assertThat(preview.getActionPreview().getIntent()).isEqualTo("SEND_BATCH_NOTIFICATION");
        assertThat(preview.getActionPreview().getRiskLevel()).isEqualTo("CRITICAL");
        assertThat(actionRepository.findAll()).hasSize(1);
    }

    @Test
    void teacherCanCreateClassPreview() {
        AgentChatResponseDTO preview = orchestrator.chat(7L, "TEACHER", null,
                "帮我开一个软件2302班，2023级，最多45人，挂到课程101和专业2下面");

        assertThat(preview.getResponseType()).isEqualTo("ACTION_PREVIEW");
        assertThat(preview.getActionPreview().getIntent()).isEqualTo("CREATE_CLASS");
        assertThat(preview.getActionPreview().getRiskLevel()).isEqualTo("HIGH");
        assertThat(actionRepository.findAll()).hasSize(1);
    }

    @Test
    void studentCannotCreateClassPreview() {
        assertThatThrownBy(() -> orchestrator.chat(7L, "STUDENT", null,
                "帮我开一个软件2302班，2023级，最多45人，挂到课程101和专业2下面"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("当前角色无权执行");

        assertThat(actionRepository.findAll()).isEmpty();
    }

    @Test
    void studentCannotCreateSendBatchNotificationPreview() {
        assertThatThrownBy(() -> orchestrator.chat(7L, "STUDENT", null,
                "给学生ID 42,43批量发送通知，标题是开课通知，内容是请按时上课，类型是course"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("当前角色无权执行");

        assertThat(actionRepository.findAll()).isEmpty();
    }

    @Test
    void studentCannotExecuteTeacherDashboardQuery() {
        assertThatThrownBy(() -> orchestrator.chat(7L, "STUDENT", null, "查看教师仪表盘"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("当前角色无权执行");

        assertThat(actionRepository.findAll()).isEmpty();
        assertThat(messageRepository.findAll()).isEmpty();
    }

    @Test
    void studentCannotExecuteLearningSummaryQuery() {
        assertThatThrownBy(() -> orchestrator.chat(7L, "STUDENT", null, "查看学生学习汇总"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("当前角色无权执行");

        assertThat(actionRepository.findAll()).isEmpty();
        assertThat(messageRepository.findAll()).isEmpty();
    }

    @Test
    void studentCannotExecuteScoreTrendQuery() {
        assertThatThrownBy(() -> orchestrator.chat(7L, "STUDENT", null, "查看班级成绩趋势"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("当前角色无权执行");

        assertThat(actionRepository.findAll()).isEmpty();
        assertThat(messageRepository.findAll()).isEmpty();
    }

    @Test
    void studentCannotExecuteKnowledgeMasteryQuery() {
        assertThatThrownBy(() -> orchestrator.chat(7L, "STUDENT", null, "查看学生21课程3知识点掌握情况"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("当前角色无权执行");

        assertThat(actionRepository.findAll()).isEmpty();
        assertThat(messageRepository.findAll()).isEmpty();
    }

    @Test
    void teacherCannotCreateStudentExamSubmissionPreview() {
        assertThatThrownBy(() -> orchestrator.chat(7L, "TEACHER", null, "帮我提交Java期末考试，用时45分钟"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("当前角色无权执行");

        assertThat(actionRepository.findAll()).isEmpty();
    }

    @Test
    void confirmRechecksRolePermissionBeforeExecutingPendingAction() {
        AgentChatResponseDTO preview = orchestrator.chat(7L, "TEACHER", null, "帮我生成一份Java模拟试卷");

        assertThatThrownBy(() -> orchestrator.confirm(
                7L,
                "STUDENT",
                preview.getActionPreview().getActionId(),
                preview.getActionPreview().getIdempotencyKey()))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("无权确认该 Agent 操作");
    }

    @Test
    void confirmRejectsActionOwnedByAnotherUserSession() {
        AgentChatResponseDTO preview = orchestrator.chat(7L, "TEACHER", null, "帮我生成一份Java模拟试卷");

        assertThatThrownBy(() -> orchestrator.confirm(
                8L,
                "TEACHER",
                preview.getActionPreview().getActionId(),
                preview.getActionPreview().getIdempotencyKey()))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("无权确认该 Agent 操作");
    }

    @Test
    void getActionReturnsOwnedActionWithParsedPayloads() {
        AgentSessionEntity session = new AgentSessionEntity();
        session.setUserId(7L);
        session.setUserRole("TEACHER");
        session.setStatus("ACTIVE");
        AgentSessionEntity savedSession = sessionRepository.save(session);

        AgentActionEntity action = new AgentActionEntity();
        action.setSessionId(savedSession.getId());
        action.setIntent("PUBLISH_ASSIGNMENT");
        action.setStatus("EXECUTED");
        action.setRiskLevel("MEDIUM");
        action.setPreviewJson("{\"title\":\"Spring Cloud实验\"}");
        action.setRequestJson("{\"intent\":\"PUBLISH_ASSIGNMENT\",\"slots\":{\"courseId\":12}}");
        action.setResultJson("{\"status\":\"EXECUTED\",\"assignmentId\":100}");
        action.setIdempotencyKey("idem-history-1");
        AgentActionEntity savedAction = actionRepository.save(action);

        AgentActionDTO dto = orchestrator.getAction(7L, "TEACHER", savedAction.getId());

        assertThat(dto.getActionId()).isEqualTo(savedAction.getId());
        assertThat(dto.getSessionId()).isEqualTo(String.valueOf(savedSession.getId()));
        assertThat(dto.getStatus()).isEqualTo("EXECUTED");
        assertThat(dto.getPreview()).containsEntry("title", "Spring Cloud实验");
        assertThat(dto.getRequest()).containsEntry("intent", "PUBLISH_ASSIGNMENT");
        assertThat(dto.getResult()).containsEntry("assignmentId", 100);
    }

    @Test
    void getActionRejectsActionOwnedByAnotherUserSession() {
        AgentSessionEntity session = new AgentSessionEntity();
        session.setUserId(7L);
        session.setUserRole("TEACHER");
        session.setStatus("ACTIVE");
        AgentSessionEntity savedSession = sessionRepository.save(session);

        AgentActionEntity action = new AgentActionEntity();
        action.setSessionId(savedSession.getId());
        action.setIntent("PUBLISH_ASSIGNMENT");
        action.setStatus("PENDING_CONFIRMATION");
        action.setRiskLevel("MEDIUM");
        action.setPreviewJson("{\"title\":\"Spring Cloud实验\"}");
        action.setRequestJson("{\"intent\":\"PUBLISH_ASSIGNMENT\",\"slots\":{\"courseId\":12}}");
        action.setIdempotencyKey("idem-history-2");
        AgentActionEntity savedAction = actionRepository.save(action);

        assertThatThrownBy(() -> orchestrator.getAction(8L, "TEACHER", savedAction.getId()))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("无权查看该 Agent 操作");
    }

    @Test
    void listSessionsReturnsOnlyOwnedRoleSessions() {
        AgentSessionEntity teacherSession = new AgentSessionEntity();
        teacherSession.setUserId(7L);
        teacherSession.setUserRole("TEACHER");
        teacherSession.setStatus("ACTIVE");
        teacherSession.setPendingIntent("PUBLISH_ASSIGNMENT");
        AgentSessionEntity savedTeacherSession = sessionRepository.save(teacherSession);

        AgentSessionEntity studentSession = new AgentSessionEntity();
        studentSession.setUserId(7L);
        studentSession.setUserRole("STUDENT");
        studentSession.setStatus("ACTIVE");
        sessionRepository.save(studentSession);

        AgentSessionEntity otherUserSession = new AgentSessionEntity();
        otherUserSession.setUserId(8L);
        otherUserSession.setUserRole("TEACHER");
        otherUserSession.setStatus("ACTIVE");
        sessionRepository.save(otherUserSession);

        var sessions = orchestrator.listSessions(7L, "TEACHER");

        assertThat(sessions).hasSize(1);
        assertThat(sessions.get(0).getSessionId()).isEqualTo(String.valueOf(savedTeacherSession.getId()));
        assertThat(sessions.get(0).getUserRole()).isEqualTo("TEACHER");
        assertThat(sessions.get(0).getPendingIntent()).isEqualTo("PUBLISH_ASSIGNMENT");
        assertThat(sessions.get(0).getActions()).isEmpty();
    }

    @Test
    void getSessionReturnsOwnedSessionWithActions() {
        AgentSessionEntity session = new AgentSessionEntity();
        session.setUserId(7L);
        session.setUserRole("TEACHER");
        session.setStatus("ACTIVE");
        session.setPendingSlotsJson("{\"title\":\"Spring Cloud实验\"}");
        AgentSessionEntity savedSession = sessionRepository.save(session);

        AgentActionEntity action = new AgentActionEntity();
        action.setSessionId(savedSession.getId());
        action.setIntent("PUBLISH_ASSIGNMENT");
        action.setStatus("PENDING_CONFIRMATION");
        action.setRiskLevel("MEDIUM");
        action.setPreviewJson("{\"title\":\"Spring Cloud实验\"}");
        action.setRequestJson("{\"intent\":\"PUBLISH_ASSIGNMENT\",\"slots\":{\"courseId\":12}}");
        action.setIdempotencyKey("idem-session-history-1");
        actionRepository.save(action);

        AgentSessionDTO dto = orchestrator.getSession(7L, "TEACHER", savedSession.getId());

        assertThat(dto.getSessionId()).isEqualTo(String.valueOf(savedSession.getId()));
        assertThat(dto.getUserRole()).isEqualTo("TEACHER");
        assertThat(dto.getPendingSlots()).containsEntry("title", "Spring Cloud实验");
        assertThat(dto.getActions()).hasSize(1);
        assertThat(dto.getActions().get(0).getIntent()).isEqualTo("PUBLISH_ASSIGNMENT");
        assertThat(dto.getActions().get(0).getPreview()).containsEntry("title", "Spring Cloud实验");
    }

    @Test
    void chatPersistsUserAndAssistantMessagesInSessionHistory() {
        AgentChatResponseDTO response = orchestrator.chat(7L, "STUDENT", null, "今天食堂吃什么");

        AgentSessionDTO dto = orchestrator.getSession(7L, "STUDENT", Long.valueOf(response.getSessionId()));

        assertThat(dto.getMessages()).hasSize(2);
        AgentMessageDTO userMessage = dto.getMessages().get(0);
        AgentMessageDTO assistantMessage = dto.getMessages().get(1);
        assertThat(userMessage.getRole()).isEqualTo("USER");
        assertThat(userMessage.getContent()).isEqualTo("今天食堂吃什么");
        assertThat(assistantMessage.getRole()).isEqualTo("ASSISTANT");
        assertThat(assistantMessage.getContent()).isEqualTo("暂时还不能处理这个请求。");
        assertThat(messageRepository.findAll()).hasSize(2);
    }

    @Test
    void getSessionRejectsSessionOwnedByAnotherUser() {
        AgentSessionEntity session = new AgentSessionEntity();
        session.setUserId(7L);
        session.setUserRole("TEACHER");
        session.setStatus("ACTIVE");
        AgentSessionEntity savedSession = sessionRepository.save(session);

        assertThatThrownBy(() -> orchestrator.getSession(8L, "TEACHER", savedSession.getId()))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("无权查看该 Agent 会话");
    }

    @Test
    void chatDoesNotReuseSessionOwnedByAnotherUser() {
        AgentSessionEntity session = new AgentSessionEntity();
        session.setUserId(7L);
        session.setUserRole("TEACHER");
        session.setStatus("ACTIVE");
        AgentSessionEntity savedSession = sessionRepository.save(session);

        AgentChatResponseDTO response = orchestrator.chat(8L, "TEACHER", String.valueOf(savedSession.getId()),
                "帮我生成一份Java模拟试卷");

        assertThat(response.getSessionId()).isNotEqualTo(String.valueOf(savedSession.getId()));
        AgentSessionEntity newSession = sessionRepository.findById(Long.valueOf(response.getSessionId())).orElseThrow();
        assertThat(newSession.getUserId()).isEqualTo(8L);
        assertThat(newSession.getUserRole()).isEqualTo("TEACHER");
    }

    @Test
    void cancelPendingActionOwnedByUser() {
        AgentSessionEntity session = new AgentSessionEntity();
        session.setUserId(7L);
        session.setUserRole("TEACHER");
        session.setStatus("ACTIVE");
        AgentSessionEntity savedSession = sessionRepository.save(session);

        AgentActionEntity action = new AgentActionEntity();
        action.setSessionId(savedSession.getId());
        action.setIntent("PUBLISH_ASSIGNMENT");
        action.setStatus("PENDING_CONFIRMATION");
        action.setRiskLevel("MEDIUM");
        action.setPreviewJson("{\"title\":\"Spring Cloud实验\"}");
        action.setRequestJson("{\"intent\":\"PUBLISH_ASSIGNMENT\",\"slots\":{\"courseId\":12}}");
        action.setIdempotencyKey("idem-cancel-1");
        AgentActionEntity savedAction = actionRepository.save(action);

        var result = orchestrator.cancel(7L, "TEACHER", savedAction.getId());

        assertThat(result.getActionId()).isEqualTo(savedAction.getId());
        assertThat(result.getIntent()).isEqualTo("PUBLISH_ASSIGNMENT");
        assertThat(result.getStatus()).isEqualTo("CANCELLED");
        assertThat(result.getMessage()).isEqualTo("操作已取消。");
        assertThat(result.getResult()).containsEntry("message", "操作已取消。");
        AgentActionEntity reloaded = actionRepository.findById(savedAction.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo("CANCELLED");
        assertThat(reloaded.getResultJson()).contains("操作已取消。");
        assertThat(reloaded.getExecutedAt()).isNull();
        assertThat(auditLogRepository.findAll()).isEmpty();
    }

    @Test
    void cancelAlreadyCancelledActionIsIdempotentForOwner() {
        AgentSessionEntity session = new AgentSessionEntity();
        session.setUserId(7L);
        session.setUserRole("TEACHER");
        session.setStatus("ACTIVE");
        AgentSessionEntity savedSession = sessionRepository.save(session);

        AgentActionEntity action = new AgentActionEntity();
        action.setSessionId(savedSession.getId());
        action.setIntent("PUBLISH_ASSIGNMENT");
        action.setStatus("CANCELLED");
        action.setRiskLevel("MEDIUM");
        action.setPreviewJson("{\"title\":\"Spring Cloud实验\"}");
        action.setRequestJson("{\"intent\":\"PUBLISH_ASSIGNMENT\",\"slots\":{\"courseId\":12}}");
        action.setResultJson("{\"status\":\"CANCELLED\",\"message\":\"操作已取消。\"}");
        action.setIdempotencyKey("idem-cancel-repeat");
        AgentActionEntity savedAction = actionRepository.save(action);

        var result = orchestrator.cancel(7L, "TEACHER", savedAction.getId());

        assertThat(result.getStatus()).isEqualTo("CANCELLED");
        assertThat(result.getMessage()).isEqualTo("操作已取消。");
        assertThat(result.getResult()).containsEntry("status", "CANCELLED")
                .containsEntry("message", "操作已取消。");
    }

    @Test
    void cancelRejectsActionOwnedByAnotherUserSession() {
        AgentSessionEntity session = new AgentSessionEntity();
        session.setUserId(7L);
        session.setUserRole("TEACHER");
        session.setStatus("ACTIVE");
        AgentSessionEntity savedSession = sessionRepository.save(session);

        AgentActionEntity action = new AgentActionEntity();
        action.setSessionId(savedSession.getId());
        action.setIntent("PUBLISH_ASSIGNMENT");
        action.setStatus("PENDING_CONFIRMATION");
        action.setRiskLevel("MEDIUM");
        action.setPreviewJson("{\"title\":\"Spring Cloud实验\"}");
        action.setRequestJson("{\"intent\":\"PUBLISH_ASSIGNMENT\",\"slots\":{\"courseId\":12}}");
        action.setIdempotencyKey("idem-cancel-2");
        AgentActionEntity savedAction = actionRepository.save(action);

        assertThatThrownBy(() -> orchestrator.cancel(8L, "TEACHER", savedAction.getId()))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("无权取消该 Agent 操作");
    }

    @Test
    void cancelRejectsAlreadyExecutedAction() {
        AgentSessionEntity session = new AgentSessionEntity();
        session.setUserId(7L);
        session.setUserRole("TEACHER");
        session.setStatus("ACTIVE");
        AgentSessionEntity savedSession = sessionRepository.save(session);

        AgentActionEntity action = new AgentActionEntity();
        action.setSessionId(savedSession.getId());
        action.setIntent("PUBLISH_ASSIGNMENT");
        action.setStatus("EXECUTED");
        action.setRiskLevel("MEDIUM");
        action.setPreviewJson("{\"title\":\"Spring Cloud实验\"}");
        action.setRequestJson("{\"intent\":\"PUBLISH_ASSIGNMENT\",\"slots\":{\"courseId\":12}}");
        action.setIdempotencyKey("idem-cancel-3");
        AgentActionEntity savedAction = actionRepository.save(action);

        assertThatThrownBy(() -> orchestrator.cancel(7L, "TEACHER", savedAction.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not pending confirmation");
    }
}
