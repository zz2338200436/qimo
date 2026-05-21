package com._202510007517.platform.common.event.outbox;

import java.time.Instant;
import java.util.List;

public interface OutboxEventRepository {

    void save(OutboxEventEntity event);

    List<OutboxEventEntity> findDuePendingEvents(int limit, Instant now);

    void markPublished(long id, Instant publishedAt);

    void markFailed(long id, int retryCount, Instant nextRetryAt, String lastError, int maxRetries);
}
