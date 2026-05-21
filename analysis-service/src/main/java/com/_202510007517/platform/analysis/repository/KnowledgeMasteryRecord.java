package com._202510007517.platform.analysis.repository;

import java.math.BigDecimal;
import java.time.Instant;

public record KnowledgeMasteryRecord(
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
