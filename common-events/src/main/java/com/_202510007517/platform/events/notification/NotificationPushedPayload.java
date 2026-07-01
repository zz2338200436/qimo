package com._202510007517.platform.events.notification;

import java.time.Instant;
import java.util.Objects;

public record NotificationPushedPayload(
        Long notificationId,
        Long recipientId,
        String channel,
        String title,
        String businessType,
        Long businessId,
        Instant pushedAt) {

    public NotificationPushedPayload {
        Objects.requireNonNull(notificationId, "notificationId must not be null");
        Objects.requireNonNull(recipientId, "recipientId must not be null");
        Objects.requireNonNull(channel, "channel must not be null");
        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(businessType, "businessType must not be null");
        Objects.requireNonNull(businessId, "businessId must not be null");
        Objects.requireNonNull(pushedAt, "pushedAt must not be null");
    }
}
