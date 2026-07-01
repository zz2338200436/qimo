package com._202510007517.platform.analysis.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ScoreTrendDTO(
        Long studentId,
        Long courseId,
        Long classId,
        String sourceType,
        Long sourceId,
        Long submissionId,
        Integer score,
        Integer maxScore,
        BigDecimal scoreRate,
        Instant occurredAt) {
}
