package com._202510007517.platform.agent.tool;

import com._202510007517.platform.agent.client.StudentCourseEdgeClient;
import com._202510007517.platform.course.api.dto.CourseDTO;
import com._202510007517.platform.course.api.feign.CourseFeignClient;
import com._202510007517.platform.common.web.ResponseResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CourseDetailLookupToolTest {

    private final CourseFeignClient teacherCourseClient = mock(CourseFeignClient.class);
    private final StudentCourseEdgeClient studentCourseClient = mock(StudentCourseEdgeClient.class);
    private final CourseDetailLookupTool tool = new CourseDetailLookupTool(teacherCourseClient, studentCourseClient);

    @Test
    void getsTeacherCourseDetailThroughCourseService() {
        CourseDTO course = new CourseDTO();
        course.setId(1001L);
        course.setCourseName("Java 分布式框架");
        when(teacherCourseClient.getCourse(1001L)).thenReturn(course);

        Map<String, Object> result = tool.execute(7L, "TEACHER", Map.of("courseId", 1001L));

        assertThat(result).containsEntry("status", "EXECUTED");
        assertThat(result.get("course")).isSameAs(course);
        verify(teacherCourseClient).getCourse(1001L);
        verify(studentCourseClient, never()).getStudentCourse(null, null);
    }

    @Test
    void getsStudentCourseDetailThroughCourseService() {
        CourseDTO course = new CourseDTO();
        course.setId(1001L);
        course.setCourseName("Java 分布式框架");
        when(studentCourseClient.getStudentCourse("42", 1001L)).thenReturn(ResponseResult.success(course));

        Map<String, Object> result = tool.execute(42L, "STUDENT", Map.of("courseId", 1001L));

        assertThat(result).containsEntry("status", "EXECUTED");
        assertThat(result.get("course")).isSameAs(course);
        verify(studentCourseClient).getStudentCourse("42", 1001L);
        verify(teacherCourseClient, never()).getCourse(null);
    }

    @Test
    void rejectsLookupWhenCourseIdIsMissing() {
        Map<String, Object> result = tool.execute(7L, "TEACHER", Map.of());

        assertThat(result)
                .containsEntry("status", "VALIDATION_FAILED")
                .containsEntry("message", "查询课程详情需要 courseId。");
        verify(teacherCourseClient, never()).getCourse(null);
        verify(studentCourseClient, never()).getStudentCourse(null, null);
    }
}
