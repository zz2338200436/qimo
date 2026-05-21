package com._202510007517.platform.analysis.repository;

import com._202510007517.platform.analysis.api.dto.KnowledgeMasteryDTO;
import com._202510007517.platform.analysis.api.dto.ScoreTrendDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Repository
public class JdbcAnalysisRepository implements AnalysisRepository {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DAY_LABEL_FORMATTER = DateTimeFormatter.ofPattern("MM-dd");
    private static final long COURSE_LEVEL_KNOWLEDGE_POINT_ID = 0L;

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    public JdbcAnalysisRepository(JdbcTemplate jdbcTemplate) {
        this(jdbcTemplate, Clock.systemDefaultZone());
    }

    JdbcAnalysisRepository(JdbcTemplate jdbcTemplate, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
    }

    @Override
    public void upsertScoreTrend(ScoreTrendRecord record) {
        jdbcTemplate.update("""
                        INSERT INTO score_trends (
                            student_id, course_id, class_id, source_type, source_id, submission_id,
                            score, max_score, score_rate, occurred_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON DUPLICATE KEY UPDATE
                            score = VALUES(score),
                            max_score = VALUES(max_score),
                            score_rate = VALUES(score_rate),
                            occurred_at = VALUES(occurred_at)
                        """,
                record.studentId(),
                record.courseId(),
                record.classId(),
                record.sourceType(),
                record.sourceId(),
                record.submissionId(),
                record.score(),
                record.maxScore(),
                record.scoreRate(),
                Timestamp.from(record.occurredAt()));
    }

    @Override
    public void upsertKnowledgeMastery(KnowledgeMasteryRecord record) {
        jdbcTemplate.update("""
                        INSERT INTO kp_mastery (
                            student_id, course_id, class_id, knowledge_point_id, mastery_score, evidence_count,
                            last_source_type, last_source_id, last_event_id, updated_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON DUPLICATE KEY UPDATE
                            class_id = VALUES(class_id),
                            mastery_score = ((mastery_score * evidence_count) + VALUES(mastery_score))
                                    / (evidence_count + 1),
                            evidence_count = evidence_count + 1,
                            last_source_type = VALUES(last_source_type),
                            last_source_id = VALUES(last_source_id),
                            last_event_id = VALUES(last_event_id),
                            updated_at = VALUES(updated_at)
                        """,
                record.studentId(),
                record.courseId(),
                record.classId(),
                toStoredKnowledgePointId(record.knowledgePointId()),
                record.masteryScore(),
                record.evidenceCount(),
                record.lastSourceType(),
                record.lastSourceId(),
                record.lastEventId(),
                Timestamp.from(record.updatedAt()));
    }

    @Override
    public List<ScoreTrendDTO> listScoreTrends(Long classId, Long courseId, Instant since) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT student_id, course_id, class_id, source_type, source_id, submission_id,
                       score, max_score, score_rate, occurred_at
                FROM score_trends
                WHERE 1 = 1
                """);
        if (classId != null) {
            sql.append(" AND class_id = ?");
            args.add(classId);
        }
        if (courseId != null) {
            sql.append(" AND course_id = ?");
            args.add(courseId);
        }
        if (since != null) {
            sql.append(" AND occurred_at >= ?");
            args.add(Timestamp.from(since));
        }
        sql.append(" ORDER BY occurred_at ASC, id ASC");

        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> new ScoreTrendDTO(
                rs.getLong("student_id"),
                rs.getLong("course_id"),
                readNullableLong(rs, "class_id"),
                rs.getString("source_type"),
                rs.getLong("source_id"),
                rs.getLong("submission_id"),
                readNullableInteger(rs, "score"),
                readNullableInteger(rs, "max_score"),
                rs.getBigDecimal("score_rate"),
                rs.getTimestamp("occurred_at").toInstant()), args.toArray());
    }

    @Override
    public List<KnowledgeMasteryDTO> listKnowledgeMastery(Long studentId, Long courseId) {
        return jdbcTemplate.query("""
                        SELECT student_id, course_id, class_id, knowledge_point_id, mastery_score, evidence_count,
                               last_source_type, last_source_id, last_event_id, updated_at
                        FROM kp_mastery
                        WHERE student_id = ? AND course_id = ?
                        ORDER BY updated_at DESC, knowledge_point_id ASC, id DESC
                        """,
                (rs, rowNum) -> new KnowledgeMasteryDTO(
                        rs.getLong("student_id"),
                        rs.getLong("course_id"),
                        readNullableLong(rs, "class_id"),
                        fromStoredKnowledgePointId(readNullableLong(rs, "knowledge_point_id")),
                        rs.getBigDecimal("mastery_score"),
                        rs.getInt("evidence_count"),
                        rs.getString("last_source_type"),
                        readNullableLong(rs, "last_source_id"),
                        rs.getString("last_event_id"),
                        rs.getTimestamp("updated_at").toInstant()),
                studentId,
                courseId);
    }

    @Override
    public List<KnowledgeMasteryDTO> listKnowledgeMasteryByScope(Long classId, Long courseId) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT student_id, course_id, class_id, knowledge_point_id, mastery_score, evidence_count,
                       last_source_type, last_source_id, last_event_id, updated_at
                FROM kp_mastery
                WHERE 1 = 1
                """);
        if (classId != null) {
            sql.append(" AND class_id = ?");
            args.add(classId);
        }
        if (courseId != null) {
            sql.append(" AND course_id = ?");
            args.add(courseId);
        }
        sql.append(" ORDER BY updated_at DESC, knowledge_point_id ASC, id DESC");

        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> new KnowledgeMasteryDTO(
                rs.getLong("student_id"),
                rs.getLong("course_id"),
                readNullableLong(rs, "class_id"),
                fromStoredKnowledgePointId(readNullableLong(rs, "knowledge_point_id")),
                rs.getBigDecimal("mastery_score"),
                rs.getInt("evidence_count"),
                rs.getString("last_source_type"),
                readNullableLong(rs, "last_source_id"),
                rs.getString("last_event_id"),
                rs.getTimestamp("updated_at").toInstant()), args.toArray());
    }

    @Override
    public Map<String, Object> getTeacherDashboard(Long teacherId, Long classId, Long courseId, String timeRange) {
        List<ScoreTrendDTO> scoreRows = listScoreTrends(classId, courseId, resolveSince(timeRange));
        List<ScoreTrendDTO> scopedScores = scoreRows.stream()
                .filter(row -> courseId == null || courseId.equals(row.courseId()))
                .filter(row -> classId == null || classId.equals(row.classId()))
                .toList();
        List<Map<String, Object>> masteryRows = readTeacherMasteryRows(classId, courseId);

        List<Long> courseIds = scopedScores.stream()
                .map(ScoreTrendDTO::courseId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
        List<Long> studentIds = scopedScores.stream()
                .map(ScoreTrendDTO::studentId)
                .filter(java.util.Objects::nonNull)
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
                .filter(java.util.Objects::nonNull)
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
    public List<Map<String, Object>> listStudentKnowledgePoints(
            Long studentId,
            String semester,
            Long courseId,
            String timeRange) {
        return readKnowledgePointStats(studentId, courseId);
    }

    @Override
    public Map<String, Object> getStudentKnowledgePointDetail(Long studentId, Long knowledgePointId) {
        return readKnowledgePointStats(studentId, knowledgePointId).stream()
                .filter(point -> knowledgePointId.equals(point.get("id")))
                .findFirst()
                .orElseThrow(() -> new com._202510007517.platform.common.exception.ResourceNotFoundException("知识点不存在"));
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
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(DISTINCT submission_id) AS completed_tasks,
                       AVG(score) AS average_score
                FROM score_trends
                WHERE student_id = ?
                  AND score IS NOT NULL
                """);
        args.add(studentId);
        if (courseId != null) {
            sql.append(" AND course_id = ?");
            args.add(courseId);
        }

        return jdbcTemplate.query(sql.toString(), rs -> {
            if (!rs.next()) {
                return new TaskScoreSummary(0, 0.0);
            }
            int completedTasks = rs.getInt("completed_tasks");
            double averageScore = rs.getDouble("average_score");
            if (rs.wasNull()) {
                averageScore = 0.0;
            }
            return new TaskScoreSummary(completedTasks, averageScore);
        }, args.toArray());
    }

    private List<Map<String, Object>> readKnowledgePointStats(Long studentId, Long courseId) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT course_id, knowledge_point_id, mastery_score, evidence_count
                FROM kp_mastery
                WHERE student_id = ?
                """);
        args.add(studentId);
        if (courseId != null) {
            sql.append(" AND course_id = ?");
            args.add(courseId);
        }
        sql.append(" ORDER BY updated_at DESC, id DESC");

        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
            Long rowCourseId = rs.getLong("course_id");
            Long knowledgePointId = fromStoredKnowledgePointId(readNullableLong(rs, "knowledge_point_id"));
            Long pointId = knowledgePointId == null ? rowCourseId : knowledgePointId;
            String pointName = knowledgePointId == null ? "课程 " + rowCourseId : "知识点 " + knowledgePointId;
            double mastery = roundOne(rs.getBigDecimal("mastery_score").doubleValue() * 100.0);
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
            point.put("practiceCount", rs.getInt("evidence_count"));
            return point;
        }, args.toArray());
    }

    private List<Map<String, Object>> readTeacherMasteryRows(Long classId, Long courseId) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT student_id, course_id, class_id, knowledge_point_id, mastery_score, evidence_count, updated_at
                FROM kp_mastery
                WHERE 1 = 1
                """);
        if (classId != null) {
            sql.append(" AND class_id = ?");
            args.add(classId);
        }
        if (courseId != null) {
            sql.append(" AND course_id = ?");
            args.add(courseId);
        }
        sql.append(" ORDER BY updated_at DESC, id DESC");

        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("studentId", rs.getLong("student_id"));
            row.put("courseId", rs.getLong("course_id"));
            row.put("classId", readNullableLong(rs, "class_id"));
            row.put("knowledgePointId", fromStoredKnowledgePointId(readNullableLong(rs, "knowledge_point_id")));
            row.put("mastery", roundOne(rs.getBigDecimal("mastery_score").doubleValue() * 100.0));
            row.put("evidenceCount", rs.getInt("evidence_count"));
            row.put("updatedAt", rs.getTimestamp("updated_at").toInstant());
            return row;
        }, args.toArray());
    }

    private static Instant resolveSince(String timeRange) {
        if (timeRange == null || timeRange.isBlank() || "all".equalsIgnoreCase(timeRange)) {
            return null;
        }
        String normalized = timeRange.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "7d", "week" -> Instant.now().minus(java.time.Duration.ofDays(7));
            case "30d", "month" -> Instant.now().minus(java.time.Duration.ofDays(30));
            case "90d", "quarter" -> Instant.now().minus(java.time.Duration.ofDays(90));
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
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT source_type, occurred_at
                FROM score_trends
                WHERE student_id = ?
                  AND occurred_at >= ?
                  AND occurred_at < ?
                """);
        args.add(studentId);
        args.add(Timestamp.valueOf(startDate.atStartOfDay()));
        args.add(Timestamp.valueOf(endDateExclusive.atStartOfDay()));
        if (courseId != null) {
            sql.append(" AND course_id = ?");
            args.add(courseId);
        }

        jdbcTemplate.query(sql.toString(), rs -> {
            LocalDate date = rs.getTimestamp("occurred_at").toLocalDateTime().toLocalDate();
            String bucket = studyTimeBucket(date, startDate, weekly);
            if (bucket != null) {
                buckets.merge(bucket, scoreTrendHours(rs.getString("source_type")), Double::sum);
            }
        }, args.toArray());
    }

    private void mergeKnowledgeMasteryHours(
            Map<String, Double> buckets,
            Long studentId,
            Long courseId,
            LocalDate startDate,
            LocalDate endDateExclusive,
            boolean weekly) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT evidence_count, updated_at
                FROM kp_mastery
                WHERE student_id = ?
                  AND updated_at >= ?
                  AND updated_at < ?
                """);
        args.add(studentId);
        args.add(Timestamp.valueOf(startDate.atStartOfDay()));
        args.add(Timestamp.valueOf(endDateExclusive.atStartOfDay()));
        if (courseId != null) {
            sql.append(" AND course_id = ?");
            args.add(courseId);
        }

        jdbcTemplate.query(sql.toString(), rs -> {
            LocalDate date = rs.getTimestamp("updated_at").toLocalDateTime().toLocalDate();
            String bucket = studyTimeBucket(date, startDate, weekly);
            if (bucket != null) {
                double cap = weekly ? 56.0 : 8.0;
                double hours = Math.min(rs.getInt("evidence_count") * 0.5, cap);
                buckets.merge(bucket, hours, Double::sum);
            }
        }, args.toArray());
    }

    private static String studyTimeBucket(LocalDate date, LocalDate startDate, boolean weekly) {
        if (!weekly) {
            return date.format(DATE_FORMATTER);
        }
        long weekOffset = java.time.temporal.ChronoUnit.WEEKS.between(startDate, date.with(DayOfWeek.MONDAY));
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

    private static Long readNullableLong(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private static long toStoredKnowledgePointId(Long knowledgePointId) {
        return knowledgePointId == null ? COURSE_LEVEL_KNOWLEDGE_POINT_ID : knowledgePointId;
    }

    private static Long fromStoredKnowledgePointId(Long knowledgePointId) {
        return knowledgePointId == null || knowledgePointId == COURSE_LEVEL_KNOWLEDGE_POINT_ID
                ? null
                : knowledgePointId;
    }

    private static Integer readNullableInteger(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private record TaskScoreSummary(int completedTasks, double averageScore) {
    }
}
