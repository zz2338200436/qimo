package com._202510007517.platform.assignment.service;

import com._202510007517.platform.assignment.api.dto.AssignmentDTO;
import com._202510007517.platform.assignment.api.dto.TeacherAssignmentGradeRequestDTO;
import com._202510007517.platform.assignment.api.dto.TeacherAssignmentUpsertRequestDTO;
import com._202510007517.platform.assignment.domain.AssignmentRecord;
import com._202510007517.platform.assignment.domain.AssignmentSubmissionRecord;
import com._202510007517.platform.assignment.repository.AssignmentRepository;
import com._202510007517.platform.common.event.outbox.OutboxEventEntity;
import com._202510007517.platform.common.event.outbox.OutboxEventRepository;
import com._202510007517.platform.course.api.dto.CourseAssignmentDTO;
import com._202510007517.platform.course.api.dto.CourseDTO;
import com._202510007517.platform.course.api.feign.CourseFeignClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TeacherAssignmentCommandServiceTest {

    @Test
    void createAssignmentPersistsCoreFieldsAndClassLinks() {
        FakeAssignmentRepository repository = new FakeAssignmentRepository();
        CourseFeignClient courseFeignClient = mock(CourseFeignClient.class);
        when(courseFeignClient.getCourse(101L)).thenReturn(course(101L, 7L, "分布式框架技术", 36));
        when(courseFeignClient.listCourseAssignments(7L, 101L, null)).thenReturn(List.of(
                classAssignment(11L, 101L),
                classAssignment(12L, 101L),
                classAssignment(11L, 101L)
        ));

        TeacherAssignmentCommandService service = new TeacherAssignmentCommandService(
                repository,
                courseFeignClient,
                new InMemoryOutboxEventRepository(),
                new ObjectMapper().findAndRegisterModules());

        AssignmentDTO created = service.createAssignment(7L, request(
                "Homework 2",
                101L,
                "chapter 2",
                "2026-09-01 08:00:00",
                "2026-09-15 23:59:59",
                100
        ));

        assertThat(created.getId()).isEqualTo(4001L);
        assertThat(created.getCourseId()).isEqualTo(101L);
        assertThat(created.getTotalStudents()).isEqualTo(36);
        assertThat(repository.savedAssignments).hasSize(1);
        assertThat(repository.assignmentClasses.get(4001L)).containsExactly(11L, 12L);
    }

    @Test
    void updateAssignmentRefreshesCourseDetailsAndClassLinks() {
        FakeAssignmentRepository repository = new FakeAssignmentRepository();
        CourseFeignClient courseFeignClient = mock(CourseFeignClient.class);
        when(courseFeignClient.getCourse(202L)).thenReturn(course(202L, 7L, "微服务架构", 24));
        when(courseFeignClient.listCourseAssignments(7L, 202L, null)).thenReturn(List.of(
                classAssignment(21L, 202L)
        ));

        TeacherAssignmentCommandService service = new TeacherAssignmentCommandService(
                repository,
                courseFeignClient,
                new InMemoryOutboxEventRepository(),
                new ObjectMapper().findAndRegisterModules());

        AssignmentDTO updated = service.updateAssignment(7L, 2001L, request(
                "Homework 1 revised",
                202L,
                "chapter 3",
                "2026-09-01T08:00:00.000Z",
                "2026-09-20T23:59:59.000Z",
                120
        ));

        assertThat(updated.getCourseId()).isEqualTo(202L);
        assertThat(updated.getMaxScore()).isEqualTo(120);
        assertThat(updated.getTotalStudents()).isEqualTo(24);
        assertThat(repository.assignment.getDueDate()).isEqualTo("2026-09-20 23:59:59");
        assertThat(repository.assignmentClasses.get(2001L)).containsExactly(21L);
    }

    @Test
    void deleteAssignmentRemovesPersistedAssignment() {
        FakeAssignmentRepository repository = new FakeAssignmentRepository();
        CourseFeignClient courseFeignClient = mock(CourseFeignClient.class);
        TeacherAssignmentCommandService service = new TeacherAssignmentCommandService(
                repository,
                courseFeignClient,
                new InMemoryOutboxEventRepository(),
                new ObjectMapper().findAndRegisterModules());

        service.deleteAssignment(7L, 2001L);

        assertThat(repository.deletedAssignmentIds).containsExactly(2001L);
    }

    @Test
    void gradeSubmissionMarksSubmissionAndRefreshesAssignmentSummary() {
        FakeAssignmentRepository repository = new FakeAssignmentRepository();
        CourseFeignClient courseFeignClient = mock(CourseFeignClient.class);
        InMemoryOutboxEventRepository outboxEventRepository = new InMemoryOutboxEventRepository();
        TeacherAssignmentCommandService service = new TeacherAssignmentCommandService(
                repository,
                courseFeignClient,
                outboxEventRepository,
                new ObjectMapper().findAndRegisterModules());

        TeacherAssignmentGradeRequestDTO request = new TeacherAssignmentGradeRequestDTO();
        request.setScore(95);
        request.setTeacherComment("做得不错");
        request.setGraded(true);

        service.gradeSubmission(7L, 3001L, request);

        assertThat(repository.submission.getGraded()).isTrue();
        assertThat(repository.submission.getScore()).isEqualTo(95);
        assertThat(repository.submission.getTeacherComment()).isEqualTo("做得不错");
        assertThat(repository.assignment.getGradedCount()).isEqualTo(1);
        assertThat(repository.assignment.getStatus()).isEqualTo("graded");
        assertThat(repository.lastUpdatedGradedCount).isEqualTo(1);
        assertThat(outboxEventRepository.savedEvents).hasSize(1);
        assertThat(outboxEventRepository.savedEvents.get(0).bindingName()).isEqualTo("assignment.graded");
        assertThat(outboxEventRepository.savedEvents.get(0).eventType()).isEqualTo("AssignmentGradedEvent");
    }

    @Test
    void createAssignmentFailsClosedWhenCourseServiceCannotProvideOwnedCourse() {
        FakeAssignmentRepository repository = new FakeAssignmentRepository();
        CourseFeignClient courseFeignClient = mock(CourseFeignClient.class);
        when(courseFeignClient.getCourse(101L)).thenReturn(null);

        TeacherAssignmentCommandService service = new TeacherAssignmentCommandService(
                repository,
                courseFeignClient,
                new InMemoryOutboxEventRepository(),
                new ObjectMapper().findAndRegisterModules());

        assertThatThrownBy(() -> service.createAssignment(7L, request(
                "Homework 2",
                101L,
                "chapter 2",
                "2026-09-01 08:00:00",
                "2026-09-15 23:59:59",
                100
        ))).hasMessageContaining("课程不存在");
    }

    private static final class InMemoryOutboxEventRepository implements OutboxEventRepository {
        private final List<OutboxEventEntity> savedEvents = new ArrayList<>();

        @Override
        public void save(OutboxEventEntity event) {
            savedEvents.add(event);
        }

        @Override
        public List<OutboxEventEntity> findDuePendingEvents(int limit, Instant now) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void markPublished(long id, Instant publishedAt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void markFailed(long id, int retryCount, Instant nextRetryAt, String lastError, int maxRetries) {
            throw new UnsupportedOperationException();
        }
    }

    private static TeacherAssignmentUpsertRequestDTO request(String title,
                                                             Long courseId,
                                                             String description,
                                                             String publishDate,
                                                             String dueDate,
                                                             Integer maxScore) {
        TeacherAssignmentUpsertRequestDTO request = new TeacherAssignmentUpsertRequestDTO();
        request.setTitle(title);
        request.setCourseId(courseId);
        request.setDescription(description);
        request.setPublishDate(publishDate);
        request.setDueDate(dueDate);
        request.setIsActive(true);
        request.setMaxScore(maxScore);
        return request;
    }

    private static CourseDTO course(Long id, Long teacherId, String name, Integer studentCount) {
        CourseDTO dto = new CourseDTO();
        dto.setId(id);
        dto.setTeacherId(teacherId);
        dto.setCourseName(name);
        dto.setStudentCount(studentCount);
        return dto;
    }

    private static CourseAssignmentDTO classAssignment(Long classId, Long courseId) {
        CourseAssignmentDTO dto = new CourseAssignmentDTO();
        dto.setClassId(classId);
        dto.setCourseId(courseId);
        return dto;
    }

    private static class FakeAssignmentRepository implements AssignmentRepository {
        private final AssignmentRecord assignment = seededAssignment();
        private final AssignmentSubmissionRecord submission = seededSubmission();
        private final List<AssignmentRecord> savedAssignments = new ArrayList<>();
        private final Map<Long, List<Long>> assignmentClasses = new LinkedHashMap<>();
        private final List<Long> deletedAssignmentIds = new ArrayList<>();
        private long nextAssignmentId = 4001L;
        private Integer lastUpdatedGradedCount;

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
            return Optional.empty();
        }

        @Override
        public List<AssignmentSubmissionRecord> findSubmissionsByStudentId(Long studentId) {
            return List.of();
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
            this.lastUpdatedGradedCount = assignment.getGradedCount();
        }

        @Override
        public AssignmentRecord insertAssignment(AssignmentRecord assignment) {
            assignment.setId(nextAssignmentId++);
            savedAssignments.add(copy(assignment));
            return assignment;
        }

        @Override
        public void updateAssignmentDetails(AssignmentRecord assignment) {
            this.assignment.setTitle(assignment.getTitle());
            this.assignment.setDescription(assignment.getDescription());
            this.assignment.setCourseId(assignment.getCourseId());
            this.assignment.setPublishDate(assignment.getPublishDate());
            this.assignment.setDueDate(assignment.getDueDate());
            this.assignment.setIsActive(assignment.getIsActive());
            this.assignment.setMaxScore(assignment.getMaxScore());
            this.assignment.setTeacherId(assignment.getTeacherId());
            this.assignment.setTotalStudents(assignment.getTotalStudents());
        }

        @Override
        public void replaceAssignmentClasses(Long assignmentId, List<Long> classIds) {
            assignmentClasses.put(assignmentId, new ArrayList<>(classIds));
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
            deletedAssignmentIds.add(assignmentId);
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
            record.setGraded(false);
            record.setIsLate(false);
            record.setLatePenalty(null);
            record.setScore(null);
            record.setTeacherComment(null);
            return record;
        }

        private static AssignmentRecord copy(AssignmentRecord source) {
            AssignmentRecord target = new AssignmentRecord();
            target.setId(source.getId());
            target.setTitle(source.getTitle());
            target.setDescription(source.getDescription());
            target.setCourseId(source.getCourseId());
            target.setDueDate(source.getDueDate());
            target.setPublishDate(source.getPublishDate());
            target.setIsActive(source.getIsActive());
            target.setTeacherId(source.getTeacherId());
            target.setMaxScore(source.getMaxScore());
            target.setSubmissionCount(source.getSubmissionCount());
            target.setGradedCount(source.getGradedCount());
            target.setStatus(source.getStatus());
            target.setTotalStudents(source.getTotalStudents());
            return target;
        }
    }
}
