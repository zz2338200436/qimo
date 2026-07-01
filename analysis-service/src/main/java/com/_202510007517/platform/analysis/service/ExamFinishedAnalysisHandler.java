package com._202510007517.platform.analysis.service;

import com._202510007517.platform.analysis.repository.AnalysisRepository;
import com._202510007517.platform.analysis.repository.KnowledgeMasteryRecord;
import com._202510007517.platform.analysis.repository.ScoreTrendRecord;
import com._202510007517.platform.common.event.idempotency.IdempotentEventHandler;
import com._202510007517.platform.common.event.outbox.OutboxEventEntity;
import com._202510007517.platform.common.event.outbox.OutboxEventRepository;
import com._202510007517.platform.events.EventAggregate;
import com._202510007517.platform.events.exam.ExamFinishedEvent;
import com._202510007517.platform.events.warning.EarlyWarningRaisedEvent;
import com._202510007517.platform.events.warning.EarlyWarningRaisedPayload;
import com._202510007517.platform.exam.api.feign.ExamFeignClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.List;

@Component
public class ExamFinishedAnalysisHandler {

    static final String CONSUMER_NAME = "analysis-service.exam-finished";
    static final String SOURCE_TYPE = "exam";
    static final String EARLY_WARNING_BINDING = "early.warning.raised";
    private static final BigDecimal LOW_SCORE_THRESHOLD = BigDecimal.valueOf(0.60).setScale(4, RoundingMode.HALF_UP);

    private final IdempotentEventHandler idempotentEventHandler;
    private final AnalysisRepository analysisRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final ExamFeignClient examFeignClient;

    public ExamFinishedAnalysisHandler(IdempotentEventHandler idempotentEventHandler,
                                       AnalysisRepository analysisRepository,
                                       OutboxEventRepository outboxEventRepository,
                                       ObjectMapper objectMapper) {
        this(idempotentEventHandler, analysisRepository, outboxEventRepository, objectMapper, null);
    }

    @Autowired
    public ExamFinishedAnalysisHandler(IdempotentEventHandler idempotentEventHandler,
                                       AnalysisRepository analysisRepository,
                                       OutboxEventRepository outboxEventRepository,
                                       ObjectMapper objectMapper,
                                       ExamFeignClient examFeignClient) {
        this.idempotentEventHandler = idempotentEventHandler;
        this.analysisRepository = analysisRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
        this.examFeignClient = examFeignClient;
    }

    @Transactional
    public boolean handle(ExamFinishedEvent event) {
        return idempotentEventHandler.handle(
                event.eventId(),
                event.getClass().getSimpleName(),
                CONSUMER_NAME,
                () -> {
                    BigDecimal scoreRate = scoreRate(event.payload().score(), event.payload().maxScore());
                    analysisRepository.upsertScoreTrend(new ScoreTrendRecord(
                            event.payload().studentId(),
                            event.payload().courseId(),
                            event.payload().classId(),
                            SOURCE_TYPE,
                            event.payload().examId(),
                            event.payload().submissionId(),
                            event.payload().score(),
                            event.payload().maxScore(),
                            scoreRate,
                            event.payload().finishedAt()));
                    upsertKnowledgeMastery(event, scoreRate);
                    if (scoreRate.compareTo(LOW_SCORE_THRESHOLD) < 0) {
                        persistEarlyWarningRaisedEvent(event, scoreRate);
                    }
                });
    }

    private void upsertKnowledgeMastery(ExamFinishedEvent event, BigDecimal scoreRate) {
        List<Long> knowledgePointIds = resolveKnowledgePointIds(event.payload().examId());
        if (knowledgePointIds.isEmpty()) {
            upsertKnowledgeMastery(event, scoreRate, null);
            return;
        }
        for (Long knowledgePointId : knowledgePointIds) {
            upsertKnowledgeMastery(event, scoreRate, knowledgePointId);
        }
    }

    private void upsertKnowledgeMastery(ExamFinishedEvent event, BigDecimal scoreRate, Long knowledgePointId) {
        analysisRepository.upsertKnowledgeMastery(new KnowledgeMasteryRecord(
                event.payload().studentId(),
                event.payload().courseId(),
                event.payload().classId(),
                knowledgePointId,
                scoreRate,
                1,
                SOURCE_TYPE,
                event.payload().examId(),
                event.eventId(),
                event.occurredAt()));
    }

    private List<Long> resolveKnowledgePointIds(Long examId) {
        if (examId == null || examFeignClient == null) {
            return List.of();
        }
        try {
            List<Long> ids = examFeignClient.listKnowledgePointIds(examId);
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

    private void persistEarlyWarningRaisedEvent(ExamFinishedEvent sourceEvent, BigDecimal scoreRate) {
        EarlyWarningRaisedEvent event = buildEarlyWarningRaisedEvent(sourceEvent, scoreRate);
        Instant occurredAt = event.occurredAt();
        outboxEventRepository.save(new OutboxEventEntity(
                null,
                event.eventId(),
                event.aggregate().type(),
                event.aggregate().id(),
                event.eventType(),
                EARLY_WARNING_BINDING,
                toJson(event),
                "{}",
                0,
                0,
                occurredAt,
                null,
                occurredAt,
                occurredAt,
                null));
    }

    private EarlyWarningRaisedEvent buildEarlyWarningRaisedEvent(ExamFinishedEvent sourceEvent, BigDecimal scoreRate) {
        Long warningId = positiveWarningId(sourceEvent.eventId());
        String reason = "考试得分率 " + scoreRate.movePointRight(2).stripTrailingZeros().toPlainString()
                + "%，低于预警阈值 " + LOW_SCORE_THRESHOLD.movePointRight(2).stripTrailingZeros().toPlainString() + "%";
        return new EarlyWarningRaisedEvent(
                "early-warning-" + sourceEvent.eventId(),
                sourceEvent.occurredAt(),
                new EventAggregate("early_warning", "warning-" + warningId),
                new EarlyWarningRaisedPayload(
                        warningId,
                        sourceEvent.payload().studentId(),
                        sourceEvent.payload().courseId(),
                        "LOW_SCORE",
                        "HIGH",
                        "学情预警",
                        reason));
    }

    private static BigDecimal scoreRate(Integer score, Integer maxScore) {
        if (score == null || maxScore == null || maxScore <= 0) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(score)
                .divide(BigDecimal.valueOf(maxScore), 4, RoundingMode.HALF_UP);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("序列化学情预警事件失败", ex);
        }
    }

    private static long positiveWarningId(String sourceEventId) {
        byte[] hash = sha256(sourceEventId);
        long value = 0L;
        for (int i = 0; i < Long.BYTES; i++) {
            value = (value << 8) | (hash[i] & 0xffL);
        }
        return value == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(value);
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 digest is unavailable", ex);
        }
    }
}
