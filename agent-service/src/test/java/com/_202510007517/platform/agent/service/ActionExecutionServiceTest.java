package com._202510007517.platform.agent.service;

import com._202510007517.platform.agent.domain.AgentActionEntity;
import com._202510007517.platform.agent.model.AgentIntent;
import com._202510007517.platform.agent.repository.AgentActionRepository;
import com._202510007517.platform.agent.tool.AgentTool;
import com._202510007517.platform.agent.tool.ToolRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ActionExecutionServiceTest {

    private final FakeAgentTool publishAssignmentTool = new FakeAgentTool(AgentIntent.PUBLISH_ASSIGNMENT);
    private final AgentActionRepository actionRepository = mock(AgentActionRepository.class);
    private final AgentAuditService auditService = mock(AgentAuditService.class);
    private final ActionExecutionService service = new ActionExecutionService(
            new ToolRegistry(List.of(publishAssignmentTool)),
            actionRepository,
            auditService,
            new AgentDataMaskingPolicy(),
            new ObjectMapper()
    );

    @Test
    void executesToolAndWritesSuccessfulActionAndAudit() {
        AgentActionEntity action = pendingAction();
        Map<String, Object> request = Map.of("title", "Spring Cloud实验");
        publishAssignmentTool.result(Map.of("status", "EXECUTED", "assignmentId", 99L));

        var result = service.execute(7L, "TEACHER", action, AgentIntent.PUBLISH_ASSIGNMENT, request);

        assertThat(result.getStatus()).isEqualTo("EXECUTED");
        assertThat(result.getMessage()).isEqualTo("操作已执行。");
        assertThat(action.getStatus()).isEqualTo("EXECUTED");
        assertThat(action.getConfirmedAt()).isNotNull();
        assertThat(action.getExecutedAt()).isNotNull();
        assertThat(action.getResultJson()).contains("\"assignmentId\":99");
        var inOrder = inOrder(actionRepository, auditService);
        inOrder.verify(actionRepository).save(action);
        inOrder.verify(auditService).recordActionExecution(
                11L, 7L, "TEACHER", AgentIntent.PUBLISH_ASSIGNMENT, true, null);
    }

    @Test
    void recordsFailedActionAndAuditWhenToolThrows() {
        AgentActionEntity action = pendingAction();
        Map<String, Object> request = Map.of("title", "Spring Cloud实验");
        publishAssignmentTool.throwing(new IllegalStateException("目标服务不可用"));

        var result = service.execute(7L, "TEACHER", action, AgentIntent.PUBLISH_ASSIGNMENT, request);

        assertThat(result.getStatus()).isEqualTo("FAILED");
        assertThat(result.getMessage()).isEqualTo("目标服务不可用");
        assertThat(action.getStatus()).isEqualTo("FAILED");
        assertThat(action.getErrorMessage()).isEqualTo("目标服务不可用");
        assertThat(action.getResultJson()).contains("\"status\":\"FAILED\"");
        var inOrder = inOrder(actionRepository, auditService);
        inOrder.verify(actionRepository).save(action);
        inOrder.verify(auditService).recordActionExecution(
                11L, 7L, "TEACHER", AgentIntent.PUBLISH_ASSIGNMENT, false, "目标服务不可用");
    }

    @Test
    void recordsFailedActionAndAuditWhenToolReturnsNonExecutedStatus() {
        AgentActionEntity action = pendingAction();
        Map<String, Object> request = Map.of("title", "Spring Cloud实验");
        publishAssignmentTool.result(Map.of("status", "VALIDATION_FAILED", "message", "课程不存在"));

        var result = service.execute(7L, "TEACHER", action, AgentIntent.PUBLISH_ASSIGNMENT, request);

        assertThat(result.getStatus()).isEqualTo("FAILED");
        assertThat(result.getMessage()).isEqualTo("课程不存在");
        assertThat(action.getStatus()).isEqualTo("FAILED");
        assertThat(action.getErrorMessage()).isEqualTo("课程不存在");
        assertThat(action.getResultJson()).contains("\"VALIDATION_FAILED\"");
        verify(auditService).recordActionExecution(
                11L, 7L, "TEACHER", AgentIntent.PUBLISH_ASSIGNMENT, false, "课程不存在");
    }

    @Test
    void masksSensitiveExecutionResultAndErrorBeforeSavingAndAuditing() {
        AgentActionEntity action = pendingAction();
        publishAssignmentTool.result(Map.of(
                "status", "VALIDATION_FAILED",
                "message", "手机号13812345678 token=secret-token",
                "password", "abc123",
                "details", Map.of(
                        "apiKey", "key-123",
                        "items", List.of("身份证110101199001011234", Map.of("secret", "hidden"))
                )
        ));

        var result = service.execute(7L, "TEACHER", action, AgentIntent.PUBLISH_ASSIGNMENT, Map.of());

        assertThat(result.getMessage()).isEqualTo("手机号138****5678 token=***");
        assertThat(action.getErrorMessage()).isEqualTo("手机号138****5678 token=***");
        assertThat(action.getResultJson()).contains(
                "138****5678", "token=***", "\"password\":\"***\"", "\"apiKey\":\"***\"",
                "110101********1234", "\"secret\":\"***\""
        );
        assertThat(action.getResultJson()).doesNotContain(
                "13812345678", "secret-token", "abc123", "key-123", "110101199001011234", "hidden"
        );
        verify(auditService).recordActionExecution(
                11L, 7L, "TEACHER", AgentIntent.PUBLISH_ASSIGNMENT, false, "手机号138****5678 token=***");
    }

    @Test
    void recordsFailedActionWhenThrownExceptionMessageIsNull() {
        AgentActionEntity action = pendingAction();
        publishAssignmentTool.throwing(new IllegalStateException());

        var result = service.execute(7L, "TEACHER", action, AgentIntent.PUBLISH_ASSIGNMENT, Map.of());

        assertThat(result.getStatus()).isEqualTo("FAILED");
        assertThat(result.getMessage()).isEqualTo("操作执行失败。");
        assertThat(action.getStatus()).isEqualTo("FAILED");
        assertThat(action.getErrorMessage()).isEqualTo("操作执行失败。");
        assertThat(action.getResultJson()).contains("\"status\":\"FAILED\"", "\"message\":\"操作执行失败。\"");
        verify(auditService).recordActionExecution(
                11L, 7L, "TEACHER", AgentIntent.PUBLISH_ASSIGNMENT, false, "操作执行失败。");
    }

    @Test
    void preservesExistingConfirmedAtFromSecondConfirmationStage() {
        AgentActionEntity action = pendingAction();
        LocalDateTime stagedConfirmationTime = LocalDateTime.of(2026, 6, 14, 9, 30);
        action.setConfirmedAt(stagedConfirmationTime);
        publishAssignmentTool.result(Map.of("status", "EXECUTED"));

        service.execute(7L, "TEACHER", action, AgentIntent.PUBLISH_ASSIGNMENT, Map.of());

        assertThat(action.getConfirmedAt()).isEqualTo(stagedConfirmationTime);
    }

    private AgentActionEntity pendingAction() {
        AgentActionEntity action = new AgentActionEntity();
        action.setId(11L);
        action.setSessionId(3L);
        action.setIntent("PUBLISH_ASSIGNMENT");
        action.setStatus("PENDING_CONFIRMATION");
        action.setRiskLevel("MEDIUM");
        action.setRequestJson("{\"intent\":\"PUBLISH_ASSIGNMENT\",\"slots\":{\"title\":\"Spring Cloud实验\"}}");
        action.setIdempotencyKey("idem-1");
        return action;
    }

    private static class FakeAgentTool implements AgentTool {
        private final AgentIntent intent;
        private BiFunction<Long, String, Map<String, Object>> executor = (userId, userRole) -> Map.of("status", "EXECUTED");

        private FakeAgentTool(AgentIntent intent) {
            this.intent = intent;
        }

        @Override
        public AgentIntent intent() {
            return intent;
        }

        @Override
        public Map<String, Object> execute(Long userId, String userRole, Map<String, Object> request) {
            return executor.apply(userId, userRole);
        }

        private void result(Map<String, Object> result) {
            executor = (userId, userRole) -> result;
        }

        private void throwing(RuntimeException exception) {
            executor = (userId, userRole) -> {
                throw exception;
            };
        }
    }
}
