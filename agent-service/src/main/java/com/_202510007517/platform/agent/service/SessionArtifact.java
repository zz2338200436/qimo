package com._202510007517.platform.agent.service;

import java.util.Map;

public record SessionArtifact(
        String type,
        String key,
        Map<String, Object> payload,
        String createdAt,
        String updatedAt
) {
}
