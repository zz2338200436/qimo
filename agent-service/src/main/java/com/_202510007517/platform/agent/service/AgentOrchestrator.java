package com._202510007517.platform.agent.service;

import com._202510007517.platform.agent.api.dto.AgentActionDTO;
import com._202510007517.platform.agent.api.dto.AgentChatResponseDTO;
import com._202510007517.platform.agent.api.dto.AgentExecutionResultDTO;
import com._202510007517.platform.agent.api.dto.AgentSessionDTO;
import com._202510007517.platform.agent.domain.AgentActionEntity;
import com._202510007517.platform.agent.domain.AgentSessionEntity;
import com._202510007517.platform.agent.model.AgentIntent;
import com._202510007517.platform.agent.model.RecognizedIntent;
import com._202510007517.platform.agent.rag.RagKnowledgeService;
import com._202510007517.platform.agent.repository.AgentActionRepository;
import com._202510007517.platform.agent.tool.ToolRegistry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AgentOrchestrator {
    private static final String STATUS_PENDING_CONFIRMATION = "PENDING_CONFIRMATION";
    private static final String STATUS_PENDING_SECOND_CONFIRMATION = "PENDING_SECOND_CONFIRMATION";
    private static final String STATUS_EXECUTED = "EXECUTED";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_EXPIRED = "EXPIRED";
    private static final String STATUS_CANCELLED = "CANCELLED";

    private final IntentRecognitionService intentRecognitionService;
    private final AgentActionMapper actionMapper;
    private final ActionPlanningService actionPlanningService;
    private final ActionConfirmationService actionConfirmationService;
    private final ActionExecutionService actionExecutionService;
    private final AgentSessionService sessionService;
    private final AgentActionRepository actionRepository;
    private final ToolRegistry toolRegistry;
    private final AgentPermissionPolicy permissionPolicy;
    private final AgentConfirmationPolicy confirmationPolicy;
    private final AgentContextEnrichmentService contextEnrichmentService;
    private final AgentSlotRequirementService slotRequirementService;
    private final GeneralChatService generalChatService;
    private final RagKnowledgeService ragKnowledgeService;
    private final ObjectMapper objectMapper;

    public AgentOrchestrator(IntentRecognitionService intentRecognitionService,
                             AgentActionMapper actionMapper,
                             ActionPlanningService actionPlanningService,
                             ActionConfirmationService actionConfirmationService,
                             ActionExecutionService actionExecutionService,
                             AgentSessionService sessionService,
                             AgentActionRepository actionRepository,
                             ToolRegistry toolRegistry,
                             AgentPermissionPolicy permissionPolicy,
                             AgentConfirmationPolicy confirmationPolicy,
                             AgentContextEnrichmentService contextEnrichmentService,
                             AgentSlotRequirementService slotRequirementService,
                             GeneralChatService generalChatService,
                             RagKnowledgeService ragKnowledgeService,
                             ObjectMapper objectMapper) {
        this.intentRecognitionService = intentRecognitionService;
        this.actionMapper = actionMapper;
        this.actionPlanningService = actionPlanningService;
        this.actionConfirmationService = actionConfirmationService;
        this.actionExecutionService = actionExecutionService;
        this.sessionService = sessionService;
        this.actionRepository = actionRepository;
        this.toolRegistry = toolRegistry;
        this.permissionPolicy = permissionPolicy;
        this.confirmationPolicy = confirmationPolicy;
        this.contextEnrichmentService = contextEnrichmentService;
        this.slotRequirementService = slotRequirementService;
        this.generalChatService = generalChatService;
        this.ragKnowledgeService = ragKnowledgeService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AgentChatResponseDTO chat(Long userId, String userRole, String sessionId, String message) {
        AgentSessionEntity session = sessionService.resolveSession(userId, userRole, sessionId);
        sessionService.saveMessage(session.getId(), "USER", message, null);
        RecognizedIntent recognizedIntent = mergePendingContext(session, intentRecognitionService.recognize(message));
        AgentChatResponseDTO response;
        if (recognizedIntent.intent() == AgentIntent.UNKNOWN) {
            clearPendingContext(session);
            response = textResponse(session, generalChatService.reply(userId, userRole, message));
            sessionService.saveAssistantMessage(session.getId(), response, recognizedIntent);
            return response;
        }
        permissionPolicy.assertAllowed(userId, userRole, recognizedIntent.intent());
        if (recognizedIntent.intent() == AgentIntent.QUERY_RAG_KNOWLEDGE) {
            clearPendingContext(session);
            response = textResponse(session, ragKnowledgeService.answer(userId, userRole, message));
            sessionService.saveAssistantMessage(session.getId(), response, recognizedIntent);
            return response;
        }
        recognizedIntent = contextEnrichmentService.enrich(userId, userRole, recognizedIntent);
        var missingSlots = slotRequirementService.missingSlots(recognizedIntent, message);
        if (!missingSlots.isEmpty()) {
            savePendingContext(session, recognizedIntent);
            response = textResponse(session, slotRequirementService.buildPrompt(recognizedIntent.intent(), missingSlots));
            sessionService.saveAssistantMessage(session.getId(), response, recognizedIntent);
            return response;
        }
        clearPendingContext(session);
        if (!confirmationPolicy.requiresConfirmation(recognizedIntent.intent())) {
            response = dataResponse(session, recognizedIntent.intent(), recognizedIntent.slots(), userId, userRole);
            sessionService.saveAssistantMessage(session.getId(), response, recognizedIntent);
            return response;
        }
        response = actionPlanningService.createPreviewResponse(session, recognizedIntent);
        sessionService.saveAssistantMessage(session.getId(), response, recognizedIntent);
        return response;
    }

    @Transactional(noRollbackFor = IllegalStateException.class)
    public AgentExecutionResultDTO confirm(Long userId, String userRole, Long actionId, String idempotencyKey) {
        return confirm(userId, userRole, actionId, idempotencyKey, null);
    }

    @Transactional(noRollbackFor = IllegalStateException.class)
    public AgentExecutionResultDTO confirm(Long userId, String userRole, Long actionId, String idempotencyKey,
                                           String secondConfirmationText) {
        AgentActionEntity action = actionConfirmationService.requireActionForConfirmation(
                userId, userRole, actionId, idempotencyKey);
        if (STATUS_EXECUTED.equals(action.getStatus())) {
            return toExecutionResultDto(action, "操作已执行。", readJsonMap(action.getResultJson()));
        }

        AgentIntent intent = AgentIntent.valueOf(action.getIntent());
        permissionPolicy.assertAllowed(userId, userRole, intent);
        if (confirmationPolicy.requiresSecondConfirmation(intent)) {
            if (STATUS_PENDING_CONFIRMATION.equals(action.getStatus())) {
                return enterSecondConfirmationStage(action);
            }
            if (!hasSecondConfirmationText(secondConfirmationText)) {
                return secondConfirmationResult(action, readRequestSlots(action.getRequestJson()));
            }
            validateSecondConfirmation(intent, secondConfirmationText);
        } else if (!STATUS_PENDING_CONFIRMATION.equals(action.getStatus())) {
            throw new IllegalStateException("Agent action is not pending confirmation.");
        }

        Map<String, Object> request = readRequestSlots(action.getRequestJson());
        return actionExecutionService.execute(userId, userRole, action, intent, request);
    }

    private AgentExecutionResultDTO enterSecondConfirmationStage(AgentActionEntity action) {
        action.setStatus(STATUS_PENDING_SECOND_CONFIRMATION);
        action.setConfirmedAt(LocalDateTime.now());
        Map<String, Object> request = readRequestSlots(action.getRequestJson());
        Map<String, Object> result = secondConfirmationPayload(request);
        action.setResultJson(writeJson(result));
        actionRepository.save(action);
        return secondConfirmationResult(action, request);
    }

    private AgentExecutionResultDTO secondConfirmationResult(AgentActionEntity action, Map<String, Object> request) {
        return toExecutionResultDto(action, secondConfirmationMessage(), secondConfirmationPayload(request));
    }

    private Map<String, Object> secondConfirmationPayload(Map<String, Object> request) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", STATUS_PENDING_SECOND_CONFIRMATION);
        result.put("message", secondConfirmationMessage());
        result.put("secondConfirmationPhrase", AgentActionMapper.SECOND_CONFIRMATION_PHRASE);
        result.put("secondConfirmationPrompt", secondConfirmationMessage());
        addOperationCounts(result, request);
        return result;
    }

    private void addOperationCounts(Map<String, Object> result, Map<String, Object> request) {
        Object studentIds = request.get("studentIds");
        if (studentIds instanceof List<?> ids) {
            int recipientCount = (int) ids.stream().distinct().count();
            result.put("recipientCount", recipientCount);
            result.put("operationCount", recipientCount);
        }
    }

    private String secondConfirmationMessage() {
        return "该操作需要二级确认，请输入“" + AgentActionMapper.SECOND_CONFIRMATION_PHRASE + "”。";
    }

    private boolean hasSecondConfirmationText(String secondConfirmationText) {
        return secondConfirmationText != null && !secondConfirmationText.trim().isEmpty();
    }

    private void validateSecondConfirmation(AgentIntent intent, String secondConfirmationText) {
        if (!confirmationPolicy.requiresSecondConfirmation(intent)) {
            return;
        }
        if (!AgentActionMapper.SECOND_CONFIRMATION_PHRASE.equals(secondConfirmationText == null ? null : secondConfirmationText.trim())) {
            throw new IllegalArgumentException(secondConfirmationMessage());
        }
    }

    @Transactional
    public AgentExecutionResultDTO cancel(Long userId, String userRole, Long actionId) {
        AgentActionEntity action = actionConfirmationService.requireActionForCancellation(userId, userRole, actionId);

        Map<String, Object> result = cancelledResult();
        if (!STATUS_CANCELLED.equals(action.getStatus())) {
            action.setStatus(STATUS_CANCELLED);
            action.setResultJson(writeJson(result));
            actionRepository.save(action);
        }

        AgentExecutionResultDTO dto = new AgentExecutionResultDTO();
        dto.setActionId(action.getId());
        dto.setIntent(action.getIntent());
        dto.setStatus(action.getStatus());
        dto.setMessage("操作已取消。");
        dto.setResult(result);
        return dto;
    }

    private Map<String, Object> cancelledResult() {
        return Map.of("status", STATUS_CANCELLED, "message", "操作已取消。");
    }

    private AgentExecutionResultDTO toExecutionResultDto(AgentActionEntity action, String message,
                                                         Map<String, Object> result) {
        AgentExecutionResultDTO dto = new AgentExecutionResultDTO();
        dto.setActionId(action.getId());
        dto.setIntent(action.getIntent());
        dto.setStatus(action.getStatus());
        dto.setMessage(message);
        dto.setResult(result);
        return dto;
    }

    @Transactional(readOnly = true)
    public AgentActionDTO getAction(Long userId, String userRole, Long actionId) {
        return sessionService.getAction(userId, userRole, actionId);
    }

    @Transactional(readOnly = true)
    public List<AgentSessionDTO> listSessions(Long userId, String userRole) {
        return sessionService.listSessions(userId, userRole);
    }

    @Transactional(readOnly = true)
    public AgentSessionDTO getSession(Long userId, String userRole, Long sessionId) {
        return sessionService.getSession(userId, userRole, sessionId);
    }

    private RecognizedIntent mergePendingContext(AgentSessionEntity session, RecognizedIntent current) {
        if (session.getPendingIntent() == null || session.getPendingIntent().isBlank()) {
            return current;
        }
        AgentIntent pendingIntent = AgentIntent.valueOf(session.getPendingIntent());
        if (current.intent() != AgentIntent.UNKNOWN && current.intent() != pendingIntent) {
            return current;
        }
        Map<String, Object> slots = new LinkedHashMap<>(readPendingSlots(session.getPendingSlotsJson()));
        slots.putAll(current.slots());
        return new RecognizedIntent(pendingIntent, Math.max(current.confidence(), 0.8), slots, current.missingSlots());
    }

    private void savePendingContext(AgentSessionEntity session, RecognizedIntent recognizedIntent) {
        session.setPendingIntent(recognizedIntent.intent().name());
        session.setPendingSlotsJson(writeJson(recognizedIntent.slots()));
        sessionService.save(session);
    }

    private void clearPendingContext(AgentSessionEntity session) {
        if (session.getPendingIntent() == null && session.getPendingSlotsJson() == null) {
            return;
        }
        session.setPendingIntent(null);
        session.setPendingSlotsJson(null);
        sessionService.save(session);
    }

    private AgentChatResponseDTO textResponse(AgentSessionEntity session, String message) {
        AgentChatResponseDTO response = new AgentChatResponseDTO();
        response.setSessionId(String.valueOf(session.getId()));
        response.setResponseType("TEXT");
        response.setMessage(message);
        return response;
    }

    private AgentChatResponseDTO dataResponse(AgentSessionEntity session, AgentIntent intent, Map<String, Object> slots,
                                              Long userId, String userRole) {
        Map<String, Object> result = toolRegistry.resolve(intent).execute(userId, userRole, slots);
        AgentChatResponseDTO response = new AgentChatResponseDTO();
        response.setSessionId(String.valueOf(session.getId()));
        response.setResponseType("DATA");
        response.setMessage("查询完成。");
        response.setData(result);
        return response;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize agent action payload.", ex);
        }
    }

    private Map<String, Object> readPendingSlots(String pendingSlotsJson) {
        if (pendingSlotsJson == null || pendingSlotsJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(pendingSlotsJson, new TypeReference<>() {
            });
        } catch (JsonProcessingException ex) {
            return Map.of();
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
            throw new IllegalStateException("Failed to parse agent action payload.", ex);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readRequestSlots(String requestJson) {
        try {
            Map<String, Object> payload = objectMapper.readValue(requestJson, new TypeReference<>() {
            });
            Object slots = payload.get("slots");
            if (slots instanceof Map<?, ?> slotMap) {
                return (Map<String, Object>) slotMap;
            }
            return Map.of();
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to parse agent action payload.", ex);
        }
    }

}
