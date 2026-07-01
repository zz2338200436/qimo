package com._202510007517.platform.events.warning;

import java.util.Objects;

public record EarlyWarningRaisedPayload(
        Long warningId,
        Long studentId,
        Long courseId,
        String warningType,
        String level,
        String title,
        String reason) {

    public EarlyWarningRaisedPayload {
        Objects.requireNonNull(warningId, "warningId must not be null");
        Objects.requireNonNull(studentId, "studentId must not be null");
        Objects.requireNonNull(courseId, "courseId must not be null");
        Objects.requireNonNull(warningType, "warningType must not be null");
        Objects.requireNonNull(level, "level must not be null");
        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(reason, "reason must not be null");
    }
}
