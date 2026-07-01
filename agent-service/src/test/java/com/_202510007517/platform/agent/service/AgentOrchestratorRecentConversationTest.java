package com._202510007517.platform.agent.service;

import com._202510007517.platform.agent.api.dto.AgentChatResponseDTO;
import com._202510007517.platform.agent.api.dto.AgentMessageDTO;
import com._202510007517.platform.agent.domain.AgentSessionEntity;
import com._202510007517.platform.agent.model.AgentIntent;
import com._202510007517.platform.agent.model.PlannerDecision;
import com._202510007517.platform.agent.model.RecognizedIntent;
import com._202510007517.platform.agent.rag.RagKnowledgeService;
import com._202510007517.platform.agent.repository.AgentActionRepository;
import com._202510007517.platform.agent.tool.ToolRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.atLeastOnce;

class AgentOrchestratorRecentConversationTest {

    private final IntentRecognitionService intentRecognitionService = mock(IntentRecognitionService.class);
    private final AgentActionMapper actionMapper = mock(AgentActionMapper.class);
    private final ActionPlanningService actionPlanningService = mock(ActionPlanningService.class);
    private final ActionConfirmationService actionConfirmationService = mock(ActionConfirmationService.class);
    private final ActionExecutionService actionExecutionService = mock(ActionExecutionService.class);
    private final AgentSessionService sessionService = mock(AgentSessionService.class);
    private final AgentActionRepository actionRepository = mock(AgentActionRepository.class);
    private final ToolRegistry toolRegistry = mock(ToolRegistry.class);
    private final AgentPermissionPolicy permissionPolicy = mock(AgentPermissionPolicy.class);
    private final AgentConfirmationPolicy confirmationPolicy = mock(AgentConfirmationPolicy.class);
    private final AgentAssignmentDraftService assignmentDraftService = mock(AgentAssignmentDraftService.class);
    private final AgentContextEnrichmentService contextEnrichmentService = mock(AgentContextEnrichmentService.class);
    private final AgentSlotRequirementService slotRequirementService = mock(AgentSlotRequirementService.class);
    private final GeneralChatService generalChatService = mock(GeneralChatService.class);
    private final com._202510007517.platform.agent.assistant.AssistantConversationService assistantConversationService = mock(
            com._202510007517.platform.agent.assistant.AssistantConversationService.class);
    private final RagKnowledgeService ragKnowledgeService = mock(RagKnowledgeService.class);
    private final PlannerService plannerService = mock(PlannerService.class);
    private final AgentArtifactService artifactService = mock(AgentArtifactService.class);
    private final ResponseComposerService responseComposerService = mock(ResponseComposerService.class);
    private final PlannerToolCatalog plannerToolCatalog = mock(PlannerToolCatalog.class);
    private final AgentOrchestrator orchestrator = new AgentOrchestrator(
            intentRecognitionService,
            actionMapper,
            actionPlanningService,
            actionConfirmationService,
            actionExecutionService,
            sessionService,
            actionRepository,
            toolRegistry,
            permissionPolicy,
            confirmationPolicy,
            assignmentDraftService,
            contextEnrichmentService,
            slotRequirementService,
            generalChatService,
            assistantConversationService,
            ragKnowledgeService,
            plannerService,
            artifactService,
            responseComposerService,
            plannerToolCatalog,
            new ObjectMapper()
    );

    @Test
    @SuppressWarnings("unchecked")
    void recentConversationIsPassedIntoIntentRecognitionForFollowUpRequests() {
        AgentSessionEntity session = new AgentSessionEntity();
        session.setId(99L);
        session.setUserId(7L);
        session.setUserRole("TEACHER");
        session.setStatus("ACTIVE");
        List<AgentMessageDTO> recentMessages = List.of(
                messageDto(1L, "USER", "先帮我生成五道 Java 基础题"),
                messageDto(2L, "ASSISTANT", "已经生成了五道题"),
                messageDto(3L, "USER", "前面那五道题再发我一下")
        );
        when(sessionService.resolveSession(7L, "TEACHER", "99")).thenReturn(session);
        when(sessionService.loadRecentMessages(99L, 10)).thenReturn(recentMessages);
        when(artifactService.loadArtifacts(session)).thenReturn(Map.of());
        when(ragKnowledgeService.retrieve(7L, "TEACHER", "前面那五道题再发我一下"))
                .thenReturn(new RagKnowledgeService.Retrieval(List.of(), RagKnowledgeService.RetrievalStatus.READY));
        when(plannerService.plan(any(PlannerContext.class)))
                .thenAnswer(invocation -> new FallbackPlannerService(intentRecognitionService, new PlannerToolCatalog())
                        .plan(invocation.getArgument(0)));
        when(intentRecognitionService.recognize(eq("前面那五道题再发我一下"), eq(recentMessages)))
                .thenReturn(new RecognizedIntent(AgentIntent.UNKNOWN, 0.0, Map.of()));
        when(generalChatService.reply(7L, "TEACHER", "99", "前面那五道题再发我一下", recentMessages))
                .thenReturn("这是一次追问回复。");

        AgentChatResponseDTO response = orchestrator.chat(7L, "TEACHER", "99", "前面那五道题再发我一下");

        var messagesCaptor = forClass(List.class);
        verify(intentRecognitionService).recognize(eq("前面那五道题再发我一下"), messagesCaptor.capture());
        assertThat((List<AgentMessageDTO>) messagesCaptor.getValue())
                .extracting(AgentMessageDTO::getRole, AgentMessageDTO::getContent)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("USER", "先帮我生成五道 Java 基础题"),
                        org.assertj.core.groups.Tuple.tuple("ASSISTANT", "已经生成了五道题"),
                        org.assertj.core.groups.Tuple.tuple("USER", "前面那五道题再发我一下")
                );
        assertThat(response.getResponseType()).isEqualTo("TEXT");
        assertThat(response.getMessage()).isEqualTo("这是一次追问回复。");
        verify(sessionService).saveMessage(99L, "USER", "前面那五道题再发我一下", null);
        verify(generalChatService).reply(7L, "TEACHER", "99", "前面那五道题再发我一下", recentMessages);
    }

    @Test
    void plannerToolCallUsesArtifactDrivenPublishInsteadOfLegacyIntentRecognition() {
        AgentSessionEntity session = new AgentSessionEntity();
        session.setId(99L);
        session.setUserId(7L);
        session.setUserRole("TEACHER");
        session.setStatus("ACTIVE");
        when(sessionService.resolveSession(7L, "TEACHER", "99")).thenReturn(session);
        when(sessionService.loadRecentMessages(99L, 10)).thenReturn(List.of());
        when(artifactService.summarizeArtifacts(any())).thenReturn(Map.of("latestGeneratedArtifactKey", "latest_generated_questions"));
        when(ragKnowledgeService.retrieve(7L, "TEACHER", "把生成的题目发到云计算技术1班"))
                .thenReturn(new RagKnowledgeService.Retrieval(List.of(), RagKnowledgeService.RetrievalStatus.READY));
        when(artifactService.loadArtifacts(session)).thenReturn(Map.of(
                "latest_generated_questions",
                new SessionArtifact(
                        "generated_questions",
                        "latest_generated_questions",
                        Map.of(
                                "title", "Java课堂练习",
                                "content", "题目如下：\n1. 题目A",
                                "questions", List.of(Map.of("content", "题目A"))
                        ),
                        "2026-06-23T23:00:00",
                        "2026-06-23T23:00:00"
                )));
        when(plannerService.plan(any(PlannerContext.class)))
                .thenReturn(PlannerDecision.toolCall(
                        "publish_assignment",
                        Map.of("className", "云计算技术1班"),
                        "latest_generated_questions",
                        "use generated questions artifact"
                ));
        when(plannerToolCatalog.resolve("publish_assignment")).thenReturn(AgentIntent.PUBLISH_ASSIGNMENT);
        when(contextEnrichmentService.enrich(eq(7L), eq("TEACHER"), any(RecognizedIntent.class)))
                .thenAnswer(invocation -> invocation.getArgument(2));
        when(assignmentDraftService.enrich(any(RecognizedIntent.class), any(), any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(slotRequirementService.missingSlots(any(RecognizedIntent.class), eq("把生成的题目发到云计算技术1班")))
                .thenReturn(List.of());
        when(confirmationPolicy.requiresConfirmation(AgentIntent.PUBLISH_ASSIGNMENT)).thenReturn(true);

        AgentChatResponseDTO preview = new AgentChatResponseDTO();
        preview.setSessionId("99");
        preview.setResponseType("ACTION_PREVIEW");
        preview.setMessage("请确认是否执行该操作。");
        when(actionPlanningService.createPreviewResponse(eq(session), any(RecognizedIntent.class)))
                .thenReturn(preview);

        AgentChatResponseDTO response = orchestrator.chat(7L, "TEACHER", "99", "把生成的题目发到云计算技术1班");

        assertThat(response.getResponseType()).isEqualTo("ACTION_PREVIEW");
        verify(intentRecognitionService, never()).recognize(any(), any());
        verify(plannerToolCatalog, atLeastOnce()).resolve("publish_assignment");
        verify(actionPlanningService).createPreviewResponse(eq(session), any(RecognizedIntent.class));
    }

    @Test
    void plannerToolCallMergesPendingAssignmentSlotsBeforeValidation() {
        AgentSessionEntity session = new AgentSessionEntity();
        session.setId(99L);
        session.setUserId(7L);
        session.setUserRole("TEACHER");
        session.setStatus("ACTIVE");
        session.setPendingIntent("PUBLISH_ASSIGNMENT");
        session.setPendingSlotsJson("""
                {
                  "title": "Java课堂练习",
                  "content": "题目如下：\\n1. 题目A",
                  "dueDate": "2026-06-30 23:59:59",
                  "maxScore": 100
                }
                """);
        when(sessionService.resolveSession(7L, "TEACHER", "99")).thenReturn(session);
        when(sessionService.loadRecentMessages(99L, 10)).thenReturn(List.of());
        when(artifactService.loadArtifacts(session)).thenReturn(Map.of());
        when(artifactService.summarizeArtifacts(any())).thenReturn(Map.of());
        when(ragKnowledgeService.retrieve(7L, "TEACHER", "发布到云计算技术1班"))
                .thenReturn(new RagKnowledgeService.Retrieval(List.of(), RagKnowledgeService.RetrievalStatus.READY));
        when(plannerService.plan(any(PlannerContext.class)))
                .thenReturn(PlannerDecision.toolCall(
                        "publish_assignment",
                        Map.of("className", "云计算技术1班"),
                        null,
                        "planner_follow_up"
                ));
        when(plannerToolCatalog.resolve("publish_assignment")).thenReturn(AgentIntent.PUBLISH_ASSIGNMENT);
        when(assignmentDraftService.enrich(any(RecognizedIntent.class), eq("发布到云计算技术1班"), any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(contextEnrichmentService.enrich(eq(7L), eq("TEACHER"), any(RecognizedIntent.class)))
                .thenAnswer(invocation -> invocation.getArgument(2));
        when(slotRequirementService.missingSlots(any(RecognizedIntent.class), eq("发布到云计算技术1班")))
                .thenReturn(List.of());
        when(confirmationPolicy.requiresConfirmation(AgentIntent.PUBLISH_ASSIGNMENT)).thenReturn(true);
        AgentChatResponseDTO preview = new AgentChatResponseDTO();
        preview.setSessionId("99");
        preview.setResponseType("ACTION_PREVIEW");
        when(actionPlanningService.createPreviewResponse(eq(session), any(RecognizedIntent.class)))
                .thenReturn(preview);

        AgentChatResponseDTO response = orchestrator.chat(7L, "TEACHER", "99", "发布到云计算技术1班");

        assertThat(response.getResponseType()).isEqualTo("ACTION_PREVIEW");
        var intentCaptor = forClass(RecognizedIntent.class);
        verify(slotRequirementService).missingSlots(intentCaptor.capture(), eq("发布到云计算技术1班"));
        assertThat(intentCaptor.getValue().slots())
                .containsEntry("title", "Java课堂练习")
                .containsEntry("content", "题目如下：\n1. 题目A")
                .containsEntry("dueDate", "2026-06-30 23:59:59")
                .containsEntry("maxScore", 100)
                .containsEntry("className", "云计算技术1班");
    }

    private AgentMessageDTO messageDto(Long id, String role, String content) {
        AgentMessageDTO dto = new AgentMessageDTO();
        dto.setMessageId(id);
        dto.setSessionId("99");
        dto.setRole(role);
        dto.setContent(content);
        return dto;
    }
}
