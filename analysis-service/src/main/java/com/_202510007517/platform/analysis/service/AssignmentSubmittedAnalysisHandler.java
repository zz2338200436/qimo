package com._202510007517.platform.analysis.service;

import com._202510007517.platform.analysis.repository.AnalysisRepository;
import com._202510007517.platform.analysis.repository.KnowledgeMasteryRecord;
import com._202510007517.platform.assignment.api.feign.AssignmentFeignClient;
import com._202510007517.platform.common.event.idempotency.IdempotentEventHandler;
import com._202510007517.platform.events.assignment.AssignmentSubmittedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Component
public class AssignmentSubmittedAnalysisHandler {

    static final String CONSUMER_NAME = "analysis-service.assignment-submitted";
    static final String SOURCE_TYPE = "assignment";
    private static final BigDecimal SUBMISSION_EVIDENCE_SCORE = BigDecimal.valueOf(0.60).setScale(4);

    private final IdempotentEventHandler idempotentEventHandler;
    private final AnalysisRepository analysisRepository;
    private final AssignmentFeignClient assignmentFeignClient;

    public AssignmentSubmittedAnalysisHandler(IdempotentEventHandler idempotentEventHandler,
                                              AnalysisRepository analysisRepository) {
        this(idempotentEventHandler, analysisRepository, null);
    }

    @Autowired
    public AssignmentSubmittedAnalysisHandler(IdempotentEventHandler idempotentEventHandler,
                                              AnalysisRepository analysisRepository,
                                              AssignmentFeignClient assignmentFeignClient) {
        this.idempotentEventHandler = idempotentEventHandler;
        this.analysisRepository = analysisRepository;
        this.assignmentFeignClient = assignmentFeignClient;
    }

    @Transactional
    public boolean handle(AssignmentSubmittedEvent event) {
        return idempotentEventHandler.handle(
                event.eventId(),
                event.getClass().getSimpleName(),
                CONSUMER_NAME,
                () -> upsertKnowledgeMastery(event));
    }

    private void upsertKnowledgeMastery(AssignmentSubmittedEvent event) {
        List<Long> knowledgePointIds = resolveKnowledgePointIds(event.payload().assignmentId());
        if (knowledgePointIds.isEmpty()) {
            upsertKnowledgeMastery(event, null);
            return;
        }
        for (Long knowledgePointId : knowledgePointIds) {
            upsertKnowledgeMastery(event, knowledgePointId);
        }
    }

    private void upsertKnowledgeMastery(AssignmentSubmittedEvent event, Long knowledgePointId) {
        analysisRepository.upsertKnowledgeMastery(new KnowledgeMasteryRecord(
                event.payload().studentId(),
                event.payload().courseId(),
                event.payload().classId(),
                knowledgePointId,
                SUBMISSION_EVIDENCE_SCORE,
                1,
                SOURCE_TYPE,
                event.payload().assignmentId(),
                event.eventId(),
                event.occurredAt() == null ? Instant.now() : event.occurredAt()));
    }

    private List<Long> resolveKnowledgePointIds(Long assignmentId) {
        if (assignmentId == null || assignmentFeignClient == null) {
            return List.of();
        }
        try {
            List<Long> ids = assignmentFeignClient.listKnowledgePointIds(assignmentId);
            if (ids == null || ids.isEmpty()) {
                return List.of();
            }
            return ids.stream()
                    .filter(java.util.Objects::nonNull)
                    .distinct()
                    .toList();
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }
}
