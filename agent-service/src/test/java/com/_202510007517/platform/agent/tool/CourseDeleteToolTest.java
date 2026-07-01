package com._202510007517.platform.agent.tool;

import com._202510007517.platform.course.api.feign.CourseFeignClient;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class CourseDeleteToolTest {

    private final CourseFeignClient courseClient = mock(CourseFeignClient.class);
    private final CourseDeleteTool tool = new CourseDeleteTool(courseClient);

    @Test
    void deletesCourseThroughCourseServiceForTeacher() {
        Map<String, Object> result = tool.execute(7L, "TEACHER", Map.of("courseId", 101L));

        assertThat(result)
                .containsEntry("status", "EXECUTED")
                .containsEntry("courseId", 101L);
        verify(courseClient).deleteCourse(eq(101L));
    }

    @Test
    void rejectsCourseDeleteForNonTeacher() {
        Map<String, Object> result = tool.execute(42L, "STUDENT", Map.of("courseId", 101L));

        assertThat(result)
                .containsEntry("status", "PERMISSION_DENIED")
                .containsEntry("message", "只有教师可以删除课程。");
        verify(courseClient, never()).deleteCourse(any());
    }

    @Test
    void reportsMissingCourseIdBeforeRemoteCall() {
        Map<String, Object> result = tool.execute(7L, "TEACHER", Map.of());

        assertThat(result)
                .containsEntry("status", "VALIDATION_FAILED")
                .containsEntry("message", "删除课程需要 courseId。");
        verify(courseClient, never()).deleteCourse(any());
    }
}
