package com._202510007517.platform.events.warning;

import java.util.Objects;

public record EarlyWarningRollbackPayload(
        Long warningId,
        String sourceEventId,
        String rollbackReason) {

    public EarlyWarningRollbackPayload {
        Objects.requireNonNull(warningId, "warningId must not be null");
        Objects.requireNonNull(sourceEventId, "sourceEventId must not be null");
        Objects.requireNonNull(rollbackReason, "rollbackReason must not be null");
    }
}
