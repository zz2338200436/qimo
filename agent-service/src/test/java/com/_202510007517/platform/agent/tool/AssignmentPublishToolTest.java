package com._202510007517.platform.agent.tool;

import com._202510007517.platform.agent.client.TeacherAssignmentEdgeClient;
import com._202510007517.platform.assignment.api.dto.AssignmentDTO;
import com._202510007517.platform.common.web.ResponseResult;
import com._202510007517.platform.course.api.dto.CourseDTO;
import com._202510007517.platform.course.api.feign.CourseFeignClient;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AssignmentPublishToolTest {

    private final TeacherAssignmentEdgeClient assignmentClient = mock(TeacherAssignmentEdgeClient.class);
    private final CourseFeignClient courseClient = mock(CourseFeignClient.class);
    private final AssignmentPublishTool tool = new AssignmentPublishTool(assignmentClient, courseClient);

    @Test
    void publishesAssignmentOnlyWhenTeacherOwnsCourse() {
        CourseDTO course = new CourseDTO();
        course.setId(100L);
        course.setTeacherId(7L);
        when(courseClient.listTeacherCourses(7L, null, null, null, null)).thenReturn(List.of(course));
        AssignmentDTO assignment = new AssignmentDTO();
        assignment.setId(300L);
        when(assignmentClient.createAssignment(eq("7"), any())).thenReturn(ResponseResult.success(assignment));

        Map<String, Object> result = tool.execute(7L, "TEACHER", publishRequest(100L));

        assertThat(result).containsEntry("status", "EXECUTED");
        verify(assignmentClient).createAssignment(eq("7"), any());
    }

    @Test
    void rejectsAssignmentPublishingWhenCourseDoesNotBelongToTeacher() {
        CourseDTO ownedCourse = new CourseDTO();
        ownedCourse.setId(200L);
        ownedCourse.setTeacherId(7L);
        when(courseClient.listTeacherCourses(7L, null, null, null, null)).thenReturn(List.of(ownedCourse));

        Map<String, Object> result = tool.execute(7L, "TEACHER", publishRequest(100L));

        assertThat(result)
                .containsEntry("status", "PERMISSION_DENIED")
                .containsEntry("message", "当前教师无权在该课程下发布作业。");
        verify(assignmentClient, never()).createAssignment(any(), any());
    }

    private static Map<String, Object> publishRequest(Long courseId) {
        return Map.of(
                "title", "Spring Cloud实验",
                "courseId", courseId,
                "dueDate", "2026-06-20 22:00",
                "maxScore", 100
        );
    }
}
