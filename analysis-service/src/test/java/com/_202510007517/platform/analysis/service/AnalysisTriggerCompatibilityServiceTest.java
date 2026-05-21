package com._202510007517.platform.analysis.service;

import com._202510007517.platform.analysis.api.dto.KnowledgeMasteryDTO;
import com._202510007517.platform.analysis.api.dto.ScoreTrendDTO;
import com._202510007517.platform.analysis.repository.AnalysisRepository;
import com._202510007517.platform.analysis.repository.AnalysisTriggerJob;
import com._202510007517.platform.analysis.repository.AnalysisTriggerJobRepository;
import com._202510007517.platform.analysis.repository.EarlyWarningRepository;
import com._202510007517.platform.analysis.repository.KnowledgeMasteryRecord;
import com._202510007517.platform.analysis.repository.ScoreTrendRecord;
import com._202510007517.platform.analysis.web.dto.EarlyWarningDTO;
import com._202510007517.platform.course.api.dto.CourseDTO;
import com._202510007517.platform.course.api.feign.CourseFeignClient;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AnalysisTriggerCompatibilityServiceTest {

    @Test
    void triggerClassAnalysisRecordsJobAndCreatesReadModelWarnings() {
        FakeAnalysisRepository analysisRepository = new FakeAnalysisRepository();
        analysisRepository.scoreRows.add(score(42L, 2L, 1L, 55));
        analysisRepository.scoreRows.add(score(43L, 2L, 1L, 82));
        analysisRepository.scoreRows.add(score(44L, 2L, 1L, 50));
        analysisRepository.masteryRows.add(mastery(43L, 2L, 1L, "0.4500"));
        FakeEarlyWarningRepository warningRepository = new FakeEarlyWarningRepository();
        warningRepository.pendingKeys.add(key(7L, 44L, 2L, "LOW_SCORE"));
        FakeAnalysisTriggerJobRepository jobRepository = new FakeAnalysisTriggerJobRepository();
        AnalysisTriggerCompatibilityService service = new AnalysisTriggerCompatibilityService(
                new TrackingAnalysisQueryService(analysisRepository),
                analysisRepository,
                warningRepository,
                jobRepository);

        int analyzedStudents = service.triggerClassAnalysis(7L, 1L, 2L);

        assertThat(analyzedStudents).isEqualTo(3);
        assertThat(warningRepository.createdWarnings)
                .extracting(warning -> warning.studentId() + ":" + warning.warningType())
                .containsExactly("42:LOW_SCORE", "43:PROGRESS");
        assertThat(jobRepository.createdJobs)
                .singleElement()
                .satisfies(job -> assertThat(job)
                        .extracting(AnalysisTriggerJob::triggerType, AnalysisTriggerJob::teacherId,
                                AnalysisTriggerJob::classId, AnalysisTriggerJob::courseId,
                                AnalysisTriggerJob::studentId)
                        .containsExactly("CLASS_ANALYSIS", 7L, 1L, 2L, null));
        assertThat(jobRepository.completedJobs)
                .singleElement()
                .satisfies(job -> assertThat(job)
                        .extracting(AnalysisTriggerJob::status, AnalysisTriggerJob::requestedCount,
                                AnalysisTriggerJob::warningCount, AnalysisTriggerJob::message)
                        .containsExactly("COMPLETED", 3, 2, "班级学情分析完成"));
    }

    @Test
    void triggerStudentAnalysisRecordsJobAndRunsKnowledgeAnalysis() {
        FakeAnalysisRepository analysisRepository = new FakeAnalysisRepository();
        analysisRepository.scoreRows.add(score(42L, 2L, 1L, 58));
        TrackingAnalysisQueryService queryService = new TrackingAnalysisQueryService(analysisRepository);
        FakeEarlyWarningRepository warningRepository = new FakeEarlyWarningRepository();
        FakeAnalysisTriggerJobRepository jobRepository = new FakeAnalysisTriggerJobRepository();
        AnalysisTriggerCompatibilityService service = new AnalysisTriggerCompatibilityService(
                queryService,
                analysisRepository,
                warningRepository,
                jobRepository);

        service.triggerStudentAnalysis(7L, 42L, 2L);

        assertThat(queryService.analyzedStudentId).isEqualTo(42L);
        assertThat(queryService.analyzedCourseId).isEqualTo(2L);
        assertThat(warningRepository.createdWarnings)
                .singleElement()
                .satisfies(warning -> assertThat(warning)
                        .extracting(EarlyWarningDTO::studentId, EarlyWarningDTO::courseId,
                                EarlyWarningDTO::warningType)
                        .containsExactly(42L, 2L, "LOW_SCORE"));
        assertThat(jobRepository.completedJobs)
                .singleElement()
                .satisfies(job -> assertThat(job)
                        .extracting(AnalysisTriggerJob::triggerType, AnalysisTriggerJob::status,
                                AnalysisTriggerJob::requestedCount, AnalysisTriggerJob::warningCount)
                        .containsExactly("STUDENT_ANALYSIS", "COMPLETED", 1, 1));
    }

    @Test
    void triggerStudentAnalysisRejectsUnownedCourseBeforeCreatingJob() {
        FakeAnalysisRepository analysisRepository = new FakeAnalysisRepository();
        TrackingAnalysisQueryService queryService = new TrackingAnalysisQueryService(analysisRepository);
        FakeEarlyWarningRepository warningRepository = new FakeEarlyWarningRepository();
        FakeAnalysisTriggerJobRepository jobRepository = new FakeAnalysisTriggerJobRepository();
        CourseFeignClient courseFeignClient = mock(CourseFeignClient.class);
        when(courseFeignClient.getCourse(2L)).thenReturn(course(2L, 99L));
        AnalysisTriggerCompatibilityService service = new AnalysisTriggerCompatibilityService(
                queryService,
                analysisRepository,
                warningRepository,
                jobRepository,
                courseFeignClient);

        assertThatThrownBy(() -> service.triggerStudentAnalysis(7L, 42L, 2L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("课程不存在或无权访问");

        assertThat(queryService.analyzedStudentId).isNull();
        assertThat(jobRepository.createdJobs).isEmpty();
        assertThat(warningRepository.createdWarnings).isEmpty();
    }

    @Test
    void triggerClassAnalysisRejectsClassOutsideTeacherScopeBeforeCreatingJob() {
        FakeAnalysisRepository analysisRepository = new FakeAnalysisRepository();
        FakeEarlyWarningRepository warningRepository = new FakeEarlyWarningRepository();
        FakeAnalysisTriggerJobRepository jobRepository = new FakeAnalysisTriggerJobRepository();
        CourseFeignClient courseFeignClient = mock(CourseFeignClient.class);
        when(courseFeignClient.getCourse(2L)).thenReturn(course(2L, 7L));
        when(courseFeignClient.listTeacherClasses(7L, null, null, null, null, 2L)).thenReturn(List.of());
        AnalysisTriggerCompatibilityService service = new AnalysisTriggerCompatibilityService(
                new TrackingAnalysisQueryService(analysisRepository),
                analysisRepository,
                warningRepository,
                jobRepository,
                courseFeignClient);

        assertThatThrownBy(() -> service.triggerClassAnalysis(7L, 1L, 2L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("班级不存在或无权访问");

        assertThat(jobRepository.createdJobs).isEmpty();
        assertThat(warningRepository.createdWarnings).isEmpty();
    }

    @Test
    void triggerStudentAnalysisContinuesWhenCourseServiceIsUnavailable() {
        FakeAnalysisRepository analysisRepository = new FakeAnalysisRepository();
        analysisRepository.scoreRows.add(score(42L, 2L, 1L, 58));
        TrackingAnalysisQueryService queryService = new TrackingAnalysisQueryService(analysisRepository);
        FakeEarlyWarningRepository warningRepository = new FakeEarlyWarningRepository();
        FakeAnalysisTriggerJobRepository jobRepository = new FakeAnalysisTriggerJobRepository();
        CourseFeignClient courseFeignClient = mock(CourseFeignClient.class);
        when(courseFeignClient.getCourse(2L)).thenThrow(new IllegalStateException("course-service down"));
        AnalysisTriggerCompatibilityService service = new AnalysisTriggerCompatibilityService(
                queryService,
                analysisRepository,
                warningRepository,
                jobRepository,
                courseFeignClient);

        service.triggerStudentAnalysis(7L, 42L, 2L);

        assertThat(queryService.analyzedStudentId).isEqualTo(42L);
        assertThat(jobRepository.createdJobs).hasSize(1);
        assertThat(warningRepository.createdWarnings).hasSize(1);
    }

    private static ScoreTrendDTO score(Long studentId, Long courseId, Long classId, int score) {
        return new ScoreTrendDTO(
                studentId,
                courseId,
                classId,
                "exam",
                studentId + 1000,
                studentId + 2000,
                score,
                100,
                BigDecimal.valueOf(score).movePointLeft(2),
                Instant.parse("2026-05-20T08:00:00Z"));
    }

    private static KnowledgeMasteryDTO mastery(Long studentId, Long courseId, Long classId, String score) {
        return new KnowledgeMasteryDTO(
                studentId,
                courseId,
                classId,
                null,
                new BigDecimal(score),
                2,
                "exam",
                studentId + 3000,
                "event-" + studentId,
                Instant.parse("2026-05-20T08:00:00Z"));
    }

    private static String key(Long teacherId, Long studentId, Long courseId, String warningType) {
        return teacherId + ":" + studentId + ":" + courseId + ":" + warningType;
    }

    private static CourseDTO course(Long courseId, Long teacherId) {
        CourseDTO course = new CourseDTO();
        course.setId(courseId);
        course.setTeacherId(teacherId);
        course.setCourseName("课程 " + courseId);
        return course;
    }

    private static final class TrackingAnalysisQueryService extends AnalysisQueryService {

        private Long analyzedStudentId;
        private Long analyzedCourseId;

        private TrackingAnalysisQueryService(AnalysisRepository analysisRepository) {
            super(analysisRepository);
        }

        @Override
        public void analyzeStudentKnowledgeMastery(Long teacherId, Long studentId, Long courseId) {
            super.analyzeStudentKnowledgeMastery(teacherId, studentId, courseId);
            this.analyzedStudentId = studentId;
            this.analyzedCourseId = courseId;
        }
    }

    private static final class FakeAnalysisRepository implements AnalysisRepository {

        private final List<ScoreTrendDTO> scoreRows = new ArrayList<>();
        private final List<KnowledgeMasteryDTO> masteryRows = new ArrayList<>();

        @Override
        public void upsertScoreTrend(ScoreTrendRecord record) {
        }

        @Override
        public void upsertKnowledgeMastery(KnowledgeMasteryRecord record) {
        }

        @Override
        public List<ScoreTrendDTO> listScoreTrends(Long classId, Long courseId, Instant since) {
            return scoreRows.stream()
                    .filter(row -> classId == null || classId.equals(row.classId()))
                    .filter(row -> courseId == null || courseId.equals(row.courseId()))
                    .toList();
        }

        @Override
        public List<KnowledgeMasteryDTO> listKnowledgeMastery(Long studentId, Long courseId) {
            return masteryRows.stream()
                    .filter(row -> studentId == null || studentId.equals(row.studentId()))
                    .filter(row -> courseId == null || courseId.equals(row.courseId()))
                    .toList();
        }

        @Override
        public List<KnowledgeMasteryDTO> listKnowledgeMasteryByScope(Long classId, Long courseId) {
            return masteryRows.stream()
                    .filter(row -> classId == null || classId.equals(row.classId()))
                    .filter(row -> courseId == null || courseId.equals(row.courseId()))
                    .toList();
        }

        @Override
        public Map<String, Object> getTeacherDashboard(Long teacherId, Long classId, Long courseId, String timeRange) {
            return Map.of();
        }

        @Override
        public Map<String, Object> getTeacherLearningSummary(Long teacherId, Long classId, Long courseId, String timeRange) {
            return Map.of();
        }

        @Override
        public List<Map<String, Object>> listStudentStudyTimeDistribution(
                Long studentId,
                String type,
                String semester,
                Long courseId,
                String timeRange) {
            return List.of();
        }

        @Override
        public Map<String, Object> getStudentLearningStats(
                Long studentId,
                String semester,
                Long courseId,
                String timeRange) {
            return Map.of();
        }

        @Override
        public List<Map<String, Object>> listStudentKnowledgePoints(
                Long studentId,
                String semester,
                Long courseId,
                String timeRange) {
            return List.of();
        }

        @Override
        public Map<String, Object> getStudentKnowledgePointDetail(Long studentId, Long knowledgePointId) {
            return Map.of();
        }
    }

    private static final class FakeEarlyWarningRepository implements EarlyWarningRepository {

        private final Set<String> pendingKeys = new LinkedHashSet<>();
        private final List<EarlyWarningDTO> createdWarnings = new ArrayList<>();

        @Override
        public boolean existsPendingWarning(Long teacherId, Long studentId, Long courseId, String warningType) {
            return pendingKeys.contains(key(teacherId, studentId, courseId, warningType));
        }

        @Override
        public EarlyWarningDTO insert(
                Long teacherId,
                Long studentId,
                Long courseId,
                String warningType,
                String warningLevel,
                String warningMessage,
                String assessmentType,
                Long relatedAssessmentId) {
            EarlyWarningDTO warning = new EarlyWarningDTO(
                    (long) createdWarnings.size() + 1,
                    studentId,
                    courseId,
                    teacherId,
                    warningType,
                    warningLevel,
                    warningMessage,
                    Instant.parse("2026-05-20T08:00:00Z"),
                    false,
                    null,
                    null,
                    null,
                    assessmentType,
                    relatedAssessmentId,
                    "学生 " + studentId,
                    "课程 " + courseId,
                    "pending",
                    warningMessage,
                    "建议安排课后辅导，重点讲解薄弱知识点");
            pendingKeys.add(key(teacherId, studentId, courseId, warningType));
            createdWarnings.add(warning);
            return warning;
        }

        @Override
        public List<EarlyWarningDTO> findUnresolvedByTeacherId(Long teacherId) {
            return List.of();
        }

        @Override
        public List<EarlyWarningDTO> findByStudentId(Long studentId) {
            return List.of();
        }

        @Override
        public List<EarlyWarningDTO> findWarningsByCondition(
                Long teacherId,
                Long classId,
                Long courseId,
                String warningType,
                String status,
                int offset,
                int size) {
            return List.of();
        }

        @Override
        public long countWarningsByCondition(Long teacherId, Long classId, Long courseId, String warningType, String status) {
            return 0;
        }

        @Override
        public long countTotalWarnings(Long teacherId, Long classId, Long courseId) {
            return 0;
        }

        @Override
        public long countPendingWarnings(Long teacherId, Long classId, Long courseId) {
            return 0;
        }

        @Override
        public long countWarningsByType(Long teacherId, String warningType, Long classId, Long courseId) {
            return 0;
        }

        @Override
        public EarlyWarningDTO findWarningById(Long teacherId, Long warningId) {
            return null;
        }

        @Override
        public List<EarlyWarningDTO> findWarningsForExport(
                Long teacherId,
                Long classId,
                Long courseId,
                String warningType,
                String status) {
            return List.of();
        }

        @Override
        public List<EarlyWarningDTO> findByCourseId(
                Long teacherId,
                Long courseId,
                String warningType,
                String warningLevel,
                Boolean isResolved) {
            return List.of();
        }

        @Override
        public boolean updateWarningStatus(Long teacherId, Long warningId, String status, String resolvedNote) {
            return false;
        }

        @Override
        public boolean deleteById(Long teacherId, Long warningId) {
            return false;
        }
    }

    private static final class FakeAnalysisTriggerJobRepository implements AnalysisTriggerJobRepository {

        private final List<AnalysisTriggerJob> createdJobs = new ArrayList<>();
        private final List<AnalysisTriggerJob> completedJobs = new ArrayList<>();

        @Override
        public AnalysisTriggerJob create(
                Long teacherId,
                String triggerType,
                Long classId,
                Long courseId,
                Long studentId) {
            AnalysisTriggerJob job = new AnalysisTriggerJob(
                    (long) createdJobs.size() + 1,
                    teacherId,
                    triggerType,
                    classId,
                    courseId,
                    studentId,
                    "RUNNING",
                    0,
                    0,
                    null,
                    Instant.parse("2026-05-20T08:00:00Z"),
                    null);
            createdJobs.add(job);
            return job;
        }

        @Override
        public AnalysisTriggerJob complete(
                Long jobId,
                String status,
                int requestedCount,
                int warningCount,
                String message) {
            AnalysisTriggerJob created = createdJobs.stream()
                    .filter(job -> job.id().equals(jobId))
                    .findFirst()
                    .orElseThrow();
            AnalysisTriggerJob completed = new AnalysisTriggerJob(
                    created.id(),
                    created.teacherId(),
                    created.triggerType(),
                    created.classId(),
                    created.courseId(),
                    created.studentId(),
                    status,
                    requestedCount,
                    warningCount,
                    message,
                    created.createdAt(),
                    Instant.parse("2026-05-20T08:00:01Z"));
            completedJobs.add(completed);
            return completed;
        }
    }
}
