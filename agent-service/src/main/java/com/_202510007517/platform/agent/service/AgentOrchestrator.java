package com._202510007517.platform.agent.service;

import com._202510007517.platform.agent.assistant.AgentAssistantRequest;
import com._202510007517.platform.agent.assistant.AgentAssistantType;
import com._202510007517.platform.agent.assistant.AssistantConversationService;
import com._202510007517.platform.agent.api.dto.AgentActionDTO;
import com._202510007517.platform.agent.api.dto.AgentChatResponseDTO;
import com._202510007517.platform.agent.api.dto.AgentExecutionResultDTO;
import com._202510007517.platform.agent.api.dto.AgentMessageDTO;
import com._202510007517.platform.agent.api.dto.AgentSessionDTO;
import com._202510007517.platform.agent.domain.AgentActionEntity;
import com._202510007517.platform.agent.domain.AgentSessionEntity;
import com._202510007517.platform.agent.model.AgentIntent;
import com._202510007517.platform.agent.model.PlannerDecision;
import com._202510007517.platform.agent.model.PlannerMode;
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
import java.util.LinkedHashMap;
import java.util.List;
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
    private final AgentAssignmentDraftService assignmentDraftService;
    private final AgentContextEnrichmentService contextEnrichmentService;
    private final AgentSlotRequirementService slotRequirementService;
    private final GeneralChatService generalChatService;
    private final AssistantConversationService assistantConversationService;
    private final RagKnowledgeService ragKnowledgeService;
    private final PlannerService plannerService;
    private final AgentArtifactService artifactService;
    private final ResponseComposerService responseComposerService;
    private final PlannerToolCatalog plannerToolCatalog;
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
                             AgentAssignmentDraftService assignmentDraftService,
                             AgentContextEnrichmentService contextEnrichmentService,
                             AgentSlotRequirementService slotRequirementService,
                             GeneralChatService generalChatService,
                             AssistantConversationService assistantConversationService,
                             RagKnowledgeService ragKnowledgeService,
                             PlannerService plannerService,
                             AgentArtifactService artifactService,
                             ResponseComposerService responseComposerService,
                             PlannerToolCatalog plannerToolCatalog,
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
        this.assignmentDraftService = assignmentDraftService;
        this.contextEnrichmentService = contextEnrichmentService;
        this.slotRequirementService = slotRequirementService;
        this.generalChatService = generalChatService;
        this.assistantConversationService = assistantConversationService;
        this.ragKnowledgeService = ragKnowledgeService;
        this.plannerService = plannerService;
        this.artifactService = artifactService;
        this.responseComposerService = responseComposerService;
        this.plannerToolCatalog = plannerToolCatalog;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AgentChatResponseDTO chat(Long userId, String userRole, String sessionId, String message) {
        return chat(userId, userRole, sessionId, message, Map.of());
    }

    @Transactional
    public AgentChatResponseDTO chat(Long userId, String userRole, String sessionId, String message,
                                     Map<String, Object> pageContext) {
        AgentSessionEntity session = sessionService.resolveSession(userId, userRole, sessionId);
        sessionService.saveMessage(session.getId(), "USER", message, null);
        List<AgentMessageDTO> recentMessages = sessionService.loadRecentMessages(session.getId(), 10);
        Map<String, SessionArtifact> artifacts = artifactService.loadArtifacts(session);
        RagKnowledgeService.Retrieval retrieval = safeRetrieval(userId, userRole, message);

        PlannerDecision decision = plannerService.plan(new PlannerContext(
                userId,
                userRole,
                session,
                message,
                recentMessages,
                artifacts,
                pageContext == null ? Map.of() : pageContext,
                retrieval
        ));

        AgentChatResponseDTO response = switch (decision.mode()) {
            case ANSWER -> answerResponse(session, userId, userRole, message, decision, retrieval, artifacts);
            case CLARIFY -> clarificationResponse(session, decision, artifacts, retrieval);
            case TOOL_CALL -> routeToolDecision(session, userId, userRole, message, pageContext, decision, artifacts, retrieval);
        };

        sessionService.saveAssistantMessage(session.getId(), response, toRecognizedIntent(decision));
        return response;
    }

    @Transactional
    public AgentChatResponseDTO streamChat(Long userId,
                                           String userRole,
                                           com._202510007517.platform.agent.api.dto.AgentChatRequestDTO request,
                                           AgentStreamingCallback callback) throws Exception {
        AgentSessionEntity session = sessionService.resolveSession(userId, userRole, request.getSessionId());
        sessionService.saveMessage(session.getId(), "USER", request.getMessage(), null);
        List<AgentMessageDTO> recentMessages = sessionService.loadRecentMessages(session.getId(), 10);
        Map<String, SessionArtifact> artifacts = artifactService.loadArtifacts(session);
        RagKnowledgeService.Retrieval retrieval = safeRetrieval(userId, userRole, request.getMessage());

        PlannerDecision decision = plannerService.plan(new PlannerContext(
                userId,
                userRole,
                session,
                request.getMessage(),
                recentMessages,
                artifacts,
                request.getContext() == null ? Map.of() : request.getContext(),
                retrieval
        ));

        AgentChatResponseDTO response = switch (decision.mode()) {
            case ANSWER -> streamAnswerResponse(session, userId, userRole, request.getMessage(), decision, retrieval, artifacts, callback);
            case CLARIFY -> clarificationResponse(session, decision, artifacts, retrieval);
            case TOOL_CALL -> streamToolDecision(session, userId, userRole, request.getMessage(), request.getContext(), decision, artifacts, retrieval, callback);
        };

        sessionService.saveAssistantMessage(session.getId(), response, toRecognizedIntent(decision));
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
        AgentExecutionResultDTO result = actionExecutionService.execute(userId, userRole, action, intent, request);
        persistExecutionArtifacts(action, intent, result);
        return result;
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

    @Transactional
    public AgentSessionDTO updateSessionTitle(Long userId, String userRole, Long sessionId, String title) {
        return sessionService.updateSessionTitle(userId, userRole, sessionId, title);
    }

    @Transactional
    public void deleteSession(Long userId, String userRole, Long sessionId) {
        sessionService.deleteSession(userId, userRole, sessionId);
    }

    private AgentChatResponseDTO routeToolDecision(AgentSessionEntity session,
                                                   Long userId,
                                                   String userRole,
                                                   String message,
                                                   Map<String, Object> pageContext,
                                                   PlannerDecision decision,
                                                   Map<String, SessionArtifact> artifacts,
                                                   RagKnowledgeService.Retrieval retrieval) {
        AgentIntent intent;
        try {
            intent = plannerToolCatalog.resolve(decision.toolName());
        } catch (IllegalArgumentException ex) {
            return legacyFallbackResponse(session, userId, userRole, message, decision, artifacts, retrieval);
        }
        if (intent == AgentIntent.QUERY_RAG_KNOWLEDGE) {
            return knowledgeAnswerResponse(session, userId, userRole, message, decision, artifacts, retrieval);
        }

        permissionPolicy.assertAllowed(userId, userRole, intent);
        RecognizedIntent recognizedIntent = toRecognizedIntent(intent, decision, artifacts);
        recognizedIntent = mergePendingContext(session, recognizedIntent, message);
        recognizedIntent = assignmentDraftService.enrich(recognizedIntent, message, pageContext == null ? Map.of() : pageContext);
        recognizedIntent = contextEnrichmentService.enrich(userId, userRole, recognizedIntent);
        recognizedIntent = contextEnrichmentService.enrichFromSessionArtifacts(session, recognizedIntent);
        recognizedIntent = mergeAttachmentsFromPageContext(recognizedIntent, pageContext);

        List<String> missingSlots = slotRequirementService.missingSlots(recognizedIntent, message);
        if (!missingSlots.isEmpty()) {
            savePendingContext(session, recognizedIntent);
            AgentChatResponseDTO response = baseResponse(session, "TEXT");
            response.setMessage(slotRequirementService.buildPrompt(recognizedIntent.intent(), missingSlots));
            response.setPlannerDecision(decision);
            response.setArtifactSummary(artifactService.summarizeArtifacts(artifacts));
            response.setRetrievalStatus(retrievalStatus(retrieval));
            return response;
        }
        clearPendingContext(session);

        if (confirmationPolicy.requiresConfirmation(intent)) {
            AgentChatResponseDTO preview = actionPlanningService.createPreviewResponse(session, recognizedIntent);
            preview.setPlannerDecision(decision);
            preview.setArtifactSummary(artifactService.summarizeArtifacts(artifacts));
            preview.setRetrievalStatus(retrievalStatus(retrieval));
            return preview;
        }

        Map<String, Object> toolResult = toolRegistry.resolve(intent).execute(userId, userRole, recognizedIntent.slots());
        artifactService.saveToolArtifacts(session, decision.toolName(), toolResult);
        saveGeneratedQuestionsForAssignment(session, intent, toolResult);
        sessionService.save(session);

        AgentChatResponseDTO response = baseResponse(session, "DATA");
        response.setData(toolResult);
        response.setToolResult(toolResult);
        response.setPlannerDecision(decision);
        response.setArtifactSummary(artifactService.summarizeArtifacts(artifactService.loadArtifacts(session)));
        response.setRetrievalStatus(retrievalStatus(retrieval));
        response.setMessage(responseComposerService.composeToolReply(decision, toolResult, retrievalResults(retrieval)));
        return response;
    }

    private AgentChatResponseDTO streamToolDecision(AgentSessionEntity session,
                                                    Long userId,
                                                    String userRole,
                                                    String message,
                                                    Map<String, Object> pageContext,
                                                    PlannerDecision decision,
                                                    Map<String, SessionArtifact> artifacts,
                                                    RagKnowledgeService.Retrieval retrieval,
                                                    AgentStreamingCallback callback) throws Exception {
        AgentIntent intent;
        try {
            intent = plannerToolCatalog.resolve(decision.toolName());
        } catch (IllegalArgumentException ex) {
            return streamLegacyFallbackResponse(session, userId, userRole, message, decision, artifacts, retrieval, callback);
        }
        if (intent == AgentIntent.QUERY_RAG_KNOWLEDGE) {
            return streamKnowledgeAnswerResponse(session, userId, userRole, message, decision, artifacts, retrieval, callback);
        }
        return routeToolDecision(session, userId, userRole, message, pageContext, decision, artifacts, retrieval);
    }

    private AgentChatResponseDTO answerResponse(AgentSessionEntity session,
                                                Long userId,
                                                String userRole,
                                                String message,
                                                PlannerDecision decision,
                                                RagKnowledgeService.Retrieval retrieval,
                                                Map<String, SessionArtifact> artifacts) {
        if ("legacy_unknown".equals(decision.plannerReason())) {
            return legacyFallbackResponse(session, userId, userRole, message, decision, artifacts, retrieval);
        }
        clearPendingContext(session);
        return directAnswerResponse(session, decision, retrieval, artifacts);
    }

    private AgentChatResponseDTO streamAnswerResponse(AgentSessionEntity session,
                                                      Long userId,
                                                      String userRole,
                                                      String message,
                                                      PlannerDecision decision,
                                                      RagKnowledgeService.Retrieval retrieval,
                                                      Map<String, SessionArtifact> artifacts,
                                                      AgentStreamingCallback callback) throws Exception {
        if ("legacy_unknown".equals(decision.plannerReason())) {
            return streamLegacyFallbackResponse(session, userId, userRole, message, decision, artifacts, retrieval, callback);
        }
        return answerResponse(session, userId, userRole, message, decision, retrieval, artifacts);
    }

    private AgentChatResponseDTO directAnswerResponse(AgentSessionEntity session,
                                                      PlannerDecision decision,
                                                      RagKnowledgeService.Retrieval retrieval,
                                                      Map<String, SessionArtifact> artifacts) {
        AgentChatResponseDTO response = baseResponse(session, "TEXT");
        response.setMessage(responseComposerService.composeDirectAnswer(decision, retrievalResults(retrieval)));
        response.setPlannerDecision(decision);
        response.setArtifactSummary(artifactService.summarizeArtifacts(artifacts));
        response.setRetrievalStatus(retrievalStatus(retrieval));
        return response;
    }

    private AgentChatResponseDTO clarificationResponse(AgentSessionEntity session,
                                                       PlannerDecision decision,
                                                       Map<String, SessionArtifact> artifacts,
                                                       RagKnowledgeService.Retrieval retrieval) {
        AgentChatResponseDTO response = baseResponse(session, "TEXT");
        response.setMessage(responseComposerService.composeClarification(decision));
        response.setPlannerDecision(decision);
        response.setArtifactSummary(artifactService.summarizeArtifacts(artifacts));
        response.setRetrievalStatus(retrievalStatus(retrieval));
        return response;
    }

    private AgentChatResponseDTO legacyFallbackResponse(AgentSessionEntity session,
                                                        Long userId,
                                                        String userRole,
                                                        String message,
                                                        PlannerDecision decision,
                                                        Map<String, SessionArtifact> artifacts,
                                                        RagKnowledgeService.Retrieval retrieval) {
        clearPendingContext(session);
        List<AgentMessageDTO> recentMessages = sessionService.loadRecentMessages(session.getId(), 10);
        String reply = generalChatService.reply(
                userId,
                userRole,
                String.valueOf(session.getId()),
                message,
                recentMessages);
        if (reply == null) {
            reply = generalChatService.reply(userId, userRole, String.valueOf(session.getId()), message);
        }
        AgentChatResponseDTO response = baseResponse(session, "TEXT");
        response.setMessage(reply);
        response.setPlannerDecision(decision);
        response.setArtifactSummary(artifactService.summarizeArtifacts(artifacts));
        response.setRetrievalStatus(retrievalStatus(retrieval));
        return response;
    }

    private AgentChatResponseDTO streamLegacyFallbackResponse(AgentSessionEntity session,
                                                              Long userId,
                                                              String userRole,
                                                              String message,
                                                              PlannerDecision decision,
                                                              Map<String, SessionArtifact> artifacts,
                                                              RagKnowledgeService.Retrieval retrieval,
                                                              AgentStreamingCallback callback) throws Exception {
        clearPendingContext(session);
        List<AgentMessageDTO> recentMessages = sessionService.loadRecentMessages(session.getId(), 10);
        String reply = generalChatService.replyStream(
                userId,
                userRole,
                String.valueOf(session.getId()),
                message,
                recentMessages,
                callback);
        if (reply == null) {
            reply = generalChatService.reply(userId, userRole, String.valueOf(session.getId()), message);
        }
        AgentChatResponseDTO response = baseResponse(session, "TEXT");
        response.setMessage(reply);
        response.setPlannerDecision(decision);
        response.setArtifactSummary(artifactService.summarizeArtifacts(artifacts));
        response.setRetrievalStatus(retrievalStatus(retrieval));
        return response;
    }

    private AgentChatResponseDTO knowledgeAnswerResponse(AgentSessionEntity session,
                                                         Long userId,
                                                         String userRole,
                                                         String message,
                                                         PlannerDecision decision,
                                                         Map<String, SessionArtifact> artifacts,
                                                         RagKnowledgeService.Retrieval retrieval) {
        clearPendingContext(session);
        String reply = assistantConversationService.reply(
                AgentAssistantType.KNOWLEDGE,
                new AgentAssistantRequest(
                        userId,
                        userRole,
                        String.valueOf(session.getId()),
                        message,
                        sessionService.loadRecentMessages(session.getId(), 10))
        );
        AgentChatResponseDTO response = baseResponse(session, "TEXT");
        response.setMessage(reply);
        response.setPlannerDecision(decision);
        response.setArtifactSummary(artifactService.summarizeArtifacts(artifacts));
        response.setRetrievalStatus(retrievalStatus(retrieval));
        return response;
    }

    private AgentChatResponseDTO streamKnowledgeAnswerResponse(AgentSessionEntity session,
                                                               Long userId,
                                                               String userRole,
                                                               String message,
                                                               PlannerDecision decision,
                                                               Map<String, SessionArtifact> artifacts,
                                                               RagKnowledgeService.Retrieval retrieval,
                                                               AgentStreamingCallback callback) throws Exception {
        clearPendingContext(session);
        String reply = assistantConversationService.replyStream(
                AgentAssistantType.KNOWLEDGE,
                new AgentAssistantRequest(
                        userId,
                        userRole,
                        String.valueOf(session.getId()),
                        message,
                        sessionService.loadRecentMessages(session.getId(), 10)),
                callback
        );
        AgentChatResponseDTO response = baseResponse(session, "TEXT");
        response.setMessage(reply);
        response.setPlannerDecision(decision);
        response.setArtifactSummary(artifactService.summarizeArtifacts(artifacts));
        response.setRetrievalStatus(retrievalStatus(retrieval));
        return response;
    }

    private RecognizedIntent toRecognizedIntent(AgentIntent intent,
                                                PlannerDecision decision,
                                                Map<String, SessionArtifact> artifacts) {
        Map<String, Object> slots = new LinkedHashMap<>();
        if (decision.arguments() != null) {
            slots.putAll(decision.arguments());
        }
        if (decision.artifactRef() != null && artifacts.containsKey(decision.artifactRef())) {
            SessionArtifact artifact = artifacts.get(decision.artifactRef());
            if (artifact.payload() != null) {
                artifact.payload().forEach(slots::putIfAbsent);
            }
        }
        return new RecognizedIntent(intent, 0.95, slots);
    }

    private RecognizedIntent toRecognizedIntent(PlannerDecision decision) {
        if (decision.mode() != PlannerMode.TOOL_CALL) {
            return new RecognizedIntent(AgentIntent.UNKNOWN, 0.0, Map.of());
        }
        try {
            return new RecognizedIntent(plannerToolCatalog.resolve(decision.toolName()), 0.95, decision.arguments());
        } catch (IllegalArgumentException ex) {
            return new RecognizedIntent(AgentIntent.UNKNOWN, 0.0, Map.of());
        }
    }

    private AgentChatResponseDTO baseResponse(AgentSessionEntity session, String responseType) {
        AgentChatResponseDTO response = new AgentChatResponseDTO();
        response.setSessionId(String.valueOf(session.getId()));
        response.setResponseType(responseType);
        return response;
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

    private RagKnowledgeService.Retrieval safeRetrieval(Long userId, String userRole, String message) {
        RagKnowledgeService.Retrieval retrieval = ragKnowledgeService.retrieve(userId, userRole, message);
        if (retrieval != null) {
            return retrieval;
        }
        return new RagKnowledgeService.Retrieval(List.of(), RagKnowledgeService.RetrievalStatus.DISABLED);
    }

    private String retrievalStatus(RagKnowledgeService.Retrieval retrieval) {
        return retrieval == null || retrieval.status() == null
                ? RagKnowledgeService.RetrievalStatus.DISABLED.name()
                : retrieval.status().name();
    }

    private List<?> retrievalResults(RagKnowledgeService.Retrieval retrieval) {
        return retrieval == null || retrieval.results() == null ? List.of() : retrieval.results();
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

    private void saveGeneratedQuestionsForAssignment(AgentSessionEntity session, AgentIntent intent, Object data) {
        if (intent != AgentIntent.GENERATE_QUESTIONS || !(data instanceof Map<?, ?> rawData)) {
            return;
        }
        Map<?, ?> source = rawData;
        Object questions = source.get("questions");
        if (!(questions instanceof List<?> questionList) || questionList.isEmpty()) {
            Object nested = rawData.get("aiResult");
            if (nested instanceof Map<?, ?> nestedMap) {
                source = nestedMap;
                questions = nestedMap.get("questions");
            }
        }
        if (!(questions instanceof List<?> questionList) || questionList.isEmpty()) {
            return;
        }
        Map<String, Object> pendingSlots = new LinkedHashMap<>();
        pendingSlots.put("selectionMode", AgentAssignmentDraftService.GENERATED_QUESTIONS);
        Object topic = source.get("topic");
        if (topic != null && !String.valueOf(topic).isBlank()) {
            pendingSlots.put("title", String.valueOf(topic) + "课堂练习");
        }
        pendingSlots.put("content", formatGeneratedQuestions(questionList));
        savePendingContext(session, new RecognizedIntent(AgentIntent.PUBLISH_ASSIGNMENT, 0.8, pendingSlots));
    }

    private String formatGeneratedQuestions(List<?> questions) {
        StringBuilder builder = new StringBuilder("题目如下：");
        for (int index = 0; index < questions.size(); index++) {
            Object question = questions.get(index);
            builder.append(System.lineSeparator()).append(index + 1).append(". ");
            if (question instanceof Map<?, ?> questionMap) {
                Object content = questionMap.get("content");
                builder.append(content == null ? "" : content);
                Object answer = questionMap.get("answer");
                if (answer != null && !String.valueOf(answer).isBlank()) {
                    builder.append(System.lineSeparator()).append("答案：").append(answer);
                }
                continue;
            }
            builder.append(question);
        }
        return builder.toString();
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize agent action payload.", ex);
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

    private RecognizedIntent mergePendingContext(AgentSessionEntity session, RecognizedIntent current, String message) {
        if (session.getPendingIntent() == null || session.getPendingIntent().isBlank()) {
            return current;
        }
        if (current.intent() == AgentIntent.UNKNOWN && isLikelySmallTalk(message)) {
            return current;
        }
        AgentIntent pendingIntent = AgentIntent.valueOf(session.getPendingIntent());
        if (current.intent() != AgentIntent.UNKNOWN && current.intent() != pendingIntent) {
            return current;
        }
        Map<String, Object> slots = new LinkedHashMap<>(readPendingSlots(session.getPendingSlotsJson()));
        if (current.slots() != null) {
            current.slots().forEach((key, value) -> {
                if (value != null) {
                    slots.put(key, value);
                }
            });
        }
        return new RecognizedIntent(pendingIntent, Math.max(current.confidence(), 0.8), slots, current.missingSlots());
    }

    private boolean isLikelySmallTalk(String message) {
        if (message == null) {
            return false;
        }
        String normalized = message.trim().toLowerCase();
        return normalized.equals("hi")
                || normalized.equals("hello")
                || normalized.equals("你好")
                || normalized.equals("您好")
                || normalized.equals("你是谁")
                || normalized.equals("你是什么模型");
    }

    private RecognizedIntent mergeAttachmentsFromPageContext(RecognizedIntent intent, Map<String, Object> pageContext) {
        if (pageContext == null
                || (intent.intent() != AgentIntent.PUBLISH_ASSIGNMENT && intent.intent() != AgentIntent.PUBLISH_EXAM)) {
            return intent;
        }
        Object attachments = pageContext.get("attachments");
        if (!(attachments instanceof List<?> attachmentList) || attachmentList.isEmpty()) {
            return intent;
        }
        Map<String, Object> slots = new LinkedHashMap<>(intent.slots());
        slots.put("attachments", attachmentList);
        return new RecognizedIntent(intent.intent(), intent.confidence(), slots, intent.missingSlots());
    }

    private void persistExecutionArtifacts(AgentActionEntity action, AgentIntent intent, AgentExecutionResultDTO result) {
        if (result == null || result.getResult() == null || !"EXECUTED".equals(result.getStatus())) {
            return;
        }
        AgentSessionEntity session = sessionService.resolveSessionOwner(action.getSessionId());
        artifactService.saveToolArtifacts(session, plannerToolCatalog.reverseResolve(intent), result.getResult());
        sessionService.save(session);
    }
}
