package com._202510007517.platform.assignment.service;

import com._202510007517.platform.assignment.api.dto.AssignmentDTO;
import com._202510007517.platform.assignment.api.dto.AssignmentSubmissionDTO;
import com._202510007517.platform.assignment.api.dto.TeacherAssignmentGradeRequestDTO;
import com._202510007517.platform.assignment.api.dto.TeacherAssignmentUpsertRequestDTO;
import com._202510007517.platform.assignment.domain.AssignmentRecord;
import com._202510007517.platform.assignment.domain.AssignmentSubmissionRecord;
import com._202510007517.platform.assignment.repository.AssignmentRepository;
import com._202510007517.platform.common.event.outbox.OutboxEventEntity;
import com._202510007517.platform.common.event.outbox.OutboxEventRepository;
import com._202510007517.platform.common.exception.ResourceNotFoundException;
import com._202510007517.platform.course.api.dto.CourseAssignmentDTO;
import com._202510007517.platform.course.api.dto.CourseDTO;
import com._202510007517.platform.course.api.feign.CourseFeignClient;
import com._202510007517.platform.events.EventAggregate;
import com._202510007517.platform.events.assignment.AssignmentGradedEvent;
import com._202510007517.platform.events.assignment.AssignmentGradedPayload;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class TeacherAssignmentCommandService {

    private static final DateTimeFormatter LEGACY_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter HTML_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private final AssignmentRepository assignmentRepository;
    private final CourseFeignClient courseFeignClient;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public TeacherAssignmentCommandService(AssignmentRepository assignmentRepository,
                                           CourseFeignClient courseFeignClient,
                                           OutboxEventRepository outboxEventRepository,
                                           ObjectMapper objectMapper) {
        this.assignmentRepository = assignmentRepository;
        this.courseFeignClient = courseFeignClient;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public AssignmentDTO createAssignment(Long teacherId, TeacherAssignmentUpsertRequestDTO request) {
        CourseDTO course = requireOwnedCourse(teacherId, request.getCourseId());

        AssignmentRecord assignment = new AssignmentRecord();
        assignment.setTitle(request.getTitle());
        assignment.setDescription(request.getDescription());
        assignment.setCourseId(request.getCourseId());
        assignment.setDueDate(normalizeDateTime(request.getDueDate(), "截止时间"));
        assignment.setPublishDate(resolvePublishDate(request.getPublishDate()));
        assignment.setIsActive(request.getIsActive() == null ? Boolean.TRUE : request.getIsActive());
        assignment.setTeacherId(teacherId);
        assignment.setMaxScore(request.getMaxScore());
        assignment.setSubmissionCount(0);
        assignment.setGradedCount(0);
        assignment.setStatus("pending");
        assignment.setTotalStudents(resolveStudentCount(course));

        AssignmentRecord saved = assignmentRepository.insertAssignment(assignment);
        assignmentRepository.replaceAssignmentClasses(saved.getId(), resolveCourseClassIds(teacherId, request.getCourseId()));
        return toDto(saved, course);
    }

    @Transactional(rollbackFor = Exception.class)
    public AssignmentDTO updateAssignment(Long teacherId, Long assignmentId, TeacherAssignmentUpsertRequestDTO request) {
        AssignmentRecord existing = requireTeacherAssignment(teacherId, assignmentId);
        CourseDTO course = requireOwnedCourse(teacherId, request.getCourseId());

        existing.setTitle(request.getTitle());
        existing.setDescription(request.getDescription());
        existing.setCourseId(request.getCourseId());
        existing.setDueDate(normalizeDateTime(request.getDueDate(), "截止时间"));
        if (request.getPublishDate() != null && !request.getPublishDate().isBlank()) {
            existing.setPublishDate(normalizeDateTime(request.getPublishDate(), "发布时间"));
        }
        existing.setIsActive(request.getIsActive() == null ? Boolean.TRUE : request.getIsActive());
        existing.setTeacherId(teacherId);
        existing.setMaxScore(request.getMaxScore());
        existing.setTotalStudents(resolveStudentCount(course));

        assignmentRepository.updateAssignmentDetails(existing);
        assignmentRepository.replaceAssignmentClasses(existing.getId(), resolveCourseClassIds(teacherId, request.getCourseId()));
        return toDto(existing, course);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteAssignment(Long teacherId, Long assignmentId) {
        requireTeacherAssignment(teacherId, assignmentId);
        assignmentRepository.deleteAssignmentCascade(assignmentId);
    }

    @Transactional(rollbackFor = Exception.class)
    public AssignmentSubmissionDTO gradeSubmission(Long teacherId,
                                                   Long submissionId,
                                                   TeacherAssignmentGradeRequestDTO request) {
        if (request == null || request.getScore() == null) {
            throw new IllegalArgumentException("分数不能为空");
        }
        AssignmentSubmissionRecord submission = assignmentRepository.findSubmissionById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("作业提交记录不存在"));
        AssignmentRecord assignment = requireTeacherAssignment(teacherId, submission.getAssignmentId());

        submission.setScore(request.getScore());
        submission.setTeacherComment(request.getTeacherComment());
        submission.setGraded(request.getGraded() == null || request.getGraded());
        assignmentRepository.updateSubmission(submission);

        refreshAssignmentSummary(assignment);
        persistAssignmentGradedEvent(assignment, submission);
        return toSubmissionDto(submission);
    }

    private AssignmentRecord requireTeacherAssignment(Long teacherId, Long assignmentId) {
        AssignmentRecord assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("作业不存在"));
        if (!teacherId.equals(assignment.getTeacherId())) {
            throw new ResourceNotFoundException("作业不存在");
        }
        return assignment;
    }

    private CourseDTO requireOwnedCourse(Long teacherId, Long courseId) {
        CourseDTO course = courseFeignClient.getCourse(courseId);
        if (course == null) {
            throw new ResourceNotFoundException("课程不存在");
        }
        if (course.getTeacherId() != null && !teacherId.equals(course.getTeacherId())) {
            throw new IllegalArgumentException("无权关联其他教师的课程");
        }
        return course;
    }

    private List<Long> resolveCourseClassIds(Long teacherId, Long courseId) {
        List<CourseAssignmentDTO> assignments = courseFeignClient.listCourseAssignments(teacherId, courseId, null);
        if (assignments == null || assignments.isEmpty()) {
            return List.of();
        }
        Set<Long> classIds = new LinkedHashSet<>();
        for (CourseAssignmentDTO assignment : assignments) {
            if (assignment != null && assignment.getClassId() != null) {
                classIds.add(assignment.getClassId());
            }
        }
        return new ArrayList<>(classIds);
    }

    private static Integer resolveStudentCount(CourseDTO course) {
        return course.getStudentCount() == null ? 0 : course.getStudentCount();
    }

    private static String resolvePublishDate(String publishDate) {
        if (publishDate == null || publishDate.isBlank()) {
            return LocalDateTime.now().format(LEGACY_DATE_TIME);
        }
        return normalizeDateTime(publishDate, "发布时间");
    }

    private static String normalizeDateTime(String value, String fieldName) {
        LocalDateTime dateTime = parseDateTime(value);
        if (dateTime == null) {
            throw new IllegalArgumentException("无效的" + fieldName + "格式: " + value);
        }
        return dateTime.format(LEGACY_DATE_TIME);
    }

    private void refreshAssignmentSummary(AssignmentRecord assignment) {
        List<AssignmentSubmissionRecord> submissions = assignmentRepository.findSubmissionsByAssignmentId(assignment.getId());
        int submittedCount = submissions.size();
        int gradedCount = (int) submissions.stream()
                .filter(submission -> Boolean.TRUE.equals(submission.getGraded()))
                .count();
        assignment.setSubmissionCount(submittedCount);
        assignment.setGradedCount(gradedCount);
        assignment.setStatus(submittedCount == 0 ? "pending" : (gradedCount > 0 ? "graded" : "submitted"));
        assignmentRepository.updateAssignment(assignment);
    }

    private void persistAssignmentGradedEvent(AssignmentRecord assignment, AssignmentSubmissionRecord submission) {
        Instant occurredAt = parseEventInstant(submission.getSubmissionDate());
        AssignmentGradedEvent event = new AssignmentGradedEvent(
                buildAssignmentGradedEventId(submission),
                occurredAt,
                new EventAggregate("assignment_submission", String.valueOf(submission.getId())),
                new AssignmentGradedPayload(
                        assignment.getId(),
                        submission.getId(),
                        submission.getStudentId(),
                        assignment.getCourseId(),
                        submission.getScore(),
                        submission.getTeacherComment(),
                        occurredAt));

        outboxEventRepository.save(new OutboxEventEntity(
                null,
                event.eventId(),
                event.aggregate().type(),
                event.aggregate().id(),
                event.eventType(),
                "assignment.graded",
                toJson(event),
                "{}",
                0,
                0,
                occurredAt,
                null,
                occurredAt,
                occurredAt,
                null));
    }

    private static String buildAssignmentGradedEventId(AssignmentSubmissionRecord submission) {
        return "assignment-graded-" + submission.getId() + "-" + submission.getScore()
                + "-" + shortSha256(normalizeTeacherComment(submission.getTeacherComment()));
    }

    private static String normalizeTeacherComment(String value) {
        return value == null ? "" : value.trim();
    }

    private static String shortSha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(12);
            for (int i = 0; i < 6; i++) {
                builder.append(String.format("%02x", hash[i]));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", ex);
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("序列化作业批改事件失败", ex);
        }
    }

    private static Instant parseEventInstant(String dateTime) {
        LocalDateTime parsed = parseDateTime(dateTime);
        if (parsed == null) {
            return Instant.now();
        }
        return parsed.toInstant(ZoneOffset.ofHours(8));
    }

    private static LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return OffsetDateTime.parse(value).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDateTime.parse(value, LEGACY_DATE_TIME);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDateTime.parse(value, HTML_DATE_TIME);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return Instant.parse(value).atZone(ZoneId.systemDefault()).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDate.parse(value).atStartOfDay();
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private static AssignmentDTO toDto(AssignmentRecord record, CourseDTO course) {
        AssignmentDTO dto = new AssignmentDTO();
        dto.setId(record.getId());
        dto.setTitle(record.getTitle());
        dto.setDescription(record.getDescription());
        dto.setCourseId(record.getCourseId());
        dto.setDueDate(record.getDueDate());
        dto.setPublishDate(record.getPublishDate());
        dto.setIsActive(record.getIsActive());
        dto.setTeacherId(record.getTeacherId());
        dto.setMaxScore(record.getMaxScore());
        dto.setSubmissionCount(record.getSubmissionCount());
        dto.setSubmittedCount(record.getSubmissionCount());
        dto.setGradedCount(record.getGradedCount());
        dto.setStatus(record.getStatus());
        dto.setTotalStudents(record.getTotalStudents());
        if (course != null) {
            dto.setCourseName(course.getCourseName());
            dto.setTeacherName(course.getTeacherName());
        }
        return dto;
    }

    private static AssignmentSubmissionDTO toSubmissionDto(AssignmentSubmissionRecord record) {
        AssignmentSubmissionDTO dto = new AssignmentSubmissionDTO();
        dto.setId(record.getId());
        dto.setAssignmentId(record.getAssignmentId());
        dto.setStudentId(record.getStudentId());
        dto.setContent(record.getContent());
        dto.setSubmissionDate(record.getSubmissionDate());
        dto.setGraded(Boolean.TRUE.equals(record.getGraded()));
        dto.setIsLate(Boolean.TRUE.equals(record.getIsLate()));
        dto.setLatePenalty(record.getLatePenalty());
        dto.setScore(record.getScore());
        dto.setTeacherComment(record.getTeacherComment());
        dto.setStatus(Boolean.TRUE.equals(record.getGraded()) ? "graded"
                : (Boolean.TRUE.equals(record.getIsLate()) ? "late" : "submitted"));
        return dto;
    }
}
