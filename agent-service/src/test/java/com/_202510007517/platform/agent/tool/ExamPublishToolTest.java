package com._202510007517.platform.agent.tool;

import com._202510007517.platform.agent.client.TeacherExamEdgeClient;
import com._202510007517.platform.course.api.dto.CourseDTO;
import com._202510007517.platform.course.api.feign.CourseFeignClient;
import com._202510007517.platform.common.web.ResponseResult;
import com._202510007517.platform.exam.api.dto.ExamDTO;
import com._202510007517.platform.exam.api.dto.TeacherExamUpsertRequestDTO;
import com._202510007517.platform.exam.api.feign.ExamFeignClient;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExamPublishToolTest {

    private final ExamFeignClient examClient = mock(ExamFeignClient.class);
    private final TeacherExamEdgeClient teacherExamEdgeClient = mock(TeacherExamEdgeClient.class);
    private final CourseFeignClient courseClient = mock(CourseFeignClient.class);
    private final ExamPublishTool tool = new ExamPublishTool(examClient, teacherExamEdgeClient, courseClient);

    @Test
    void publishesExamOnlyWhenTeacherOwnsCourse() {
        CourseDTO ownedCourse = new CourseDTO();
        ownedCourse.setId(100L);
        when(courseClient.listTeacherCourses(7L, null, null, null, null)).thenReturn(List.of(ownedCourse));

        Map<String, Object> result = tool.execute(7L, "TEACHER", publishRequest(100L));

        assertThat(result).containsEntry("status", "EXECUTED");
        verify(examClient).createTeacherExam(eq(7L), any(TeacherExamUpsertRequestDTO.class));
    }

    @Test
    void rejectsExamPublishingWhenTeacherDoesNotOwnCourse() {
        CourseDTO ownedCourse = new CourseDTO();
        ownedCourse.setId(200L);
        when(courseClient.listTeacherCourses(7L, null, null, null, null)).thenReturn(List.of(ownedCourse));

        Map<String, Object> result = tool.execute(7L, "TEACHER", publishRequest(100L));

        assertThat(result)
                .containsEntry("status", "PERMISSION_DENIED")
                .containsEntry("message", "当前教师无权在该课程下发布考试。");
        verify(examClient, never()).createTeacherExam(any(), any());
    }

    @Test
    void usesMultipartEndpointWhenAttachmentsArePresent() {
        CourseDTO ownedCourse = new CourseDTO();
        ownedCourse.setId(100L);
        when(courseClient.listTeacherCourses(7L, null, null, null, null)).thenReturn(List.of(ownedCourse));
        ExamDTO exam = new ExamDTO();
        exam.setId(501L);
        when(teacherExamEdgeClient.createExamWithFiles(eq("7"), any(), any()))
                .thenReturn(ResponseResult.success(exam));

        Map<String, Object> result = tool.execute(7L, "TEACHER", Map.of(
                "title", "Java期末考试",
                "description", "闭卷考试",
                "courseId", 100L,
                "startTime", "2026-06-20T09:00",
                "endTime", "2026-06-20T10:30",
                "duration", 90,
                "isOnline", true,
                "attachments", List.of(Map.of(
                        "name", "考试附件.txt",
                        "contentType", "text/plain",
                        "base64", Base64.getEncoder().encodeToString("hello".getBytes())
                ))
        ));

        assertThat(result).containsEntry("status", "EXECUTED");
        verify(teacherExamEdgeClient).createExamWithFiles(eq("7"), any(), any());
        verify(examClient, never()).createTeacherExam(any(), any());
    }

    private static Map<String, Object> publishRequest(Long courseId) {
        return Map.of(
                "title", "Java期末考试",
                "description", "闭卷考试",
                "courseId", courseId,
                "startTime", "2026-06-20T09:00",
                "endTime", "2026-06-20T10:30",
                "duration", 90,
                "isOnline", true
        );
    }
}
