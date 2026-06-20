package com._202510007517.platform.analysis.service;

import com._202510007517.platform.analysis.api.dto.KnowledgeMasteryDTO;
import com._202510007517.platform.analysis.api.dto.ScoreTrendDTO;
import com._202510007517.platform.analysis.repository.AnalysisRepository;
import com._202510007517.platform.assignment.api.dto.AssignmentDTO;
import com._202510007517.platform.assignment.api.feign.AssignmentFeignClient;
import com._202510007517.platform.course.api.dto.CourseDTO;
import com._202510007517.platform.course.api.dto.TeacherClassDTO;
import com._202510007517.platform.course.api.feign.CourseFeignClient;
import com._202510007517.platform.exam.api.dto.ExamDTO;
import com._202510007517.platform.exam.api.feign.ExamFeignClient;
import com._202510007517.platform.user.api.dto.UserProfileDTO;
import com._202510007517.platform.user.api.feign.UserFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class AnalysisQueryService {

    private final AnalysisRepository analysisRepository;
    private final CourseFeignClient courseFeignClient;
    private final UserFeignClient userFeignClient;
    private final AssignmentFeignClient assignmentFeignClient;
    private final ExamFeignClient examFeignClient;
    private final JdbcTemplate jdbcTemplate;

    public AnalysisQueryService(AnalysisRepository analysisRepository) {
        this(analysisRepository, null, null, null, null, null);
    }

    public AnalysisQueryService(AnalysisRepository analysisRepository, CourseFeignClient courseFeignClient) {
        this(analysisRepository, courseFeignClient, null, null, null, null);
    }

    public AnalysisQueryService(
            AnalysisRepository analysisRepository,
            CourseFeignClient courseFeignClient,
            UserFeignClient userFeignClient) {
        this(analysisRepository, courseFeignClient, userFeignClient, null, null, null);
    }

    public AnalysisQueryService(
            AnalysisRepository analysisRepository,
            CourseFeignClient courseFeignClient,
            UserFeignClient userFeignClient,
            AssignmentFeignClient assignmentFeignClient) {
        this(analysisRepository, courseFeignClient, userFeignClient, assignmentFeignClient, null, null);
    }

    public AnalysisQueryService(
            AnalysisRepository analysisRepository,
            CourseFeignClient courseFeignClient,
            UserFeignClient userFeignClient,
            AssignmentFeignClient assignmentFeignClient,
            ExamFeignClient examFeignClient) {
        this(analysisRepository, courseFeignClient, userFeignClient, assignmentFeignClient, examFeignClient, null);
    }

    @Autowired
    public AnalysisQueryService(
            AnalysisRepository analysisRepository,
            CourseFeignClient courseFeignClient,
            UserFeignClient userFeignClient,
            AssignmentFeignClient assignmentFeignClient,
            ExamFeignClient examFeignClient,
            JdbcTemplate jdbcTemplate) {
        this.analysisRepository = analysisRepository;
        this.courseFeignClient = courseFeignClient;
        this.userFeignClient = userFeignClient;
        this.assignmentFeignClient = assignmentFeignClient;
        this.examFeignClient = examFeignClient;
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ScoreTrendDTO> listScoreTrend(Long teacherId, Long classId, Long courseId, String timeRange) {
        if (teacherId == null) {
            throw new IllegalArgumentException("缺少教师身份");
        }
        return analysisRepository.listScoreTrends(classId, courseId, resolveSince(timeRange));
    }

    public List<KnowledgeMasteryDTO> listStudentKnowledgeMastery(Long teacherId, Long studentId, Long courseId) {
        if (teacherId == null) {
            throw new IllegalArgumentException("缺少教师身份");
        }
        return analysisRepository.listKnowledgeMastery(studentId, courseId);
    }

    public Map<String, Object> getTeacherDashboard(Long teacherId, Long classId, Long courseId, String timeRange) {
        if (teacherId == null) {
            throw new IllegalArgumentException("缺少教师身份");
        }
        return analysisRepository.getTeacherDashboard(teacherId, classId, courseId, timeRange);
    }

    public Map<String, Object> getTeacherLearningSummary(Long teacherId, Long classId, Long courseId, String timeRange) {
        if (teacherId == null) {
            throw new IllegalArgumentException("缺少教师身份");
        }
        return analysisRepository.getTeacherLearningSummary(teacherId, classId, courseId, timeRange);
    }

    public List<Map<String, Object>> listKnowledgePointMasteryStats(Long teacherId, Long courseId) {
        if (teacherId == null) {
            throw new IllegalArgumentException("缺少教师身份");
        }
        if (courseId == null) {
            throw new IllegalArgumentException("缺少课程ID");
        }
        List<KnowledgeMasteryDTO> masteryRows = analysisRepository.listKnowledgeMasteryByScope(null, courseId);
        if (masteryRows.isEmpty()) {
            return List.of();
        }
        Map<Long, String> knowledgePointNames = resolveKnowledgePointNames(masteryRows);
        Map<Long, List<KnowledgeMasteryDTO>> rowsByKnowledgePoint = groupByKnowledgePoint(masteryRows);
        List<Map<String, Object>> stats = new ArrayList<>();
        int orderIndex = 1;
        for (Map.Entry<Long, List<KnowledgeMasteryDTO>> entry : rowsByKnowledgePoint.entrySet()) {
            Map<String, Object> summary = toKnowledgeMasterySummary(entry.getKey(), entry.getValue(), orderIndex++);
            String displayName = knowledgePointNames.get(entry.getKey());
            if (displayName != null) {
                summary.put("knowledgePointName", displayName);
                summary.put("pointName", displayName);
            }
            stats.add(summary);
        }
        return stats;
    }

    public void analyzeStudentKnowledgeMastery(Long teacherId, Long studentId, Long courseId) {
        if (teacherId == null) {
            throw new IllegalArgumentException("缺少教师身份");
        }
        if (studentId == null) {
            throw new IllegalArgumentException("缺少学生ID");
        }
        if (courseId == null) {
            throw new IllegalArgumentException("缺少课程ID");
        }
    }

    public Map<String, Object> getTeacherKnowledgePointAnalysis(
            Long teacherId,
            Long courseId,
            Long classId,
            Long studentId,
            Long knowledgePointId) {
        if (teacherId == null) {
            throw new IllegalArgumentException("缺少教师身份");
        }
        CourseMetadata requestedCourse = resolveCourseMetadata(teacherId, courseId);
        if (requestedCourse.forbidden()) {
            return emptyTeacherKnowledgePointAnalysis("课程不存在或无权访问");
        }
        ClassMetadata requestedClass = resolveClassMetadata(teacherId, courseId, classId);
        if (requestedClass.forbidden()) {
            return emptyTeacherKnowledgePointAnalysis("班级不存在或无权访问");
        }
        List<KnowledgeMasteryDTO> masteryRows = analysisRepository.listKnowledgeMasteryByScope(classId, courseId)
                .stream()
                .filter(row -> studentId == null || studentId.equals(row.studentId()))
                .filter(row -> knowledgePointId == null || knowledgePointId.equals(effectiveKnowledgePointId(row)))
                .toList();
        Map<Long, List<KnowledgeMasteryDTO>> rowsByKnowledgePoint = new LinkedHashMap<>();
        for (KnowledgeMasteryDTO row : masteryRows) {
            if (row.courseId() == null) {
                continue;
            }
            rowsByKnowledgePoint.computeIfAbsent(effectiveKnowledgePointId(row), ignored -> new ArrayList<>()).add(row);
        }
        Map<Long, String> knowledgePointNames = resolveKnowledgePointNames(masteryRows);

        List<Map<String, Object>> distribution = new ArrayList<>();
        List<Map<String, Object>> weakTopics = new ArrayList<>();
        List<Double> excellentStudentAverage = new ArrayList<>();
        int orderIndex = 1;
        for (Map.Entry<Long, List<KnowledgeMasteryDTO>> entry : rowsByKnowledgePoint.entrySet()) {
            Map<String, Object> summary = toKnowledgeMasterySummary(entry.getKey(), entry.getValue(), orderIndex++);
            String knowledgePointName = knowledgePointDisplayName(entry.getValue().get(0), requestedCourse, knowledgePointNames);
            distribution.add(Map.of(
                    "knowledgePointId", summary.get("knowledgePointId"),
                    "knowledgePointName", knowledgePointName,
                    "masteryRate", summary.get("masteryRate"),
                    "difficulty", summary.get("difficulty"),
                    "orderIndex", summary.get("orderIndex")));
            if (((Number) summary.get("masteryRate")).doubleValue() < 60.0) {
                weakTopics.add(Map.of(
                        "knowledgePointId", summary.get("knowledgePointId"),
                        "knowledgePointName", knowledgePointName,
                        "averageMastery", summary.get("masteryRate"),
                        "studentCount", summary.get("totalStudents"),
                        "difficulty", summary.get("difficulty")));
            }
            excellentStudentAverage.add(roundOne(entry.getValue().stream()
                    .filter(row -> toMasteryRate(row) >= 90.0)
                    .mapToDouble(AnalysisQueryService::toMasteryRate)
                    .average()
                    .orElse(0.0)));
        }

        List<Map<String, Object>> atRiskStudents = buildAtRiskStudents(teacherId, masteryRows, requestedCourse, knowledgePointNames);
        Map<String, Object> analysis = new LinkedHashMap<>();
        analysis.put("courseName", requestedCourse.pageCourseName());
        analysis.put("className", requestedClass.pageClassName());
        analysis.put("knowledgePointDistribution", distribution);
        analysis.put("atRiskStudents", atRiskStudents);
        analysis.put("weakTopics", weakTopics);
        analysis.put("excellentStudentAverage", excellentStudentAverage);
        return analysis;
    }

    public List<Map<String, Object>> listStudentStudyTimeDistribution(
            Long studentId,
            String type,
            String semester,
            Long courseId,
            String timeRange) {
        if (studentId == null) {
            throw new IllegalArgumentException("缺少学生身份");
        }
        return analysisRepository.listStudentStudyTimeDistribution(studentId, type, semester, courseId, timeRange);
    }

    public Map<String, Object> getStudentLearningStats(
            Long studentId,
            String semester,
            Long courseId,
            String timeRange) {
        if (studentId == null) {
            throw new IllegalArgumentException("缺少学生身份");
        }
        Map<String, Object> stats = new LinkedHashMap<>(
                analysisRepository.getStudentLearningStats(studentId, semester, courseId, timeRange));
        Object knowledgePoints = stats.get("knowledgePoints");
        if (knowledgePoints instanceof List<?> rows) {
            List<Map<String, Object>> normalizedRows = new ArrayList<>();
            for (Object row : rows) {
                if (row instanceof Map<?, ?> map) {
                    Map<String, Object> normalizedRow = new LinkedHashMap<>();
                    for (Map.Entry<?, ?> entry : map.entrySet()) {
                        if (entry.getKey() instanceof String key) {
                            normalizedRow.put(key, entry.getValue());
                        }
                    }
                    normalizedRows.add(normalizedRow);
                }
            }
            stats.put("knowledgePoints", withResolvedStudentKnowledgePointNames(normalizedRows));
        }
        return stats;
    }

    public List<Map<String, Object>> listStudentKnowledgePoints(
            Long studentId,
            String semester,
            Long courseId,
            String timeRange) {
        if (studentId == null) {
            throw new IllegalArgumentException("缺少学生身份");
        }
        return withResolvedStudentKnowledgePointNames(
                analysisRepository.listStudentKnowledgePoints(studentId, semester, courseId, timeRange));
    }

    public Map<String, Object> getStudentKnowledgePointDetail(Long studentId, Long knowledgePointId) {
        if (studentId == null) {
            throw new IllegalArgumentException("缺少学生身份");
        }
        if (knowledgePointId == null) {
            throw new IllegalArgumentException("缺少知识点ID");
        }
        return withResolvedStudentKnowledgePointNames(List.of(
                analysisRepository.getStudentKnowledgePointDetail(studentId, knowledgePointId)))
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("知识点详情解析失败"));
    }

    private static Instant resolveSince(String timeRange) {
        if (timeRange == null || timeRange.isBlank() || "all".equalsIgnoreCase(timeRange)) {
            return null;
        }
        String normalized = timeRange.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "7d", "week" -> Instant.now().minus(Duration.ofDays(7));
            case "30d", "month" -> Instant.now().minus(Duration.ofDays(30));
            case "90d", "quarter" -> Instant.now().minus(Duration.ofDays(90));
            default -> null;
        };
    }

    private static double roundOne(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private static Map<Long, List<KnowledgeMasteryDTO>> groupByKnowledgePoint(List<KnowledgeMasteryDTO> masteryRows) {
        Map<Long, List<KnowledgeMasteryDTO>> rowsByKnowledgePoint = new LinkedHashMap<>();
        for (KnowledgeMasteryDTO row : masteryRows) {
            if (row.courseId() == null) {
                continue;
            }
            rowsByKnowledgePoint.computeIfAbsent(effectiveKnowledgePointId(row), ignored -> new ArrayList<>()).add(row);
        }
        return rowsByKnowledgePoint;
    }

    private static Map<String, Object> toKnowledgeMasterySummary(
            Long knowledgePointId,
            List<KnowledgeMasteryDTO> masteryRows,
            int orderIndex) {
        int excellentCount = 0;
        int goodCount = 0;
        int averageCount = 0;
        int poorCount = 0;
        double masteryTotal = 0.0;
        int masteryCount = 0;
        List<Long> studentIds = new ArrayList<>();
        for (KnowledgeMasteryDTO row : masteryRows) {
            if (row.studentId() != null && !studentIds.contains(row.studentId())) {
                studentIds.add(row.studentId());
            }
            if (row.masteryScore() == null) {
                continue;
            }
            double masteryRate = toMasteryRate(row);
            masteryTotal += masteryRate;
            masteryCount++;
            if (masteryRate >= 90.0) {
                excellentCount++;
            } else if (masteryRate >= 75.0) {
                goodCount++;
            } else if (masteryRate >= 60.0) {
                averageCount++;
            } else {
                poorCount++;
            }
        }

        double averageMasteryRate = masteryCount == 0 ? 0.0 : roundOne(masteryTotal / masteryCount);
        Long courseId = masteryRows.stream()
                .map(KnowledgeMasteryDTO::courseId)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        String pointName = masteryRows.stream().anyMatch(row -> row.knowledgePointId() != null)
                ? "知识点 " + knowledgePointId
                : "课程 " + knowledgePointId;
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("knowledgePointId", knowledgePointId);
        stats.put("courseId", courseId);
        stats.put("knowledgePointName", pointName);
        stats.put("pointName", pointName);
        stats.put("difficulty", averageMasteryRate >= 80.0 ? "中等" : "困难");
        stats.put("orderIndex", orderIndex);
        stats.put("studentCount", studentIds.size());
        stats.put("totalStudents", studentIds.size());
        stats.put("masteryRate", averageMasteryRate);
        stats.put("excellentCount", excellentCount);
        stats.put("goodCount", goodCount);
        stats.put("averageCount", averageCount);
        stats.put("poorCount", poorCount);
        return stats;
    }

    private static Long effectiveKnowledgePointId(KnowledgeMasteryDTO row) {
        return row.knowledgePointId() == null ? row.courseId() : row.knowledgePointId();
    }

    private static String knowledgePointDisplayName(
            KnowledgeMasteryDTO row,
            CourseMetadata requestedCourse,
            Map<Long, String> knowledgePointNames) {
        if (row.knowledgePointId() != null) {
            return knowledgePointNames.getOrDefault(row.knowledgePointId(), "知识点 " + row.knowledgePointId());
        }
        if (requestedCourse != null) {
            return requestedCourse.nameFor(row.courseId());
        }
        return "课程 " + row.courseId();
    }

    private List<Map<String, Object>> buildAtRiskStudents(
            Long teacherId,
            List<KnowledgeMasteryDTO> masteryRows,
            CourseMetadata requestedCourse,
            Map<Long, String> knowledgePointNames) {
        Map<Long, List<KnowledgeMasteryDTO>> rowsByStudent = new LinkedHashMap<>();
        for (KnowledgeMasteryDTO row : masteryRows) {
            if (row.studentId() == null || row.masteryScore() == null || toMasteryRate(row) >= 60.0) {
                continue;
            }
            rowsByStudent.computeIfAbsent(row.studentId(), ignored -> new ArrayList<>()).add(row);
        }
        Map<Long, String> studentNames = resolveUserNames(new ArrayList<>(rowsByStudent.keySet()));
        List<Map<String, Object>> students = new ArrayList<>();
        for (Map.Entry<Long, List<KnowledgeMasteryDTO>> entry : rowsByStudent.entrySet()) {
            List<Map<String, Object>> weakKnowledgePoints = new ArrayList<>();
            for (KnowledgeMasteryDTO row : entry.getValue()) {
                Map<String, Object> weakPoint = new LinkedHashMap<>();
                weakPoint.put("knowledgePointId", effectiveKnowledgePointId(row));
                weakPoint.put("knowledgePointName", knowledgePointDisplayName(row, requestedCourse, knowledgePointNames));
                weakPoint.put("sourceType", row.lastSourceType());
                weakPoint.put("sourceId", row.lastSourceId());
                weakPoint.put("sourceName", resolveSourceName(teacherId, row));
                weakPoint.put("masteryRate", toMasteryRate(row));
                weakKnowledgePoints.add(weakPoint);
            }
            Map<String, Object> student = new LinkedHashMap<>();
            student.put("studentId", entry.getKey());
            student.put("studentName", studentNames.getOrDefault(entry.getKey(), "学生 " + entry.getKey()));
            student.put("weakKnowledgePoints", weakKnowledgePoints);
            students.add(student);
        }
        return students;
    }

    private Map<Long, String> resolveKnowledgePointNames(List<KnowledgeMasteryDTO> masteryRows) {
        if (jdbcTemplate == null || masteryRows.isEmpty()) {
            return Map.of();
        }
        List<Long> knowledgePointIds = masteryRows.stream()
                .map(KnowledgeMasteryDTO::knowledgePointId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (knowledgePointIds.isEmpty()) {
            return Map.of();
        }

        String placeholders = String.join(",", knowledgePointIds.stream().map(ignored -> "?").toList());
        try {
            return jdbcTemplate.query(
                    "SELECT id, point_name FROM sc_course.teacher_knowledge_points WHERE id IN (" + placeholders + ")",
                    rs -> {
                        Map<Long, String> names = new LinkedHashMap<>();
                        while (rs.next()) {
                            names.put(rs.getLong("id"), rs.getString("point_name"));
                        }
                        return names;
                    },
                    knowledgePointIds.toArray());
        } catch (DataAccessException ignored) {
            return Map.of();
        }
    }

    private List<Map<String, Object>> withResolvedStudentKnowledgePointNames(List<Map<String, Object>> rows) {
        if (jdbcTemplate == null || rows.isEmpty()) {
            return rows;
        }
        List<Long> knowledgePointIds = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Object idValue = row.get("knowledgePointId");
            if (idValue instanceof Number number) {
                Long knowledgePointId = number.longValue();
                if (!knowledgePointIds.contains(knowledgePointId)) {
                    knowledgePointIds.add(knowledgePointId);
                }
            }
        }
        if (knowledgePointIds.isEmpty()) {
            return rows;
        }
        Map<Long, String> names = resolveKnowledgePointNamesByIds(knowledgePointIds);
        if (names.isEmpty()) {
            return rows;
        }
        List<Map<String, Object>> resolvedRows = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> resolved = new LinkedHashMap<>(row);
            Object idValue = resolved.get("knowledgePointId");
            Long knowledgePointId = idValue instanceof Number number ? number.longValue() : null;
            String pointName = knowledgePointId == null ? null : names.get(knowledgePointId);
            if (pointName != null && !pointName.isBlank()) {
                resolved.put("name", pointName);
                resolved.put("pointName", pointName);
                resolved.put("description", pointName + " 的掌握汇总");
            }
            resolvedRows.add(resolved);
        }
        return resolvedRows;
    }

    private Map<Long, String> resolveKnowledgePointNamesByIds(List<Long> knowledgePointIds) {
        String placeholders = String.join(",", knowledgePointIds.stream().map(ignored -> "?").toList());
        try {
            return jdbcTemplate.query(
                    "SELECT id, point_name FROM sc_course.teacher_knowledge_points WHERE id IN (" + placeholders + ")",
                    rs -> {
                        Map<Long, String> names = new LinkedHashMap<>();
                        while (rs.next()) {
                            names.put(rs.getLong("id"), rs.getString("point_name"));
                        }
                        return names;
                    },
                    knowledgePointIds.toArray());
        } catch (DataAccessException ignored) {
            try {
                return jdbcTemplate.query(
                        "SELECT id, point_name FROM teacher_knowledge_points WHERE id IN (" + placeholders + ")",
                        rs -> {
                            Map<Long, String> names = new LinkedHashMap<>();
                            while (rs.next()) {
                                names.put(rs.getLong("id"), rs.getString("point_name"));
                            }
                            return names;
                        },
                        knowledgePointIds.toArray());
            } catch (DataAccessException fallbackIgnored) {
                return Map.of();
            }
        }
    }

    private static double toMasteryRate(KnowledgeMasteryDTO row) {
        return row.masteryScore() == null ? 0.0 : roundOne(row.masteryScore().doubleValue() * 100.0);
    }

    private CourseMetadata resolveCourseMetadata(Long teacherId, Long courseId) {
        if (courseId == null || courseFeignClient == null) {
            return new CourseMetadata(false, null, null);
        }
        try {
            CourseDTO course = courseFeignClient.getCourse(courseId);
            if (course == null || (course.getTeacherId() != null && !teacherId.equals(course.getTeacherId()))) {
                return new CourseMetadata(true, courseId, null);
            }
            return new CourseMetadata(false, courseId, blankToNull(course.getCourseName()));
        } catch (RuntimeException ignored) {
            return new CourseMetadata(false, courseId, null);
        }
    }

    private ClassMetadata resolveClassMetadata(Long teacherId, Long courseId, Long classId) {
        if (classId == null || courseFeignClient == null) {
            return new ClassMetadata(false, classId, null);
        }
        try {
            List<TeacherClassDTO> classes = courseFeignClient.listTeacherClasses(
                    teacherId,
                    null,
                    null,
                    null,
                    null,
                    courseId);
            return classes.stream()
                    .filter(teacherClass -> classId.equals(teacherClass.getId()))
                    .findFirst()
                    .map(teacherClass -> new ClassMetadata(false, classId, blankToNull(teacherClass.getClassName())))
                    .orElseGet(() -> new ClassMetadata(true, classId, null));
        } catch (RuntimeException ignored) {
            return new ClassMetadata(false, classId, null);
        }
    }

    private Map<Long, String> resolveUserNames(List<Long> userIds) {
        if (userIds.isEmpty() || userFeignClient == null) {
            return Map.of();
        }
        try {
            List<UserProfileDTO> users = userFeignClient.listByIds(userIds);
            Map<Long, String> names = new LinkedHashMap<>();
            for (UserProfileDTO user : users) {
                if (user == null || user.getId() == null) {
                    continue;
                }
                String displayName = firstPresent(user.getName(), user.getUsername());
                if (displayName != null) {
                    names.put(user.getId(), displayName);
                }
            }
            return names;
        } catch (RuntimeException ignored) {
            return Map.of();
        }
    }

    private String resolveSourceName(Long teacherId, KnowledgeMasteryDTO row) {
        if ("assignment".equalsIgnoreCase(row.lastSourceType())) {
            return resolveAssignmentName(teacherId, row);
        }
        if ("exam".equalsIgnoreCase(row.lastSourceType())) {
            return resolveExamName(teacherId, row);
        }
        if (row.lastSourceId() == null) {
            return "课程 " + row.courseId();
        }
        return firstPresent(row.lastSourceType(), "来源") + " " + row.lastSourceId();
    }

    private String resolveAssignmentName(Long teacherId, KnowledgeMasteryDTO row) {
        if (row.lastSourceId() == null || assignmentFeignClient == null) {
            return genericAssignmentName(row.lastSourceId());
        }
        try {
            AssignmentDTO assignment = assignmentFeignClient.getAssignment(row.lastSourceId());
            if (assignment == null || !belongsToCurrentTeacherAndCourse(assignment, teacherId, row.courseId())) {
                return genericAssignmentName(row.lastSourceId());
            }
            return firstPresent(assignment.getTitle(), genericAssignmentName(row.lastSourceId()));
        } catch (RuntimeException ignored) {
            return genericAssignmentName(row.lastSourceId());
        }
    }

    private static boolean belongsToCurrentTeacherAndCourse(AssignmentDTO assignment, Long teacherId, Long courseId) {
        if (assignment.getTeacherId() != null && !assignment.getTeacherId().equals(teacherId)) {
            return false;
        }
        return assignment.getCourseId() == null || courseId == null || assignment.getCourseId().equals(courseId);
    }

    private static String genericAssignmentName(Long assignmentId) {
        return assignmentId == null ? "作业" : "作业 " + assignmentId;
    }

    private String resolveExamName(Long teacherId, KnowledgeMasteryDTO row) {
        if (row.lastSourceId() == null || examFeignClient == null) {
            return genericExamName(row.lastSourceId());
        }
        try {
            ExamDTO exam = examFeignClient.getTeacherExam(row.lastSourceId(), teacherId);
            if (exam == null || !belongsToCurrentCourse(exam, row.courseId())) {
                return genericExamName(row.lastSourceId());
            }
            return firstPresent(exam.getTitle(), genericExamName(row.lastSourceId()));
        } catch (RuntimeException ignored) {
            return genericExamName(row.lastSourceId());
        }
    }

    private static boolean belongsToCurrentCourse(ExamDTO exam, Long courseId) {
        return exam.getCourseId() == null || courseId == null || exam.getCourseId().equals(courseId);
    }

    private static String genericExamName(Long examId) {
        return examId == null ? "考试" : "考试 " + examId;
    }

    private static Map<String, Object> emptyTeacherKnowledgePointAnalysis(String courseName) {
        Map<String, Object> analysis = new LinkedHashMap<>();
        analysis.put("courseName", courseName);
        analysis.put("knowledgePointDistribution", List.of());
        analysis.put("atRiskStudents", List.of());
        analysis.put("weakTopics", List.of());
        analysis.put("excellentStudentAverage", List.of());
        return analysis;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static String firstPresent(String first, String second) {
        String normalizedFirst = blankToNull(first);
        return normalizedFirst == null ? blankToNull(second) : normalizedFirst;
    }

    private record CourseMetadata(boolean forbidden, Long courseId, String courseName) {

        private String pageCourseName() {
            if (courseId == null) {
                return "所有课程";
            }
            return courseName == null ? "课程 " + courseId : courseName;
        }

        private String nameFor(Long candidateCourseId) {
            if (courseId != null && courseId.equals(candidateCourseId) && courseName != null) {
                return courseName;
            }
            return "课程 " + candidateCourseId;
        }
    }

    private record ClassMetadata(boolean forbidden, Long classId, String className) {

        private String pageClassName() {
            if (classId == null) {
                return "所有班级";
            }
            return className == null ? "班级 " + classId : className;
        }
    }
}
