package com._202510007517.platform.assignment.service;

import com._202510007517.platform.assignment.api.dto.AssignmentDTO;
import com._202510007517.platform.assignment.api.dto.AssignmentStudentScoreDTO;
import com._202510007517.platform.assignment.api.dto.AssignmentSubmissionDTO;
import com._202510007517.platform.assignment.api.dto.AssignmentSubmitRequestDTO;
import com._202510007517.platform.assignment.domain.AssignmentRecord;
import com._202510007517.platform.assignment.domain.AssignmentSubmissionRecord;
import com._202510007517.platform.assignment.repository.AssignmentRepository;
import com._202510007517.platform.common.event.outbox.OutboxEventEntity;
import com._202510007517.platform.common.event.outbox.OutboxEventRepository;
import com._202510007517.platform.common.exception.ResourceNotFoundException;
import com._202510007517.platform.course.api.dto.CourseDTO;
import com._202510007517.platform.course.api.feign.CourseFeignClient;
import com._202510007517.platform.events.EventAggregate;
import com._202510007517.platform.events.assignment.AssignmentSubmittedEvent;
import com._202510007517.platform.events.assignment.AssignmentSubmittedPayload;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com._202510007517.platform.user.api.dto.UserProfileDTO;
import com._202510007517.platform.user.api.feign.UserFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class AssignmentApplicationService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AssignmentRepository assignmentRepository;
    private final CourseFeignClient courseFeignClient;
    private final UserFeignClient userFeignClient;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final AssessmentAttachmentService attachmentService;

    public AssignmentApplicationService(AssignmentRepository assignmentRepository,
                                        CourseFeignClient courseFeignClient,
                                        UserFeignClient userFeignClient,
                                        OutboxEventRepository outboxEventRepository,
                                        ObjectMapper objectMapper) {
        this(assignmentRepository,
                courseFeignClient,
                userFeignClient,
                outboxEventRepository,
                objectMapper,
                AssessmentAttachmentService.none());
    }

    @Autowired
    public AssignmentApplicationService(AssignmentRepository assignmentRepository,
                                        CourseFeignClient courseFeignClient,
                                        UserFeignClient userFeignClient,
                                        OutboxEventRepository outboxEventRepository,
                                        ObjectMapper objectMapper,
                                        AssessmentAttachmentService attachmentService) {
        this.assignmentRepository = assignmentRepository;
        this.courseFeignClient = courseFeignClient;
        this.userFeignClient = userFeignClient;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
        this.attachmentService = attachmentService;
    }

    public AssignmentDTO getAssignment(Long assignmentId) {
        return toDto(assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("作业不存在")));
    }

    public List<AssignmentDTO> listByCourse(Long courseId) {
        return assignmentRepository.findByCourseId(courseId).stream()
                .map(AssignmentApplicationService::toDto)
                .toList();
    }

    public List<Long> listKnowledgePointIds(Long assignmentId) {
        getAssignment(assignmentId);
        return assignmentRepository.findKnowledgePointIdsByAssignmentId(assignmentId);
    }

    @Transactional(rollbackFor = Exception.class)
    public AssignmentSubmissionDTO submit(Long assignmentId, AssignmentSubmitRequestDTO request) {
        return submit(assignmentId, request, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public AssignmentSubmissionDTO submit(Long assignmentId,
                                          AssignmentSubmitRequestDTO request,
                                          MultipartFile[] files) {
        AssignmentRecord assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("作业不存在"));
        if (request == null || request.getStudentId() == null) {
            throw new IllegalArgumentException("学生ID不能为空");
        }
        if (request.getContent() == null || request.getContent().isBlank()) {
            throw new IllegalArgumentException("提交内容不能为空");
        }

        List<Long> classIds = loadStudentClassIds(request.getStudentId());
        AssignmentSubmissionRecord submission = new AssignmentSubmissionRecord();
        submission.setAssignmentId(assignmentId);
        submission.setStudentId(request.getStudentId());
        submission.setContent(request.getContent());
        submission.setSubmissionDate(request.getSubmissionDate() != null && !request.getSubmissionDate().isBlank()
                ? request.getSubmissionDate()
                : LocalDateTime.now().format(DATE_TIME_FORMATTER));
        submission.setGraded(false);
        submission.setIsLate(false);
        submission.setLatePenalty(null);
        submission.setScore(null);
        submission.setTeacherComment(null);
        AssignmentSubmissionRecord saved = assignmentRepository.insertSubmission(submission);

        assignment.setSubmissionCount((assignment.getSubmissionCount() == null ? 0 : assignment.getSubmissionCount()) + 1);
        assignment.setStatus("submitted");
        assignmentRepository.updateAssignment(assignment);
        persistSubmissionEvent(assignment, saved, classIds);
        try {
            attachmentService.saveSubmissionAttachments(saved.getId(), request.getStudentId(), files);
        } catch (IOException ex) {
            throw new IllegalStateException("保存作业提交附件失败", ex);
        }
        AssignmentSubmissionDTO dto = toSubmissionDto(saved);
        dto.setAttachments(attachmentService.getSubmissionAttachmentDtos(saved.getId()));
        return dto;
    }

    public List<AssignmentSubmissionDTO> listSubmissions(Long assignmentId) {
        getAssignment(assignmentId);
        return List.of();
    }

    public Map<String, Object> listStudentAssignments(Long studentId,
                                                      Integer page,
                                                      Integer size,
                                                      String sortBy,
                                                      String order,
                                                      Long courseId,
                                                      Boolean submitted,
                                                      Boolean isActive) {
        int safePage = clampPage(page, 1);
        int safeSize = clampSize(size, 10);

        List<Long> classIds = loadStudentClassIds(studentId);
        List<AssignmentRecord> assignments = assignmentRepository.findByClassIds(classIds);
        Map<Long, AssignmentSubmissionRecord> submissions = loadStudentSubmissionsByAssignment(studentId);
        Map<Long, String> courseNames = loadCourseNames(extractCourseIds(assignments));
        Map<Long, String> teacherNames = loadTeacherNames(extractTeacherIds(assignments));

        List<Map<String, Object>> filtered = assignments.stream()
                .filter(assignment -> courseId == null || courseId.equals(assignment.getCourseId()))
                .filter(assignment -> isActive == null || isActive.equals(assignment.getIsActive()))
                .filter(assignment -> matchesSubmitted(submissions.get(assignment.getId()), submitted))
                .sorted((left, right) -> compareAssignments(left, right, sortBy, order))
                .map(assignment -> toStudentAssignmentMap(
                        assignment,
                        submissions.get(assignment.getId()),
                        courseNames.get(assignment.getCourseId()),
                        teacherNames.get(assignment.getTeacherId())))
                .toList();

        int totalElements = filtered.size();
        int totalPages = totalElements == 0 ? 1 : (int) Math.ceil((double) totalElements / safeSize);
        int startIndex = Math.min((safePage - 1) * safeSize, totalElements);
        int endIndex = Math.min(startIndex + safeSize, totalElements);
        List<Map<String, Object>> content = filtered.subList(startIndex, endIndex);

        Map<String, Object> pageResult = new LinkedHashMap<>();
        pageResult.put("content", content);
        pageResult.put("totalPages", totalPages);
        pageResult.put("totalElements", totalElements);
        pageResult.put("size", safeSize);
        pageResult.put("number", safePage - 1);
        pageResult.put("first", safePage == 1);
        pageResult.put("last", safePage >= totalPages);
        pageResult.put("numberOfElements", content.size());
        pageResult.put("empty", content.isEmpty());
        return pageResult;
    }

    public Map<String, Object> getStudentAssignmentDetail(Long studentId, Long assignmentId) {
        AssignmentRecord assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("作业不存在"));
        List<Long> classIds = loadStudentClassIds(studentId);
        boolean belongsToStudent = assignmentRepository.findByClassIds(classIds).stream()
                .anyMatch(item -> assignmentId.equals(item.getId()));
        if (!belongsToStudent) {
            throw new ResourceNotFoundException("作业不存在");
        }

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("id", assignment.getId());
        detail.put("title", assignment.getTitle());
        detail.put("description", assignment.getDescription());
        detail.put("dueDate", assignment.getDueDate());
        detail.put("publishDate", assignment.getPublishDate());
        detail.put("isActive", assignment.getIsActive());
        detail.put("courseId", assignment.getCourseId());
        detail.put("teacherId", assignment.getTeacherId());
        detail.put("attachments", attachmentService.getAttachmentDtos(assignmentId));

        CourseDTO course = loadCourse(assignment.getCourseId());
        if (course != null) {
            detail.put("courseName", course.getCourseName());
        }
        String teacherName = loadTeacherNames(Set.of(assignment.getTeacherId())).get(assignment.getTeacherId());
        if (teacherName != null) {
            detail.put("teacherName", teacherName);
        }

        assignmentRepository.findSubmissionByAssignmentAndStudent(assignmentId, studentId)
                .ifPresent(submission -> detail.put("submission", toStudentSubmissionMap(submission)));
        return detail;
    }

    public List<Map<String, Object>> listStudentSubmissions(Long studentId) {
        List<AssignmentSubmissionRecord> submissions = assignmentRepository.findSubmissionsByStudentId(studentId);
        Set<Long> assignmentIds = new LinkedHashSet<>();
        for (AssignmentSubmissionRecord submission : submissions) {
            assignmentIds.add(submission.getAssignmentId());
        }
        Map<Long, AssignmentRecord> assignments = new LinkedHashMap<>();
        for (Long assignmentId : assignmentIds) {
            assignmentRepository.findById(assignmentId).ifPresent(assignment -> assignments.put(assignmentId, assignment));
        }
        Map<Long, String> courseNames = loadCourseNames(extractCourseIds(new ArrayList<>(assignments.values())));

        return submissions.stream()
                .map(submission -> {
                    AssignmentRecord assignment = assignments.get(submission.getAssignmentId());
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", submission.getId());
                    item.put("assignmentId", submission.getAssignmentId());
                    item.put("title", assignment != null ? assignment.getTitle() : "作业");
                    item.put("courseName", assignment != null ? courseNames.get(assignment.getCourseId()) : null);
                    item.put("content", submission.getContent());
                    item.put("submissionDate", submission.getSubmissionDate());
                    item.put("isLate", Boolean.TRUE.equals(submission.getIsLate()));
                    item.put("latePenalty", submission.getLatePenalty());
                    item.put("score", submission.getScore());
                    item.put("teacherComment", submission.getTeacherComment());
                    item.put("graded", Boolean.TRUE.equals(submission.getGraded()));
                    item.put("status", resolveSubmissionStatus(submission));
                    item.put("attachments", attachmentService.getSubmissionAttachmentDtos(submission.getId()));
                    return item;
                })
                .toList();
    }

    public List<AssignmentStudentScoreDTO> listStudentScores(Long studentId) {
        List<AssignmentSubmissionRecord> submissions = assignmentRepository.findGradedSubmissionsByStudentId(studentId);
        Set<Long> assignmentIds = new LinkedHashSet<>();
        for (AssignmentSubmissionRecord submission : submissions) {
            assignmentIds.add(submission.getAssignmentId());
        }
        Map<Long, AssignmentRecord> assignments = new LinkedHashMap<>();
        for (Long assignmentId : assignmentIds) {
            assignmentRepository.findById(assignmentId).ifPresent(assignment -> assignments.put(assignmentId, assignment));
        }
        Map<Long, String> courseNames = loadCourseNames(extractCourseIds(new ArrayList<>(assignments.values())));

        return submissions.stream()
                .map(submission -> toStudentScoreDto(assignments.get(submission.getAssignmentId()), submission, courseNames))
                .filter(item -> item != null)
                .toList();
    }

    private Map<Long, AssignmentSubmissionRecord> loadStudentSubmissionsByAssignment(Long studentId) {
        Map<Long, AssignmentSubmissionRecord> submissionMap = new LinkedHashMap<>();
        for (AssignmentSubmissionRecord submission : assignmentRepository.findSubmissionsByStudentId(studentId)) {
            submissionMap.putIfAbsent(submission.getAssignmentId(), submission);
        }
        return submissionMap;
    }

    private List<Long> loadStudentClassIds(Long studentId) {
        if (studentId == null) {
            return List.of();
        }
        try {
            List<Long> classIds = courseFeignClient.listStudentClassIds(studentId);
            return classIds == null ? List.of() : classIds;
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    private void persistSubmissionEvent(AssignmentRecord assignment,
                                        AssignmentSubmissionRecord submission,
                                        List<Long> classIds) {
        Long classId = classIds == null || classIds.isEmpty() ? null : classIds.get(0);
        if (classId == null) {
            return;
        }

        Instant occurredAt = parseSubmissionInstant(submission.getSubmissionDate());
        AssignmentSubmittedEvent event = new AssignmentSubmittedEvent(
                "assignment-submitted-" + submission.getId(),
                occurredAt,
                new EventAggregate("assignment_submission", String.valueOf(submission.getId())),
                new AssignmentSubmittedPayload(
                        assignment.getId(),
                        submission.getId(),
                        submission.getStudentId(),
                        assignment.getCourseId(),
                        classId,
                        Boolean.TRUE.equals(submission.getIsLate()),
                        occurredAt));

        outboxEventRepository.save(new OutboxEventEntity(
                null,
                event.eventId(),
                event.aggregate().type(),
                event.aggregate().id(),
                event.eventType(),
                "assignment.submitted",
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

    private Instant parseSubmissionInstant(String submissionDate) {
        if (submissionDate == null || submissionDate.isBlank()) {
            return Instant.now();
        }
        try {
            return LocalDateTime.parse(submissionDate, DATE_TIME_FORMATTER).toInstant(ZoneOffset.ofHours(8));
        } catch (DateTimeParseException ex) {
            return Instant.now();
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("序列化作业提交事件失败", ex);
        }
    }

    private Map<Long, String> loadCourseNames(Set<Long> courseIds) {
        Map<Long, String> courseNames = new LinkedHashMap<>();
        for (Long courseId : courseIds) {
            CourseDTO course = loadCourse(courseId);
            if (course != null) {
                courseNames.put(courseId, course.getCourseName());
            }
        }
        return courseNames;
    }

    private CourseDTO loadCourse(Long courseId) {
        if (courseId == null) {
            return null;
        }
        try {
            return courseFeignClient.getCourse(courseId);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private Map<Long, String> loadTeacherNames(Set<Long> teacherIds) {
        Map<Long, String> teacherNames = new LinkedHashMap<>();
        if (teacherIds.isEmpty()) {
            return teacherNames;
        }
        try {
            List<UserProfileDTO> profiles = userFeignClient.listByIds(new ArrayList<>(teacherIds));
            if (profiles != null) {
                for (UserProfileDTO profile : profiles) {
                    if (profile != null && profile.getId() != null) {
                        teacherNames.put(profile.getId(), profile.getName());
                    }
                }
            }
        } catch (RuntimeException ignored) {
            // Keep student APIs available even if enrichment fails.
        }
        return teacherNames;
    }

    private static Set<Long> extractCourseIds(List<AssignmentRecord> assignments) {
        Set<Long> courseIds = new LinkedHashSet<>();
        for (AssignmentRecord assignment : assignments) {
            if (assignment.getCourseId() != null) {
                courseIds.add(assignment.getCourseId());
            }
        }
        return courseIds;
    }

    private static Set<Long> extractTeacherIds(List<AssignmentRecord> assignments) {
        Set<Long> teacherIds = new LinkedHashSet<>();
        for (AssignmentRecord assignment : assignments) {
            if (assignment.getTeacherId() != null) {
                teacherIds.add(assignment.getTeacherId());
            }
        }
        return teacherIds;
    }

    private Map<String, Object> toStudentAssignmentMap(AssignmentRecord assignment,
                                                       AssignmentSubmissionRecord submission,
                                                       String courseName,
                                                       String teacherName) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", assignment.getId());
        item.put("title", assignment.getTitle());
        item.put("description", assignment.getDescription());
        item.put("courseId", assignment.getCourseId());
        item.put("courseName", courseName);
        item.put("dueDate", assignment.getDueDate());
        item.put("publishDate", assignment.getPublishDate());
        item.put("teacherId", assignment.getTeacherId());
        item.put("teacherName", teacherName);
        item.put("isActive", assignment.getIsActive());
        item.put("attachments", attachmentService.getAttachmentDtos(assignment.getId()));
        item.put("submission", submission != null ? toStudentSubmissionMap(submission) : null);
        return item;
    }

    private Map<String, Object> toStudentSubmissionMap(AssignmentSubmissionRecord submission) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", submission.getId());
        item.put("submissionDate", submission.getSubmissionDate());
        item.put("isLate", Boolean.TRUE.equals(submission.getIsLate()));
        item.put("latePenalty", submission.getLatePenalty());
        item.put("score", submission.getScore());
        item.put("teacherComment", submission.getTeacherComment());
        item.put("graded", Boolean.TRUE.equals(submission.getGraded()));
        item.put("content", submission.getContent());
        item.put("status", resolveSubmissionStatus(submission));
        item.put("attachments", attachmentService.getSubmissionAttachmentDtos(submission.getId()));
        return item;
    }

    private static boolean matchesSubmitted(AssignmentSubmissionRecord submission, Boolean submitted) {
        if (submitted == null) {
            return true;
        }
        return submitted.equals(submission != null);
    }

    private static int compareAssignments(AssignmentRecord left,
                                          AssignmentRecord right,
                                          String sortBy,
                                          String order) {
        String field = sortBy == null || sortBy.isBlank() ? "dueDate" : sortBy;
        int comparison;
        switch (field.toLowerCase(Locale.ROOT)) {
            case "publishdate":
                comparison = compareDateTimeStrings(left.getPublishDate(), right.getPublishDate());
                break;
            case "title":
                comparison = safeString(left.getTitle()).compareToIgnoreCase(safeString(right.getTitle()));
                break;
            case "id":
                comparison = compareLongs(left.getId(), right.getId());
                break;
            case "duedate":
            default:
                comparison = compareDateTimeStrings(left.getDueDate(), right.getDueDate());
                break;
        }
        if ("ASC".equalsIgnoreCase(order)) {
            return comparison;
        }
        return -comparison;
    }

    private static int compareDateTimeStrings(String left, String right) {
        LocalDateTime leftDate = parseDateTime(left);
        LocalDateTime rightDate = parseDateTime(right);
        if (leftDate == null && rightDate == null) {
            return 0;
        }
        if (leftDate == null) {
            return 1;
        }
        if (rightDate == null) {
            return -1;
        }
        return leftDate.compareTo(rightDate);
    }

    private static int compareLongs(Long left, Long right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        return left.compareTo(right);
    }

    private static String safeString(String value) {
        return value == null ? "" : value;
    }

    private static int clampPage(Integer page, int defaultValue) {
        if (page == null || page < 1) {
            return defaultValue;
        }
        return page;
    }

    private static int clampSize(Integer size, int defaultValue) {
        if (size == null) {
            return defaultValue;
        }
        return Math.max(1, Math.min(size, 100));
    }

    private static String resolveSubmissionStatus(AssignmentSubmissionRecord submission) {
        if (Boolean.TRUE.equals(submission.getGraded())) {
            return "graded";
        }
        if (Boolean.TRUE.equals(submission.getIsLate())) {
            return "late";
        }
        return "submitted";
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
            return LocalDateTime.parse(value, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDate.parse(value).atStartOfDay();
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private static AssignmentDTO toDto(AssignmentRecord record) {
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
        dto.setGradedCount(record.getGradedCount());
        dto.setStatus(record.getStatus());
        dto.setTotalStudents(record.getTotalStudents());
        return dto;
    }

    private static AssignmentSubmissionDTO toSubmissionDto(AssignmentSubmissionRecord record) {
        AssignmentSubmissionDTO dto = new AssignmentSubmissionDTO();
        dto.setId(record.getId());
        dto.setAssignmentId(record.getAssignmentId());
        dto.setStudentId(record.getStudentId());
        dto.setContent(record.getContent());
        dto.setSubmissionDate(record.getSubmissionDate());
        dto.setGraded(record.getGraded());
        dto.setIsLate(record.getIsLate());
        dto.setScore(record.getScore());
        dto.setTeacherComment(record.getTeacherComment());
        return dto;
    }

    private static AssignmentStudentScoreDTO toStudentScoreDto(AssignmentRecord assignment,
                                                               AssignmentSubmissionRecord submission,
                                                               Map<Long, String> courseNames) {
        if (assignment == null || submission == null) {
            return null;
        }
        AssignmentStudentScoreDTO dto = new AssignmentStudentScoreDTO();
        dto.setType("assignment");
        dto.setRelatedId(assignment.getId());
        dto.setTitle(assignment.getTitle());
        String courseName = assignment.getCourseId() == null ? null : courseNames.get(assignment.getCourseId());
        dto.setCourseName(courseName);
        dto.setCompletedAt(submission.getSubmissionDate());
        dto.setSubmitDate(submission.getSubmissionDate());
        dto.setScore(submission.getScore());
        dto.setTotalScore(assignment.getMaxScore() == null ? 100 : assignment.getMaxScore());
        dto.setRank(null);
        return dto;
    }
}
