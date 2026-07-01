package com._202510007517.platform.analysis.service;

import com._202510007517.platform.analysis.api.dto.KnowledgeMasteryDTO;
import com._202510007517.platform.analysis.api.dto.ScoreTrendDTO;
import com._202510007517.platform.analysis.repository.AnalysisRepository;
import com._202510007517.platform.analysis.repository.AnalysisTriggerJob;
import com._202510007517.platform.analysis.repository.AnalysisTriggerJobRepository;
import com._202510007517.platform.analysis.repository.EarlyWarningRepository;
import com._202510007517.platform.course.api.dto.CourseDTO;
import com._202510007517.platform.course.api.dto.TeacherClassDTO;
import com._202510007517.platform.course.api.feign.CourseFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class AnalysisTriggerCompatibilityService {

    private static final int LOW_SCORE_THRESHOLD = 60;
    private static final BigDecimal LOW_MASTERY_THRESHOLD = new BigDecimal("0.6000");

    private final AnalysisQueryService analysisQueryService;
    private final AnalysisRepository analysisRepository;
    private final EarlyWarningRepository earlyWarningRepository;
    private final AnalysisTriggerJobRepository analysisTriggerJobRepository;
    private final CourseFeignClient courseFeignClient;

    public AnalysisTriggerCompatibilityService(
            AnalysisQueryService analysisQueryService,
            AnalysisRepository analysisRepository,
            EarlyWarningRepository earlyWarningRepository,
            AnalysisTriggerJobRepository analysisTriggerJobRepository) {
        this(analysisQueryService, analysisRepository, earlyWarningRepository, analysisTriggerJobRepository, null);
    }

    @Autowired
    public AnalysisTriggerCompatibilityService(
            AnalysisQueryService analysisQueryService,
            AnalysisRepository analysisRepository,
            EarlyWarningRepository earlyWarningRepository,
            AnalysisTriggerJobRepository analysisTriggerJobRepository,
            CourseFeignClient courseFeignClient) {
        this.analysisQueryService = analysisQueryService;
        this.analysisRepository = analysisRepository;
        this.earlyWarningRepository = earlyWarningRepository;
        this.analysisTriggerJobRepository = analysisTriggerJobRepository;
        this.courseFeignClient = courseFeignClient;
    }

    public void triggerWarningAnalysis(Long teacherId) {
        requireTeacher(teacherId);
        AnalysisTriggerJob job = analysisTriggerJobRepository.create(
                teacherId,
                "WARNING_ANALYSIS",
                null,
                null,
                null);
        AnalysisResult result = analyzeScope(teacherId, null, null, null);
        analysisTriggerJobRepository.complete(
                job.id(),
                "COMPLETED",
                result.requestedCount(),
                result.warningCount(),
                "学情预警分析完成");
    }

    public void triggerKnowledgePointAnalysis(Long teacherId) {
        requireTeacher(teacherId);
        AnalysisTriggerJob job = analysisTriggerJobRepository.create(
                teacherId,
                "KNOWLEDGE_POINT_ANALYSIS",
                null,
                null,
                null);
        List<KnowledgeMasteryDTO> masteries = analysisRepository.listKnowledgeMasteryByScope(null, null);
        int warningCount = createMasteryWarnings(teacherId, masteries);
        int requestedCount = countStudents(List.of(), masteries);
        analysisTriggerJobRepository.complete(
                job.id(),
                "COMPLETED",
                requestedCount,
                warningCount,
                "知识点分析更新完成");
    }

    public void triggerStudentAnalysis(Long teacherId, Long studentId, Long courseId) {
        requireTeacher(teacherId);
        if (studentId == null) {
            throw new IllegalArgumentException("缺少学生ID");
        }
        if (courseId == null) {
            throw new IllegalArgumentException("缺少课程ID");
        }
        validateTeacherCourseAccess(teacherId, courseId);
        AnalysisTriggerJob job = analysisTriggerJobRepository.create(
                teacherId,
                "STUDENT_ANALYSIS",
                null,
                courseId,
                studentId);
        analysisQueryService.analyzeStudentKnowledgeMastery(teacherId, studentId, courseId);
        AnalysisResult result = analyzeScope(teacherId, null, courseId, studentId);
        analysisTriggerJobRepository.complete(
                job.id(),
                "COMPLETED",
                Math.max(1, result.requestedCount()),
                result.warningCount(),
                "学生学情分析完成");
    }

    public int triggerClassAnalysis(Long teacherId, Long classId, Long courseId) {
        requireTeacher(teacherId);
        if (classId == null) {
            throw new IllegalArgumentException("缺少班级ID");
        }
        if (courseId == null) {
            throw new IllegalArgumentException("缺少课程ID");
        }
        validateTeacherCourseAccess(teacherId, courseId);
        validateTeacherClassAccess(teacherId, classId, courseId);
        AnalysisTriggerJob job = analysisTriggerJobRepository.create(
                teacherId,
                "CLASS_ANALYSIS",
                classId,
                courseId,
                null);
        AnalysisResult result = analyzeScope(teacherId, classId, courseId, null);
        analysisTriggerJobRepository.complete(
                job.id(),
                "COMPLETED",
                result.requestedCount(),
                result.warningCount(),
                "班级学情分析完成");
        return result.requestedCount();
    }

    private static void requireTeacher(Long teacherId) {
        if (teacherId == null) {
            throw new IllegalArgumentException("缺少教师身份");
        }
    }

    private void validateTeacherCourseAccess(Long teacherId, Long courseId) {
        if (courseId == null || courseFeignClient == null) {
            return;
        }
        CourseDTO course;
        try {
            course = courseFeignClient.getCourse(courseId);
        } catch (RuntimeException ignored) {
            return;
        }
        if (course == null || (course.getTeacherId() != null && !teacherId.equals(course.getTeacherId()))) {
            throw new IllegalArgumentException("课程不存在或无权访问");
        }
    }

    private void validateTeacherClassAccess(Long teacherId, Long classId, Long courseId) {
        if (classId == null || courseFeignClient == null) {
            return;
        }
        List<TeacherClassDTO> classes;
        try {
            classes = courseFeignClient.listTeacherClasses(
                    teacherId,
                    null,
                    null,
                    null,
                    null,
                    courseId);
        } catch (RuntimeException ignored) {
            return;
        }
        if (classes == null) {
            return;
        }
        boolean visible = classes.stream().anyMatch(teacherClass -> classId.equals(teacherClass.getId()));
        if (!visible) {
            throw new IllegalArgumentException("班级不存在或无权访问");
        }
    }

    private AnalysisResult analyzeScope(Long teacherId, Long classId, Long courseId, Long studentId) {
        List<ScoreTrendDTO> scores = analysisRepository.listScoreTrends(classId, courseId, (Instant) null).stream()
                .filter(row -> studentId == null || studentId.equals(row.studentId()))
                .toList();
        List<KnowledgeMasteryDTO> masteries = studentId == null
                ? analysisRepository.listKnowledgeMasteryByScope(classId, courseId)
                : analysisRepository.listKnowledgeMastery(studentId, courseId).stream()
                        .filter(row -> classId == null || classId.equals(row.classId()))
                        .toList();

        int warningCount = createScoreWarnings(teacherId, scores) + createMasteryWarnings(teacherId, masteries);
        return new AnalysisResult(countStudents(scores, masteries), warningCount);
    }

    private int createScoreWarnings(Long teacherId, List<ScoreTrendDTO> scores) {
        int created = 0;
        for (ScoreTrendDTO score : scores) {
            if (score.score() == null || score.score() >= LOW_SCORE_THRESHOLD) {
                continue;
            }
            if (earlyWarningRepository.existsPendingWarning(
                    teacherId,
                    score.studentId(),
                    score.courseId(),
                    "LOW_SCORE")) {
                continue;
            }
            earlyWarningRepository.insert(
                    teacherId,
                    score.studentId(),
                    score.courseId(),
                    "LOW_SCORE",
                    "HIGH",
                    "阶段测验低于及格线",
                    score.sourceType(),
                    score.sourceId());
            created++;
        }
        return created;
    }

    private int createMasteryWarnings(Long teacherId, List<KnowledgeMasteryDTO> masteries) {
        int created = 0;
        for (KnowledgeMasteryDTO mastery : masteries) {
            if (mastery.masteryScore() == null || mastery.masteryScore().compareTo(LOW_MASTERY_THRESHOLD) >= 0) {
                continue;
            }
            if (earlyWarningRepository.existsPendingWarning(
                    teacherId,
                    mastery.studentId(),
                    mastery.courseId(),
                    "PROGRESS")) {
                continue;
            }
            earlyWarningRepository.insert(
                    teacherId,
                    mastery.studentId(),
                    mastery.courseId(),
                    "PROGRESS",
                    "MEDIUM",
                    "知识点掌握度低于预期",
                    mastery.lastSourceType() == null ? "knowledge_mastery" : mastery.lastSourceType(),
                    mastery.lastSourceId());
            created++;
        }
        return created;
    }

    private static int countStudents(List<ScoreTrendDTO> scores, List<KnowledgeMasteryDTO> masteries) {
        Set<Long> studentIds = new LinkedHashSet<>();
        scores.stream().map(ScoreTrendDTO::studentId).forEach(studentIds::add);
        masteries.stream().map(KnowledgeMasteryDTO::studentId).forEach(studentIds::add);
        return studentIds.size();
    }

    private record AnalysisResult(int requestedCount, int warningCount) {
    }
}
