package com._202510007517.platform.agent.service;

import com._202510007517.platform.agent.model.AgentIntent;
import com._202510007517.platform.agent.model.RecognizedIntent;
import com._202510007517.platform.assignment.api.feign.AssignmentFeignClient;
import com._202510007517.platform.course.api.dto.CourseAssignmentDTO;
import com._202510007517.platform.course.api.dto.CourseDTO;
import com._202510007517.platform.course.api.feign.CourseFeignClient;
import com._202510007517.platform.exam.api.dto.ExamDTO;
import com._202510007517.platform.exam.api.feign.ExamFeignClient;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentContextEnrichmentServiceTest {

    private final CourseFeignClient courseClient = mock(CourseFeignClient.class);
    private final AssignmentFeignClient assignmentClient = mock(AssignmentFeignClient.class);
    private final ExamFeignClient examClient = mock(ExamFeignClient.class);
    private final AgentArtifactService artifactService = new AgentArtifactService(new com.fasterxml.jackson.databind.ObjectMapper());
    private final AgentContextEnrichmentService service =
            new AgentContextEnrichmentService(courseClient, assignmentClient, examClient, artifactService);

    @Test
    void resolvesStudentAssignmentTitleFromPendingAssignments() {
        when(assignmentClient.listStudentAssignments(7L, 1, 100, "dueDate", "DESC", null, false, true))
                .thenReturn(Map.of("content", List.of(Map.of("id", 100L, "title", "数据库实验报告"))));
        RecognizedIntent intent = new RecognizedIntent(
                AgentIntent.SUBMIT_ASSIGNMENT,
                0.92,
                Map.of("assignmentTitle", "数据库实验报告", "content", "报告已完成"),
                List.of());

        RecognizedIntent enriched = service.enrich(7L, "STUDENT", intent);

        assertThat(enriched.slots())
                .containsEntry("assignmentId", 100L)
                .containsEntry("resolvedAssignmentTitle", "数据库实验报告");
        assertThat(enriched.missingSlots()).isEmpty();
    }

    @Test
    void resolvesStudentExamTitleFromVisibleExams() {
        ExamDTO visibleExam = new ExamDTO();
        visibleExam.setId(200L);
        visibleExam.setTitle("Java期末考试");
        when(examClient.listByStudent(7L)).thenReturn(List.of(visibleExam));
        RecognizedIntent intent = new RecognizedIntent(
                AgentIntent.SUBMIT_EXAM,
                0.92,
                Map.of("examTitle", "Java期末考试", "answers", Map.of("1", "A")),
                List.of());

        RecognizedIntent enriched = service.enrich(7L, "STUDENT", intent);

        assertThat(enriched.slots())
                .containsEntry("examId", 200L)
                .containsEntry("resolvedExamTitle", "Java期末考试");
        assertThat(enriched.missingSlots()).isEmpty();
    }

    @Test
    void resolvesTeacherCourseBySemesterAndClassNameWhenCourseNameIsAmbiguous() {
        when(courseClient.listTeacherCourses(7L, null, null, null, null))
                .thenReturn(List.of(
                        course(91005L, "云计算技术", "CLOUD-001", "2025-2026-2"),
                        course(91006L, "云计算技术", "CLOUD-002", "2024-2025-2")));
        when(courseClient.listCourseAssignments(7L, null, null))
                .thenReturn(List.of(
                        assignment(1L, 91005L, "云计算技术", "软件工程23级1班", "2025-2026-2"),
                        assignment(2L, 91006L, "云计算技术", "软件工程23级2班", "2024-2025-2")));
        RecognizedIntent intent = new RecognizedIntent(
                AgentIntent.PUBLISH_ASSIGNMENT,
                0.92,
                Map.of(
                        "courseName", "云计算技术",
                        "className", "软件工程23级1班",
                        "semester", "2025-2026-2",
                        "title", "Java基础",
                        "maxScore", 100,
                        "dueDate", "2026-06-17 23:59"
                ),
                List.of());

        RecognizedIntent enriched = service.enrich(7L, "TEACHER", intent);

        assertThat(enriched.slots())
                .containsEntry("courseId", 91005L)
                .containsEntry("resolvedCourseName", "云计算技术");
        assertThat(enriched.missingSlots()).isEmpty();
    }

    @Test
    void resolvesPublishAssignmentCourseFromClassNameOnly() {
        when(courseClient.listCourseAssignments(7L, null, null))
                .thenReturn(List.of(
                        assignment(1L, 91005L, "云计算技术", "云计算技术1班", "2025-2026-2")));
        RecognizedIntent intent = new RecognizedIntent(
                AgentIntent.PUBLISH_ASSIGNMENT,
                0.92,
                Map.of(
                        "className", "云计算技术1班",
                        "title", "作业1",
                        "maxScore", 100,
                        "dueDate", "2026年7月27日"
                ),
                List.of());

        RecognizedIntent enriched = service.enrich(7L, "TEACHER", intent);

        assertThat(enriched.slots())
                .containsEntry("courseId", 91005L)
                .containsEntry("resolvedCourseName", "云计算技术");
        assertThat(enriched.missingSlots()).isEmpty();
    }

    @Test
    void asksForClearerCourseWhenClassNameMatchesMultipleTeachingAssignments() {
        when(courseClient.listCourseAssignments(7L, null, null))
                .thenReturn(List.of(
                        assignment(1L, 91005L, "云计算技术", "云计算技术1班", "2025-2026-2"),
                        assignment(2L, 91006L, "分布式系统", "云计算技术1班", "2025-2026-2")));
        RecognizedIntent intent = new RecognizedIntent(
                AgentIntent.PUBLISH_ASSIGNMENT,
                0.92,
                Map.of(
                        "className", "云计算技术1班",
                        "title", "作业1",
                        "maxScore", 100,
                        "dueDate", "2026年7月27日"
                ),
                List.of());

        RecognizedIntent enriched = service.enrich(7L, "TEACHER", intent);

        assertThat(enriched.slots()).doesNotContainKey("courseId");
        assertThat(enriched.missingSlots()).contains("更明确的课程");
    }

    @Test
    void resolvesFirstAvailableClassForTestRandomPublishTarget() {
        when(courseClient.listCourseAssignments(7L, null, null))
                .thenReturn(List.of(
                        assignment(1L, 91005L, "云计算技术", "云计算技术1班", "2025-2026-2"),
                        assignment(2L, 91006L, "分布式系统", "软件工程23级1班", "2025-2026-2")));
        RecognizedIntent intent = new RecognizedIntent(
                AgentIntent.PUBLISH_ASSIGNMENT,
                0.92,
                Map.of(
                        "targetSelectionMode", "FIRST_AVAILABLE_CLASS_FOR_TEST",
                        "title", "Java基础课堂练习",
                        "maxScore", 100,
                        "dueDate", "2026-06-30 23:59:59"
                ),
                List.of());

        RecognizedIntent enriched = service.enrich(7L, "TEACHER", intent);

        assertThat(enriched.slots())
                .containsEntry("courseId", 91005L)
                .containsEntry("resolvedCourseName", "云计算技术")
                .containsEntry("className", "云计算技术1班");
        assertThat(enriched.missingSlots()).isEmpty();
    }

    @Test
    void resolvesTeacherCourseDetailFromSingleTeacherCourseContext() {
        when(courseClient.listTeacherCourses(7L, null, null, null, null))
                .thenReturn(List.of(course(12L, "Java 分布式框架", "JAVA-001", "2026-Spring")));
        RecognizedIntent intent = new RecognizedIntent(
                AgentIntent.QUERY_COURSE_DETAIL,
                0.9,
                Map.of(),
                List.of());

        RecognizedIntent enriched = service.enrich(7L, "TEACHER", intent);

        assertThat(enriched.slots())
                .containsEntry("courseId", 12L)
                .containsEntry("resolvedCourseName", "Java 分布式框架");
    }

    @Test
    void resolvesAssignmentDetailFromLatestPublishedAssignmentArtifact() {
        com._202510007517.platform.agent.domain.AgentSessionEntity session =
                new com._202510007517.platform.agent.domain.AgentSessionEntity();
        artifactService.saveArtifacts(session, Map.of(
                "latest_published_assignment",
                new SessionArtifact(
                        "assignment",
                        "latest_published_assignment",
                        Map.of("assignmentId", 99L, "title", "Spring Cloud实验", "courseId", 12L),
                        "2026-06-30T12:00:00",
                        "2026-06-30T12:00:00"
                )));
        RecognizedIntent intent = new RecognizedIntent(
                AgentIntent.QUERY_ASSIGNMENT_DETAIL,
                0.91,
                Map.of(),
                List.of());

        RecognizedIntent enriched = service.enrichFromSessionArtifacts(session, intent);

        assertThat(enriched.slots())
                .containsEntry("assignmentId", 99L)
                .containsEntry("resolvedAssignmentTitle", "Spring Cloud实验")
                .containsEntry("courseId", 12L);
    }

    @Test
    void resolvesExamDetailFromLatestPublishedExamArtifact() {
        com._202510007517.platform.agent.domain.AgentSessionEntity session =
                new com._202510007517.platform.agent.domain.AgentSessionEntity();
        artifactService.saveArtifacts(session, Map.of(
                "latest_published_exam",
                new SessionArtifact(
                        "exam",
                        "latest_published_exam",
                        Map.of("examId", 88L, "title", "期末考试", "courseId", 12L),
                        "2026-06-30T12:00:00",
                        "2026-06-30T12:00:00"
                )));
        RecognizedIntent intent = new RecognizedIntent(
                AgentIntent.QUERY_EXAM_DETAIL,
                0.91,
                Map.of(),
                List.of());

        RecognizedIntent enriched = service.enrichFromSessionArtifacts(session, intent);

        assertThat(enriched.slots())
                .containsEntry("examId", 88L)
                .containsEntry("resolvedExamTitle", "期末考试")
                .containsEntry("courseId", 12L);
    }

    @Test
    void resolvesCourseDetailFromLatestCreatedCourseArtifact() {
        com._202510007517.platform.agent.domain.AgentSessionEntity session =
                new com._202510007517.platform.agent.domain.AgentSessionEntity();
        artifactService.saveArtifacts(session, Map.of(
                "latest_created_course",
                new SessionArtifact(
                        "course",
                        "latest_created_course",
                        Map.of("courseId", 12L, "courseName", "Java 分布式框架"),
                        "2026-06-30T12:00:00",
                        "2026-06-30T12:00:00"
                )));
        RecognizedIntent intent = new RecognizedIntent(
                AgentIntent.QUERY_COURSE_DETAIL,
                0.91,
                Map.of(),
                List.of());

        RecognizedIntent enriched = service.enrichFromSessionArtifacts(session, intent);

        assertThat(enriched.slots())
                .containsEntry("courseId", 12L)
                .containsEntry("resolvedCourseName", "Java 分布式框架");
    }

    @Test
    void resolvesClassDetailFromLatestCreatedClassArtifact() {
        com._202510007517.platform.agent.domain.AgentSessionEntity session =
                new com._202510007517.platform.agent.domain.AgentSessionEntity();
        artifactService.saveArtifacts(session, Map.of(
                "latest_created_class",
                new SessionArtifact(
                        "class",
                        "latest_created_class",
                        Map.of("classId", 2301L, "className", "软件2301"),
                        "2026-06-30T12:00:00",
                        "2026-06-30T12:00:00"
                )));
        RecognizedIntent intent = new RecognizedIntent(
                AgentIntent.QUERY_CLASS_DETAIL,
                0.91,
                Map.of(),
                List.of());

        RecognizedIntent enriched = service.enrichFromSessionArtifacts(session, intent);

        assertThat(enriched.slots())
                .containsEntry("classId", 2301L)
                .containsEntry("resolvedClassName", "软件2301");
    }

    private CourseDTO course(Long id, String courseName, String courseCode, String semester) {
        CourseDTO course = new CourseDTO();
        course.setId(id);
        course.setCourseName(courseName);
        course.setCourseCode(courseCode);
        course.setSemester(semester);
        return course;
    }

    private CourseAssignmentDTO assignment(Long assignmentId, Long courseId, String courseName,
                                           String className, String semester) {
        CourseAssignmentDTO assignment = new CourseAssignmentDTO();
        assignment.setAssignmentId(assignmentId);
        assignment.setCourseId(courseId);
        assignment.setCourseName(courseName);
        assignment.setClassName(className);
        assignment.setSemester(semester);
        return assignment;
    }
}
