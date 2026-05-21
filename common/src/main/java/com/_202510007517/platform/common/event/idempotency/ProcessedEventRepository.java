package com._202510007517.platform.common.event.idempotency;

public interface ProcessedEventRepository {

    boolean insertIfAbsent(ProcessedEventEntity event);

    void delete(String eventId, String consumerName);
}
