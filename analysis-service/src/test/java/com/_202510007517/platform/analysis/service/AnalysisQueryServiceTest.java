package com._202510007517.platform.analysis.service;

import com._202510007517.platform.analysis.api.dto.KnowledgeMasteryDTO;
import com._202510007517.platform.analysis.api.dto.ScoreTrendDTO;
import com._202510007517.platform.analysis.repository.AnalysisRepository;
import com._202510007517.platform.analysis.repository.KnowledgeMasteryRecord;
import com._202510007517.platform.analysis.repository.ScoreTrendRecord;
import com._202510007517.platform.assignment.api.dto.AssignmentDTO;
import com._202510007517.platform.assignment.api.feign.AssignmentFeignClient;
import com._202510007517.platform.course.api.dto.CourseDTO;
import com._202510007517.platform.course.api.dto.TeacherClassDTO;
import com._202510007517.platform.course.api.feign.CourseFeignClient;
import com._202510007517.platform.exam.api.dto.ExamDTO;
import com._202510007517.platform.exam.api.feign.ExamFeignClient;
import com._202510007517.platform.user.api.dto.UserProfileDTO;
import com._202510007517.platform.user.api.feign.UserFeignClient;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnalysisQueryServiceTest {

    @Test
    void listKnowledgePointMasteryStatsAggregatesCourseReadModelForLegacyFields() {
        FakeAnalysisRepository repository = new FakeAnalysisRepository();
        repository.masteryRows.add(mastery(42L, 2L, 1L, "0.9000"));
        repository.masteryRows.add(mastery(43L, 2L, 1L, "0.7600"));
        repository.masteryRows.add(mastery(44L, 2L, 2L, "0.6200"));
        repository.masteryRows.add(mastery(45L, 2L, 2L, "0.5000"));
        repository.masteryRows.add(mastery(46L, 3L, 2L, "0.9900"));
        AnalysisQueryService service = new AnalysisQueryService(repository);

        List<Map<String, Object>> rows = service.listKnowledgePointMasteryStats(7L, 2L);

        assertThat(repository.requestedClassId).isNull();
        assertThat(repository.requestedCourseId).isEqualTo(2L);
        assertThat(rows)
                .singleElement()
                .satisfies(row -> assertThat(row)
                        .containsEntry("knowledgePointId", 2L)
                        .containsEntry("knowledgePointName", "课程 2")
                        .containsEntry("pointName", "课程 2")
                        .containsEntry("difficulty", "困难")
                        .containsEntry("studentCount", 4)
                        .containsEntry("totalStudents", 4)
                        .containsEntry("masteryRate", 69.5)
                        .containsEntry("excellentCount", 1)
                        .containsEntry("goodCount", 1)
                        .containsEntry("averageCount", 1)
                        .containsEntry("poorCount", 1));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getTeacherKnowledgePointAnalysisBuildsLegacyDashboardDataFromReadModel() {
        FakeAnalysisRepository repository = new FakeAnalysisRepository();
        repository.masteryRows.add(mastery(42L, 2L, 1L, "0.9200"));
        repository.masteryRows.add(mastery(43L, 2L, 1L, "0.5800"));
        repository.masteryRows.add(mastery(44L, 3L, 1L, "0.4500"));
        repository.masteryRows.add(mastery(45L, 2L, 2L, "0.3000"));
        AnalysisQueryService service = new AnalysisQueryService(repository);

        Map<String, Object> analysis = service.getTeacherKnowledgePointAnalysis(7L, null, 1L, null, null);

        assertThat(repository.requestedClassId).isEqualTo(1L);
        assertThat(repository.requestedCourseId).isNull();
        assertThat(analysis).containsEntry("courseName", "所有课程");
        assertThat((List<Map<String, Object>>) analysis.get("knowledgePointDistribution"))
                .hasSize(2)
                .first()
                .satisfies(row -> assertThat(row)
                        .containsEntry("knowledgePointId", 2L)
                        .containsEntry("knowledgePointName", "课程 2")
                        .containsEntry("masteryRate", 75.0)
                        .containsEntry("difficulty", "困难")
                        .containsEntry("orderIndex", 1));
        assertThat((List<Map<String, Object>>) analysis.get("weakTopics"))
                .singleElement()
                .satisfies(row -> assertThat(row)
                        .containsEntry("knowledgePointId", 3L)
                        .containsEntry("averageMastery", 45.0)
                        .containsEntry("studentCount", 1));
        assertThat((List<Map<String, Object>>) analysis.get("atRiskStudents"))
                .extracting(row -> row.get("studentId"))
                .containsExactly(43L, 44L);
        assertThat((List<Double>) analysis.get("excellentStudentAverage"))
                .containsExactly(92.0, 0.0);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getTeacherKnowledgePointAnalysisFiltersAndGroupsByRealKnowledgePointId() {
        FakeAnalysisRepository repository = new FakeAnalysisRepository();
        repository.masteryRows.add(mastery(42L, 2L, 1L, 501L, "0.7200"));
        repository.masteryRows.add(mastery(43L, 2L, 1L, 501L, "0.4800", "assignment", 88L));
        repository.masteryRows.add(mastery(44L, 2L, 1L, 502L, "0.9000"));
        AnalysisQueryService service = new AnalysisQueryService(repository);

        Map<String, Object> analysis = service.getTeacherKnowledgePointAnalysis(7L, 2L, 1L, null, 501L);

        assertThat((List<Map<String, Object>>) analysis.get("knowledgePointDistribution"))
                .singleElement()
                .satisfies(row -> assertThat(row)
                        .containsEntry("knowledgePointId", 501L)
                        .containsEntry("knowledgePointName", "知识点 501")
                        .containsEntry("masteryRate", 60.0)
                        .containsEntry("difficulty", "困难"));
        assertThat((List<Map<String, Object>>) analysis.get("weakTopics"))
                .isEmpty();
        List<Map<String, Object>> atRiskStudents = (List<Map<String, Object>>) analysis.get("atRiskStudents");
        List<Map<String, Object>> weakKnowledgePoints =
                (List<Map<String, Object>>) atRiskStudents.get(0).get("weakKnowledgePoints");
        assertThat(weakKnowledgePoints)
                .singleElement()
                .satisfies(row -> assertThat(row)
                        .containsEntry("knowledgePointId", 501L)
                        .containsEntry("knowledgePointName", "知识点 501")
                        .containsEntry("sourceType", "assignment")
                        .containsEntry("sourceId", 88L));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getTeacherKnowledgePointAnalysisUsesOwnedCourseNameFromCourseService() {
        FakeAnalysisRepository repository = new FakeAnalysisRepository();
        repository.masteryRows.add(mastery(42L, 2L, 1L, "0.9200"));
        repository.masteryRows.add(mastery(43L, 2L, 1L, "0.5800"));
        CourseFeignClient courseFeignClient = mock(CourseFeignClient.class);
        when(courseFeignClient.getCourse(2L)).thenReturn(course(2L, "分布式框架技术", 7L));
        when(courseFeignClient.listTeacherClasses(7L, null, null, null, null, 2L))
                .thenReturn(List.of(teacherClass(1L, 7L, "软件工程 2301")));
        AnalysisQueryService service = new AnalysisQueryService(repository, courseFeignClient);

        Map<String, Object> analysis = service.getTeacherKnowledgePointAnalysis(7L, 2L, 1L, null, null);

        assertThat(analysis).containsEntry("courseName", "分布式框架技术");
        assertThat(analysis).containsEntry("className", "软件工程 2301");
        assertThat((List<Map<String, Object>>) analysis.get("knowledgePointDistribution"))
                .singleElement()
                .satisfies(row -> assertThat(row)
                        .containsEntry("knowledgePointId", 2L)
                        .containsEntry("knowledgePointName", "分布式框架技术"));
        verify(courseFeignClient).getCourse(2L);
        verify(courseFeignClient).listTeacherClasses(7L, null, null, null, null, 2L);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getTeacherKnowledgePointAnalysisReturnsLegacyEmptyDataForUnownedCourse() {
        FakeAnalysisRepository repository = new FakeAnalysisRepository();
        repository.masteryRows.add(mastery(42L, 2L, 1L, "0.9200"));
        CourseFeignClient courseFeignClient = mock(CourseFeignClient.class);
        when(courseFeignClient.getCourse(2L)).thenReturn(course(2L, "分布式框架技术", 99L));
        AnalysisQueryService service = new AnalysisQueryService(repository, courseFeignClient);

        Map<String, Object> analysis = service.getTeacherKnowledgePointAnalysis(7L, 2L, 1L, null, null);

        assertThat(analysis).containsEntry("courseName", "课程不存在或无权访问");
        assertThat((List<Map<String, Object>>) analysis.get("knowledgePointDistribution")).isEmpty();
        assertThat((List<Map<String, Object>>) analysis.get("weakTopics")).isEmpty();
        assertThat((List<Map<String, Object>>) analysis.get("atRiskStudents")).isEmpty();
        assertThat((List<Double>) analysis.get("excellentStudentAverage")).isEmpty();
        verify(courseFeignClient).getCourse(2L);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getTeacherKnowledgePointAnalysisUsesUserServiceNamesForAtRiskStudents() {
        FakeAnalysisRepository repository = new FakeAnalysisRepository();
        repository.masteryRows.add(mastery(42L, 2L, 1L, "0.5800"));
        repository.masteryRows.add(mastery(43L, 2L, 1L, "0.9100"));
        UserFeignClient userFeignClient = mock(UserFeignClient.class);
        when(userFeignClient.listByIds(List.of(42L)))
                .thenReturn(List.of(user(42L, "王小明", "student42")));
        AnalysisQueryService service = new AnalysisQueryService(repository, null, userFeignClient);

        Map<String, Object> analysis = service.getTeacherKnowledgePointAnalysis(7L, null, 1L, null, null);

        assertThat((List<Map<String, Object>>) analysis.get("atRiskStudents"))
                .singleElement()
                .satisfies(row -> assertThat(row)
                        .containsEntry("studentId", 42L)
                        .containsEntry("studentName", "王小明"));
        verify(userFeignClient).listByIds(List.of(42L));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getTeacherKnowledgePointAnalysisUsesAssignmentNameForAtRiskWeakPoints() {
        FakeAnalysisRepository repository = new FakeAnalysisRepository();
        repository.masteryRows.add(mastery(42L, 2L, 1L, "0.5800", "assignment", 88L));
        CourseFeignClient courseFeignClient = mock(CourseFeignClient.class);
        when(courseFeignClient.getCourse(2L)).thenReturn(course(2L, "分布式框架技术", 7L));
        when(courseFeignClient.listTeacherClasses(7L, null, null, null, null, 2L))
                .thenReturn(List.of(teacherClass(1L, 7L, "软件工程 2301")));
        AssignmentFeignClient assignmentFeignClient = mock(AssignmentFeignClient.class);
        when(assignmentFeignClient.getAssignment(88L))
                .thenReturn(assignment(88L, "第一次作业", 2L, 7L));
        AnalysisQueryService service = new AnalysisQueryService(
                repository,
                courseFeignClient,
                null,
                assignmentFeignClient);

        Map<String, Object> analysis = service.getTeacherKnowledgePointAnalysis(7L, 2L, 1L, null, null);

        List<Map<String, Object>> atRiskStudents = (List<Map<String, Object>>) analysis.get("atRiskStudents");
        List<Map<String, Object>> weakKnowledgePoints =
                (List<Map<String, Object>>) atRiskStudents.get(0).get("weakKnowledgePoints");
        assertThat(weakKnowledgePoints)
                .singleElement()
                .satisfies(row -> assertThat(row)
                        .containsEntry("sourceType", "assignment")
                        .containsEntry("sourceId", 88L)
                        .containsEntry("sourceName", "第一次作业"));
        verify(assignmentFeignClient).getAssignment(88L);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getTeacherKnowledgePointAnalysisDoesNotExposeUnownedAssignmentNameForAtRiskWeakPoints() {
        FakeAnalysisRepository repository = new FakeAnalysisRepository();
        repository.masteryRows.add(mastery(42L, 2L, 1L, "0.5800", "assignment", 88L));
        CourseFeignClient courseFeignClient = mock(CourseFeignClient.class);
        when(courseFeignClient.getCourse(2L)).thenReturn(course(2L, "分布式框架技术", 7L));
        when(courseFeignClient.listTeacherClasses(7L, null, null, null, null, 2L))
                .thenReturn(List.of(teacherClass(1L, 7L, "软件工程 2301")));
        AssignmentFeignClient assignmentFeignClient = mock(AssignmentFeignClient.class);
        when(assignmentFeignClient.getAssignment(88L))
                .thenReturn(assignment(88L, "其他老师的作业", 2L, 99L));
        AnalysisQueryService service = new AnalysisQueryService(
                repository,
                courseFeignClient,
                null,
                assignmentFeignClient);

        Map<String, Object> analysis = service.getTeacherKnowledgePointAnalysis(7L, 2L, 1L, null, null);

        List<Map<String, Object>> atRiskStudents = (List<Map<String, Object>>) analysis.get("atRiskStudents");
        List<Map<String, Object>> weakKnowledgePoints =
                (List<Map<String, Object>>) atRiskStudents.get(0).get("weakKnowledgePoints");
        assertThat(weakKnowledgePoints)
                .singleElement()
                .satisfies(row -> assertThat(row)
                        .containsEntry("sourceType", "assignment")
                        .containsEntry("sourceId", 88L)
                        .containsEntry("sourceName", "作业 88"));
        verify(assignmentFeignClient).getAssignment(88L);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getTeacherKnowledgePointAnalysisUsesExamNameForAtRiskWeakPoints() {
        FakeAnalysisRepository repository = new FakeAnalysisRepository();
        repository.masteryRows.add(mastery(42L, 2L, 1L, "0.5800", "exam", 9001L));
        CourseFeignClient courseFeignClient = mock(CourseFeignClient.class);
        when(courseFeignClient.getCourse(2L)).thenReturn(course(2L, "分布式框架技术", 7L));
        when(courseFeignClient.listTeacherClasses(7L, null, null, null, null, 2L))
                .thenReturn(List.of(teacherClass(1L, 7L, "软件工程 2301")));
        ExamFeignClient examFeignClient = mock(ExamFeignClient.class);
        when(examFeignClient.getTeacherExam(9001L, 7L))
                .thenReturn(exam(9001L, "期中考试", 2L));
        AnalysisQueryService service = new AnalysisQueryService(
                repository,
                courseFeignClient,
                null,
                null,
                examFeignClient);

        Map<String, Object> analysis = service.getTeacherKnowledgePointAnalysis(7L, 2L, 1L, null, null);

        List<Map<String, Object>> atRiskStudents = (List<Map<String, Object>>) analysis.get("atRiskStudents");
        List<Map<String, Object>> weakKnowledgePoints =
                (List<Map<String, Object>>) atRiskStudents.get(0).get("weakKnowledgePoints");
        assertThat(weakKnowledgePoints)
                .singleElement()
                .satisfies(row -> assertThat(row)
                        .containsEntry("sourceType", "exam")
                        .containsEntry("sourceId", 9001L)
                        .containsEntry("sourceName", "期中考试"));
        verify(examFeignClient).getTeacherExam(9001L, 7L);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getTeacherKnowledgePointAnalysisReturnsLegacyEmptyDataForUnownedClass() {
        FakeAnalysisRepository repository = new FakeAnalysisRepository();
        repository.masteryRows.add(mastery(42L, 2L, 99L, "0.5800"));
        CourseFeignClient courseFeignClient = mock(CourseFeignClient.class);
        when(courseFeignClient.getCourse(2L)).thenReturn(course(2L, "分布式框架技术", 7L));
        when(courseFeignClient.listTeacherClasses(7L, null, null, null, null, 2L))
                .thenReturn(List.of(teacherClass(1L, 7L, "软件工程 2301")));
        AnalysisQueryService service = new AnalysisQueryService(repository, courseFeignClient, null);

        Map<String, Object> analysis = service.getTeacherKnowledgePointAnalysis(7L, 2L, 99L, null, null);

        assertThat(repository.requestedClassId).isNull();
        assertThat(analysis).containsEntry("courseName", "班级不存在或无权访问");
        assertThat((List<Map<String, Object>>) analysis.get("knowledgePointDistribution")).isEmpty();
        assertThat((List<Map<String, Object>>) analysis.get("weakTopics")).isEmpty();
        assertThat((List<Map<String, Object>>) analysis.get("atRiskStudents")).isEmpty();
        assertThat((List<Double>) analysis.get("excellentStudentAverage")).isEmpty();
        verify(courseFeignClient).listTeacherClasses(7L, null, null, null, null, 2L);
    }

    private static KnowledgeMasteryDTO mastery(Long studentId, Long courseId, Long classId, String score) {
        return mastery(studentId, courseId, classId, score, "exam", studentId + 1000);
    }

    private static KnowledgeMasteryDTO mastery(
            Long studentId,
            Long courseId,
            Long classId,
            Long knowledgePointId,
            String score) {
        return mastery(studentId, courseId, classId, knowledgePointId, score, "exam", studentId + 1000);
    }

    private static KnowledgeMasteryDTO mastery(
            Long studentId,
            Long courseId,
            Long classId,
            Long knowledgePointId,
            String score,
            String sourceType,
            Long sourceId) {
        return new KnowledgeMasteryDTO(
                studentId,
                courseId,
                classId,
                knowledgePointId,
                new BigDecimal(score),
                1,
                sourceType,
                sourceId,
                "event-" + studentId,
                Instant.parse("2026-05-20T08:00:00Z"));
    }

    private static KnowledgeMasteryDTO mastery(
            Long studentId,
            Long courseId,
            Long classId,
            String score,
            String sourceType,
            Long sourceId) {
        return new KnowledgeMasteryDTO(
                studentId,
                courseId,
                classId,
                null,
                new BigDecimal(score),
                1,
                sourceType,
                sourceId,
                "event-" + studentId,
                Instant.parse("2026-05-20T08:00:00Z"));
    }

    private static CourseDTO course(Long courseId, String courseName, Long teacherId) {
        CourseDTO course = new CourseDTO();
        course.setId(courseId);
        course.setCourseName(courseName);
        course.setTeacherId(teacherId);
        return course;
    }

    private static TeacherClassDTO teacherClass(Long classId, Long teacherId, String className) {
        TeacherClassDTO teacherClass = new TeacherClassDTO();
        teacherClass.setId(classId);
        teacherClass.setTeacherId(teacherId);
        teacherClass.setClassName(className);
        return teacherClass;
    }

    private static AssignmentDTO assignment(Long assignmentId, String title, Long courseId, Long teacherId) {
        AssignmentDTO assignment = new AssignmentDTO();
        assignment.setId(assignmentId);
        assignment.setTitle(title);
        assignment.setCourseId(courseId);
        assignment.setTeacherId(teacherId);
        return assignment;
    }

    private static ExamDTO exam(Long examId, String title, Long courseId) {
        ExamDTO exam = new ExamDTO();
        exam.setId(examId);
        exam.setTitle(title);
        exam.setCourseId(courseId);
        return exam;
    }

    private static UserProfileDTO user(Long userId, String name, String username) {
        UserProfileDTO user = new UserProfileDTO();
        user.setId(userId);
        user.setName(name);
        user.setUsername(username);
        return user;
    }

    private static final class FakeAnalysisRepository implements AnalysisRepository {

        private final List<KnowledgeMasteryDTO> masteryRows = new ArrayList<>();
        private Long requestedClassId;
        private Long requestedCourseId;

        @Override
        public void upsertScoreTrend(ScoreTrendRecord record) {
        }

        @Override
        public void upsertKnowledgeMastery(KnowledgeMasteryRecord record) {
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
            requestedClassId = classId;
            requestedCourseId = courseId;
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
}
