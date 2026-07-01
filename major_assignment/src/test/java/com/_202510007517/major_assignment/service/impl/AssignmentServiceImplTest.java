package com._202510007517.major_assignment.service.impl;

import com._202510007517.major_assignment.entity.Assignment;
import com._202510007517.major_assignment.entity.Course;
import com._202510007517.major_assignment.entity.User;
import com._202510007517.major_assignment.mapper.AssignmentMapper;
import com._202510007517.major_assignment.service.AssignmentSubmissionService;
import com._202510007517.major_assignment.service.CourseService;
import com._202510007517.major_assignment.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AssignmentServiceImplTest {

    @Test
    void getAssignmentsWithPagination_boundsOutOfRangePageAndPreservesSharedPageShape() {
        AssignmentMapper mapper = mock(AssignmentMapper.class);
        AssignmentSubmissionService submissionService = mock(AssignmentSubmissionService.class);
        CourseService courseService = mock(CourseService.class);
        UserService userService = mock(UserService.class);

        AssignmentServiceImpl service = new AssignmentServiceImpl();
        ReflectionTestUtils.setField(service, "assignmentMapper", mapper);
        ReflectionTestUtils.setField(service, "assignmentSubmissionService", submissionService);
        ReflectionTestUtils.setField(service, "courseService", courseService);
        ReflectionTestUtils.setField(service, "userService", userService);

        Assignment a1 = buildAssignment(1L, "作业一");
        Assignment a2 = buildAssignment(2L, "作业二");
        Assignment a3 = buildAssignment(3L, "作业三");

        when(mapper.getAssignmentsByStudentId(7L)).thenReturn(List.of(a1, a2, a3));

        Map<String, Object> result = service.getAssignmentsWithPagination(7L, 9, 2, "id", "ASC", null, null, null);

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
                .singleElement()
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                .containsEntry("id", 3L)
                .containsKey("submission");
        assertThat(result.get("pageable"))
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                .containsEntry("pageNumber", 1)
                .containsEntry("pageSize", 2)
                .containsEntry("offset", 2);
    }

    @Test
    void getAssignmentsWithPagination_reusesCourseAndTeacherLookupsWithinSamePage() {
        AssignmentMapper mapper = mock(AssignmentMapper.class);
        AssignmentSubmissionService submissionService = mock(AssignmentSubmissionService.class);
        CourseService courseService = mock(CourseService.class);
        UserService userService = mock(UserService.class);

        AssignmentServiceImpl service = new AssignmentServiceImpl();
        ReflectionTestUtils.setField(service, "assignmentMapper", mapper);
        ReflectionTestUtils.setField(service, "assignmentSubmissionService", submissionService);
        ReflectionTestUtils.setField(service, "courseService", courseService);
        ReflectionTestUtils.setField(service, "userService", userService);

        Assignment a1 = buildAssignment(1L, "作业一");
        Assignment a2 = buildAssignment(2L, "作业二");
        Assignment a3 = buildAssignment(3L, "作业三");

        when(mapper.getAssignmentsByStudentId(8L)).thenReturn(List.of(a1, a2, a3));

        Course course = new Course();
        course.setId(100L);
        course.setCourseName("软件工程");
        User teacher = new User();
        teacher.setId(200L);
        teacher.setName("张老师");

        when(courseService.findById(100L)).thenReturn(course);
        when(userService.findById(200L)).thenReturn(teacher);

        Map<String, Object> result = service.getAssignmentsWithPagination(8L, 1, 3, "id", "ASC", null, null, null);

        assertThat(result.get("content"))
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.LIST)
                .hasSize(3)
                .allSatisfy(item -> assertThat(item)
                        .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                        .containsEntry("courseName", "软件工程")
                        .containsEntry("teacherName", "张老师"));

        verify(courseService, times(1)).findById(100L);
        verify(userService, times(1)).findById(200L);
    }

    private Assignment buildAssignment(Long id, String title) {
        Assignment assignment = new Assignment();
        assignment.setId(id);
        assignment.setTitle(title);
        assignment.setDescription(title + "描述");
        assignment.setCourseId(100L);
        assignment.setTeacherId(200L);
        assignment.setPublishDate(new Date(id));
        assignment.setDueDate(new Date(id + 1000));
        assignment.setIsActive(Boolean.TRUE);
        return assignment;
    }
}
