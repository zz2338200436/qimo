package com._202510007517.platform.agent.service;

import com._202510007517.platform.agent.model.AgentIntent;
import com._202510007517.platform.agent.model.PlannerDecision;
import com._202510007517.platform.agent.model.RecognizedIntent;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class FallbackPlannerService implements PlannerService {

    private final IntentRecognitionService intentRecognitionService;
    private final PlannerToolCatalog plannerToolCatalog;

    public FallbackPlannerService(IntentRecognitionService intentRecognitionService,
                                  PlannerToolCatalog plannerToolCatalog) {
        this.intentRecognitionService = intentRecognitionService;
        this.plannerToolCatalog = plannerToolCatalog;
    }

    @Override
    public PlannerDecision plan(PlannerContext context) {
        Map<String, SessionArtifact> artifacts = context.artifacts() == null ? Map.of() : context.artifacts();
        if (artifacts.containsKey("latest_generated_questions")
                && isToolbarPublishCommand(context.pageContext())) {
            SessionArtifact artifact = artifacts.get("latest_generated_questions");
            Map<String, Object> arguments = new LinkedHashMap<>();
            String className = extractNaturalClassName(context.message());
            if (className != null) {
                arguments.put("className", className);
            }
            Object payload = artifact.payload();
            if (payload instanceof Map<?, ?> payloadMap) {
                copyIfPresent(arguments, payloadMap, "title");
                copyIfPresent(arguments, payloadMap, "content");
            }
            return PlannerDecision.toolCall(
                    "publish_assignment",
                    arguments,
                    "latest_generated_questions",
                    "artifact_follow_up_publish"
            );
        }

        RecognizedIntent recognizedIntent = safeRecognize(context);
        recognizedIntent = mergePendingContext(context, recognizedIntent);
        if (recognizedIntent.intent() == AgentIntent.UNKNOWN) {
            return PlannerDecision.answer(null, "legacy_unknown");
        }
        return PlannerDecision.toolCall(
                plannerToolCatalog.reverseResolve(recognizedIntent.intent()),
                recognizedIntent.slots(),
                null,
                "legacy_intent_fallback"
        );
    }

    private RecognizedIntent mergePendingContext(PlannerContext context, RecognizedIntent current) {
        if (context.session() == null
                || context.session().getPendingIntent() == null
                || context.session().getPendingIntent().isBlank()) {
            return current;
        }
        if (current.intent() == AgentIntent.UNKNOWN && isLikelySmallTalk(context.message())) {
            return current;
        }
        AgentIntent pendingIntent = AgentIntent.valueOf(context.session().getPendingIntent());
        if (current.intent() != AgentIntent.UNKNOWN && current.intent() != pendingIntent) {
            return current;
        }
        Map<String, Object> slots = new LinkedHashMap<>(readPendingSlots(context.session().getPendingSlotsJson()));
        if (current.slots() != null) {
            slots.putAll(current.slots());
        }
        return new RecognizedIntent(pendingIntent, Math.max(current.confidence(), 0.8), slots, current.missingSlots());
    }

    private RecognizedIntent safeRecognize(PlannerContext context) {
        RecognizedIntent recognizedIntent = intentRecognitionService.recognize(context.message(), context.recentMessages());
        if (recognizedIntent == null) {
            recognizedIntent = intentRecognitionService.recognize(context.message());
        }
        if (recognizedIntent != null) {
            return recognizedIntent;
        }
        return new RecognizedIntent(AgentIntent.UNKNOWN, 0.0, Map.of());
    }

    private boolean isToolbarPublishCommand(Map<String, Object> pageContext) {
        if (pageContext == null || pageContext.isEmpty()) {
            return false;
        }
        return "toolbar".equals(String.valueOf(pageContext.get("agentCommandSource")))
                && "发布给班级".equals(String.valueOf(pageContext.get("agentCommand")));
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

    private String extractNaturalClassName(String message) {
        if (message == null) {
            return null;
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("([\\u4e00-\\u9fa5A-Za-z0-9]+(?:\\d{2,4}级)?\\d+班)")
                .matcher(message);
        return matcher.find() ? matcher.group(1) : null;
    }

    private void copyIfPresent(Map<String, Object> target, Map<?, ?> source, String key) {
        Object value = source.get(key);
        if (value != null && !String.valueOf(value).isBlank()) {
            target.put(key, value);
        }
    }

    private Map<String, Object> readPendingSlots(String pendingSlotsJson) {
        if (pendingSlotsJson == null || pendingSlotsJson.isBlank()) {
            return Map.of();
        }
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(
                    pendingSlotsJson,
                    new com.fasterxml.jackson.core.type.TypeReference<>() {
                    });
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            return Map.of();
        }
    }
}
