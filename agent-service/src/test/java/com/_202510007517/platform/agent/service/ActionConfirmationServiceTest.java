package com._202510007517.platform.agent.service;

import com._202510007517.platform.agent.domain.AgentActionEntity;
import com._202510007517.platform.agent.domain.AgentSessionEntity;
import com._202510007517.platform.agent.repository.AgentActionRepository;
import com._202510007517.platform.agent.repository.AgentSessionRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ActionConfirmationServiceTest {

    private final AgentActionRepository actionRepository = mock(AgentActionRepository.class);
    private final AgentSessionRepository sessionRepository = mock(AgentSessionRepository.class);
    private final ActionConfirmationService service = new ActionConfirmationService(
            actionRepository,
            sessionRepository,
            new AgentIdempotencyService()
    );

    @Test
    void loadsOwnedPendingActionForConfirmation() {
        AgentActionEntity action = pendingAction("idem-1");
        AgentSessionEntity session = ownerSession(7L, "TEACHER");
        when(actionRepository.findById(11L)).thenReturn(Optional.of(action));
        when(sessionRepository.findById(3L)).thenReturn(Optional.of(session));

        AgentActionEntity result = service.requireActionForConfirmation(7L, "TEACHER", 11L, "idem-1");

        assertThat(result).isSameAs(action);
    }

    @Test
    void rejectsMismatchedIdempotencyKeyBeforeConfirmation() {
        AgentActionEntity action = pendingAction("idem-1");
        AgentSessionEntity session = ownerSession(7L, "TEACHER");
        when(actionRepository.findById(11L)).thenReturn(Optional.of(action));
        when(sessionRepository.findById(3L)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> service.requireActionForConfirmation(7L, "TEACHER", 11L, "wrong"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("idempotency key mismatch");
    }

    @Test
    void rejectsWrongOwnerBeforeCheckingIdempotencyKey() {
        AgentActionEntity action = pendingAction("idem-1");
        AgentSessionEntity session = ownerSession(7L, "TEACHER");
        when(actionRepository.findById(11L)).thenReturn(Optional.of(action));
        when(sessionRepository.findById(3L)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> service.requireActionForConfirmation(8L, "TEACHER", 11L, "wrong"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("无权确认");
    }

    @Test
    void returnsExecutedActionWithoutExpiringReplay() {
        AgentActionEntity action = pendingAction("idem-1");
        action.setStatus("EXECUTED");
        action.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        AgentSessionEntity session = ownerSession(7L, "TEACHER");
        when(actionRepository.findById(11L)).thenReturn(Optional.of(action));
        when(sessionRepository.findById(3L)).thenReturn(Optional.of(session));

        AgentActionEntity result = service.requireActionForConfirmation(7L, "TEACHER", 11L, "idem-1");

        assertThat(result).isSameAs(action);
        assertThat(action.getStatus()).isEqualTo("EXECUTED");
    }

    @Test
    void expiresPendingActionBeforeConfirmation() {
        AgentActionEntity action = pendingAction("idem-1");
        action.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        AgentSessionEntity session = ownerSession(7L, "TEACHER");
        when(actionRepository.findById(11L)).thenReturn(Optional.of(action));
        when(sessionRepository.findById(3L)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> service.requireActionForConfirmation(7L, "TEACHER", 11L, "idem-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("expired");
        assertThat(action.getStatus()).isEqualTo("EXPIRED");
        assertThat(action.getErrorMessage()).contains("expired");
        verify(actionRepository).save(action);
    }

    @Test
    void rejectsCancellingExecutedAction() {
        AgentActionEntity action = pendingAction("idem-1");
        action.setStatus("EXECUTED");
        when(actionRepository.findById(11L)).thenReturn(Optional.of(action));

        assertThatThrownBy(() -> service.requireActionForCancellation(7L, "TEACHER", 11L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not pending confirmation");
    }

    private AgentActionEntity pendingAction(String idempotencyKey) {
        AgentActionEntity action = new AgentActionEntity();
        action.setId(11L);
        action.setSessionId(3L);
        action.setIntent("PUBLISH_ASSIGNMENT");
        action.setStatus("PENDING_CONFIRMATION");
        action.setRiskLevel("MEDIUM");
        action.setIdempotencyKey(idempotencyKey);
        action.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        return action;
    }

    private AgentSessionEntity ownerSession(Long userId, String userRole) {
        AgentSessionEntity session = new AgentSessionEntity();
        session.setId(3L);
        session.setUserId(userId);
        session.setUserRole(userRole);
        session.setStatus("ACTIVE");
        return session;
    }
}
