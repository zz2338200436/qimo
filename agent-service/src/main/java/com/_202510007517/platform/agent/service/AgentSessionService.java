package com._202510007517.platform.agent.service;

import com._202510007517.platform.agent.api.dto.AgentActionDTO;
import com._202510007517.platform.agent.api.dto.AgentChatResponseDTO;
import com._202510007517.platform.agent.api.dto.AgentMessageDTO;
import com._202510007517.platform.agent.api.dto.AgentSessionDTO;
import com._202510007517.platform.agent.domain.AgentActionEntity;
import com._202510007517.platform.agent.domain.AgentMessageEntity;
import com._202510007517.platform.agent.domain.AgentSessionEntity;
import com._202510007517.platform.agent.model.RecognizedIntent;
import com._202510007517.platform.agent.repository.AgentActionRepository;
import com._202510007517.platform.agent.repository.AgentMessageRepository;
import com._202510007517.platform.agent.repository.AgentSessionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class AgentSessionService {
    private final AgentSessionRepository sessionRepository;
    private final AgentActionRepository actionRepository;
    private final AgentMessageRepository messageRepository;
    private final AgentDataMaskingPolicy dataMaskingPolicy;
    private final ObjectMapper objectMapper;

    public AgentSessionService(AgentSessionRepository sessionRepository,
                               AgentActionRepository actionRepository,
                               AgentMessageRepository messageRepository,
                               AgentDataMaskingPolicy dataMaskingPolicy,
                               ObjectMapper objectMapper) {
        this.sessionRepository = sessionRepository;
        this.actionRepository = actionRepository;
        this.messageRepository = messageRepository;
        this.dataMaskingPolicy = dataMaskingPolicy;
        this.objectMapper = objectMapper;
    }

    public AgentSessionEntity resolveSession(Long userId, String userRole, String sessionId) {
        if (sessionId != null && !sessionId.isBlank()) {
            try {
                return sessionRepository.findById(Long.parseLong(sessionId))
                        .filter(session -> session.getUserId().equals(userId)
                                && session.getUserRole().equalsIgnoreCase(userRole))
                        .orElseGet(() -> createSession(userId, userRole));
            } catch (NumberFormatException ignored) {
                return createSession(userId, userRole);
            }
        }
        return createSession(userId, userRole);
    }

    public AgentSessionEntity save(AgentSessionEntity session) {
        return sessionRepository.save(session);
    }

    public void saveAssistantMessage(Long sessionId, AgentChatResponseDTO response,
                                     RecognizedIntent recognizedIntent) {
        saveMessage(sessionId, "ASSISTANT", response.getMessage(), Map.of(
                "responseType", response.getResponseType(),
                "intent", recognizedIntent.intent().name()
        ));
    }

    public void saveMessage(Long sessionId, String role, String content, Map<String, Object> metadata) {
        AgentMessageEntity message = new AgentMessageEntity();
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(dataMaskingPolicy.maskText(content));
        Map<String, Object> maskedMetadata = dataMaskingPolicy.maskMetadata(metadata);
        message.setMetadataJson(maskedMetadata.isEmpty() ? null : writeJson(maskedMetadata));
        messageRepository.save(message);
    }

    @Transactional(readOnly = true)
    public AgentActionDTO getAction(Long userId, String userRole, Long actionId) {
        AgentActionEntity action = actionRepository.findById(actionId)
                .orElseThrow(() -> new IllegalArgumentException("Agent action not found: " + actionId));
        AgentSessionEntity ownerSession = sessionRepository.findById(action.getSessionId())
                .orElseThrow(() -> new IllegalStateException("Agent action session not found: " + action.getSessionId()));
        assertActionViewer(ownerSession, userId, userRole);
        return toActionDto(action);
    }

    @Transactional(readOnly = true)
    public List<AgentSessionDTO> listSessions(Long userId, String userRole) {
        return sessionRepository.findByUserIdAndUserRoleIgnoreCaseOrderByUpdatedAtDesc(userId, userRole).stream()
                .map(session -> toSessionDto(session, List.of()))
                .toList();
    }

    @Transactional(readOnly = true)
    public AgentSessionDTO getSession(Long userId, String userRole, Long sessionId) {
        AgentSessionEntity session = sessionRepository.findByIdAndUserIdAndUserRoleIgnoreCase(sessionId, userId, userRole)
                .orElseThrow(() -> new SecurityException("无权查看该 Agent 会话。"));
        List<AgentActionDTO> actions = actionRepository.findBySessionIdOrderByCreatedAtAsc(session.getId()).stream()
                .map(this::toActionDto)
                .toList();
        List<AgentMessageDTO> messages = messageRepository.findBySessionIdOrderByCreatedAtAscIdAsc(session.getId()).stream()
                .map(this::toMessageDto)
                .toList();
        return toSessionDto(session, messages, actions);
    }

    private AgentSessionEntity createSession(Long userId, String userRole) {
        AgentSessionEntity session = new AgentSessionEntity();
        session.setUserId(userId);
        session.setUserRole(userRole);
        session.setStatus("ACTIVE");
        return sessionRepository.save(session);
    }

    private void assertActionViewer(AgentSessionEntity ownerSession, Long userId, String userRole) {
        if (!ownerSession.getUserId().equals(userId) || !ownerSession.getUserRole().equalsIgnoreCase(userRole)) {
            throw new SecurityException("无权查看该 Agent 操作。");
        }
    }

    private AgentActionDTO toActionDto(AgentActionEntity action) {
        AgentActionDTO dto = new AgentActionDTO();
        dto.setActionId(action.getId());
        dto.setSessionId(String.valueOf(action.getSessionId()));
        dto.setIntent(action.getIntent());
        dto.setStatus(action.getStatus());
        dto.setRiskLevel(action.getRiskLevel());
        dto.setPreview(readJsonMap(action.getPreviewJson()));
        dto.setRequest(readJsonMap(action.getRequestJson()));
        dto.setResult(readJsonMap(action.getResultJson()));
        dto.setErrorMessage(action.getErrorMessage());
        dto.setExpiresAt(action.getExpiresAt());
        dto.setConfirmedAt(action.getConfirmedAt());
        dto.setExecutedAt(action.getExecutedAt());
        dto.setCreatedAt(action.getCreatedAt());
        dto.setUpdatedAt(action.getUpdatedAt());
        return dto;
    }

    private AgentMessageDTO toMessageDto(AgentMessageEntity message) {
        AgentMessageDTO dto = new AgentMessageDTO();
        dto.setMessageId(message.getId());
        dto.setSessionId(String.valueOf(message.getSessionId()));
        dto.setRole(message.getRole());
        dto.setContent(message.getContent());
        dto.setMetadata(readJsonMap(message.getMetadataJson()));
        dto.setCreatedAt(message.getCreatedAt());
        return dto;
    }

    private AgentSessionDTO toSessionDto(AgentSessionEntity session, List<AgentActionDTO> actions) {
        return toSessionDto(session, List.of(), actions);
    }

    private AgentSessionDTO toSessionDto(AgentSessionEntity session, List<AgentMessageDTO> messages,
                                         List<AgentActionDTO> actions) {
        AgentSessionDTO dto = new AgentSessionDTO();
        dto.setSessionId(String.valueOf(session.getId()));
        dto.setUserRole(session.getUserRole());
        dto.setStatus(session.getStatus());
        dto.setPendingIntent(session.getPendingIntent());
        dto.setPendingSlots(readJsonMap(session.getPendingSlotsJson()));
        dto.setCreatedAt(session.getCreatedAt());
        dto.setUpdatedAt(session.getUpdatedAt());
        dto.setMessages(messages);
        dto.setActions(actions);
        return dto;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize agent session payload.", ex);
        }
    }

    private Map<String, Object> readJsonMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to parse agent session payload.", ex);
        }
    }
}
