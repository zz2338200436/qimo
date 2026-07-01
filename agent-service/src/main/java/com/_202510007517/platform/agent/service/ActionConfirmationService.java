package com._202510007517.platform.agent.service;

import com._202510007517.platform.agent.domain.AgentActionEntity;
import com._202510007517.platform.agent.domain.AgentSessionEntity;
import com._202510007517.platform.agent.repository.AgentActionRepository;
import com._202510007517.platform.agent.repository.AgentSessionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ActionConfirmationService {
    static final String STATUS_PENDING_CONFIRMATION = "PENDING_CONFIRMATION";
    static final String STATUS_PENDING_SECOND_CONFIRMATION = "PENDING_SECOND_CONFIRMATION";
    static final String STATUS_EXECUTED = "EXECUTED";
    static final String STATUS_EXPIRED = "EXPIRED";
    static final String STATUS_CANCELLED = "CANCELLED";

    private final AgentActionRepository actionRepository;
    private final AgentSessionRepository sessionRepository;
    private final AgentIdempotencyService idempotencyService;

    public ActionConfirmationService(AgentActionRepository actionRepository,
                                     AgentSessionRepository sessionRepository,
                                     AgentIdempotencyService idempotencyService) {
        this.actionRepository = actionRepository;
        this.sessionRepository = sessionRepository;
        this.idempotencyService = idempotencyService;
    }

    public AgentActionEntity requireActionForConfirmation(Long userId, String userRole,
                                                          Long actionId, String idempotencyKey) {
        AgentActionEntity action = findAction(actionId);
        AgentSessionEntity ownerSession = findSession(action);
        assertActionOwner(ownerSession, userId, userRole, "无权确认该 Agent 操作。");
        idempotencyService.assertMatches(action, idempotencyKey);
        if (STATUS_EXECUTED.equals(action.getStatus())) {
            return action;
        }
        assertPendingConfirmationStatus(action);
        expireIfNeeded(action);
        return action;
    }

    public AgentActionEntity requireActionForCancellation(Long userId, String userRole, Long actionId) {
        AgentActionEntity action = findAction(actionId);
        if (!STATUS_PENDING_CONFIRMATION.equals(action.getStatus())
                && !STATUS_PENDING_SECOND_CONFIRMATION.equals(action.getStatus())
                && !STATUS_CANCELLED.equals(action.getStatus())) {
            throw new IllegalStateException("Agent action is not pending confirmation.");
        }
        if (!STATUS_CANCELLED.equals(action.getStatus())) {
            expireIfNeeded(action);
        }
        AgentSessionEntity ownerSession = findSession(action);
        assertActionOwner(ownerSession, userId, userRole, "无权取消该 Agent 操作。");
        return action;
    }

    private AgentActionEntity findAction(Long actionId) {
        return actionRepository.findById(actionId)
                .orElseThrow(() -> new IllegalArgumentException("Agent action not found: " + actionId));
    }

    private AgentSessionEntity findSession(AgentActionEntity action) {
        return sessionRepository.findById(action.getSessionId())
                .orElseThrow(() -> new IllegalStateException("Agent action session not found: " + action.getSessionId()));
    }

    private void assertActionOwner(AgentSessionEntity ownerSession, Long userId, String userRole, String message) {
        if (!ownerSession.getUserId().equals(userId) || !ownerSession.getUserRole().equalsIgnoreCase(userRole)) {
            throw new SecurityException(message);
        }
    }

    private void assertPendingConfirmationStatus(AgentActionEntity action) {
        if (!STATUS_PENDING_CONFIRMATION.equals(action.getStatus())
                && !STATUS_PENDING_SECOND_CONFIRMATION.equals(action.getStatus())) {
            throw new IllegalStateException("Agent action is not pending confirmation.");
        }
    }

    private void expireIfNeeded(AgentActionEntity action) {
        if (action.getExpiresAt() == null || !action.getExpiresAt().isBefore(LocalDateTime.now())) {
            return;
        }
        action.setStatus(STATUS_EXPIRED);
        action.setErrorMessage("Agent action has expired.");
        actionRepository.save(action);
        throw new IllegalStateException("Agent action has expired.");
    }
}
