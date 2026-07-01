package com._202510007517.major_assignment.service.impl;

import com._202510007517.major_assignment.entity.Exam;
import com._202510007517.major_assignment.entity.Course;
import com._202510007517.major_assignment.entity.User;
import com._202510007517.major_assignment.mapper.ExamMapper;
import com._202510007517.major_assignment.service.CourseService;
import com._202510007517.major_assignment.service.ExamSubmissionService;
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

class ExamServiceImplTest {

    @Test
    void getExamsWithPagination_boundsOutOfRangePageAndPreservesSharedPageShape() {
        ExamMapper mapper = mock(ExamMapper.class);
        ExamSubmissionService submissionService = mock(ExamSubmissionService.class);
        CourseService courseService = mock(CourseService.class);
        UserService userService = mock(UserService.class);

        ExamServiceImpl service = new ExamServiceImpl();
        ReflectionTestUtils.setField(service, "examMapper", mapper);
        ReflectionTestUtils.setField(service, "examSubmissionService", submissionService);
        ReflectionTestUtils.setField(service, "courseService", courseService);
        ReflectionTestUtils.setField(service, "userService", userService);

        Exam e1 = buildExam(1L, "考试一");
        Exam e2 = buildExam(2L, "考试二");
        Exam e3 = buildExam(3L, "考试三");

        when(mapper.getExamsByStudentId(9L)).thenReturn(List.of(e1, e2, e3));

        Map<String, Object> result = service.getExamsWithPagination(9L, 9, 2, null, null, null, null, null);

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
    void getExamsWithPagination_reusesCourseAndTeacherLookupsWithinSamePage() {
        ExamMapper mapper = mock(ExamMapper.class);
        ExamSubmissionService submissionService = mock(ExamSubmissionService.class);
        CourseService courseService = mock(CourseService.class);
        UserService userService = mock(UserService.class);

        ExamServiceImpl service = new ExamServiceImpl();
        ReflectionTestUtils.setField(service, "examMapper", mapper);
        ReflectionTestUtils.setField(service, "examSubmissionService", submissionService);
        ReflectionTestUtils.setField(service, "courseService", courseService);
        ReflectionTestUtils.setField(service, "userService", userService);

        Exam e1 = buildExam(1L, "考试一");
        Exam e2 = buildExam(2L, "考试二");
        Exam e3 = buildExam(3L, "考试三");

        when(mapper.getExamsByStudentId(10L)).thenReturn(List.of(e1, e2, e3));

        Course course = new Course();
        course.setId(100L);
        course.setCourseName("数据库系统");
        User teacher = new User();
        teacher.setId(300L);
        teacher.setName("李老师");

        when(courseService.findById(100L)).thenReturn(course);
        when(userService.findById(300L)).thenReturn(teacher);

        Map<String, Object> result = service.getExamsWithPagination(10L, 1, 3, null, null, null, null, null);

        assertThat(result.get("content"))
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.LIST)
                .hasSize(3)
                .allSatisfy(item -> assertThat(item)
                        .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                        .containsEntry("courseName", "数据库系统")
                        .containsEntry("teacherName", "李老师"));

        verify(courseService, times(1)).findById(100L);
        verify(userService, times(1)).findById(300L);
    }

    private Exam buildExam(Long id, String title) {
        Exam exam = new Exam();
        exam.setId(id);
        exam.setTitle(title);
        exam.setDescription(title + "描述");
        exam.setCourseId(100L);
        exam.setTeacherId(300L);
        exam.setStartTime(new Date(id * 1000));
        exam.setEndTime(new Date(id * 1000 + 60000));
        exam.setPublishDate(new Date(id));
        exam.setDuration(90L);
        exam.setIsActive(Boolean.TRUE);
        exam.setIsOnline(Boolean.TRUE);
        return exam;
    }
}
