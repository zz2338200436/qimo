package com._202510007517.platform.agent.service;

import com._202510007517.platform.agent.domain.AgentAuditLogEntity;
import com._202510007517.platform.agent.model.AgentIntent;
import com._202510007517.platform.agent.repository.AgentAuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AgentAuditServiceTest {

    private final AgentAuditLogRepository auditLogRepository = mock(AgentAuditLogRepository.class);
    private final AgentAuditService service = new AgentAuditService(auditLogRepository, new AgentDataMaskingPolicy());

    @Test
    void recordsSuccessfulActionExecutionAudit() {
        service.recordActionExecution(11L, 7L, "TEACHER", AgentIntent.PUBLISH_ASSIGNMENT, true, null);

        var auditCaptor = forClass(AgentAuditLogEntity.class);
        verify(auditLogRepository).save(auditCaptor.capture());
        AgentAuditLogEntity audit = auditCaptor.getValue();
        assertThat(audit.getActionId()).isEqualTo(11L);
        assertThat(audit.getUserId()).isEqualTo(7L);
        assertThat(audit.getUserRole()).isEqualTo("TEACHER");
        assertThat(audit.getOperation()).isEqualTo("PUBLISH_ASSIGNMENT");
        assertThat(audit.getTargetService()).isEqualTo("assignment-service");
        assertThat(audit.getTargetResource()).isEqualTo("agent_action:11");
        assertThat(audit.getSuccess()).isTrue();
        assertThat(audit.getErrorMessage()).isNull();
    }

    @Test
    void recordsFailedActionExecutionAudit() {
        service.recordActionExecution(12L, 8L, "STUDENT", AgentIntent.SUBMIT_EXAM, false, "考试已截止");

        var auditCaptor = forClass(AgentAuditLogEntity.class);
        verify(auditLogRepository).save(auditCaptor.capture());
        AgentAuditLogEntity audit = auditCaptor.getValue();
        assertThat(audit.getActionId()).isEqualTo(12L);
        assertThat(audit.getUserId()).isEqualTo(8L);
        assertThat(audit.getUserRole()).isEqualTo("STUDENT");
        assertThat(audit.getOperation()).isEqualTo("SUBMIT_EXAM");
        assertThat(audit.getTargetService()).isEqualTo("exam-service");
        assertThat(audit.getSuccess()).isFalse();
        assertThat(audit.getErrorMessage()).isEqualTo("考试已截止");
    }

    @Test
    void masksSensitiveAuditErrorMessageBeforeSaving() {
        service.recordActionExecution(13L, 7L, "TEACHER", AgentIntent.PUBLISH_ASSIGNMENT, false,
                "手机号13812345678 password=abc123");

        var auditCaptor = forClass(AgentAuditLogEntity.class);
        verify(auditLogRepository).save(auditCaptor.capture());
        AgentAuditLogEntity audit = auditCaptor.getValue();
        assertThat(audit.getErrorMessage()).isEqualTo("手机号138****5678 password=***");
    }

    @ParameterizedTest
    @MethodSource("targetServiceMappings")
    void mapsEveryIntentToOwnedTargetService(AgentIntent intent, String expectedTargetService) {
        assertThat(service.targetService(intent)).isEqualTo(expectedTargetService);
    }

    private static Stream<Arguments> targetServiceMappings() {
        return Stream.of(
                Arguments.of(AgentIntent.PUBLISH_ASSIGNMENT, "assignment-service"),
                Arguments.of(AgentIntent.UPDATE_ASSIGNMENT, "assignment-service"),
                Arguments.of(AgentIntent.DELETE_ASSIGNMENT, "assignment-service"),
                Arguments.of(AgentIntent.GRADE_ASSIGNMENT, "assignment-service"),
                Arguments.of(AgentIntent.SUBMIT_ASSIGNMENT, "assignment-service"),
                Arguments.of(AgentIntent.QUERY_PENDING_ASSIGNMENTS, "assignment-service"),
                Arguments.of(AgentIntent.QUERY_ASSIGNMENTS, "assignment-service"),
                Arguments.of(AgentIntent.QUERY_ASSIGNMENT_DETAIL, "assignment-service"),
                Arguments.of(AgentIntent.QUERY_ASSIGNMENT_SUBMISSIONS, "assignment-service"),
                Arguments.of(AgentIntent.CREATE_COURSE, "course-service"),
                Arguments.of(AgentIntent.UPDATE_COURSE, "course-service"),
                Arguments.of(AgentIntent.DELETE_COURSE, "course-service"),
                Arguments.of(AgentIntent.CREATE_CLASS, "course-service"),
                Arguments.of(AgentIntent.QUERY_COURSES, "course-service"),
                Arguments.of(AgentIntent.QUERY_COURSE_DETAIL, "course-service"),
                Arguments.of(AgentIntent.QUERY_CLASSES, "course-service"),
                Arguments.of(AgentIntent.QUERY_CLASS_DETAIL, "course-service"),
                Arguments.of(AgentIntent.PUBLISH_EXAM, "exam-service"),
                Arguments.of(AgentIntent.UPDATE_EXAM, "exam-service"),
                Arguments.of(AgentIntent.DELETE_EXAM, "exam-service"),
                Arguments.of(AgentIntent.GRADE_EXAM, "exam-service"),
                Arguments.of(AgentIntent.SUBMIT_EXAM, "exam-service"),
                Arguments.of(AgentIntent.QUERY_EXAMS, "exam-service"),
                Arguments.of(AgentIntent.QUERY_EXAM_DETAIL, "exam-service"),
                Arguments.of(AgentIntent.QUERY_EXAM_SUBMISSIONS, "exam-service"),
                Arguments.of(AgentIntent.QUERY_SCORES, "exam-service"),
                Arguments.of(AgentIntent.GENERATE_QUESTIONS, "ai-service"),
                Arguments.of(AgentIntent.GENERATE_EXAM, "ai-service"),
                Arguments.of(AgentIntent.GENERATE_LEARNING_SUGGESTIONS, "ai-service"),
                Arguments.of(AgentIntent.QUERY_NOTIFICATIONS, "notification-service"),
                Arguments.of(AgentIntent.QUERY_UNREAD_NOTIFICATION_COUNT, "notification-service"),
                Arguments.of(AgentIntent.MARK_ALL_NOTIFICATIONS_READ, "notification-service"),
                Arguments.of(AgentIntent.MARK_NOTIFICATION_READ, "notification-service"),
                Arguments.of(AgentIntent.DELETE_NOTIFICATION, "notification-service"),
                Arguments.of(AgentIntent.DELETE_ALL_READ_NOTIFICATIONS, "notification-service"),
                Arguments.of(AgentIntent.SEND_NOTIFICATION, "notification-service"),
                Arguments.of(AgentIntent.SEND_BATCH_NOTIFICATION, "notification-service"),
                Arguments.of(AgentIntent.QUERY_STUDENT_STATS, "analysis-service"),
                Arguments.of(AgentIntent.QUERY_STUDY_TIME_DISTRIBUTION, "analysis-service"),
                Arguments.of(AgentIntent.QUERY_TEACHER_DASHBOARD, "analysis-service"),
                Arguments.of(AgentIntent.QUERY_LEARNING_SUMMARY, "analysis-service"),
                Arguments.of(AgentIntent.QUERY_SCORE_TREND, "analysis-service"),
                Arguments.of(AgentIntent.QUERY_KNOWLEDGE_MASTERY, "analysis-service"),
                Arguments.of(AgentIntent.QUERY_RAG_KNOWLEDGE, "agent-service"),
                Arguments.of(AgentIntent.UNKNOWN, "agent-service")
        );
    }
}
