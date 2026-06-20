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
    private final AgentContextEnrichmentService service =
            new AgentContextEnrichmentService(courseClient, assignmentClient, examClient);

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
