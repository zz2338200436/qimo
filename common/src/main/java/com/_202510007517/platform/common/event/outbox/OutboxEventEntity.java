package com._202510007517.platform.common.event.outbox;

import java.time.Instant;

public record OutboxEventEntity(
        Long id,
        String eventId,
        String aggregateType,
        String aggregateId,
        String eventType,
        String bindingName,
        String payload,
        String headers,
        int status,
        int retryCount,
        Instant nextRetryAt,
        String lastError,
        Instant createdAt,
        Instant updatedAt,
        Instant publishedAt) {
}
