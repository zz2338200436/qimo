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

class CourseUpdateToolTest {

    private final CourseFeignClient courseClient = mock(CourseFeignClient.class);
    private final CourseUpdateTool tool = new CourseUpdateTool(courseClient);

    @Test
    void updatesCourseThroughCourseServiceForTeacher() {
        CourseDTO course = new CourseDTO();
        course.setId(101L);
        course.setCourseName("高级分布式框架技术");
        when(courseClient.updateCourse(eq(101L), eq(7L), any(CourseUpsertRequestDTO.class))).thenReturn(course);

        Map<String, Object> result = tool.execute(7L, "TEACHER", Map.of(
                "courseId", 101L,
                "courseName", "高级分布式框架技术",
                "courseCode", "DFT201",
                "description", "进阶 Spring Cloud 与 Agent 实践",
                "credit", 4,
                "totalHours", 64,
                "courseCategory", "专业核心",
                "courseStatus", "active",
                "semester", "2026秋"
        ));

        assertThat(result)
                .containsEntry("status", "EXECUTED")
                .containsEntry("course", course);
        var requestCaptor = org.mockito.ArgumentCaptor.forClass(CourseUpsertRequestDTO.class);
        verify(courseClient).updateCourse(eq(101L), eq(7L), requestCaptor.capture());
        CourseUpsertRequestDTO request = requestCaptor.getValue();
        assertThat(request.getCourseName()).isEqualTo("高级分布式框架技术");
        assertThat(request.getCourseCode()).isEqualTo("DFT201");
        assertThat(request.getDescription()).isEqualTo("进阶 Spring Cloud 与 Agent 实践");
        assertThat(request.getCredit()).isEqualTo(4);
        assertThat(request.getTotalHours()).isEqualTo(64);
        assertThat(request.getCourseCategory()).isEqualTo("专业核心");
        assertThat(request.getCourseStatus()).isEqualTo("active");
        assertThat(request.getSemester()).isEqualTo("2026秋");
    }

    @Test
    void rejectsCourseUpdateForNonTeacher() {
        Map<String, Object> result = tool.execute(42L, "STUDENT", Map.of(
                "courseId", 101L,
                "courseName", "高级分布式框架技术",
                "courseCode", "DFT201",
                "credit", 4,
                "totalHours", 64
        ));

        assertThat(result)
                .containsEntry("status", "PERMISSION_DENIED")
                .containsEntry("message", "只有教师可以更新课程。");
        verify(courseClient, never()).updateCourse(any(), any(), any());
    }

    @Test
    void reportsMissingRequiredSlotsBeforeRemoteCall() {
        Map<String, Object> result = tool.execute(7L, "TEACHER", Map.of("courseId", 101L));

        assertThat(result).containsEntry("status", "VALIDATION_FAILED");
        verify(courseClient, never()).updateCourse(any(), any(), any());
    }
}
