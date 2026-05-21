package com._202510007517.platform.analysis.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record KnowledgeMasteryDTO(
        Long studentId,
        Long courseId,
        Long classId,
        Long knowledgePointId,
        BigDecimal masteryScore,
        Integer evidenceCount,
        String lastSourceType,
        Long lastSourceId,
        String lastEventId,
        Instant updatedAt) {
}
