package com._202510007517.platform.agent.tool;

import com._202510007517.platform.course.api.dto.CourseDTO;
import com._202510007517.platform.course.api.dto.CourseUpsertRequestDTO;
import com._202510007517.platform.course.api.feign.CourseFeignClient;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CourseCreateToolTest {

    private final CourseFeignClient courseClient = mock(CourseFeignClient.class);
    private final CourseCreateTool tool = new CourseCreateTool(courseClient);

    @Test
    void createsCourseThroughCourseServiceForTeacher() {
        CourseDTO course = new CourseDTO();
        course.setId(101L);
        course.setCourseName("分布式框架技术");
        when(courseClient.createCourse(eq(7L), any(CourseUpsertRequestDTO.class))).thenReturn(course);

        Map<String, Object> result = tool.execute(7L, "TEACHER", Map.of(
                "courseName", "分布式框架技术",
                "courseCode", "DFT101",
                "description", "Spring Cloud 与 Agent 实践",
                "credit", 3,
                "totalHours", 48,
                "courseCategory", "必修",
                "courseStatus", "active",
                "semester", "2026春"
        ));

        assertThat(result)
                .containsEntry("status", "EXECUTED")
                .containsEntry("course", course);
        var requestCaptor = org.mockito.ArgumentCaptor.forClass(CourseUpsertRequestDTO.class);
        verify(courseClient).createCourse(eq(7L), requestCaptor.capture());
        CourseUpsertRequestDTO request = requestCaptor.getValue();
        assertThat(request.getCourseName()).isEqualTo("分布式框架技术");
        assertThat(request.getCourseCode()).isEqualTo("DFT101");
        assertThat(request.getDescription()).isEqualTo("Spring Cloud 与 Agent 实践");
        assertThat(request.getCredit()).isEqualTo(3);
        assertThat(request.getTotalHours()).isEqualTo(48);
        assertThat(request.getCourseCategory()).isEqualTo("必修");
        assertThat(request.getCourseStatus()).isEqualTo("active");
        assertThat(request.getSemester()).isEqualTo("2026春");
    }

    @Test
    void rejectsCourseCreationForNonTeacher() {
        Map<String, Object> result = tool.execute(42L, "STUDENT", Map.of(
                "courseName", "分布式框架技术",
                "courseCode", "DFT101",
                "credit", 3,
                "totalHours", 48
        ));

        assertThat(result)
                .containsEntry("status", "PERMISSION_DENIED")
                .containsEntry("message", "只有教师可以创建课程。");
        verify(courseClient, never()).createCourse(any(), any());
    }

    @Test
    void reportsMissingRequiredSlotsBeforeRemoteCall() {
        Map<String, Object> result = tool.execute(7L, "TEACHER", Map.of("courseName", "分布式框架技术"));

        assertThat(result).containsEntry("status", "VALIDATION_FAILED");
        verify(courseClient, never()).createCourse(any(), any());
    }
}
