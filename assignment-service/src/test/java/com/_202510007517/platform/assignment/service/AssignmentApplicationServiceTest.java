package com._202510007517.platform.assignment.service;

import com._202510007517.platform.assignment.api.dto.AssignmentDTO;
import com._202510007517.platform.assignment.api.dto.AssignmentStudentScoreDTO;
import com._202510007517.platform.assignment.api.dto.AssignmentSubmitRequestDTO;
import com._202510007517.platform.assignment.api.dto.AssignmentSubmissionDTO;
import com._202510007517.platform.assignment.domain.AssignmentRecord;
import com._202510007517.platform.assignment.domain.AssignmentSubmissionRecord;
import com._202510007517.platform.assignment.repository.AssignmentRepository;
import com._202510007517.platform.common.event.outbox.OutboxEventEntity;
import com._202510007517.platform.common.event.outbox.OutboxEventRepository;
import com._202510007517.platform.course.api.dto.CourseDTO;
import com._202510007517.platform.course.api.feign.CourseFeignClient;
import com._202510007517.platform.user.api.dto.UserProfileDTO;
import com._202510007517.platform.user.api.feign.UserFeignClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AssignmentApplicationServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void listByCourseReturnsSeededAssignments() {
        AssignmentApplicationService service = new AssignmentApplicationService(
                new FakeAssignmentRepository(), mock(CourseFeignClient.class), mock(UserFeignClient.class),
                new InMemoryOutboxEventRepository(), objectMapper);

        List<AssignmentDTO> assignments = service.listByCourse(101L);

        assertThat(assignments).hasSize(1);
        assertThat(assignments.get(0).getId()).isEqualTo(2001L);
        assertThat(assignments.get(0).getTitle()).isEqualTo("Homework 1");
    }

    @Test
    void listKnowledgePointIdsReturnsAssignmentMappings() {
        AssignmentApplicationService service = new AssignmentApplicationService(
                new FakeAssignmentRepository(), mock(CourseFeignClient.class), mock(UserFeignClient.class),
                new InMemoryOutboxEventRepository(), objectMapper);

        List<Long> knowledgePointIds = service.listKnowledgePointIds(2001L);

        assertThat(knowledgePointIds).containsExactly(501L, 502L);
    }

    @Test
    void submitCreatesSubmissionForExistingAssignment() {
        FakeAssignmentRepository repository = new FakeAssignmentRepository();
        InMemoryOutboxEventRepository outboxRepository = new InMemoryOutboxEventRepository();
        AssignmentApplicationService service = new AssignmentApplicationService(
                repository, mock(CourseFeignClient.class), mock(UserFeignClient.class),
                outboxRepository, objectMapper);
        AssignmentSubmitRequestDTO request = new AssignmentSubmitRequestDTO();
        request.setStudentId(42L);
        request.setContent("my answer");
        request.setSubmissionDate("2026-09-01 10:00:00");

        AssignmentSubmissionDTO submission = service.submit(2001L, request);

        assertThat(submission.getId()).isNotNull();
        assertThat(submission.getAssignmentId()).isEqualTo(2001L);
        assertThat(submission.getStudentId()).isEqualTo(42L);
        assertThat(submission.getContent()).isEqualTo("my answer");
        assertThat(repository.assignment.getSubmissionCount()).isEqualTo(1);
        assertThat(repository.assignment.getStatus()).isEqualTo("submitted");
    }

    @Test
    void submitPersistsAssignmentSubmittedEventToOutbox() {
        FakeAssignmentRepository repository = new FakeAssignmentRepository();
        CourseFeignClient courseFeignClient = mock(CourseFeignClient.class);
        when(courseFeignClient.listStudentClassIds(42L)).thenReturn(List.of(501L));
        InMemoryOutboxEventRepository outboxRepository = new InMemoryOutboxEventRepository();
        AssignmentApplicationService service = new AssignmentApplicationService(
                repository, courseFeignClient, mock(UserFeignClient.class),
                outboxRepository, objectMapper);
        AssignmentSubmitRequestDTO request = new AssignmentSubmitRequestDTO();
        request.setStudentId(42L);
        request.setContent("my answer");
        request.setSubmissionDate("2026-09-01 10:00:00");

        service.submit(2001L, request);

        assertThat(outboxRepository.outboxEvents).hasSize(1);
        OutboxEventEntity event = outboxRepository.outboxEvents.get(0);
        assertThat(event.aggregateType()).isEqualTo("assignment_submission");
        assertThat(event.aggregateId()).isEqualTo("3001");
        assertThat(event.eventType()).isEqualTo("AssignmentSubmittedEvent");
        assertThat(event.bindingName()).isEqualTo("assignment.submitted");
        assertThat(event.payload()).contains("\"assignmentId\":2001");
        assertThat(event.payload()).contains("\"submissionId\":3001");
        assertThat(event.payload()).contains("\"studentId\":42");
        assertThat(event.payload()).contains("\"courseId\":101");
        assertThat(event.payload()).contains("\"classId\":501");
    }

    @Test
    void getAssignmentRejectsMissingId() {
        AssignmentApplicationService service = new AssignmentApplicationService(
                new FakeAssignmentRepository(), mock(CourseFeignClient.class), mock(UserFeignClient.class),
                new InMemoryOutboxEventRepository(), objectMapper);

        assertThatThrownBy(() -> service.getAssignment(9999L))
                .hasMessageContaining("作业不存在");
    }

    @Test
    void listStudentAssignmentsReturnsPageWithSubmissionAndCourseName() {
        FakeAssignmentRepository repository = new FakeAssignmentRepository();
        CourseFeignClient courseFeignClient = mock(CourseFeignClient.class);
        UserFeignClient userFeignClient = mock(UserFeignClient.class);
        when(courseFeignClient.getCourse(101L)).thenReturn(course(101L, "分布式框架技术"));
        when(courseFeignClient.listStudentClassIds(42L)).thenReturn(List.of(501L));
        when(userFeignClient.listByIds(anyList())).thenReturn(List.of(user(7L, "张老师")));

        AssignmentApplicationService service = new AssignmentApplicationService(repository, courseFeignClient, userFeignClient,
                new InMemoryOutboxEventRepository(), objectMapper);

        Map<String, Object> page = service.listStudentAssignments(42L, 1, 10, "dueDate", "DESC", null, null, true);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) page.get("content");
        assertThat(content).hasSize(1);
        assertThat(content.get(0)).containsEntry("courseName", "分布式框架技术");
        assertThat(content.get(0)).containsEntry("teacherName", "张老师");
        assertThat(content.get(0)).containsKey("submission");
    }

    @Test
    void getStudentAssignmentDetailReturnsSubmissionDetails() {
        FakeAssignmentRepository repository = new FakeAssignmentRepository();
        CourseFeignClient courseFeignClient = mock(CourseFeignClient.class);
        UserFeignClient userFeignClient = mock(UserFeignClient.class);
        when(courseFeignClient.getCourse(101L)).thenReturn(course(101L, "分布式框架技术"));
        when(courseFeignClient.listStudentClassIds(42L)).thenReturn(List.of(501L));
        when(userFeignClient.listByIds(anyList())).thenReturn(List.of(user(7L, "张老师")));

        AssignmentApplicationService service = new AssignmentApplicationService(repository, courseFeignClient, userFeignClient,
                new InMemoryOutboxEventRepository(), objectMapper);

        Map<String, Object> detail = service.getStudentAssignmentDetail(42L, 2001L);

        assertThat(detail).containsEntry("courseName", "分布式框架技术");
        assertThat(detail).containsEntry("teacherName", "张老师");
        assertThat(detail).containsKey("submission");
        assertThat(detail.get("submission")).isInstanceOf(Map.class);
    }

    @Test
    void listStudentSubmissionsReturnsOwnAssignmentSubmissions() {
        FakeAssignmentRepository repository = new FakeAssignmentRepository();
        CourseFeignClient courseFeignClient = mock(CourseFeignClient.class);
        when(courseFeignClient.getCourse(101L)).thenReturn(course(101L, "分布式框架技术"));

        AssignmentApplicationService service = new AssignmentApplicationService(
                repository, courseFeignClient, mock(UserFeignClient.class),
                new InMemoryOutboxEventRepository(), objectMapper);

        List<Map<String, Object>> submissions = service.listStudentSubmissions(42L);

        assertThat(submissions).hasSize(1);
        assertThat(submissions.get(0)).containsEntry("courseName", "分布式框架技术");
        assertThat(submissions.get(0)).containsEntry("title", "Homework 1");
        assertThat(submissions.get(0)).containsEntry("graded", true);
    }

    @Test
    void listStudentAssignmentsUsesStudentClassIdsFromCourseService() {
        FakeAssignmentRepository repository = new FakeAssignmentRepository();
        CourseFeignClient courseFeignClient = mock(CourseFeignClient.class);
        when(courseFeignClient.listStudentClassIds(42L)).thenReturn(List.of(501L));
        when(courseFeignClient.getCourse(101L)).thenReturn(course(101L, "分布式框架技术"));

        AssignmentApplicationService service = new AssignmentApplicationService(
                repository, courseFeignClient, mock(UserFeignClient.class),
                new InMemoryOutboxEventRepository(), objectMapper);

        Map<String, Object> page = service.listStudentAssignments(42L, 1, 10, "dueDate", "DESC", null, null, true);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) page.get("content");
        assertThat(content).hasSize(1);
        assertThat(repository.lastRequestedClassIds).containsExactly(501L);
    }

    @Test
    void listStudentAssignmentsReturnsEmptyPageWhenCourseServiceDegradesToNoClassIds() {
        FakeAssignmentRepository repository = new FakeAssignmentRepository();
        CourseFeignClient courseFeignClient = mock(CourseFeignClient.class);
        when(courseFeignClient.listStudentClassIds(42L)).thenReturn(List.of());

        AssignmentApplicationService service = new AssignmentApplicationService(
                repository, courseFeignClient, mock(UserFeignClient.class),
                new InMemoryOutboxEventRepository(), objectMapper);

        Map<String, Object> page = service.listStudentAssignments(42L, 1, 10, "dueDate", "DESC", null, null, true);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) page.get("content");
        assertThat(content).isEmpty();
        assertThat(page.get("totalElements")).isEqualTo(0);
    }

    @Test
    void listStudentScoresReturnsOnlyGradedScoresWithCourseName() {
        FakeAssignmentRepository repository = new FakeAssignmentRepository();
        CourseFeignClient courseFeignClient = mock(CourseFeignClient.class);
        when(courseFeignClient.getCourse(101L)).thenReturn(course(101L, "分布式框架技术"));

        AssignmentApplicationService service = new AssignmentApplicationService(
                repository, courseFeignClient, mock(UserFeignClient.class),
                new InMemoryOutboxEventRepository(), objectMapper);

        List<AssignmentStudentScoreDTO> scores = service.listStudentScores(42L);

        assertThat(scores).hasSize(1);
        AssignmentStudentScoreDTO item = scores.get(0);
        assertThat(item.getType()).isEqualTo("assignment");
        assertThat(item.getRelatedId()).isEqualTo(2001L);
        assertThat(item.getTitle()).isEqualTo("Homework 1");
        assertThat(item.getCourseName()).isEqualTo("分布式框架技术");
        assertThat(item.getCompletedAt()).isEqualTo("2026-09-01 10:00:00");
        assertThat(item.getSubmitDate()).isEqualTo("2026-09-01 10:00:00");
        assertThat(item.getScore()).isEqualTo(95);
        assertThat(item.getTotalScore()).isEqualTo(100);
        assertThat(item.getRank()).isNull();
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
        private long nextSubmissionId = 3001L;
        private List<Long> lastRequestedClassIds = List.of();
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
            throw new AssertionError("studentId-based lookup should not be used after class-based visibility migration");
        }

        @Override
        public List<AssignmentRecord> findByClassIds(List<Long> classIds) {
            lastRequestedClassIds = classIds;
            return classIds != null && classIds.contains(501L) ? List.of(assignment) : List.of();
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
            return submission.getStudentId().equals(studentId) && Boolean.TRUE.equals(submission.getGraded()) && submission.getScore() != null
                    ? List.of(submission)
                    : List.of();
        }

        @Override
        public Optional<AssignmentSubmissionRecord> findSubmissionById(Long submissionId) {
            return Optional.empty();
        }

        @Override
        public AssignmentSubmissionRecord insertSubmission(AssignmentSubmissionRecord submission) {
            submission.setId(nextSubmissionId++);
            return submission;
        }

        @Override
        public void updateSubmission(AssignmentSubmissionRecord submission) {
            this.submission.setContent(submission.getContent());
            this.submission.setSubmissionDate(submission.getSubmissionDate());
            this.submission.setGraded(submission.getGraded());
            this.submission.setIsLate(submission.getIsLate());
            this.submission.setLatePenalty(submission.getLatePenalty());
            this.submission.setScore(submission.getScore());
            this.submission.setTeacherComment(submission.getTeacherComment());
        }

        @Override
        public void updateAssignment(AssignmentRecord assignment) {
            this.assignment.setSubmissionCount(assignment.getSubmissionCount());
            this.assignment.setGradedCount(assignment.getGradedCount());
            this.assignment.setStatus(assignment.getStatus());
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
            return assignment.getId().equals(assignmentId) ? List.of(501L, 502L) : List.of();
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
            record.setDueDate("2026-09-15 23:59:59");
            record.setPublishDate("2026-09-01 08:00:00");
            record.setIsActive(true);
            record.setTeacherId(7L);
            record.setMaxScore(100);
            record.setSubmissionCount(0);
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
            record.setGraded(true);
            record.setIsLate(false);
            record.setLatePenalty(null);
            record.setScore(95);
            record.setTeacherComment(null);
            return record;
        }
    }

    private static class InMemoryOutboxEventRepository implements OutboxEventRepository {
        private final List<OutboxEventEntity> outboxEvents = new java.util.ArrayList<>();

        @Override
        public void save(OutboxEventEntity event) {
            outboxEvents.add(event);
        }

        @Override
        public List<OutboxEventEntity> findDuePendingEvents(int limit, java.time.Instant now) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void markPublished(long id, java.time.Instant publishedAt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void markFailed(long id, int retryCount, java.time.Instant nextRetryAt, String lastError, int maxRetries) {
            throw new UnsupportedOperationException();
        }
    }
}
