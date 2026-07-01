package com._202510007517.platform.assignment.service;

import com._202510007517.platform.assignment.api.dto.AssignmentDTO;
import com._202510007517.platform.assignment.api.dto.AssignmentSubmissionDTO;
import com._202510007517.platform.assignment.domain.AssignmentRecord;
import com._202510007517.platform.assignment.domain.AssignmentSubmissionRecord;
import com._202510007517.platform.assignment.repository.AssignmentRepository;
import com._202510007517.platform.course.api.dto.CourseDTO;
import com._202510007517.platform.course.api.feign.CourseFeignClient;
import com._202510007517.platform.user.api.dto.UserProfileDTO;
import com._202510007517.platform.user.api.feign.UserFeignClient;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TeacherAssignmentQueryServiceTest {

    @Test
    void listAssignmentsBuildsTeacherFacingPage() {
        CourseFeignClient courseFeignClient = mock(CourseFeignClient.class);
        UserFeignClient userFeignClient = mock(UserFeignClient.class);
        when(courseFeignClient.getCourse(101L)).thenReturn(course(101L, "分布式框架技术"));
        when(userFeignClient.listByIds(anyList())).thenReturn(List.of(user(7L, "张老师")));

        TeacherAssignmentQueryService service = new TeacherAssignmentQueryService(
                new FakeAssignmentRepository(), courseFeignClient, userFeignClient);

        Map<String, Object> page = service.listAssignments(7L, 1, 10, "Homework", 101L, true, "submitted");

        @SuppressWarnings("unchecked")
        List<AssignmentDTO> content = (List<AssignmentDTO>) page.get("content");
        assertThat(content).hasSize(1);
        assertThat(content.get(0).getCourseName()).isEqualTo("分布式框架技术");
        assertThat(content.get(0).getTeacherName()).isEqualTo("张老师");
        assertThat(content.get(0).getSubmittedCount()).isEqualTo(1);
        assertThat(content.get(0).getStatus()).isEqualTo("submitted");
        assertThat(page.get("totalElements")).isEqualTo(1);
    }

    @Test
    void getAssignmentDetailIncludesSubmissionsAndStudentNames() {
        CourseFeignClient courseFeignClient = mock(CourseFeignClient.class);
        UserFeignClient userFeignClient = mock(UserFeignClient.class);
        AssessmentAttachmentService attachmentService = mock(AssessmentAttachmentService.class);
        List<Map<String, Object>> submissionAttachments = List.of(Map.of(
                "id", 7001L,
                "name", "学生作业附件.pdf",
                "downloadUrl", "/api/attachments/assignment/7001/download"
        ));
        when(courseFeignClient.getCourse(101L)).thenReturn(course(101L, "分布式框架技术"));
        when(userFeignClient.listByIds(anyList())).thenReturn(List.of(
                user(7L, "张老师"),
                user(42L, "李同学")
        ));
        when(attachmentService.getSubmissionAttachmentDtos(3001L)).thenReturn(submissionAttachments);

        TeacherAssignmentQueryService service = new TeacherAssignmentQueryService(
                new FakeAssignmentRepository(), courseFeignClient, userFeignClient, attachmentService);

        AssignmentDTO detail = service.getAssignmentDetail(7L, 2001L);

        assertThat(detail.getCourseName()).isEqualTo("分布式框架技术");
        assertThat(detail.getSubmissions()).hasSize(1);
        AssignmentSubmissionDTO submission = detail.getSubmissions().get(0);
        assertThat(submission.getStudentName()).isEqualTo("李同学");
        assertThat(submission.getStatus()).isEqualTo("submitted");
        assertThat(submission.getAttachments()).isEqualTo(submissionAttachments);
    }

    @Test
    void listSubmissionsBuildsTeacherFacingPage() {
        CourseFeignClient courseFeignClient = mock(CourseFeignClient.class);
        UserFeignClient userFeignClient = mock(UserFeignClient.class);
        AssessmentAttachmentService attachmentService = mock(AssessmentAttachmentService.class);
        List<Map<String, Object>> submissionAttachments = List.of(Map.of(
                "id", 7001L,
                "name", "学生作业附件.pdf",
                "downloadUrl", "/api/attachments/assignment/7001/download"
        ));
        when(userFeignClient.listByIds(anyList())).thenReturn(List.of(user(42L, "李同学")));
        when(attachmentService.getSubmissionAttachmentDtos(3001L)).thenReturn(submissionAttachments);

        TeacherAssignmentQueryService service = new TeacherAssignmentQueryService(
                new FakeAssignmentRepository(), courseFeignClient, userFeignClient, attachmentService);

        Map<String, Object> page = service.listSubmissions(7L, 1, 10, "id", "DESC", null, 42L, false);

        @SuppressWarnings("unchecked")
        List<AssignmentSubmissionDTO> content = (List<AssignmentSubmissionDTO>) page.get("content");
        assertThat(content).hasSize(1);
        assertThat(content.get(0).getId()).isEqualTo(3001L);
        assertThat(content.get(0).getTitle()).isEqualTo("Homework 1");
        assertThat(content.get(0).getStudentName()).isEqualTo("李同学");
        assertThat(content.get(0).getStatus()).isEqualTo("submitted");
        assertThat(content.get(0).getAttachments()).isEqualTo(submissionAttachments);
        assertThat(page.get("totalElements")).isEqualTo(1);
    }

    @Test
    void getAssignmentSubmissionIncludesSubmissionAttachments() {
        CourseFeignClient courseFeignClient = mock(CourseFeignClient.class);
        UserFeignClient userFeignClient = mock(UserFeignClient.class);
        AssessmentAttachmentService attachmentService = mock(AssessmentAttachmentService.class);
        List<Map<String, Object>> submissionAttachments = List.of(Map.of(
                "id", 7001L,
                "name", "学生作业附件.pdf",
                "downloadUrl", "/api/attachments/assignment/7001/download"
        ));
        when(userFeignClient.listByIds(anyList())).thenReturn(List.of(user(42L, "李同学")));
        when(attachmentService.getSubmissionAttachmentDtos(3001L)).thenReturn(submissionAttachments);

        TeacherAssignmentQueryService service = new TeacherAssignmentQueryService(
                new FakeAssignmentRepository(), courseFeignClient, userFeignClient, attachmentService);

        AssignmentSubmissionDTO submission = service.getAssignmentSubmission(7L, 3001L);

        assertThat(submission.getAttachments()).isEqualTo(submissionAttachments);
        verify(attachmentService).getSubmissionAttachmentDtos(3001L);
    }

    @Test
    void listAssignmentsKeepsPageFunctionalWhenUserServiceIsUnavailable() {
        CourseFeignClient courseFeignClient = mock(CourseFeignClient.class);
        UserFeignClient userFeignClient = mock(UserFeignClient.class);
        when(courseFeignClient.getCourse(101L)).thenReturn(course(101L, "分布式框架技术"));
        when(userFeignClient.listByIds(anyList())).thenThrow(new RuntimeException("user-service down"));

        TeacherAssignmentQueryService service = new TeacherAssignmentQueryService(
                new FakeAssignmentRepository(), courseFeignClient, userFeignClient);

        Map<String, Object> page = service.listAssignments(7L, 1, 10, null, null, null, null);

        @SuppressWarnings("unchecked")
        List<AssignmentDTO> content = (List<AssignmentDTO>) page.get("content");
        assertThat(content).hasSize(1);
        assertThat(content.get(0).getCourseName()).isEqualTo("分布式框架技术");
        assertThat(content.get(0).getTeacherName()).isNull();
    }

    private static CourseDTO course(Long id, String name) {
        CourseDTO dto = new CourseDTO();
        dto.setId(id);
        dto.setCourseName(name);
        return dto;
    }

    private static UserProfileDTO user(Long id, String name) {
        UserProfileDTO dto = new UserProfileDTO();
        dto.setId(id);
        dto.setName(name);
        return dto;
    }

    private static class FakeAssignmentRepository implements AssignmentRepository {

        private final AssignmentRecord assignment = seededAssignment();
        private final AssignmentSubmissionRecord submission = seededSubmission();

        @Override
        public Optional<AssignmentRecord> findById(Long assignmentId) {
            return assignment.getId().equals(assignmentId) ? Optional.of(assignment) : Optional.empty();
        }

        @Override
        public List<AssignmentRecord> findByCourseId(Long courseId) {
            return assignment.getCourseId().equals(courseId) ? List.of(assignment) : List.of();
        }

        @Override
        public List<AssignmentRecord> findByTeacherId(Long teacherId) {
            return assignment.getTeacherId().equals(teacherId) ? List.of(assignment) : List.of();
        }

        @Override
        public List<AssignmentRecord> findByStudentId(Long studentId) {
            return List.of();
        }

        @Override
        public List<AssignmentRecord> findByClassIds(List<Long> classIds) {
            return List.of();
        }

        @Override
        public List<AssignmentSubmissionRecord> findSubmissionsByAssignmentId(Long assignmentId) {
            return assignment.getId().equals(assignmentId) ? List.of(submission) : List.of();
        }

        @Override
        public Optional<AssignmentSubmissionRecord> findSubmissionByAssignmentAndStudent(Long assignmentId, Long studentId) {
            if (assignment.getId().equals(assignmentId) && submission.getStudentId().equals(studentId)) {
                return Optional.of(submission);
            }
            return Optional.empty();
        }

        @Override
        public List<AssignmentSubmissionRecord> findSubmissionsByStudentId(Long studentId) {
            return submission.getStudentId().equals(studentId) ? List.of(submission) : List.of();
        }

        @Override
        public List<AssignmentSubmissionRecord> findGradedSubmissionsByStudentId(Long studentId) {
            return List.of();
        }

        @Override
        public Optional<AssignmentSubmissionRecord> findSubmissionById(Long submissionId) {
            return submission.getId().equals(submissionId) ? Optional.of(submission) : Optional.empty();
        }

        @Override
        public AssignmentSubmissionRecord insertSubmission(AssignmentSubmissionRecord submission) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void updateSubmission(AssignmentSubmissionRecord submission) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void updateAssignment(AssignmentRecord assignment) {
            throw new UnsupportedOperationException();
        }

        @Override
        public AssignmentRecord insertAssignment(AssignmentRecord assignment) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void updateAssignmentDetails(AssignmentRecord assignment) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void replaceAssignmentClasses(Long assignmentId, List<Long> classIds) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Long> findKnowledgePointIdsByAssignmentId(Long assignmentId) {
            return List.of();
        }

        @Override
        public void replaceAssignmentKnowledgePoints(Long assignmentId, List<Long> knowledgePointIds) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteAssignmentCascade(Long assignmentId) {
            throw new UnsupportedOperationException();
        }

        private static AssignmentRecord seededAssignment() {
            AssignmentRecord record = new AssignmentRecord();
            record.setId(2001L);
            record.setTitle("Homework 1");
            record.setDescription("Distributed systems reading notes");
            record.setCourseId(101L);
            record.setDueDate("2099-09-15 23:59:59");
            record.setPublishDate("2026-09-01 08:00:00");
            record.setIsActive(true);
            record.setTeacherId(7L);
            record.setMaxScore(100);
            record.setSubmissionCount(1);
            record.setGradedCount(0);
            record.setStatus("pending");
            record.setTotalStudents(36);
            return record;
        }

        private static AssignmentSubmissionRecord seededSubmission() {
            AssignmentSubmissionRecord record = new AssignmentSubmissionRecord();
            record.setId(3001L);
            record.setAssignmentId(2001L);
            record.setStudentId(42L);
            record.setContent("my answer");
            record.setSubmissionDate("2026-09-01 10:00:00");
            record.setGraded(false);
            record.setIsLate(false);
            record.setScore(null);
            record.setTeacherComment(null);
            return record;
        }
    }
}
