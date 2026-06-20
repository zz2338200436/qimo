package com._202510007517.platform.agent.service;

import com._202510007517.platform.agent.api.dto.AgentChatResponseDTO;
import com._202510007517.platform.agent.domain.AgentActionEntity;
import com._202510007517.platform.agent.domain.AgentMessageEntity;
import com._202510007517.platform.agent.domain.AgentSessionEntity;
import com._202510007517.platform.agent.model.AgentIntent;
import com._202510007517.platform.agent.model.RecognizedIntent;
import com._202510007517.platform.agent.repository.AgentActionRepository;
import com._202510007517.platform.agent.repository.AgentMessageRepository;
import com._202510007517.platform.agent.repository.AgentSessionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentSessionServiceTest {

    private final AgentSessionRepository sessionRepository = mock(AgentSessionRepository.class);
    private final AgentActionRepository actionRepository = mock(AgentActionRepository.class);
    private final AgentMessageRepository messageRepository = mock(AgentMessageRepository.class);
    private final AgentSessionService service = new AgentSessionService(
            sessionRepository,
            actionRepository,
            messageRepository,
            new AgentDataMaskingPolicy(),
            new ObjectMapper()
    );

    @Test
    void resolvesExistingOwnedSession() {
        AgentSessionEntity existing = session(3L, 7L, "TEACHER");
        when(sessionRepository.findById(3L)).thenReturn(Optional.of(existing));

        AgentSessionEntity resolved = service.resolveSession(7L, "TEACHER", "3");

        assertThat(resolved).isSameAs(existing);
    }

    @Test
    void createsNewSessionWhenProvidedSessionIsInvalidOrNotOwned() {
        AgentSessionEntity otherUserSession = session(3L, 8L, "TEACHER");
        AgentSessionEntity newSession = session(4L, 7L, "TEACHER");
        when(sessionRepository.findById(3L)).thenReturn(Optional.of(otherUserSession));
        when(sessionRepository.save(any())).thenReturn(newSession);

        AgentSessionEntity resolved = service.resolveSession(7L, "TEACHER", "3");

        assertThat(resolved.getId()).isEqualTo(4L);
        var sessionCaptor = forClass(AgentSessionEntity.class);
        verify(sessionRepository).save(sessionCaptor.capture());
        AgentSessionEntity created = sessionCaptor.getValue();
        assertThat(created.getUserId()).isEqualTo(7L);
        assertThat(created.getUserRole()).isEqualTo("TEACHER");
        assertThat(created.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void createsNewSessionWhenSessionIdIsNotNumeric() {
        AgentSessionEntity newSession = session(4L, 7L, "STUDENT");
        when(sessionRepository.save(any())).thenReturn(newSession);

        AgentSessionEntity resolved = service.resolveSession(7L, "STUDENT", "not-a-number");

        assertThat(resolved.getId()).isEqualTo(4L);
        var sessionCaptor = forClass(AgentSessionEntity.class);
        verify(sessionRepository).save(sessionCaptor.capture());
        assertThat(sessionCaptor.getValue().getUserRole()).isEqualTo("STUDENT");
    }

    @Test
    void savesMessageMetadataAsJson() {
        service.saveMessage(3L, "ASSISTANT", "请确认是否执行该操作。", Map.of(
                "responseType", "ACTION_PREVIEW",
                "intent", "PUBLISH_ASSIGNMENT"
        ));

        var messageCaptor = forClass(AgentMessageEntity.class);
        verify(messageRepository).save(messageCaptor.capture());
        AgentMessageEntity message = messageCaptor.getValue();
        assertThat(message.getSessionId()).isEqualTo(3L);
        assertThat(message.getRole()).isEqualTo("ASSISTANT");
        assertThat(message.getContent()).isEqualTo("请确认是否执行该操作。");
        assertThat(message.getMetadataJson()).contains("\"responseType\":\"ACTION_PREVIEW\"");
        assertThat(message.getMetadataJson()).contains("\"intent\":\"PUBLISH_ASSIGNMENT\"");
    }

    @Test
    void masksSensitiveMessageContentAndMetadataBeforeSaving() {
        service.saveMessage(3L, "USER", "手机号13812345678，password=abc123", Map.of(
                "rawModelOutput", "邮箱student@example.com token=secret-token"
        ));

        var messageCaptor = forClass(AgentMessageEntity.class);
        verify(messageRepository).save(messageCaptor.capture());
        AgentMessageEntity message = messageCaptor.getValue();
        assertThat(message.getContent()).isEqualTo("手机号138****5678，password=***");
        assertThat(message.getMetadataJson()).contains("s***@example.com");
        assertThat(message.getMetadataJson()).contains("token=***");
        assertThat(message.getMetadataJson()).doesNotContain("student@example.com", "secret-token");
    }

    @Test
    void savesAssistantMessageMetadataFromChatResponse() {
        AgentChatResponseDTO response = new AgentChatResponseDTO();
        response.setResponseType("DATA");
        response.setMessage("查询完成。");
        RecognizedIntent recognizedIntent = new RecognizedIntent(AgentIntent.QUERY_COURSES, 0.9, Map.of());

        service.saveAssistantMessage(3L, response, recognizedIntent);

        var messageCaptor = forClass(AgentMessageEntity.class);
        verify(messageRepository).save(messageCaptor.capture());
        AgentMessageEntity message = messageCaptor.getValue();
        assertThat(message.getRole()).isEqualTo("ASSISTANT");
        assertThat(message.getMetadataJson()).contains("\"responseType\":\"DATA\"");
        assertThat(message.getMetadataJson()).contains("\"intent\":\"QUERY_COURSES\"");
    }

    @Test
    void listSessionsReturnsOwnedRoleSessionsWithoutMessagesOrActions() {
        AgentSessionEntity session = session(3L, 7L, "TEACHER");
        session.setPendingIntent("PUBLISH_ASSIGNMENT");
        session.setPendingSlotsJson("{\"title\":\"Spring Cloud实验\"}");
        when(sessionRepository.findByUserIdAndUserRoleIgnoreCaseOrderByUpdatedAtDesc(7L, "TEACHER"))
                .thenReturn(List.of(session));

        var sessions = service.listSessions(7L, "TEACHER");

        assertThat(sessions).hasSize(1);
        assertThat(sessions.get(0).getSessionId()).isEqualTo("3");
        assertThat(sessions.get(0).getUserRole()).isEqualTo("TEACHER");
        assertThat(sessions.get(0).getPendingIntent()).isEqualTo("PUBLISH_ASSIGNMENT");
        assertThat(sessions.get(0).getPendingSlots()).containsEntry("title", "Spring Cloud实验");
        assertThat(sessions.get(0).getMessages()).isEmpty();
        assertThat(sessions.get(0).getActions()).isEmpty();
    }

    @Test
    void getSessionReturnsOwnedSessionWithMessagesAndActions() {
        AgentSessionEntity session = session(3L, 7L, "TEACHER");
        AgentMessageEntity message = message(21L, 3L, "USER", "发布作业");
        AgentActionEntity action = action(11L, 3L);
        when(sessionRepository.findByIdAndUserIdAndUserRoleIgnoreCase(3L, 7L, "TEACHER"))
                .thenReturn(Optional.of(session));
        when(messageRepository.findBySessionIdOrderByCreatedAtAscIdAsc(3L)).thenReturn(List.of(message));
        when(actionRepository.findBySessionIdOrderByCreatedAtAsc(3L)).thenReturn(List.of(action));

        var dto = service.getSession(7L, "TEACHER", 3L);

        assertThat(dto.getSessionId()).isEqualTo("3");
        assertThat(dto.getMessages()).hasSize(1);
        assertThat(dto.getMessages().get(0).getMessageId()).isEqualTo(21L);
        assertThat(dto.getMessages().get(0).getContent()).isEqualTo("发布作业");
        assertThat(dto.getActions()).hasSize(1);
        assertThat(dto.getActions().get(0).getActionId()).isEqualTo(11L);
        assertThat(dto.getActions().get(0).getPreview()).containsEntry("title", "Spring Cloud实验");
        assertThat(dto.getActions().get(0).getRequest()).containsEntry("intent", "PUBLISH_ASSIGNMENT");
        assertThat(dto.getActions().get(0).getResult()).containsEntry("status", "EXECUTED");
    }

    @Test
    void getActionRejectsActionOwnedByAnotherUserSession() {
        AgentActionEntity action = action(11L, 3L);
        AgentSessionEntity ownerSession = session(3L, 7L, "TEACHER");
        when(actionRepository.findById(11L)).thenReturn(Optional.of(action));
        when(sessionRepository.findById(3L)).thenReturn(Optional.of(ownerSession));

        assertThatThrownBy(() -> service.getAction(8L, "TEACHER", 11L))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("无权查看该 Agent 操作");
    }

    @Test
    void getActionRejectsMalformedActionPayloadJson() {
        AgentActionEntity action = action(11L, 3L);
        action.setPreviewJson("{bad-json");
        AgentSessionEntity ownerSession = session(3L, 7L, "TEACHER");
        when(actionRepository.findById(11L)).thenReturn(Optional.of(action));
        when(sessionRepository.findById(3L)).thenReturn(Optional.of(ownerSession));

        assertThatThrownBy(() -> service.getAction(7L, "TEACHER", 11L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to parse agent session payload");
    }

    @Test
    void getSessionRejectsSessionOwnedByAnotherUser() {
        when(sessionRepository.findByIdAndUserIdAndUserRoleIgnoreCase(3L, 8L, "TEACHER"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSession(8L, "TEACHER", 3L))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("无权查看该 Agent 会话");
    }

    private AgentSessionEntity session(Long id, Long userId, String role) {
        AgentSessionEntity session = new AgentSessionEntity();
        session.setId(id);
        session.setUserId(userId);
        session.setUserRole(role);
        session.setStatus("ACTIVE");
        return session;
    }

    private AgentMessageEntity message(Long id, Long sessionId, String role, String content) {
        AgentMessageEntity message = new AgentMessageEntity();
        message.setId(id);
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content);
        message.setMetadataJson("{\"intent\":\"PUBLISH_ASSIGNMENT\"}");
        return message;
    }

    private AgentActionEntity action(Long id, Long sessionId) {
        AgentActionEntity action = new AgentActionEntity();
        action.setId(id);
        action.setSessionId(sessionId);
        action.setIntent("PUBLISH_ASSIGNMENT");
        action.setStatus("EXECUTED");
        action.setRiskLevel("MEDIUM");
        action.setPreviewJson("{\"title\":\"Spring Cloud实验\"}");
        action.setRequestJson("{\"intent\":\"PUBLISH_ASSIGNMENT\",\"slots\":{\"courseId\":12}}");
        action.setResultJson("{\"status\":\"EXECUTED\",\"assignmentId\":99}");
        action.setIdempotencyKey("idem-1");
        return action;
    }
}
