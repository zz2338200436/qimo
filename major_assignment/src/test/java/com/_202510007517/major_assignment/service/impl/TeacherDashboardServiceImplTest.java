package com._202510007517.major_assignment.service.impl;

import com._202510007517.major_assignment.entity.Course;
import com._202510007517.major_assignment.entity.EarlyWarning;
import com._202510007517.major_assignment.entity.dto.TeacherDashboardDTO;
import com._202510007517.major_assignment.entity.User;
import com._202510007517.major_assignment.entity.dto.StudentLearningSummaryDTO;
import com._202510007517.major_assignment.mapper.AssignmentMapper;
import com._202510007517.major_assignment.mapper.AssignmentSubmissionMapper;
import com._202510007517.major_assignment.mapper.CourseMapper;
import com._202510007517.major_assignment.mapper.EarlyWarningMapper;
import com._202510007517.major_assignment.mapper.ExamMapper;
import com._202510007517.major_assignment.mapper.ExamSubmissionMapper;
import com._202510007517.major_assignment.mapper.StudentMapper;
import com._202510007517.major_assignment.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TeacherDashboardServiceImplTest {

    @Test
    void getStudentLearningSummary_reusesClassNameLookupWhenSameStudentHasMultipleCourses() {
        CourseMapper courseMapper = mock(CourseMapper.class);
        AssignmentMapper assignmentMapper = mock(AssignmentMapper.class);
        AssignmentSubmissionMapper assignmentSubmissionMapper = mock(AssignmentSubmissionMapper.class);
        EarlyWarningMapper earlyWarningMapper = mock(EarlyWarningMapper.class);
        ExamMapper examMapper = mock(ExamMapper.class);
        ExamSubmissionMapper examSubmissionMapper = mock(ExamSubmissionMapper.class);
        StudentMapper studentMapper = mock(StudentMapper.class);
        UserMapper userMapper = mock(UserMapper.class);

        TeacherDashboardServiceImpl service = new TeacherDashboardServiceImpl();
        ReflectionTestUtils.setField(service, "courseMapper", courseMapper);
        ReflectionTestUtils.setField(service, "assignmentMapper", assignmentMapper);
        ReflectionTestUtils.setField(service, "assignmentSubmissionMapper", assignmentSubmissionMapper);
        ReflectionTestUtils.setField(service, "earlyWarningMapper", earlyWarningMapper);
        ReflectionTestUtils.setField(service, "examMapper", examMapper);
        ReflectionTestUtils.setField(service, "examSubmissionMapper", examSubmissionMapper);
        ReflectionTestUtils.setField(service, "studentMapper", studentMapper);
        ReflectionTestUtils.setField(service, "userMapper", userMapper);

        Course c1 = new Course();
        c1.setId(11L);
        c1.setCourseName("软件工程");
        Course c2 = new Course();
        c2.setId(12L);
        c2.setCourseName("数据库系统");

        when(courseMapper.findByTeacherId(5L)).thenReturn(List.of(c1, c2));
        when(courseMapper.getStudentIdsByCourseId(11L)).thenReturn(List.of(100L));
        when(courseMapper.getStudentIdsByCourseId(12L)).thenReturn(List.of(100L));

        User student = new User();
        student.setId(100L);
        student.setName("张三");
        when(userMapper.findById(100L)).thenReturn(student);
        when(userMapper.getStudentClassName(100L)).thenReturn("计科 1 班");
        when(studentMapper.getTeacherStudentCoursePerformance(100L, 11L, "month")).thenReturn(Map.of(
                "averageScore", 82.0,
                "overallProgress", 76,
                "pendingAssignments", 1
        ));
        when(studentMapper.getTeacherStudentCoursePerformance(100L, 12L, "month")).thenReturn(Map.of(
                "averageScore", 82.0,
                "overallProgress", 76,
                "pendingAssignments", 1
        ));

        StudentLearningSummaryDTO result = service.getStudentLearningSummary(5L, null, null, "month");

        assertThat(result.getTotalStudents()).isEqualTo(1);
        assertThat(result.getStudentPerformances()).hasSize(2);
        assertThat(result.getStudentPerformances())
                .extracting(StudentLearningSummaryDTO.StudentPerformanceDTO::getClassName)
                .containsOnly("计科 1 班");
        assertThat(result.getStudentPerformances())
                .extracting(StudentLearningSummaryDTO.StudentPerformanceDTO::getCourseId)
                .containsExactlyInAnyOrder(11L, 12L);

        verify(userMapper, times(1)).getStudentClassName(100L);
    }

    @Test
    void getStudentLearningSummary_aggregatesSummaryMetricsPerStudentInsteadOfPerCourseRow() {
        CourseMapper courseMapper = mock(CourseMapper.class);
        AssignmentMapper assignmentMapper = mock(AssignmentMapper.class);
        AssignmentSubmissionMapper assignmentSubmissionMapper = mock(AssignmentSubmissionMapper.class);
        EarlyWarningMapper earlyWarningMapper = mock(EarlyWarningMapper.class);
        ExamMapper examMapper = mock(ExamMapper.class);
        ExamSubmissionMapper examSubmissionMapper = mock(ExamSubmissionMapper.class);
        StudentMapper studentMapper = mock(StudentMapper.class);
        UserMapper userMapper = mock(UserMapper.class);

        TeacherDashboardServiceImpl service = new TeacherDashboardServiceImpl();
        ReflectionTestUtils.setField(service, "courseMapper", courseMapper);
        ReflectionTestUtils.setField(service, "assignmentMapper", assignmentMapper);
        ReflectionTestUtils.setField(service, "assignmentSubmissionMapper", assignmentSubmissionMapper);
        ReflectionTestUtils.setField(service, "earlyWarningMapper", earlyWarningMapper);
        ReflectionTestUtils.setField(service, "examMapper", examMapper);
        ReflectionTestUtils.setField(service, "examSubmissionMapper", examSubmissionMapper);
        ReflectionTestUtils.setField(service, "studentMapper", studentMapper);
        ReflectionTestUtils.setField(service, "userMapper", userMapper);

        Course c1 = new Course();
        c1.setId(11L);
        c1.setCourseName("软件工程");
        Course c2 = new Course();
        c2.setId(12L);
        c2.setCourseName("数据库系统");

        when(courseMapper.findByTeacherId(5L)).thenReturn(List.of(c1, c2));
        when(courseMapper.getStudentIdsByCourseId(11L)).thenReturn(List.of(100L));
        when(courseMapper.getStudentIdsByCourseId(12L)).thenReturn(List.of(100L));

        User student = new User();
        student.setId(100L);
        student.setName("张三");
        when(userMapper.findById(100L)).thenReturn(student);
        when(userMapper.getStudentClassName(100L)).thenReturn("计科 1 班");
        when(studentMapper.getTeacherStudentCoursePerformance(100L, 11L, "month")).thenReturn(Map.of(
                "averageScore", 90.0,
                "overallProgress", 92,
                "pendingAssignments", 1
        ));
        when(studentMapper.getTeacherStudentCoursePerformance(100L, 12L, "month")).thenReturn(Map.of(
                "averageScore", 74.0,
                "overallProgress", 60,
                "pendingAssignments", 1
        ));

        StudentLearningSummaryDTO result = service.getStudentLearningSummary(5L, null, null, "month");

        assertThat(result.getTotalStudents()).isEqualTo(1);
        assertThat(result.getStudentPerformances()).hasSize(2);
        assertThat(result.getAverageScore()).isEqualTo(82.0);
        assertThat(result.getOverallProgress()).isEqualTo(76.0);
        assertThat(result.getTotalPendingAssignments()).isEqualTo(2);
    }

    @Test
    void getStudentLearningSummary_usesCourseScopedLearningStatsForQuarterTimeRange() {
        CourseMapper courseMapper = mock(CourseMapper.class);
        AssignmentMapper assignmentMapper = mock(AssignmentMapper.class);
        AssignmentSubmissionMapper assignmentSubmissionMapper = mock(AssignmentSubmissionMapper.class);
        EarlyWarningMapper earlyWarningMapper = mock(EarlyWarningMapper.class);
        ExamMapper examMapper = mock(ExamMapper.class);
        ExamSubmissionMapper examSubmissionMapper = mock(ExamSubmissionMapper.class);
        StudentMapper studentMapper = mock(StudentMapper.class);
        UserMapper userMapper = mock(UserMapper.class);

        TeacherDashboardServiceImpl service = new TeacherDashboardServiceImpl();
        ReflectionTestUtils.setField(service, "courseMapper", courseMapper);
        ReflectionTestUtils.setField(service, "assignmentMapper", assignmentMapper);
        ReflectionTestUtils.setField(service, "assignmentSubmissionMapper", assignmentSubmissionMapper);
        ReflectionTestUtils.setField(service, "earlyWarningMapper", earlyWarningMapper);
        ReflectionTestUtils.setField(service, "examMapper", examMapper);
        ReflectionTestUtils.setField(service, "examSubmissionMapper", examSubmissionMapper);
        ReflectionTestUtils.setField(service, "studentMapper", studentMapper);
        ReflectionTestUtils.setField(service, "userMapper", userMapper);

        Course c1 = new Course();
        c1.setId(11L);
        c1.setCourseName("软件工程");
        Course c2 = new Course();
        c2.setId(12L);
        c2.setCourseName("数据库系统");

        when(courseMapper.findByTeacherId(5L)).thenReturn(List.of(c1, c2));
        when(courseMapper.getStudentIdsByCourseId(11L)).thenReturn(List.of(100L));
        when(courseMapper.getStudentIdsByCourseId(12L)).thenReturn(List.of(100L));

        User student = new User();
        student.setId(100L);
        student.setName("张三");
        when(userMapper.findById(100L)).thenReturn(student);
        when(userMapper.getStudentClassName(100L)).thenReturn("计科 1 班");

        when(studentMapper.getTeacherStudentCoursePerformance(100L, 11L, "quarter")).thenReturn(Map.of(
                "averageScore", 91.0,
                "pendingAssignments", 2,
                "overallProgress", 78
        ));
        when(studentMapper.getTeacherStudentCoursePerformance(100L, 12L, "quarter")).thenReturn(Map.of(
                "averageScore", 63.0,
                "pendingAssignments", 4,
                "overallProgress", 40
        ));

        StudentLearningSummaryDTO result = service.getStudentLearningSummary(5L, null, null, "quarter");

        assertThat(result.getStudentPerformances())
                .extracting(
                        StudentLearningSummaryDTO.StudentPerformanceDTO::getCourseId,
                        StudentLearningSummaryDTO.StudentPerformanceDTO::getAverageScore,
                        StudentLearningSummaryDTO.StudentPerformanceDTO::getPendingAssignments,
                        StudentLearningSummaryDTO.StudentPerformanceDTO::getOverallProgress
                )
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(11L, 91, 2, 78),
                        org.assertj.core.groups.Tuple.tuple(12L, 63, 4, 40)
                );

        verify(studentMapper).getTeacherStudentCoursePerformance(100L, 11L, "quarter");
        verify(studentMapper).getTeacherStudentCoursePerformance(100L, 12L, "quarter");
    }

    @Test
    void getDashboardData_usesBatchStudentCountsWhenTeacherHasMultipleCoursesWithoutClassFilter() {
        CourseMapper courseMapper = mock(CourseMapper.class);
        AssignmentMapper assignmentMapper = mock(AssignmentMapper.class);
        AssignmentSubmissionMapper assignmentSubmissionMapper = mock(AssignmentSubmissionMapper.class);
        EarlyWarningMapper earlyWarningMapper = mock(EarlyWarningMapper.class);
        ExamMapper examMapper = mock(ExamMapper.class);
        ExamSubmissionMapper examSubmissionMapper = mock(ExamSubmissionMapper.class);
        StudentMapper studentMapper = mock(StudentMapper.class);
        UserMapper userMapper = mock(UserMapper.class);

        TeacherDashboardServiceImpl service = new TeacherDashboardServiceImpl();
        ReflectionTestUtils.setField(service, "courseMapper", courseMapper);
        ReflectionTestUtils.setField(service, "assignmentMapper", assignmentMapper);
        ReflectionTestUtils.setField(service, "assignmentSubmissionMapper", assignmentSubmissionMapper);
        ReflectionTestUtils.setField(service, "earlyWarningMapper", earlyWarningMapper);
        ReflectionTestUtils.setField(service, "examMapper", examMapper);
        ReflectionTestUtils.setField(service, "examSubmissionMapper", examSubmissionMapper);
        ReflectionTestUtils.setField(service, "studentMapper", studentMapper);
        ReflectionTestUtils.setField(service, "userMapper", userMapper);

        Course c1 = new Course();
        c1.setId(11L);
        c1.setCourseName("软件工程");
        Course c2 = new Course();
        c2.setId(12L);
        c2.setCourseName("数据库系统");

        when(courseMapper.findByTeacherId(5L)).thenReturn(List.of(c1, c2));
        when(courseMapper.getStudentIdsByClassTeacherId(5L)).thenReturn(List.of(100L, 101L, 102L));
        when(courseMapper.batchGetStudentCountByCourseIds(List.of(11L, 12L))).thenReturn(List.of(
                Map.of("courseId", 11L, "studentCount", 30),
                Map.of("courseId", 12L, "studentCount", 28)
        ));
        when(courseMapper.batchGetCourseAverageScoresByCourseIds(List.of(11L, 12L))).thenReturn(List.of(
                Map.of("courseId", 11L, "averageScore", 85.0),
                Map.of("courseId", 12L, "averageScore", 88.0)
        ));

        when(assignmentSubmissionMapper.countUngradedSubmissionsByTeacher(5L)).thenReturn(0);
        when(examSubmissionMapper.countUngradedExamsByTeacher(5L)).thenReturn(0);
        when(earlyWarningMapper.findUnresolvedByTeacherId(5L)).thenReturn(List.of());
        when(assignmentMapper.countUpcomingAssignmentsByTeacher(5L)).thenReturn(0);
        when(examMapper.countUpcomingExamsByTeacher(5L)).thenReturn(0);
        when(assignmentMapper.countMissingSubmissionsByTeacher(5L, null, null)).thenReturn(0);
        when(assignmentSubmissionMapper.getSubmissionCountsByDay(5L)).thenReturn(List.of());
        when(assignmentSubmissionMapper.getMaxDailySubmissionsByTeacher(5L)).thenReturn(0);
        when(assignmentSubmissionMapper.getRecentSubmissionsByTeacher(5L, 5)).thenReturn(List.of());
        when(examSubmissionMapper.getRecentSubmissionsByTeacher(5L, 3)).thenReturn(List.of());
        when(earlyWarningMapper.findRecentByTeacherId(5L, 3)).thenReturn(List.of());
        when(assignmentSubmissionMapper.getRecentGradedByTeacher(5L, 3)).thenReturn(List.of());

        TeacherDashboardDTO result = service.getDashboardData(5L, null, null, "month");

        assertThat(result.getTotalCourses()).isEqualTo(2);
        assertThat(result.getTotalStudents()).isEqualTo(3);
        verify(courseMapper, times(1)).batchGetStudentCountByCourseIds(List.of(11L, 12L));
        verify(courseMapper, times(1)).batchGetCourseAverageScoresByCourseIds(List.of(11L, 12L));
        verify(courseMapper, never()).getStudentCountByCourseId(11L);
        verify(courseMapper, never()).getStudentCountByCourseId(12L);
        verify(courseMapper, never()).getCourseAverageScore(11L);
        verify(courseMapper, never()).getCourseAverageScore(12L);
    }

    @Test
    void getDashboardData_preservesClassFilteredAverageScores() {
        CourseMapper courseMapper = mock(CourseMapper.class);
        AssignmentMapper assignmentMapper = mock(AssignmentMapper.class);
        AssignmentSubmissionMapper assignmentSubmissionMapper = mock(AssignmentSubmissionMapper.class);
        EarlyWarningMapper earlyWarningMapper = mock(EarlyWarningMapper.class);
        ExamMapper examMapper = mock(ExamMapper.class);
        ExamSubmissionMapper examSubmissionMapper = mock(ExamSubmissionMapper.class);
        StudentMapper studentMapper = mock(StudentMapper.class);
        UserMapper userMapper = mock(UserMapper.class);

        TeacherDashboardServiceImpl service = new TeacherDashboardServiceImpl();
        ReflectionTestUtils.setField(service, "courseMapper", courseMapper);
        ReflectionTestUtils.setField(service, "assignmentMapper", assignmentMapper);
        ReflectionTestUtils.setField(service, "assignmentSubmissionMapper", assignmentSubmissionMapper);
        ReflectionTestUtils.setField(service, "earlyWarningMapper", earlyWarningMapper);
        ReflectionTestUtils.setField(service, "examMapper", examMapper);
        ReflectionTestUtils.setField(service, "examSubmissionMapper", examSubmissionMapper);
        ReflectionTestUtils.setField(service, "studentMapper", studentMapper);
        ReflectionTestUtils.setField(service, "userMapper", userMapper);

        Course c1 = new Course();
        c1.setId(11L);
        c1.setCourseName("软件工程");

        when(courseMapper.findByTeacherId(5L)).thenReturn(List.of(c1));
        when(studentMapper.getStudentsByClassId(9L)).thenReturn(List.of(Map.of("id", 100L)));
        when(courseMapper.getStudentCountByCourseIdAndClassId(11L, 9L)).thenReturn(18);
        when(courseMapper.getCourseAverageScoreByClassId(11L, 9L)).thenReturn(72.5);
        when(courseMapper.getCourseScores(11L)).thenReturn(List.of(90.0));
        when(courseMapper.getCourseAverageScore(11L)).thenReturn(90.0);

        when(assignmentSubmissionMapper.countUngradedSubmissionsByTeacher(5L)).thenReturn(0);
        when(examSubmissionMapper.countUngradedExamsByTeacher(5L)).thenReturn(0);
        when(earlyWarningMapper.findUnresolvedByTeacherId(5L)).thenReturn(List.of());
        when(assignmentMapper.countUpcomingAssignmentsByTeacher(5L)).thenReturn(0);
        when(examMapper.countUpcomingExamsByTeacher(5L)).thenReturn(0);
        when(assignmentMapper.countMissingSubmissionsByTeacher(5L, 9L, null)).thenReturn(0);
        when(assignmentSubmissionMapper.getSubmissionCountsByDay(5L)).thenReturn(List.of());
        when(assignmentSubmissionMapper.getMaxDailySubmissionsByTeacher(5L)).thenReturn(0);
        when(assignmentSubmissionMapper.getRecentSubmissionsByTeacher(5L, 5)).thenReturn(List.of());
        when(examSubmissionMapper.getRecentSubmissionsByTeacher(5L, 3)).thenReturn(List.of());
        when(earlyWarningMapper.findRecentByTeacherId(5L, 3)).thenReturn(List.of());
        when(assignmentSubmissionMapper.getRecentGradedByTeacher(5L, 3)).thenReturn(List.of());

        TeacherDashboardDTO result = service.getDashboardData(5L, 9L, null, "month");

        assertThat(result.getAverageScores()).containsExactly(72.5);
        verify(courseMapper, times(1)).getCourseAverageScoreByClassId(11L, 9L);
        verify(courseMapper, never()).getCourseAverageScore(11L);
    }

    @Test
    void getDashboardData_exposesRealOverallProgressInsteadOfReusingAverageScore() throws Exception {
        CourseMapper courseMapper = mock(CourseMapper.class);
        AssignmentMapper assignmentMapper = mock(AssignmentMapper.class);
        AssignmentSubmissionMapper assignmentSubmissionMapper = mock(AssignmentSubmissionMapper.class);
        EarlyWarningMapper earlyWarningMapper = mock(EarlyWarningMapper.class);
        ExamMapper examMapper = mock(ExamMapper.class);
        ExamSubmissionMapper examSubmissionMapper = mock(ExamSubmissionMapper.class);
        StudentMapper studentMapper = mock(StudentMapper.class);
        UserMapper userMapper = mock(UserMapper.class);

        TeacherDashboardServiceImpl service = new TeacherDashboardServiceImpl();
        ReflectionTestUtils.setField(service, "courseMapper", courseMapper);
        ReflectionTestUtils.setField(service, "assignmentMapper", assignmentMapper);
        ReflectionTestUtils.setField(service, "assignmentSubmissionMapper", assignmentSubmissionMapper);
        ReflectionTestUtils.setField(service, "earlyWarningMapper", earlyWarningMapper);
        ReflectionTestUtils.setField(service, "examMapper", examMapper);
        ReflectionTestUtils.setField(service, "examSubmissionMapper", examSubmissionMapper);
        ReflectionTestUtils.setField(service, "studentMapper", studentMapper);
        ReflectionTestUtils.setField(service, "userMapper", userMapper);

        Course c1 = new Course();
        c1.setId(11L);
        c1.setCourseName("软件工程");
        Course c2 = new Course();
        c2.setId(12L);
        c2.setCourseName("数据库系统");

        when(courseMapper.findByTeacherId(5L)).thenReturn(List.of(c1, c2));
        when(courseMapper.getStudentIdsByClassTeacherId(5L)).thenReturn(List.of(100L));
        when(courseMapper.getStudentIdsByCourseId(11L)).thenReturn(List.of(100L));
        when(courseMapper.getStudentIdsByCourseId(12L)).thenReturn(List.of(100L));
        when(courseMapper.batchGetStudentCountByCourseIds(List.of(11L, 12L))).thenReturn(List.of(
                Map.of("courseId", 11L, "studentCount", 30),
                Map.of("courseId", 12L, "studentCount", 28)
        ));
        when(courseMapper.batchGetCourseAverageScoresByCourseIds(List.of(11L, 12L))).thenReturn(List.of(
                Map.of("courseId", 11L, "averageScore", 90.0),
                Map.of("courseId", 12L, "averageScore", 90.0)
        ));

        User student = new User();
        student.setId(100L);
        student.setName("张三");
        when(userMapper.findById(100L)).thenReturn(student);
        when(userMapper.getStudentClassName(100L)).thenReturn("计科 1 班");
        when(studentMapper.getTeacherStudentCoursePerformance(100L, 11L, "month")).thenReturn(Map.of(
                "averageScore", 90.0,
                "overallProgress", 40,
                "pendingAssignments", 1
        ));
        when(studentMapper.getTeacherStudentCoursePerformance(100L, 12L, "month")).thenReturn(Map.of(
                "averageScore", 90.0,
                "overallProgress", 60,
                "pendingAssignments", 2
        ));

        when(assignmentSubmissionMapper.countUngradedSubmissionsByTeacher(5L)).thenReturn(0);
        when(examSubmissionMapper.countUngradedExamsByTeacher(5L)).thenReturn(0);
        when(earlyWarningMapper.findUnresolvedByTeacherId(5L)).thenReturn(List.of());
        when(assignmentMapper.countUpcomingAssignmentsByTeacher(5L)).thenReturn(0);
        when(examMapper.countUpcomingExamsByTeacher(5L)).thenReturn(0);
        when(assignmentMapper.countMissingSubmissionsByTeacher(5L, null, null)).thenReturn(0);
        when(assignmentSubmissionMapper.getSubmissionCountsByDay(5L)).thenReturn(List.of());
        when(assignmentSubmissionMapper.getMaxDailySubmissionsByTeacher(5L)).thenReturn(0);
        when(assignmentSubmissionMapper.getRecentSubmissionsByTeacher(5L, 5)).thenReturn(List.of());
        when(examSubmissionMapper.getRecentSubmissionsByTeacher(5L, 3)).thenReturn(List.of());
        when(earlyWarningMapper.findRecentByTeacherId(5L, 3)).thenReturn(List.of());
        when(assignmentSubmissionMapper.getRecentGradedByTeacher(5L, 3)).thenReturn(List.of());

        TeacherDashboardDTO result = service.getDashboardData(5L, null, null, "month");

        Object overallProgressField = result.getClass().getMethod("getOverallProgress").invoke(result);
        assertThat(overallProgressField).isEqualTo(50.0);
        assertThat(result.getAverageScores()).containsExactly(90.0, 90.0);
    }

    @Test
    void getDashboardData_usesStudentNameFromRecentWarningsWithoutPerWarningUserLookup() {
        CourseMapper courseMapper = mock(CourseMapper.class);
        AssignmentMapper assignmentMapper = mock(AssignmentMapper.class);
        AssignmentSubmissionMapper assignmentSubmissionMapper = mock(AssignmentSubmissionMapper.class);
        EarlyWarningMapper earlyWarningMapper = mock(EarlyWarningMapper.class);
        ExamMapper examMapper = mock(ExamMapper.class);
        ExamSubmissionMapper examSubmissionMapper = mock(ExamSubmissionMapper.class);
        StudentMapper studentMapper = mock(StudentMapper.class);
        UserMapper userMapper = mock(UserMapper.class);

        TeacherDashboardServiceImpl service = new TeacherDashboardServiceImpl();
        ReflectionTestUtils.setField(service, "courseMapper", courseMapper);
        ReflectionTestUtils.setField(service, "assignmentMapper", assignmentMapper);
        ReflectionTestUtils.setField(service, "assignmentSubmissionMapper", assignmentSubmissionMapper);
        ReflectionTestUtils.setField(service, "earlyWarningMapper", earlyWarningMapper);
        ReflectionTestUtils.setField(service, "examMapper", examMapper);
        ReflectionTestUtils.setField(service, "examSubmissionMapper", examSubmissionMapper);
        ReflectionTestUtils.setField(service, "studentMapper", studentMapper);
        ReflectionTestUtils.setField(service, "userMapper", userMapper);

        when(courseMapper.findByTeacherId(5L)).thenReturn(List.of());
        when(courseMapper.getStudentIdsByClassTeacherId(5L)).thenReturn(List.of());
        when(assignmentSubmissionMapper.countUngradedSubmissionsByTeacher(5L)).thenReturn(0);
        when(examSubmissionMapper.countUngradedExamsByTeacher(5L)).thenReturn(0);
        when(earlyWarningMapper.findUnresolvedByTeacherId(5L)).thenReturn(List.of());
        when(assignmentMapper.countUpcomingAssignmentsByTeacher(5L)).thenReturn(0);
        when(examMapper.countUpcomingExamsByTeacher(5L)).thenReturn(0);
        when(assignmentMapper.countMissingSubmissionsByTeacher(5L, null, null)).thenReturn(0);
        when(assignmentSubmissionMapper.getSubmissionCountsByDay(5L)).thenReturn(List.of());
        when(assignmentSubmissionMapper.getMaxDailySubmissionsByTeacher(5L)).thenReturn(0);
        when(assignmentSubmissionMapper.getRecentSubmissionsByTeacher(5L, 5)).thenReturn(List.of());
        when(examSubmissionMapper.getRecentSubmissionsByTeacher(5L, 3)).thenReturn(List.of());
        when(assignmentSubmissionMapper.getRecentGradedByTeacher(5L, 3)).thenReturn(List.of());

        EarlyWarning warning = new EarlyWarning();
        warning.setStudentId(100L);
        warning.setStudentName("张三");
        warning.setWarningType("成绩");
        warning.setTriggerDate(LocalDateTime.of(2026, 5, 24, 10, 0));
        when(earlyWarningMapper.findRecentByTeacherId(5L, 3)).thenReturn(List.of(warning));

        TeacherDashboardDTO result = service.getDashboardData(5L, null, null, "month");

        assertThat(result.getRecentActivities()).isNotEmpty();
        assertThat(result.getRecentActivities())
                .extracting(TeacherDashboardDTO.RecentActivityDTO::getStudentName)
                .contains("张三");
        verify(userMapper, never()).findById(100L);
    }

    @Test
    void getDashboardData_preservesYesterdayChangeMetrics() {
        CourseMapper courseMapper = mock(CourseMapper.class);
        AssignmentMapper assignmentMapper = mock(AssignmentMapper.class);
        AssignmentSubmissionMapper assignmentSubmissionMapper = mock(AssignmentSubmissionMapper.class);
        EarlyWarningMapper earlyWarningMapper = mock(EarlyWarningMapper.class);
        ExamMapper examMapper = mock(ExamMapper.class);
        ExamSubmissionMapper examSubmissionMapper = mock(ExamSubmissionMapper.class);
        StudentMapper studentMapper = mock(StudentMapper.class);
        UserMapper userMapper = mock(UserMapper.class);

        TeacherDashboardServiceImpl service = new TeacherDashboardServiceImpl();
        ReflectionTestUtils.setField(service, "courseMapper", courseMapper);
        ReflectionTestUtils.setField(service, "assignmentMapper", assignmentMapper);
        ReflectionTestUtils.setField(service, "assignmentSubmissionMapper", assignmentSubmissionMapper);
        ReflectionTestUtils.setField(service, "earlyWarningMapper", earlyWarningMapper);
        ReflectionTestUtils.setField(service, "examMapper", examMapper);
        ReflectionTestUtils.setField(service, "examSubmissionMapper", examSubmissionMapper);
        ReflectionTestUtils.setField(service, "studentMapper", studentMapper);
        ReflectionTestUtils.setField(service, "userMapper", userMapper);

        when(courseMapper.findByTeacherId(5L)).thenReturn(List.of());
        when(courseMapper.getStudentIdsByClassTeacherId(5L)).thenReturn(List.of());
        when(assignmentSubmissionMapper.countUngradedSubmissionsByTeacher(5L)).thenReturn(11);
        when(examSubmissionMapper.countUngradedExamsByTeacher(5L)).thenReturn(6);
        when(earlyWarningMapper.findUnresolvedByTeacherId(5L)).thenReturn(List.of(new EarlyWarning(), new EarlyWarning(), new EarlyWarning(), new EarlyWarning(), new EarlyWarning(), new EarlyWarning()));
        when(assignmentMapper.countUpcomingAssignmentsByTeacher(5L)).thenReturn(4);
        when(examMapper.countUpcomingExamsByTeacher(5L)).thenReturn(3);
        when(assignmentMapper.countMissingSubmissionsByTeacher(5L, null, null)).thenReturn(11);
        when(assignmentSubmissionMapper.getSubmissionCountsByDay(5L)).thenReturn(List.of());
        when(assignmentSubmissionMapper.getMaxDailySubmissionsByTeacher(5L)).thenReturn(0);
        when(assignmentSubmissionMapper.getRecentSubmissionsByTeacher(5L, 5)).thenReturn(List.of());
        when(examSubmissionMapper.getRecentSubmissionsByTeacher(5L, 3)).thenReturn(List.of());
        when(earlyWarningMapper.findRecentByTeacherId(5L, 3)).thenReturn(List.of());
        when(assignmentSubmissionMapper.getRecentGradedByTeacher(5L, 3)).thenReturn(List.of());

        TeacherDashboardDTO result = service.getDashboardData(5L, null, null, "month");

        assertThat(result.getPendingAssignmentsChange()).isEqualTo(-8);
        assertThat(result.getPendingExamsChange()).isEqualTo(2);
        assertThat(result.getWarningCountChange()).isEqualTo(3);
        assertThat(result.getUpcomingDeadlinesChange()).isEqualTo(3);
        assertThat(result.getMissingSubmissionsChange()).isEqualTo(5);
    }

    @Test
    void getScoreTrend_passesStudentIdFilterToMapper() {
        CourseMapper courseMapper = mock(CourseMapper.class);
        AssignmentMapper assignmentMapper = mock(AssignmentMapper.class);
        AssignmentSubmissionMapper assignmentSubmissionMapper = mock(AssignmentSubmissionMapper.class);
        EarlyWarningMapper earlyWarningMapper = mock(EarlyWarningMapper.class);
        ExamMapper examMapper = mock(ExamMapper.class);
        ExamSubmissionMapper examSubmissionMapper = mock(ExamSubmissionMapper.class);
        StudentMapper studentMapper = mock(StudentMapper.class);
        UserMapper userMapper = mock(UserMapper.class);

        TeacherDashboardServiceImpl service = new TeacherDashboardServiceImpl();
        ReflectionTestUtils.setField(service, "courseMapper", courseMapper);
        ReflectionTestUtils.setField(service, "assignmentMapper", assignmentMapper);
        ReflectionTestUtils.setField(service, "assignmentSubmissionMapper", assignmentSubmissionMapper);
        ReflectionTestUtils.setField(service, "earlyWarningMapper", earlyWarningMapper);
        ReflectionTestUtils.setField(service, "examMapper", examMapper);
        ReflectionTestUtils.setField(service, "examSubmissionMapper", examSubmissionMapper);
        ReflectionTestUtils.setField(service, "studentMapper", studentMapper);
        ReflectionTestUtils.setField(service, "userMapper", userMapper);

        when(assignmentSubmissionMapper.getAssignmentScoreTrend(
                eq(5L), eq(9L), eq(11L), eq(100L), any(Date.class), any(Date.class)
        )).thenReturn(List.of(Map.of("day", "2026-05-01", "avgScore", 86.5)));

        var result = service.getScoreTrend(5L, 9L, 11L, 100L, "week");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDate()).isEqualTo("2026-05-01");
        assertThat(result.get(0).getAverageScore()).isEqualTo(86.5);
        verify(assignmentSubmissionMapper).getAssignmentScoreTrend(
                eq(5L), eq(9L), eq(11L), eq(100L), any(Date.class), any(Date.class)
        );
    }

    @Test
    void getScoreTrend_supportsQuarterTimeRange() {
        CourseMapper courseMapper = mock(CourseMapper.class);
        AssignmentMapper assignmentMapper = mock(AssignmentMapper.class);
        AssignmentSubmissionMapper assignmentSubmissionMapper = mock(AssignmentSubmissionMapper.class);
        EarlyWarningMapper earlyWarningMapper = mock(EarlyWarningMapper.class);
        ExamMapper examMapper = mock(ExamMapper.class);
        ExamSubmissionMapper examSubmissionMapper = mock(ExamSubmissionMapper.class);
        StudentMapper studentMapper = mock(StudentMapper.class);
        UserMapper userMapper = mock(UserMapper.class);

        TeacherDashboardServiceImpl service = new TeacherDashboardServiceImpl();
        ReflectionTestUtils.setField(service, "courseMapper", courseMapper);
        ReflectionTestUtils.setField(service, "assignmentMapper", assignmentMapper);
        ReflectionTestUtils.setField(service, "assignmentSubmissionMapper", assignmentSubmissionMapper);
        ReflectionTestUtils.setField(service, "earlyWarningMapper", earlyWarningMapper);
        ReflectionTestUtils.setField(service, "examMapper", examMapper);
        ReflectionTestUtils.setField(service, "examSubmissionMapper", examSubmissionMapper);
        ReflectionTestUtils.setField(service, "studentMapper", studentMapper);
        ReflectionTestUtils.setField(service, "userMapper", userMapper);

        when(assignmentSubmissionMapper.getAssignmentScoreTrend(
                eq(5L), eq(null), eq(null), eq(null), any(Date.class), any(Date.class)
        )).thenReturn(List.of(Map.of("day", "2026-05-01", "avgScore", 80.0)));

        var result = service.getScoreTrend(5L, null, null, null, "quarter");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAverageScore()).isEqualTo(80.0);
        verify(assignmentSubmissionMapper).getAssignmentScoreTrend(
                eq(5L), eq(null), eq(null), eq(null), any(Date.class), any(Date.class)
        );
    }
}
