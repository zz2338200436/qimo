package com._202510007517.major_assignment.service.impl;

import com._202510007517.major_assignment.entity.Course;
import com._202510007517.major_assignment.entity.dto.StudentDashboardDTO;
import com._202510007517.major_assignment.mapper.StudentMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StudentServiceImplTest {

    @Test
    void getLearningStats_noLongerReturnsPlaceholderChangeFields() {
        StudentMapper mapper = mock(StudentMapper.class);

        StudentServiceImpl service = new StudentServiceImpl();
        ReflectionTestUtils.setField(service, "studentMapper", mapper);

        when(mapper.getLearningStats(42L, null, null, null))
                .thenReturn(Map.of(
                        "completedAssignments", 5,
                        "averageScore", 88.5
                ));
        when(mapper.getStudyTimeDistribution(42L, "daily", null, null, null))
                .thenReturn(List.of(Map.of("study_time", 2.5)));
        when(mapper.getKnowledgePoints(42L, null, null, null))
                .thenReturn(List.of(Map.of("name", "链表", "mastery", 92.0)));

        Map<String, Object> result = service.getLearningStats(42L, null, null, null);

        assertThat(result)
                .doesNotContainKeys(
                        "studyTimeChange",
                        "completedTasksChange",
                        "averageScoreChange",
                        "knowledgeMasteryChange"
                );
        assertThat(result)
                .containsEntry("studyTime", 2.5)
                .containsEntry("completedTasks", 5);
        assertThat(result.get("averageScore")).isEqualTo(88.5);
        assertThat(result.get("knowledgeMastery")).isEqualTo(92.0);
    }

    @Test
    void studentDashboardDto_noLongerExposesPlaceholderChangeFields() {
        assertThat(Arrays.stream(StudentDashboardDTO.class.getMethods()).map(method -> method.getName()))
                .doesNotContain(
                        "getCourseCountChange",
                        "getPendingAssignmentsChange",
                        "getUpcomingExamsChange",
                        "getOverallProgressChange"
                );
    }

    @Test
    void getStudentCoursesWithPagination_boundsOutOfRangePageAndUsesSharedPageShape() {
        StudentMapper mapper = mock(StudentMapper.class);

        StudentServiceImpl service = new StudentServiceImpl();
        ReflectionTestUtils.setField(service, "studentMapper", mapper);

        Course course = new Course();
        course.setId(3003L);
        course.setCourseName("数据结构");
        course.setCourseCode("DS-003");

        when(mapper.countStudentCoursesWithFilters(42L, "进行中", "2026春", "专业课", "数据"))
                .thenReturn(3);
        when(mapper.getStudentCoursesWithFilters(42L, 2, 2, "c.course_name", "ASC", "进行中", "2026春", "专业课", "数据"))
                .thenReturn(List.of(course));

        Map<String, Object> result = service.getStudentCoursesWithPagination(
                42L, 9, 2, "courseName", "asc", "进行中", "2026春", "专业课", " 数据 ");

        assertThat(result.get("totalPages")).isEqualTo(2);
        assertThat(result.get("totalElements")).isEqualTo(3L);
        assertThat(result.get("size")).isEqualTo(2);
        assertThat(result.get("number")).isEqualTo(1);
        assertThat(result.get("first")).isEqualTo(false);
        assertThat(result.get("last")).isEqualTo(true);
        assertThat(result.get("numberOfElements")).isEqualTo(1);
        assertThat(result.get("empty")).isEqualTo(false);
        assertThat(result.get("content"))
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.LIST)
                .extracting(item -> ((Course) item).getId())
                .containsExactly(3003L);
        assertThat(result.get("pageable"))
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                .containsEntry("pageNumber", 1)
                .containsEntry("pageSize", 2)
                .containsEntry("offset", 2);
    }

    @Test
    void getStudentCoursesWithPagination_returnsEmptySharedPageShapeForNoRows() {
        StudentMapper mapper = mock(StudentMapper.class);

        StudentServiceImpl service = new StudentServiceImpl();
        ReflectionTestUtils.setField(service, "studentMapper", mapper);

        when(mapper.countStudentCoursesWithFilters(42L, null, null, null, null))
                .thenReturn(0);
        when(mapper.getStudentCoursesWithFilters(42L, 0, 10, "c.id", "DESC", null, null, null, null))
                .thenReturn(List.of());

        Map<String, Object> result = service.getStudentCoursesWithPagination(
                42L, 5, 10, null, null, null, null, null, null);

        assertThat(result.get("totalPages")).isEqualTo(0);
        assertThat(result.get("totalElements")).isEqualTo(0L);
        assertThat(result.get("size")).isEqualTo(10);
        assertThat(result.get("number")).isEqualTo(0);
        assertThat(result.get("first")).isEqualTo(true);
        assertThat(result.get("last")).isEqualTo(true);
        assertThat(result.get("numberOfElements")).isEqualTo(0);
        assertThat(result.get("empty")).isEqualTo(true);
        assertThat(result.get("content")).asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.LIST).isEmpty();
        assertThat(result.get("pageable"))
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                .containsEntry("pageNumber", 0)
                .containsEntry("pageSize", 10)
                .containsEntry("offset", 0);
    }
}
