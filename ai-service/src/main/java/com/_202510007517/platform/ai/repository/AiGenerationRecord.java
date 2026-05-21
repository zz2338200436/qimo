package com._202510007517.platform.ai.repository;

public record AiGenerationRecord(
        Long userId,
        String userRole,
        String promptKey,
        String requestType,
        String requestPayload,
        String responsePayload,
        String modelName,
        String status,
        String errorMessage,
        Long latencyMs) {
}
