package com._202510007517.platform.common.event.idempotency;

import java.time.Instant;

public record ProcessedEventEntity(
        Long id,
        String eventId,
        String eventType,
        String consumerName,
        Instant processedAt) {
}
