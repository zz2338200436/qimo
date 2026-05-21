package com._202510007517.platform.common.event.outbox;

public interface OutboxEventPublisher {

    boolean publish(OutboxEventMessage message);
}
