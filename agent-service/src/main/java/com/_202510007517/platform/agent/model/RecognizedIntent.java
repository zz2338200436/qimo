package com._202510007517.platform.agent.model;

import java.util.Map;
import java.util.List;

public record RecognizedIntent(
        AgentIntent intent,
        double confidence,
        Map<String, Object> slots,
        List<String> missingSlots
) {
    public RecognizedIntent(AgentIntent intent, double confidence, Map<String, Object> slots) {
        this(intent, confidence, slots, List.of());
    }
}
