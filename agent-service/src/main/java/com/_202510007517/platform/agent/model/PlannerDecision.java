package com._202510007517.platform.agent.model;

import java.util.LinkedHashMap;
import java.util.Map;

public record PlannerDecision(
        PlannerMode mode,
        String toolName,
        Map<String, Object> arguments,
        String artifactRef,
        String replyDraft,
        String clarificationPrompt,
        String plannerReason
) {
    public static PlannerDecision answer(String replyDraft, String plannerReason) {
        return new PlannerDecision(
                PlannerMode.ANSWER,
                null,
                Map.of(),
                null,
                replyDraft,
                null,
                plannerReason
        );
    }

    public static PlannerDecision clarify(String clarificationPrompt, String plannerReason) {
        return new PlannerDecision(
                PlannerMode.CLARIFY,
                null,
                Map.of(),
                null,
                null,
                clarificationPrompt,
                plannerReason
        );
    }

    public static PlannerDecision toolCall(String toolName,
                                           Map<String, Object> arguments,
                                           String artifactRef,
                                           String plannerReason) {
        return new PlannerDecision(
                PlannerMode.TOOL_CALL,
                toolName,
                arguments == null ? Map.of() : new LinkedHashMap<>(arguments),
                artifactRef,
                null,
                null,
                plannerReason
        );
    }
}
