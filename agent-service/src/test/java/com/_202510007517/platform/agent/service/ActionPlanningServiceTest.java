package com._202510007517.platform.agent.service;

import com._202510007517.platform.agent.api.dto.AgentChatResponseDTO;
import com._202510007517.platform.agent.domain.AgentActionEntity;
import com._202510007517.platform.agent.domain.AgentSessionEntity;
import com._202510007517.platform.agent.model.AgentIntent;
import com._202510007517.platform.agent.model.RecognizedIntent;
import com._202510007517.platform.agent.repository.AgentActionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActionPlanningServiceTest {

    private final AgentActionRepository actionRepository = mock(AgentActionRepository.class);
    private final AgentRiskPolicy riskPolicy = new AgentRiskPolicy();
    private final ActionPlanningService service = new ActionPlanningService(
            new AgentActionMapper(riskPolicy, new AgentConfirmationPolicy(riskPolicy)),
            actionRepository,
            new AgentIdempotencyService(),
            new AgentDataMaskingPolicy(),
            new ObjectMapper()
    );

    @Test
    void createsPendingActionPreviewPlanWithoutExecutingTool() {
        AgentSessionEntity session = new AgentSessionEntity();
        session.setId(55L);
        RecognizedIntent intent = new RecognizedIntent(
                AgentIntent.PUBLISH_ASSIGNMENT,
                0.92,
                Map.of("courseId", 12L, "title", "Spring Cloud实验", "dueDate", "明晚", "maxScore", 100)
        );
        when(actionRepository.save(any())).thenAnswer(invocation -> {
            AgentActionEntity action = invocation.getArgument(0);
            action.setId(77L);
            return action;
        });

        AgentChatResponseDTO response = service.createPreviewResponse(session, intent);

        assertThat(response.getSessionId()).isEqualTo("55");
        assertThat(response.getResponseType()).isEqualTo("ACTION_PREVIEW");
        assertThat(response.getActionPreview().getActionId()).isEqualTo(77L);
        assertThat(response.getActionPreview().getIdempotencyKey()).isNotBlank();
        assertThat(response.getActionPreview().getPreview()).containsEntry("title", "Spring Cloud实验");

        ArgumentCaptor<AgentActionEntity> actionCaptor = ArgumentCaptor.forClass(AgentActionEntity.class);
        verify(actionRepository).save(actionCaptor.capture());
        AgentActionEntity action = actionCaptor.getValue();
        assertThat(action.getSessionId()).isEqualTo(55L);
        assertThat(action.getIntent()).isEqualTo("PUBLISH_ASSIGNMENT");
        assertThat(action.getStatus()).isEqualTo("PENDING_CONFIRMATION");
        assertThat(action.getRiskLevel()).isEqualTo("MEDIUM");
        assertThat(action.getIdempotencyKey()).isEqualTo(response.getActionPreview().getIdempotencyKey());
        assertThat(action.getPreviewJson()).contains("Spring Cloud实验");
        assertThat(action.getRequestJson()).contains("\"intent\":\"PUBLISH_ASSIGNMENT\"");
        assertThat(action.getRequestJson()).contains("\"slots\"");
        assertThat(action.getExpiresAt()).isNotNull();
    }

    @Test
    void masksSensitiveActionPreviewAndRequestPayloadsBeforeSaving() {
        AgentSessionEntity session = new AgentSessionEntity();
        session.setId(56L);
        RecognizedIntent intent = new RecognizedIntent(
                AgentIntent.PUBLISH_ASSIGNMENT,
                0.92,
                Map.of(
                        "courseId", 12L,
                        "title", "联系13812345678",
                        "description", "邮箱student@example.com token=secret-token",
                        "password", "abc123",
                        "metadata", Map.of(
                                "api_key", "key-123",
                                "items", java.util.List.of("身份证110101199001011234", Map.of("secret", "hidden"))
                        )
                )
        );
        when(actionRepository.save(any())).thenAnswer(invocation -> {
            AgentActionEntity action = invocation.getArgument(0);
            action.setId(78L);
            return action;
        });

        service.createPreviewResponse(session, intent);

        ArgumentCaptor<AgentActionEntity> actionCaptor = ArgumentCaptor.forClass(AgentActionEntity.class);
        verify(actionRepository).save(actionCaptor.capture());
        AgentActionEntity action = actionCaptor.getValue();
        assertThat(action.getPreviewJson()).contains(
                "138****5678", "s***@example.com", "token=***", "\"api_key\":\"***\"",
                "110101********1234", "\"secret\":\"***\""
        );
        assertThat(action.getPreviewJson()).contains("\"password\":\"***\"");
        assertThat(action.getPreviewJson()).doesNotContain(
                "13812345678", "student@example.com", "secret-token", "abc123",
                "key-123", "110101199001011234", "hidden"
        );
        assertThat(action.getRequestJson()).contains(
                "138****5678", "s***@example.com", "token=***", "\"api_key\":\"***\"",
                "110101********1234", "\"secret\":\"***\""
        );
        assertThat(action.getRequestJson()).contains("\"password\":\"***\"");
        assertThat(action.getRequestJson()).doesNotContain(
                "13812345678", "student@example.com", "secret-token", "abc123",
                "key-123", "110101199001011234", "hidden"
        );
    }
}
