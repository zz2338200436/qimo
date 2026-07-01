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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AgentSessionService {
    private static final int MAX_CUSTOM_TITLE_LENGTH = 80;

    private final AgentSessionRepository sessionRepository;
    private final AgentActionRepository actionRepository;
    private final AgentMessageRepository messageRepository;
    private final AgentDataMaskingPolicy dataMaskingPolicy;
    private final AgentArtifactService artifactService;
    private final ObjectMapper objectMapper;

    public AgentSessionService(AgentSessionRepository sessionRepository,
                               AgentActionRepository actionRepository,
                               AgentMessageRepository messageRepository,
                               AgentDataMaskingPolicy dataMaskingPolicy,
                               AgentArtifactService artifactService,
                               ObjectMapper objectMapper) {
        this.sessionRepository = sessionRepository;
        this.actionRepository = actionRepository;
        this.messageRepository = messageRepository;
        this.dataMaskingPolicy = dataMaskingPolicy;
        this.artifactService = artifactService;
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

    @Transactional(readOnly = true)
    public AgentSessionEntity resolveSessionOwner(Long sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalStateException("Agent session not found: " + sessionId));
    }

    public void saveAssistantMessage(Long sessionId, AgentChatResponseDTO response,
                                     RecognizedIntent recognizedIntent) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("responseType", response.getResponseType());
        metadata.put("intent", recognizedIntent.intent().name());
        if (response.getData() != null) {
            metadata.put("data", response.getData());
        }
        if (response.getActionPreview() != null) {
            metadata.put("actionPreview", response.getActionPreview());
        }
        if (response.getArtifactSummary() != null) {
            metadata.put("artifactSummary", response.getArtifactSummary());
        }
        saveMessage(sessionId, "ASSISTANT", response.getMessage(), metadata);
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
    public List<AgentMessageDTO> loadRecentMessages(Long sessionId, int limit) {
        if (limit <= 0) {
            return List.of();
        }
        List<AgentMessageEntity> recentMessages = new ArrayList<>(messageRepository.findRecentBySessionId(sessionId, limit));
        Collections.reverse(recentMessages);
        return recentMessages.stream()
                .map(this::toMessageDto)
                .toList();
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
                .map(session -> {
                    List<AgentMessageDTO> recentMessages = loadRecentMessages(session.getId(), 3);
                    return toSessionDto(session, recentMessages, List.of());
                })
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

    @Transactional
    public AgentSessionDTO updateSessionTitle(Long userId, String userRole, Long sessionId, String title) {
        String normalizedTitle = normalizeCustomTitle(title);
        AgentSessionEntity session = sessionRepository.findByIdAndUserIdAndUserRoleIgnoreCase(sessionId, userId, userRole)
                .orElseThrow(() -> new SecurityException("无权修改该 Agent 会话。"));
        Map<String, SessionArtifact> artifacts = new LinkedHashMap<>(artifactService.loadArtifacts(session));
        String now = java.time.LocalDateTime.now().toString();
        artifacts.put(AgentArtifactService.CUSTOM_SESSION_TITLE_KEY, new SessionArtifact(
                "session_title",
                AgentArtifactService.CUSTOM_SESSION_TITLE_KEY,
                Map.of("title", normalizedTitle),
                now,
                now
        ));
        artifactService.saveArtifacts(session, artifacts);
        AgentSessionEntity saved = sessionRepository.save(session);
        return toSessionDto(saved, List.of(), List.of());
    }

    @Transactional
    public void deleteSession(Long userId, String userRole, Long sessionId) {
        AgentSessionEntity session = sessionRepository.findByIdAndUserIdAndUserRoleIgnoreCase(sessionId, userId, userRole)
                .orElseThrow(() -> new SecurityException("无权删除该 Agent 会话。"));
        messageRepository.deleteBySessionId(session.getId());
        actionRepository.deleteBySessionId(session.getId());
        sessionRepository.delete(session);
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
        Map<String, Object> artifacts = new LinkedHashMap<>(artifactService.summarizeArtifacts(artifactService.loadArtifacts(session)));
        dto.setSessionId(String.valueOf(session.getId()));
        dto.setUserRole(session.getUserRole());
        dto.setStatus(session.getStatus());
        dto.setArtifacts(artifacts);
        dto.setTitle(resolveSessionTitle(session, messages, artifacts));
        dto.setSummary(resolveSessionSummary(session, messages, artifacts));
        dto.setPendingIntent(session.getPendingIntent());
        dto.setPendingSlots(readJsonMap(session.getPendingSlotsJson()));
        dto.setCreatedAt(session.getCreatedAt());
        dto.setUpdatedAt(session.getUpdatedAt());
        dto.setMessages(messages);
        dto.setActions(actions);
        return dto;
    }

    private String resolveSessionTitle(AgentSessionEntity session,
                                       List<AgentMessageDTO> messages,
                                       Map<String, Object> artifacts) {
        Object customTitle = artifacts.get("customTitle");
        if (customTitle instanceof String custom && !custom.isBlank()) {
            return custom.trim();
        }
        Object latestGeneratedTitle = artifacts.get("latestGeneratedTitle");
        if (latestGeneratedTitle instanceof String latest && !latest.isBlank()) {
            return latest;
        }
        return messages.stream()
                .filter(message -> "USER".equalsIgnoreCase(message.getRole()))
                .map(AgentMessageDTO::getContent)
                .filter(content -> content != null && !content.isBlank())
                .map(this::normalizeSnippet)
                .filter(snippet -> !snippet.isBlank())
                .max(Comparator.comparingInt(String::length))
                .orElseGet(() -> fallbackTitle(session.getPendingIntent()));
    }

    private String resolveSessionSummary(AgentSessionEntity session,
                                         List<AgentMessageDTO> messages,
                                         Map<String, Object> artifacts) {
        Object latestGeneratedTitle = artifacts.get("latestGeneratedTitle");
        if (latestGeneratedTitle instanceof String latest && !latest.isBlank()) {
            return "最近内容：" + latest.trim();
        }
        Object questionCount = artifacts.get("latestGeneratedQuestionCount");
        if (questionCount != null) {
            return "最近生成 " + questionCount + " 道题目，可继续调整难度、题型或解析。";
        }
        return messages.stream()
                .filter(message -> "ASSISTANT".equalsIgnoreCase(message.getRole()))
                .map(AgentMessageDTO::getContent)
                .filter(content -> content != null && !content.isBlank())
                .map(this::normalizeSnippet)
                .filter(snippet -> !snippet.isBlank())
                .findFirst()
                .orElseGet(() -> messages.stream()
                        .filter(message -> "USER".equalsIgnoreCase(message.getRole()))
                        .map(AgentMessageDTO::getContent)
                        .filter(content -> content != null && !content.isBlank())
                        .map(this::normalizeSnippet)
                        .filter(snippet -> !snippet.isBlank())
                        .findFirst()
                        .orElseGet(() -> fallbackSummary(session.getPendingIntent())));
    }

    private String normalizeSnippet(String value) {
        String collapsed = value.replaceAll("\\s+", " ").trim();
        if (collapsed.length() <= 38) {
            return collapsed;
        }
        return collapsed.substring(0, 38) + "...";
    }

    private String normalizeCustomTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("会话标题不能为空。");
        }
        String normalized = title.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= MAX_CUSTOM_TITLE_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, MAX_CUSTOM_TITLE_LENGTH);
    }

    private String fallbackTitle(String pendingIntent) {
        return switch (pendingIntent == null ? "" : pendingIntent.trim().toUpperCase()) {
            case "GENERATE_QUESTIONS" -> "课堂练习题会话";
            case "GENERATE_EXAM" -> "试卷生成会话";
            case "LEARNING_SUGGESTIONS" -> "学习建议会话";
            case "QUERY_LEARNING_SUMMARY", "QUERY_EARLY_WARNINGS", "QUERY_KNOWLEDGE_MASTERY" -> "学情分析会话";
            default -> "教学对话";
        };
    }

    private String fallbackSummary(String pendingIntent) {
        return switch (pendingIntent == null ? "" : pendingIntent.trim().toUpperCase()) {
            case "GENERATE_QUESTIONS" -> "继续补充题型、难度或知识点要求。";
            case "GENERATE_EXAM" -> "继续细化课程、分值、时长或题量结构。";
            case "LEARNING_SUGGESTIONS" -> "继续追问个性化辅导建议与后续安排。";
            case "QUERY_LEARNING_SUMMARY", "QUERY_EARLY_WARNINGS", "QUERY_KNOWLEDGE_MASTERY" -> "继续查看班级、课程或知识点的学习情况。";
            default -> "继续追问、改写或整理刚才的教学材料。";
        };
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
