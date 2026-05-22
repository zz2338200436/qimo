package com._202510007517.platform.analysis.repository;

import com._202510007517.platform.analysis.api.dto.KnowledgeMasteryDTO;
import com._202510007517.platform.analysis.api.dto.ScoreTrendDTO;
import com._202510007517.platform.common.exception.ResourceNotFoundException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Repository
public class JpaAnalysisRepository implements AnalysisRepository {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DAY_LABEL_FORMATTER = DateTimeFormatter.ofPattern("MM-dd");
    private static final long COURSE_LEVEL_KNOWLEDGE_POINT_ID = 0L;

    private final ScoreTrendJpaRepository scoreTrendJpaRepository;
    private final KnowledgeMasteryJpaRepository knowledgeMasteryJpaRepository;
    private Clock clock = Clock.systemDefaultZone();

    public JpaAnalysisRepository(
            ScoreTrendJpaRepository scoreTrendJpaRepository,
            KnowledgeMasteryJpaRepository knowledgeMasteryJpaRepository) {
        this.scoreTrendJpaRepository = scoreTrendJpaRepository;
        this.knowledgeMasteryJpaRepository = knowledgeMasteryJpaRepository;
    }

    void setClock(Clock clock) {
        this.clock = clock;
    }

    @Override
    @Transactional
    public void upsertScoreTrend(ScoreTrendRecord record) {
        ScoreTrendEntity entity = scoreTrendJpaRepository.findBySourceTypeAndSubmissionId(
                        record.sourceType(),
                        record.submissionId())
                .orElseGet(ScoreTrendEntity::new);
        entity.setStudentId(record.studentId());
        entity.setCourseId(record.courseId());
        entity.setClassId(record.classId());
        entity.setSourceType(record.sourceType());
        entity.setSourceId(record.sourceId());
        entity.setSubmissionId(record.submissionId());
        entity.setScore(record.score());
        entity.setMaxScore(record.maxScore());
        entity.setScoreRate(record.scoreRate());
        entity.setOccurredAt(record.occurredAt());
        scoreTrendJpaRepository.save(entity);
    }

    @Override
    @Transactional
    public void upsertKnowledgeMastery(KnowledgeMasteryRecord record) {
        long storedKnowledgePointId = toStoredKnowledgePointId(record.knowledgePointId());
        KnowledgeMasteryEntity entity = knowledgeMasteryJpaRepository.findByStudentIdAndCourseIdAndKnowledgePointId(
                        record.studentId(),
                        record.courseId(),
                        storedKnowledgePointId)
                .orElseGet(KnowledgeMasteryEntity::new);

        if (entity.getId() == null) {
            entity.setStudentId(record.studentId());
            entity.setCourseId(record.courseId());
            entity.setKnowledgePointId(storedKnowledgePointId);
            entity.setMasteryScore(record.masteryScore());
            entity.setEvidenceCount(record.evidenceCount());
        } else {
            BigDecimal oldWeighted = entity.getMasteryScore()
                    .multiply(BigDecimal.valueOf(entity.getEvidenceCount()));
            BigDecimal nextWeighted = oldWeighted.add(record.masteryScore());
            int nextEvidenceCount = entity.getEvidenceCount() + 1;
            entity.setMasteryScore(nextWeighted
                    .divide(BigDecimal.valueOf(nextEvidenceCount), 4, RoundingMode.HALF_UP));
            entity.setEvidenceCount(nextEvidenceCount);
        }

        entity.setClassId(record.classId());
        entity.setLastSourceType(record.lastSourceType());
        entity.setLastSourceId(record.lastSourceId());
        entity.setLastEventId(record.lastEventId());
        entity.setUpdatedAt(record.updatedAt());
        knowledgeMasteryJpaRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScoreTrendDTO> listScoreTrends(Long classId, Long courseId, Instant since) {
        return scoreTrendJpaRepository.findAll(scoreTrendSpec(classId, courseId, since)).stream()
                .sorted((left, right) -> {
                    int timeCompare = left.getOccurredAt().compareTo(right.getOccurredAt());
                    if (timeCompare != 0) {
                        return timeCompare;
                    }
                    return compareNullable(left.getId(), right.getId());
                })
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<KnowledgeMasteryDTO> listKnowledgeMastery(Long studentId, Long courseId) {
        return knowledgeMasteryJpaRepository.findAll((root, query, cb) -> cb.and(
                        cb.equal(root.get("studentId"), studentId),
                        cb.equal(root.get("courseId"), courseId)))
                .stream()
                .sorted((left, right) -> {
                    int updatedCompare = right.getUpdatedAt().compareTo(left.getUpdatedAt());
                    if (updatedCompare != 0) {
                        return updatedCompare;
                    }
                    int kpCompare = compareNullable(left.getKnowledgePointId(), right.getKnowledgePointId());
                    if (kpCompare != 0) {
                        return kpCompare;
                    }
                    return compareNullable(right.getId(), left.getId());
                })
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<KnowledgeMasteryDTO> listKnowledgeMasteryByScope(Long classId, Long courseId) {
        return knowledgeMasteryJpaRepository.findAll(knowledgeMasteryScopeSpec(classId, courseId)).stream()
                .sorted((left, right) -> {
                    int updatedCompare = right.getUpdatedAt().compareTo(left.getUpdatedAt());
                    if (updatedCompare != 0) {
                        return updatedCompare;
                    }
                    int kpCompare = compareNullable(left.getKnowledgePointId(), right.getKnowledgePointId());
                    if (kpCompare != 0) {
                        return kpCompare;
                    }
                    return compareNullable(right.getId(), left.getId());
                })
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getTeacherDashboard(Long teacherId, Long classId, Long courseId, String timeRange) {
        List<ScoreTrendDTO> scoreRows = listScoreTrends(classId, courseId, resolveSince(timeRange));
        List<ScoreTrendDTO> scopedScores = scoreRows.stream()
                .filter(row -> courseId == null || courseId.equals(row.courseId()))
                .filter(row -> classId == null || classId.equals(row.classId()))
                .toList();
        List<Map<String, Object>> masteryRows = readTeacherMasteryRows(classId, courseId);

        List<Long> courseIds = scopedScores.stream()
                .map(ScoreTrendDTO::courseId)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
        List<Long> studentIds = scopedScores.stream()
                .map(ScoreTrendDTO::studentId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, Double> averageScoreByCourse = new LinkedHashMap<>();
        for (Long rowCourseId : courseIds) {
            double average = scopedScores.stream()
                    .filter(row -> rowCourseId.equals(row.courseId()))
                    .filter(row -> row.score() != null)
                    .mapToInt(ScoreTrendDTO::score)
                    .average()
                    .orElse(0.0);
            averageScoreByCourse.put(rowCourseId, roundOne(average));
        }

        List<Map<String, Object>> recentActivities = scopedScores.stream()
                .sorted((left, right) -> right.occurredAt().compareTo(left.occurredAt()))
                .limit(5)
                .map(row -> {
                    Map<String, Object> activity = new LinkedHashMap<>();
                    activity.put("activityType", "成绩记录");
                    activity.put("studentName", "学生 " + row.studentId());
                    activity.put("studentId", row.studentId());
                    activity.put("activityDate", row.occurredAt().toString());
                    activity.put("details", "学生 " + row.studentId() + " 完成了 " + row.sourceType());
                    return activity;
                })
                .toList();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalCourses", courseIds.size());
        response.put("totalCoursesChange", 0);
        response.put("totalStudents", studentIds.size());
        response.put("totalStudentsChange", 0);
        response.put("pendingAssignments", 0);
        response.put("pendingAssignmentsChange", 0);
        response.put("pendingExams", 0);
        response.put("pendingExamsChange", 0);
        response.put("missingSubmissions", 0);
        response.put("missingSubmissionsChange", 0);
        response.put("upcomingDeadlines", 0);
        response.put("upcomingDeadlinesChange", 0);
        response.put("warningCount", 0);
        response.put("warningCountChange", 0);
        response.put("courseNames", averageScoreByCourse.keySet().stream().map(id -> "课程 " + id).toList());
        response.put("averageScores", List.copyOf(averageScoreByCourse.values()));
        response.put("submissionRateDays", List.of(LocalDate.now(clock).format(DAY_LABEL_FORMATTER)));
        response.put("submissionRates", List.of(scopedScores.isEmpty() ? 0 : 100));
        response.put("recentActivities", recentActivities);
        response.put("knowledgeEvidenceCount", masteryRows.stream()
                .mapToInt(row -> ((Number) row.get("evidenceCount")).intValue())
                .sum());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getTeacherLearningSummary(Long teacherId, Long classId, Long courseId, String timeRange) {
        List<ScoreTrendDTO> scoreRows = listScoreTrends(classId, courseId, resolveSince(timeRange));
        List<ScoreTrendDTO> scopedScores = scoreRows.stream()
                .filter(row -> courseId == null || courseId.equals(row.courseId()))
                .filter(row -> classId == null || classId.equals(row.classId()))
                .toList();
        List<Map<String, Object>> masteryRows = readTeacherMasteryRows(classId, courseId);

        List<Long> studentIds = java.util.stream.Stream.concat(
                        scopedScores.stream().map(ScoreTrendDTO::studentId),
                        masteryRows.stream().map(row -> ((Number) row.get("studentId")).longValue()))
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();

        Map<Long, Double> masteryByStudent = new LinkedHashMap<>();
        for (Map<String, Object> row : masteryRows) {
            Long studentId = ((Number) row.get("studentId")).longValue();
            masteryByStudent.merge(studentId, ((Number) row.get("mastery")).doubleValue(), (left, right) -> (left + right) / 2.0);
        }

        List<Map<String, Object>> performances = new ArrayList<>();
        double scoreTotal = 0.0;
        int scoreCount = 0;
        double progressTotal = 0.0;
        int progressCount = 0;
        for (Long studentId : studentIds) {
            List<ScoreTrendDTO> studentScores = scopedScores.stream()
                    .filter(row -> studentId.equals(row.studentId()))
                    .filter(row -> row.score() != null)
                    .toList();
            double averageScore = studentScores.stream()
                    .mapToInt(ScoreTrendDTO::score)
                    .average()
                    .orElse(0.0);
            double progress = roundOne(masteryByStudent.getOrDefault(studentId, 0.0));
            if (!studentScores.isEmpty()) {
                scoreTotal += averageScore;
                scoreCount++;
            }
            if (progress > 0.0) {
                progressTotal += progress;
                progressCount++;
            }

            Map<String, Object> performance = new LinkedHashMap<>();
            performance.put("studentId", studentId);
            performance.put("realName", "学生 " + studentId);
            performance.put("className", classId == null ? "未知" : "班级 " + classId);
            performance.put("courseName", courseId == null ? "所有课程" : "课程 " + courseId);
            performance.put("averageScore", (int) Math.round(averageScore));
            performance.put("pendingAssignments", 0);
            performance.put("overallProgress", (int) Math.round(progress));
            performance.put("status", resolveStudentStatus(averageScore, progress));
            performances.add(performance);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalStudents", studentIds.size());
        response.put("averageScore", scoreCount == 0 ? 0.0 : roundOne(scoreTotal / scoreCount));
        response.put("totalPendingAssignments", 0);
        response.put("overallProgress", progressCount == 0 ? 0.0 : roundOne(progressTotal / progressCount));
        response.put("studentPerformances", performances);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listStudentStudyTimeDistribution(
            Long studentId,
            String type,
            String semester,
            Long courseId,
            String timeRange) {
        String normalizedType = normalizeStudyTimeType(type);
        LocalDate today = LocalDate.now(clock);
        return "weekly".equals(normalizedType)
                ? listWeeklyStudyTime(studentId, courseId, today)
                : listDailyStudyTime(studentId, courseId, today);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getStudentLearningStats(
            Long studentId,
            String semester,
            Long courseId,
            String timeRange) {
        List<Map<String, Object>> studyTimeRows = listStudentStudyTimeDistribution(
                studentId,
                "daily",
                semester,
                courseId,
                timeRange);
        List<Double> studyTimeDistribution = new ArrayList<>();
        double totalStudyTime = 0.0;
        for (Map<String, Object> row : studyTimeRows) {
            double hours = numberValue(row.get("study_time"));
            studyTimeDistribution.add(hours);
            totalStudyTime += hours;
        }

        TaskScoreSummary scoreSummary = readTaskScoreSummary(studentId, courseId);
        List<Map<String, Object>> knowledgePoints = readKnowledgePointStats(studentId, courseId);
        double knowledgeMastery = averageKnowledgeMastery(knowledgePoints);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("studyTime", roundOne(totalStudyTime));
        response.put("studyTimeChange", 0);
        response.put("studyTimeDistribution", studyTimeDistribution);
        response.put("completedTasks", scoreSummary.completedTasks());
        response.put("completedTasksChange", 0);
        response.put("averageScore", roundOne(scoreSummary.averageScore()));
        response.put("averageScoreChange", 0.0);
        response.put("knowledgeMastery", roundOne(knowledgeMastery));
        response.put("knowledgeMasteryChange", 0.0);
        response.put("knowledgePoints", knowledgePoints);
        response.put("studyPlan", Map.of(
                "completionPercentage", 0,
                "items", List.of()));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listStudentKnowledgePoints(
            Long studentId,
            String semester,
            Long courseId,
            String timeRange) {
        return readKnowledgePointStats(studentId, courseId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getStudentKnowledgePointDetail(Long studentId, Long knowledgePointId) {
        return readKnowledgePointStats(studentId, knowledgePointId).stream()
                .filter(point -> knowledgePointId.equals(point.get("id")))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("知识点不存在"));
    }

    private List<Map<String, Object>> listDailyStudyTime(Long studentId, Long courseId, LocalDate today) {
        LocalDate startDate = today.minusDays(6);
        Map<String, Double> hoursByDate = new LinkedHashMap<>();
        for (int dayOffset = 0; dayOffset < 7; dayOffset++) {
            LocalDate date = startDate.plusDays(dayOffset);
            hoursByDate.put(date.format(DATE_FORMATTER), 0.0);
        }

        mergeScoreTrendHours(hoursByDate, studentId, courseId, startDate, today.plusDays(1), false);
        mergeKnowledgeMasteryHours(hoursByDate, studentId, courseId, startDate, today.plusDays(1), false);

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map.Entry<String, Double> entry : hoursByDate.entrySet()) {
            LocalDate date = LocalDate.parse(entry.getKey(), DATE_FORMATTER);
            rows.add(studyTimeRow(entry.getKey(), date.format(DAY_LABEL_FORMATTER), entry.getValue()));
        }
        return rows;
    }

    private List<Map<String, Object>> listWeeklyStudyTime(Long studentId, Long courseId, LocalDate today) {
        LocalDate currentWeekStart = today.with(DayOfWeek.MONDAY);
        LocalDate startDate = currentWeekStart.minusWeeks(3);
        Map<String, Double> hoursByWeek = new LinkedHashMap<>();
        for (int weekIndex = 1; weekIndex <= 4; weekIndex++) {
            hoursByWeek.put("第" + weekIndex + "周", 0.0);
        }

        mergeScoreTrendHours(hoursByWeek, studentId, courseId, startDate, currentWeekStart.plusWeeks(1), true);
        mergeKnowledgeMasteryHours(hoursByWeek, studentId, courseId, startDate, currentWeekStart.plusWeeks(1), true);

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map.Entry<String, Double> entry : hoursByWeek.entrySet()) {
            rows.add(studyTimeRow(entry.getKey(), entry.getKey(), entry.getValue()));
        }
        return rows;
    }

    private TaskScoreSummary readTaskScoreSummary(Long studentId, Long courseId) {
        List<ScoreTrendEntity> rows = scoreTrendJpaRepository.findAll((root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("studentId"), studentId));
            predicates.add(cb.isNotNull(root.get("score")));
            if (courseId != null) {
                predicates.add(cb.equal(root.get("courseId"), courseId));
            }
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        });

        int completedTasks = (int) rows.stream()
                .map(ScoreTrendEntity::getSubmissionId)
                .filter(Objects::nonNull)
                .distinct()
                .count();
        double averageScore = rows.stream()
                .map(ScoreTrendEntity::getScore)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);
        return new TaskScoreSummary(completedTasks, averageScore);
    }

    private List<Map<String, Object>> readKnowledgePointStats(Long studentId, Long courseId) {
        return knowledgeMasteryJpaRepository.findAll((root, query, cb) -> {
                    List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
                    predicates.add(cb.equal(root.get("studentId"), studentId));
                    if (courseId != null) {
                        predicates.add(cb.equal(root.get("courseId"), courseId));
                    }
                    return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
                }).stream()
                .sorted((left, right) -> {
                    int updatedCompare = right.getUpdatedAt().compareTo(left.getUpdatedAt());
                    if (updatedCompare != 0) {
                        return updatedCompare;
                    }
                    return compareNullable(right.getId(), left.getId());
                })
                .map(entity -> {
                    Long rowCourseId = entity.getCourseId();
                    Long knowledgePointId = fromStoredKnowledgePointId(entity.getKnowledgePointId());
                    Long pointId = knowledgePointId == null ? rowCourseId : knowledgePointId;
                    String pointName = knowledgePointId == null ? "课程 " + rowCourseId : "知识点 " + knowledgePointId;
                    double mastery = roundOne(entity.getMasteryScore().doubleValue() * 100.0);
                    Map<String, Object> point = new LinkedHashMap<>();
                    point.put("id", pointId);
                    point.put("courseId", rowCourseId);
                    point.put("knowledgePointId", knowledgePointId);
                    point.put("name", pointName);
                    point.put("pointName", pointName);
                    point.put("description", knowledgePointId == null
                            ? pointName + " 的知识点掌握汇总"
                            : pointName + " 的掌握汇总");
                    point.put("difficulty", mastery >= 80.0 ? "中等" : "困难");
                    point.put("courseName", "课程 " + rowCourseId);
                    point.put("mastery", mastery);
                    point.put("practiceCount", entity.getEvidenceCount());
                    return point;
                })
                .toList();
    }

    private List<Map<String, Object>> readTeacherMasteryRows(Long classId, Long courseId) {
        return knowledgeMasteryJpaRepository.findAll(knowledgeMasteryScopeSpec(classId, courseId)).stream()
                .sorted((left, right) -> {
                    int updatedCompare = right.getUpdatedAt().compareTo(left.getUpdatedAt());
                    if (updatedCompare != 0) {
                        return updatedCompare;
                    }
                    return compareNullable(right.getId(), left.getId());
                })
                .map(entity -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("studentId", entity.getStudentId());
                    row.put("courseId", entity.getCourseId());
                    row.put("classId", entity.getClassId());
                    row.put("knowledgePointId", fromStoredKnowledgePointId(entity.getKnowledgePointId()));
                    row.put("mastery", roundOne(entity.getMasteryScore().doubleValue() * 100.0));
                    row.put("evidenceCount", entity.getEvidenceCount());
                    row.put("updatedAt", entity.getUpdatedAt());
                    return row;
                })
                .toList();
    }

    private Specification<ScoreTrendEntity> scoreTrendSpec(Long classId, Long courseId, Instant since) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (classId != null) {
                predicates.add(cb.equal(root.get("classId"), classId));
            }
            if (courseId != null) {
                predicates.add(cb.equal(root.get("courseId"), courseId));
            }
            if (since != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), since));
            }
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private Specification<KnowledgeMasteryEntity> knowledgeMasteryScopeSpec(Long classId, Long courseId) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (classId != null) {
                predicates.add(cb.equal(root.get("classId"), classId));
            }
            if (courseId != null) {
                predicates.add(cb.equal(root.get("courseId"), courseId));
            }
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private static Instant resolveSince(String timeRange) {
        if (timeRange == null || timeRange.isBlank() || "all".equalsIgnoreCase(timeRange)) {
            return null;
        }
        String normalized = timeRange.trim().toLowerCase(Locale.ROOT);
        Instant now = Instant.now();
        return switch (normalized) {
            case "7d", "week" -> now.minus(java.time.Duration.ofDays(7));
            case "30d", "month" -> now.minus(java.time.Duration.ofDays(30));
            case "90d", "quarter" -> now.minus(java.time.Duration.ofDays(90));
            default -> null;
        };
    }

    private static String resolveStudentStatus(double averageScore, double progress) {
        if (averageScore >= 85.0 || progress >= 75.0) {
            return "良好";
        }
        if (averageScore < 60.0 || progress < 50.0) {
            return "需关注";
        }
        return "一般";
    }

    private static double averageKnowledgeMastery(List<Map<String, Object>> knowledgePoints) {
        if (knowledgePoints.isEmpty()) {
            return 0.0;
        }
        double total = 0.0;
        int count = 0;
        for (Map<String, Object> point : knowledgePoints) {
            double mastery = numberValue(point.get("mastery"));
            if (mastery > 0.0) {
                total += mastery;
                count++;
            }
        }
        return count == 0 ? 0.0 : total / count;
    }

    private void mergeScoreTrendHours(
            Map<String, Double> buckets,
            Long studentId,
            Long courseId,
            LocalDate startDate,
            LocalDate endDateExclusive,
            boolean weekly) {
        Instant start = startDate.atStartOfDay(clock.getZone()).toInstant();
        Instant end = endDateExclusive.atStartOfDay(clock.getZone()).toInstant();
        List<ScoreTrendEntity> rows = scoreTrendJpaRepository.findAll((root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("studentId"), studentId));
            predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), start));
            predicates.add(cb.lessThan(root.get("occurredAt"), end));
            if (courseId != null) {
                predicates.add(cb.equal(root.get("courseId"), courseId));
            }
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        });

        for (ScoreTrendEntity entity : rows) {
            LocalDate date = LocalDate.ofInstant(entity.getOccurredAt(), clock.getZone());
            String bucket = studyTimeBucket(date, startDate, weekly);
            if (bucket != null) {
                buckets.merge(bucket, scoreTrendHours(entity.getSourceType()), Double::sum);
            }
        }
    }

    private void mergeKnowledgeMasteryHours(
            Map<String, Double> buckets,
            Long studentId,
            Long courseId,
            LocalDate startDate,
            LocalDate endDateExclusive,
            boolean weekly) {
        Instant start = startDate.atStartOfDay(clock.getZone()).toInstant();
        Instant end = endDateExclusive.atStartOfDay(clock.getZone()).toInstant();
        List<KnowledgeMasteryEntity> rows = knowledgeMasteryJpaRepository.findAll((root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("studentId"), studentId));
            predicates.add(cb.greaterThanOrEqualTo(root.get("updatedAt"), start));
            predicates.add(cb.lessThan(root.get("updatedAt"), end));
            if (courseId != null) {
                predicates.add(cb.equal(root.get("courseId"), courseId));
            }
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        });

        for (KnowledgeMasteryEntity entity : rows) {
            LocalDate date = LocalDate.ofInstant(entity.getUpdatedAt(), clock.getZone());
            String bucket = studyTimeBucket(date, startDate, weekly);
            if (bucket != null) {
                double cap = weekly ? 56.0 : 8.0;
                double hours = Math.min(entity.getEvidenceCount() * 0.5, cap);
                buckets.merge(bucket, hours, Double::sum);
            }
        }
    }

    private ScoreTrendDTO toDto(ScoreTrendEntity entity) {
        return new ScoreTrendDTO(
                entity.getStudentId(),
                entity.getCourseId(),
                entity.getClassId(),
                entity.getSourceType(),
                entity.getSourceId(),
                entity.getSubmissionId(),
                entity.getScore(),
                entity.getMaxScore(),
                entity.getScoreRate(),
                entity.getOccurredAt());
    }

    private KnowledgeMasteryDTO toDto(KnowledgeMasteryEntity entity) {
        return new KnowledgeMasteryDTO(
                entity.getStudentId(),
                entity.getCourseId(),
                entity.getClassId(),
                fromStoredKnowledgePointId(entity.getKnowledgePointId()),
                entity.getMasteryScore(),
                entity.getEvidenceCount(),
                entity.getLastSourceType(),
                entity.getLastSourceId(),
                entity.getLastEventId(),
                entity.getUpdatedAt());
    }

    private static String studyTimeBucket(LocalDate date, LocalDate startDate, boolean weekly) {
        if (!weekly) {
            return date.format(DATE_FORMATTER);
        }
        long weekOffset = ChronoUnit.WEEKS.between(startDate, date.with(DayOfWeek.MONDAY));
        if (weekOffset < 0 || weekOffset > 3) {
            return null;
        }
        return "第" + (weekOffset + 1) + "周";
    }

    private static Map<String, Object> studyTimeRow(String date, String label, double hours) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("date", date);
        row.put("label", label);
        row.put("study_time", hours);
        row.put("hours", hours);
        return row;
    }

    private static String normalizeStudyTimeType(String type) {
        if (type == null || type.isBlank()) {
            return "daily";
        }
        return "weekly".equals(type.trim().toLowerCase(Locale.ROOT)) ? "weekly" : "daily";
    }

    private static double scoreTrendHours(String sourceType) {
        return "exam".equalsIgnoreCase(sourceType) ? 2.0 : 1.0;
    }

    private static double numberValue(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value == null) {
            return 0.0;
        }
        return Double.parseDouble(value.toString());
    }

    private static double roundOne(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private static long toStoredKnowledgePointId(Long knowledgePointId) {
        return knowledgePointId == null ? COURSE_LEVEL_KNOWLEDGE_POINT_ID : knowledgePointId;
    }

    private static Long fromStoredKnowledgePointId(Long knowledgePointId) {
        return knowledgePointId == null || knowledgePointId == COURSE_LEVEL_KNOWLEDGE_POINT_ID
                ? null
                : knowledgePointId;
    }

    private static int compareNullable(Long left, Long right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return -1;
        }
        if (right == null) {
            return 1;
        }
        return Long.compare(left, right);
    }

    private record TaskScoreSummary(int completedTasks, double averageScore) {
    }
}
