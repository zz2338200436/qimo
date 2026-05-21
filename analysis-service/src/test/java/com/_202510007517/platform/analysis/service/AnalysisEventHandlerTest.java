package com._202510007517.platform.analysis.service;

import com._202510007517.platform.analysis.config.AnalysisConsumerConfiguration;
import com._202510007517.platform.analysis.api.dto.KnowledgeMasteryDTO;
import com._202510007517.platform.analysis.api.dto.ScoreTrendDTO;
import com._202510007517.platform.analysis.repository.AnalysisRepository;
import com._202510007517.platform.analysis.repository.KnowledgeMasteryRecord;
import com._202510007517.platform.analysis.repository.ScoreTrendRecord;
import com._202510007517.platform.assignment.api.feign.AssignmentFeignClient;
import com._202510007517.platform.common.event.idempotency.IdempotentEventHandler;
import com._202510007517.platform.common.event.idempotency.ProcessedEventEntity;
import com._202510007517.platform.common.event.idempotency.ProcessedEventRepository;
import com._202510007517.platform.common.event.outbox.OutboxEventEntity;
import com._202510007517.platform.common.event.outbox.OutboxEventRepository;
import com._202510007517.platform.exam.api.feign.ExamFeignClient;
import com._202510007517.platform.events.EventAggregate;
import com._202510007517.platform.events.assignment.AssignmentSubmittedEvent;
import com._202510007517.platform.events.assignment.AssignmentSubmittedPayload;
import com._202510007517.platform.events.exam.ExamFinishedEvent;
import com._202510007517.platform.events.exam.ExamFinishedPayload;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AnalysisEventHandlerTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

    @Test
    void examFinishedUpdatesScoreTrendAndKnowledgeMastery() {
        InMemoryProcessedEventRepository processedEvents = new InMemoryProcessedEventRepository();
        InMemoryAnalysisRepository analysisRepository = new InMemoryAnalysisRepository();
        ExamFinishedAnalysisHandler handler = new ExamFinishedAnalysisHandler(
                new IdempotentEventHandler(processedEvents),
                analysisRepository,
                new InMemoryOutboxEventRepository(),
                OBJECT_MAPPER);

        handler.handle(examFinishedEvent("exam-finished-analysis-1", 85, 100));

        assertThat(analysisRepository.scoreTrends).hasSize(1);
        ScoreTrendRecord scoreTrend = analysisRepository.scoreTrends.get(0);
        assertThat(scoreTrend.studentId()).isEqualTo(42L);
        assertThat(scoreTrend.courseId()).isEqualTo(5001L);
        assertThat(scoreTrend.classId()).isEqualTo(6001L);
        assertThat(scoreTrend.sourceType()).isEqualTo("exam");
        assertThat(scoreTrend.sourceId()).isEqualTo(77L);
        assertThat(scoreTrend.submissionId()).isEqualTo(9001L);
        assertThat(scoreTrend.score()).isEqualTo(85);
        assertThat(scoreTrend.maxScore()).isEqualTo(100);
        assertThat(scoreTrend.scoreRate()).isEqualByComparingTo(new BigDecimal("0.8500"));

        assertThat(analysisRepository.masteries).hasSize(1);
        KnowledgeMasteryRecord mastery = analysisRepository.masteries.get(0);
        assertThat(mastery.studentId()).isEqualTo(42L);
        assertThat(mastery.courseId()).isEqualTo(5001L);
        assertThat(mastery.knowledgePointId()).isNull();
        assertThat(mastery.masteryScore()).isEqualByComparingTo(new BigDecimal("0.8500"));
        assertThat(mastery.evidenceCount()).isEqualTo(1);
        assertThat(mastery.lastEventId()).isEqualTo("exam-finished-analysis-1");
    }

    @Test
    void examFinishedIgnoresDuplicateEventId() {
        InMemoryProcessedEventRepository processedEvents = new InMemoryProcessedEventRepository();
        InMemoryAnalysisRepository analysisRepository = new InMemoryAnalysisRepository();
        InMemoryOutboxEventRepository outboxEventRepository = new InMemoryOutboxEventRepository();
        ExamFinishedAnalysisHandler handler = new ExamFinishedAnalysisHandler(
                new IdempotentEventHandler(processedEvents),
                analysisRepository,
                outboxEventRepository,
                OBJECT_MAPPER);
        ExamFinishedEvent event = examFinishedEvent("exam-finished-analysis-dup", 45, 100);

        handler.handle(event);
        handler.handle(event);

        assertThat(analysisRepository.scoreTrends).hasSize(1);
        assertThat(analysisRepository.masteries).hasSize(1);
        assertThat(outboxEventRepository.savedEvents).hasSize(1);
    }

    @Test
    void examFinishedWithLowScorePublishesEarlyWarningRaisedOutboxEvent() throws Exception {
        InMemoryProcessedEventRepository processedEvents = new InMemoryProcessedEventRepository();
        InMemoryAnalysisRepository analysisRepository = new InMemoryAnalysisRepository();
        InMemoryOutboxEventRepository outboxEventRepository = new InMemoryOutboxEventRepository();
        ExamFinishedAnalysisHandler handler = new ExamFinishedAnalysisHandler(
                new IdempotentEventHandler(processedEvents),
                analysisRepository,
                outboxEventRepository,
                OBJECT_MAPPER);

        handler.handle(examFinishedEvent("exam-finished-analysis-low-score", 45, 100));

        assertThat(outboxEventRepository.savedEvents).hasSize(1);
        OutboxEventEntity event = outboxEventRepository.savedEvents.get(0);
        assertThat(event.eventId()).isEqualTo("early-warning-exam-finished-analysis-low-score");
        assertThat(event.aggregateType()).isEqualTo("early_warning");
        assertThat(event.aggregateId()).startsWith("warning-");
        assertThat(event.eventType()).isEqualTo("EarlyWarningRaisedEvent");
        assertThat(event.bindingName()).isEqualTo("early.warning.raised");
        assertThat(event.status()).isZero();
        assertThat(event.retryCount()).isZero();

        JsonNode payload = OBJECT_MAPPER.readTree(event.payload());
        assertThat(payload.path("payload").path("warningId").asLong()).isPositive();
        assertThat(payload.path("payload").path("studentId").asLong()).isEqualTo(42L);
        assertThat(payload.path("payload").path("courseId").asLong()).isEqualTo(5001L);
        assertThat(payload.path("payload").path("warningType").asText()).isEqualTo("LOW_SCORE");
        assertThat(payload.path("payload").path("level").asText()).isEqualTo("HIGH");
        assertThat(payload.path("payload").path("title").asText()).isEqualTo("学情预警");
    }

    @Test
    void assignmentSubmittedUpdatesKnowledgeMasteryEvidence() {
        InMemoryProcessedEventRepository processedEvents = new InMemoryProcessedEventRepository();
        InMemoryAnalysisRepository analysisRepository = new InMemoryAnalysisRepository();
        AssignmentSubmittedAnalysisHandler handler = new AssignmentSubmittedAnalysisHandler(
                new IdempotentEventHandler(processedEvents),
                analysisRepository);

        handler.handle(new AssignmentSubmittedEvent(
                "assignment-submitted-analysis-1",
                Instant.parse("2026-05-19T12:00:00Z"),
                new EventAggregate("assignment_submission", "3001"),
                new AssignmentSubmittedPayload(
                        2001L,
                        3001L,
                        42L,
                        5001L,
                        6001L,
                        false,
                        Instant.parse("2026-05-19T11:59:00Z"))));

        assertThat(analysisRepository.scoreTrends).isEmpty();
        assertThat(analysisRepository.masteries).hasSize(1);
        KnowledgeMasteryRecord mastery = analysisRepository.masteries.get(0);
        assertThat(mastery.knowledgePointId()).isNull();
        assertThat(mastery.masteryScore()).isEqualByComparingTo(new BigDecimal("0.6000"));
        assertThat(mastery.lastSourceType()).isEqualTo("assignment");
        assertThat(mastery.lastSourceId()).isEqualTo(2001L);
    }

    @Test
    void assignmentSubmittedWritesMasteryPerAssignmentKnowledgePointWhenMapped() {
        InMemoryProcessedEventRepository processedEvents = new InMemoryProcessedEventRepository();
        InMemoryAnalysisRepository analysisRepository = new InMemoryAnalysisRepository();
        AssignmentFeignClient assignmentFeignClient = mock(AssignmentFeignClient.class);
        when(assignmentFeignClient.listKnowledgePointIds(2001L)).thenReturn(List.of(501L, 502L));
        AssignmentSubmittedAnalysisHandler handler = new AssignmentSubmittedAnalysisHandler(
                new IdempotentEventHandler(processedEvents),
                analysisRepository,
                assignmentFeignClient);

        handler.handle(new AssignmentSubmittedEvent(
                "assignment-submitted-analysis-kp",
                Instant.parse("2026-05-19T12:00:00Z"),
                new EventAggregate("assignment_submission", "3001"),
                new AssignmentSubmittedPayload(
                        2001L,
                        3001L,
                        42L,
                        5001L,
                        6001L,
                        false,
                        Instant.parse("2026-05-19T11:59:00Z"))));

        assertThat(analysisRepository.masteries)
                .extracting(KnowledgeMasteryRecord::knowledgePointId)
                .containsExactly(501L, 502L);
        assertThat(analysisRepository.masteries)
                .allSatisfy(mastery -> {
                    assertThat(mastery.masteryScore()).isEqualByComparingTo(new BigDecimal("0.6000"));
                    assertThat(mastery.lastSourceType()).isEqualTo("assignment");
                    assertThat(mastery.lastSourceId()).isEqualTo(2001L);
                });
    }

    @Test
    void examFinishedWritesMasteryPerExamKnowledgePointWhenMapped() {
        InMemoryProcessedEventRepository processedEvents = new InMemoryProcessedEventRepository();
        InMemoryAnalysisRepository analysisRepository = new InMemoryAnalysisRepository();
        ExamFeignClient examFeignClient = mock(ExamFeignClient.class);
        when(examFeignClient.listKnowledgePointIds(77L)).thenReturn(List.of(701L, 702L));
        ExamFinishedAnalysisHandler handler = new ExamFinishedAnalysisHandler(
                new IdempotentEventHandler(processedEvents),
                analysisRepository,
                new InMemoryOutboxEventRepository(),
                OBJECT_MAPPER,
                examFeignClient);

        handler.handle(examFinishedEvent("exam-finished-analysis-kp", 85, 100));

        assertThat(analysisRepository.scoreTrends).hasSize(1);
        assertThat(analysisRepository.masteries)
                .extracting(KnowledgeMasteryRecord::knowledgePointId)
                .containsExactly(701L, 702L);
        assertThat(analysisRepository.masteries)
                .allSatisfy(mastery -> {
                    assertThat(mastery.masteryScore()).isEqualByComparingTo(new BigDecimal("0.8500"));
                    assertThat(mastery.lastSourceType()).isEqualTo("exam");
                    assertThat(mastery.lastSourceId()).isEqualTo(77L);
                });
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void examFinishedConsumerAcceptsRawJsonPayload() {
        InMemoryProcessedEventRepository processedEvents = new InMemoryProcessedEventRepository();
        InMemoryAnalysisRepository analysisRepository = new InMemoryAnalysisRepository();
        ExamFinishedAnalysisHandler handler = new ExamFinishedAnalysisHandler(
                new IdempotentEventHandler(processedEvents),
                analysisRepository,
                new InMemoryOutboxEventRepository(),
                OBJECT_MAPPER);
        AnalysisConsumerConfiguration configuration = new AnalysisConsumerConfiguration();
        Consumer consumer = configuration.examFinishedAnalysisConsumer(handler, OBJECT_MAPPER);

        consumer.accept(examFinishedJson().getBytes(StandardCharsets.UTF_8));

        assertThat(analysisRepository.scoreTrends).hasSize(1);
        assertThat(analysisRepository.masteries).hasSize(1);
        assertThat(analysisRepository.scoreTrends.get(0).score()).isEqualTo(85);
    }

    private static ExamFinishedEvent examFinishedEvent(String eventId, Integer score, Integer maxScore) {
        return new ExamFinishedEvent(
                eventId,
                Instant.parse("2026-05-19T12:00:00Z"),
                new EventAggregate("exam_submission", "9001"),
                new ExamFinishedPayload(
                        77L,
                        9001L,
                        42L,
                        5001L,
                        6001L,
                        score,
                        maxScore,
                        Instant.parse("2026-05-19T11:59:00Z")));
    }

    private static String examFinishedJson() {
        return """
                {"eventId":"exam-finished-analysis-json","occurredAt":"2026-05-19T12:00:00Z","aggregate":{"type":"exam_submission","id":"9001"},"payload":{"examId":77,"submissionId":9001,"studentId":42,"courseId":5001,"classId":6001,"score":85,"maxScore":100,"finishedAt":"2026-05-19T11:59:00Z"}}
                """;
    }

    private static final class InMemoryProcessedEventRepository implements ProcessedEventRepository {

        private final Set<String> seenKeys = new HashSet<>();

        @Override
        public boolean insertIfAbsent(ProcessedEventEntity event) {
            return seenKeys.add(event.eventId() + "::" + event.consumerName());
        }

        @Override
        public void delete(String eventId, String consumerName) {
            seenKeys.remove(eventId + "::" + consumerName);
        }
    }

    private static final class InMemoryAnalysisRepository implements AnalysisRepository {

        private final List<ScoreTrendRecord> scoreTrends = new ArrayList<>();
        private final List<KnowledgeMasteryRecord> masteries = new ArrayList<>();

        @Override
        public void upsertScoreTrend(ScoreTrendRecord record) {
            scoreTrends.add(record);
        }

        @Override
        public void upsertKnowledgeMastery(KnowledgeMasteryRecord record) {
            masteries.add(record);
        }

        @Override
        public List<ScoreTrendDTO> listScoreTrends(Long classId, Long courseId, Instant since) {
            return List.of();
        }

        @Override
        public List<KnowledgeMasteryDTO> listKnowledgeMastery(Long studentId, Long courseId) {
            return List.of();
        }

        @Override
        public List<KnowledgeMasteryDTO> listKnowledgeMasteryByScope(Long classId, Long courseId) {
            return List.of();
        }

        @Override
        public java.util.Map<String, Object> getTeacherDashboard(
                Long teacherId,
                Long classId,
                Long courseId,
                String timeRange) {
            return java.util.Map.of();
        }

        @Override
        public java.util.Map<String, Object> getTeacherLearningSummary(
                Long teacherId,
                Long classId,
                Long courseId,
                String timeRange) {
            return java.util.Map.of();
        }

        @Override
        public List<java.util.Map<String, Object>> listStudentStudyTimeDistribution(
                Long studentId,
                String type,
                String semester,
                Long courseId,
                String timeRange) {
            return List.of();
        }

        @Override
        public java.util.Map<String, Object> getStudentLearningStats(
                Long studentId,
                String semester,
                Long courseId,
                String timeRange) {
            return java.util.Map.of();
        }

        @Override
        public List<java.util.Map<String, Object>> listStudentKnowledgePoints(
                Long studentId,
                String semester,
                Long courseId,
                String timeRange) {
            return List.of();
        }

        @Override
        public java.util.Map<String, Object> getStudentKnowledgePointDetail(Long studentId, Long knowledgePointId) {
            return java.util.Map.of();
        }
    }

    private static final class InMemoryOutboxEventRepository implements OutboxEventRepository {

        private final List<OutboxEventEntity> savedEvents = new ArrayList<>();

        @Override
        public void save(OutboxEventEntity event) {
            savedEvents.add(event);
        }

        @Override
        public List<OutboxEventEntity> findDuePendingEvents(int limit, Instant now) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void markPublished(long id, Instant publishedAt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void markFailed(long id, int retryCount, Instant nextRetryAt, String lastError, int maxRetries) {
            throw new UnsupportedOperationException();
        }
    }
}
