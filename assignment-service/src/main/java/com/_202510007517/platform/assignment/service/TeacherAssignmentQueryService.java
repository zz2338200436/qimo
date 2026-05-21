package com._202510007517.platform.assignment.service;

import com._202510007517.platform.assignment.api.dto.AssignmentDTO;
import com._202510007517.platform.assignment.api.dto.AssignmentSubmissionDTO;
import com._202510007517.platform.assignment.domain.AssignmentRecord;
import com._202510007517.platform.assignment.domain.AssignmentSubmissionRecord;
import com._202510007517.platform.assignment.repository.AssignmentRepository;
import com._202510007517.platform.common.exception.ResourceNotFoundException;
import com._202510007517.platform.course.api.dto.CourseDTO;
import com._202510007517.platform.course.api.feign.CourseFeignClient;
import com._202510007517.platform.user.api.dto.UserProfileDTO;
import com._202510007517.platform.user.api.feign.UserFeignClient;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class TeacherAssignmentQueryService {

    private static final DateTimeFormatter LEGACY_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter HTML_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private final AssignmentRepository assignmentRepository;
    private final CourseFeignClient courseFeignClient;
    private final UserFeignClient userFeignClient;

    public TeacherAssignmentQueryService(AssignmentRepository assignmentRepository,
                                         CourseFeignClient courseFeignClient,
                                         UserFeignClient userFeignClient) {
        this.assignmentRepository = assignmentRepository;
        this.courseFeignClient = courseFeignClient;
        this.userFeignClient = userFeignClient;
    }

    public Map<String, Object> listAssignments(Long teacherId,
                                               Integer page,
                                               Integer size,
                                               String keyword,
                                               Long courseId,
                                               Boolean isActive,
                                               String status) {
        int safePage = clampPage(page, 1);
        int safeSize = clampSize(size, 10);

        List<AssignmentRecord> records = assignmentRepository.findByTeacherId(teacherId);
        Map<Long, String> courseNames = loadCourseNames(extractCourseIds(records));
        Map<Long, String> teacherNames = loadTeacherNames(Set.of(teacherId));

        List<AssignmentDTO> filtered = records.stream()
                .map(record -> toTeacherAssignment(record, courseNames, teacherNames))
                .filter(dto -> matchesKeyword(dto, keyword))
                .filter(dto -> courseId == null || courseId.equals(dto.getCourseId()))
                .filter(dto -> isActive == null || isActive.equals(dto.getIsActive()))
                .filter(dto -> status == null || status.isBlank() || status.equalsIgnoreCase(dto.getStatus()))
                .toList();

        int totalElements = filtered.size();
        int totalPages = totalElements == 0 ? 1 : (int) Math.ceil((double) totalElements / safeSize);
        int startIndex = Math.min((safePage - 1) * safeSize, totalElements);
        int endIndex = Math.min(startIndex + safeSize, totalElements);
        List<AssignmentDTO> content = filtered.subList(startIndex, endIndex);

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

    public AssignmentDTO getAssignmentDetail(Long teacherId, Long assignmentId) {
        AssignmentRecord assignment = requireTeacherAssignment(teacherId, assignmentId);
        Map<Long, String> courseNames = loadCourseNames(Set.of(assignment.getCourseId()));
        Map<Long, String> teacherNames = loadTeacherNames(Set.of(assignment.getTeacherId()));

        AssignmentDTO dto = toTeacherAssignment(assignment, courseNames, teacherNames);
        dto.setSubmissions(listAssignmentSubmissions(teacherId, assignmentId));
        return dto;
    }

    public List<AssignmentSubmissionDTO> listAssignmentSubmissions(Long teacherId, Long assignmentId) {
        AssignmentRecord assignment = requireTeacherAssignment(teacherId, assignmentId);
        List<AssignmentSubmissionRecord> submissions = assignmentRepository.findSubmissionsByAssignmentId(assignmentId);
        Map<Long, String> studentNames = loadUserNames(extractStudentIds(submissions));

        return submissions.stream()
                .map(record -> toSubmissionDto(record, assignment.getTitle(), studentNames))
                .toList();
    }

    public AssignmentSubmissionDTO getAssignmentSubmission(Long teacherId, Long submissionId) {
        AssignmentSubmissionRecord submission = assignmentRepository.findSubmissionById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("作业提交记录不存在"));
        AssignmentRecord assignment = requireTeacherAssignment(teacherId, submission.getAssignmentId());
        Map<Long, String> studentNames = loadUserNames(Set.of(submission.getStudentId()));
        return toSubmissionDto(submission, assignment.getTitle(), studentNames);
    }

    public Map<String, Object> listSubmissions(Long teacherId,
                                               Integer page,
                                               Integer size,
                                               String sortBy,
                                               String order,
                                               Long assignmentId,
                                               Long studentId,
                                               Boolean graded) {
        int safePage = clampPage(page, 1);
        int safeSize = clampSize(size, 10);

        List<AssignmentRecord> assignments = assignmentRepository.findByTeacherId(teacherId);
        Map<Long, AssignmentRecord> assignmentMap = new LinkedHashMap<>();
        List<AssignmentSubmissionRecord> records = new ArrayList<>();
        for (AssignmentRecord assignment : assignments) {
            assignmentMap.put(assignment.getId(), assignment);
            records.addAll(assignmentRepository.findSubmissionsByAssignmentId(assignment.getId()));
        }
        Map<Long, String> studentNames = loadUserNames(extractStudentIds(records));

        List<AssignmentSubmissionDTO> filtered = records.stream()
                .filter(record -> assignmentId == null || assignmentId.equals(record.getAssignmentId()))
                .filter(record -> studentId == null || studentId.equals(record.getStudentId()))
                .filter(record -> graded == null || graded.equals(Boolean.TRUE.equals(record.getGraded())))
                .sorted((left, right) -> compareSubmissions(left, right, sortBy, order))
                .map(record -> {
                    AssignmentRecord assignment = assignmentMap.get(record.getAssignmentId());
                    return toSubmissionDto(record, assignment != null ? assignment.getTitle() : null, studentNames);
                })
                .toList();

        int totalElements = filtered.size();
        int totalPages = totalElements == 0 ? 1 : (int) Math.ceil((double) totalElements / safeSize);
        int startIndex = Math.min((safePage - 1) * safeSize, totalElements);
        int endIndex = Math.min(startIndex + safeSize, totalElements);
        List<AssignmentSubmissionDTO> content = filtered.subList(startIndex, endIndex);

        Map<String, Object> pageResult = new LinkedHashMap<>();
        pageResult.put("content", content);
        pageResult.put("totalPages", totalPages);
        pageResult.put("totalElements", totalElements);
        pageResult.put("size", safeSize);
        pageResult.put("number", safePage - 1);
        pageResult.put("pageNumber", safePage);
        pageResult.put("pageSize", safeSize);
        pageResult.put("first", safePage == 1);
        pageResult.put("last", safePage >= totalPages);
        pageResult.put("numberOfElements", content.size());
        pageResult.put("empty", content.isEmpty());
        return pageResult;
    }

    private AssignmentRecord requireTeacherAssignment(Long teacherId, Long assignmentId) {
        AssignmentRecord assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("作业不存在"));
        if (!teacherId.equals(assignment.getTeacherId())) {
            throw new ResourceNotFoundException("作业不存在");
        }
        return assignment;
    }

    private static AssignmentDTO toTeacherAssignment(AssignmentRecord record,
                                                     Map<Long, String> courseNames,
                                                     Map<Long, String> teacherNames) {
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
        dto.setStatus(resolveStatus(record));
        dto.setTotalStudents(defaultNumber(record.getTotalStudents()));
        dto.setCourseName(courseNames.get(record.getCourseId()));
        dto.setTeacherName(teacherNames.get(record.getTeacherId()));
        return dto;
    }

    private static AssignmentSubmissionDTO toSubmissionDto(AssignmentSubmissionRecord record,
                                                           String assignmentTitle,
                                                           Map<Long, String> studentNames) {
        AssignmentSubmissionDTO dto = new AssignmentSubmissionDTO();
        dto.setId(record.getId());
        dto.setAssignmentId(record.getAssignmentId());
        dto.setStudentId(record.getStudentId());
        dto.setStudentName(studentNames.getOrDefault(record.getStudentId(), fallbackStudentName(record.getStudentId())));
        dto.setTitle(assignmentTitle);
        dto.setContent(record.getContent());
        dto.setSubmissionDate(record.getSubmissionDate());
        dto.setGraded(Boolean.TRUE.equals(record.getGraded()));
        dto.setIsLate(Boolean.TRUE.equals(record.getIsLate()));
        dto.setLatePenalty(record.getLatePenalty());
        dto.setScore(record.getScore());
        dto.setTeacherComment(record.getTeacherComment());
        dto.setStatus(resolveSubmissionStatus(record));
        return dto;
    }

    private Map<Long, String> loadCourseNames(Set<Long> courseIds) {
        Map<Long, String> courseNames = new LinkedHashMap<>();
        for (Long courseId : courseIds) {
            if (courseId == null) {
                continue;
            }
            try {
                CourseDTO course = courseFeignClient.getCourse(courseId);
                if (course != null) {
                    courseNames.put(courseId, course.getCourseName());
                }
            } catch (RuntimeException ignored) {
                courseNames.putIfAbsent(courseId, null);
            }
        }
        return courseNames;
    }

    private Map<Long, String> loadTeacherNames(Set<Long> teacherIds) {
        return loadUserNames(teacherIds);
    }

    private Map<Long, String> loadUserNames(Set<Long> userIds) {
        Map<Long, String> userNames = new LinkedHashMap<>();
        if (userIds.isEmpty()) {
            return userNames;
        }
        try {
            List<UserProfileDTO> profiles = userFeignClient.listByIds(new ArrayList<>(userIds));
            if (profiles != null) {
                for (UserProfileDTO profile : profiles) {
                    if (profile != null && profile.getId() != null) {
                        userNames.put(profile.getId(), profile.getName());
                    }
                }
            }
        } catch (RuntimeException ignored) {
            // Keep the API functional even when enrichment services are unavailable.
        }
        return userNames;
    }

    private static Set<Long> extractCourseIds(List<AssignmentRecord> records) {
        Set<Long> courseIds = new LinkedHashSet<>();
        for (AssignmentRecord record : records) {
            if (record.getCourseId() != null) {
                courseIds.add(record.getCourseId());
            }
        }
        return courseIds;
    }

    private static Set<Long> extractStudentIds(List<AssignmentSubmissionRecord> submissions) {
        Set<Long> studentIds = new LinkedHashSet<>();
        for (AssignmentSubmissionRecord submission : submissions) {
            if (submission.getStudentId() != null) {
                studentIds.add(submission.getStudentId());
            }
        }
        return studentIds;
    }

    private static boolean matchesKeyword(AssignmentDTO dto, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String title = dto.getTitle() == null ? "" : dto.getTitle().toLowerCase(Locale.ROOT);
        return title.contains(keyword.toLowerCase(Locale.ROOT));
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

    private static int compareSubmissions(AssignmentSubmissionRecord left,
                                          AssignmentSubmissionRecord right,
                                          String sortBy,
                                          String order) {
        String field = sortBy == null || sortBy.isBlank() ? "id" : sortBy;
        int comparison;
        switch (field.toLowerCase(Locale.ROOT)) {
            case "submissiondate":
                comparison = compareDateTimeStrings(left.getSubmissionDate(), right.getSubmissionDate());
                break;
            case "studentid":
                comparison = compareLongs(left.getStudentId(), right.getStudentId());
                break;
            case "score":
                comparison = compareIntegers(left.getScore(), right.getScore());
                break;
            case "id":
            default:
                comparison = compareLongs(left.getId(), right.getId());
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

    private static int compareIntegers(Integer left, Integer right) {
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

    private static Integer defaultNumber(Integer value) {
        return value == null ? 0 : value;
    }

    private static String resolveStatus(AssignmentRecord record) {
        LocalDateTime dueDate = parseDateTime(record.getDueDate());
        boolean closed = Boolean.FALSE.equals(record.getIsActive())
                || (dueDate != null && dueDate.isBefore(LocalDateTime.now()));
        if (closed) {
            return "closed";
        }
        int submittedCount = defaultNumber(record.getSubmissionCount());
        int gradedCount = defaultNumber(record.getGradedCount());
        if (submittedCount > 0) {
            return gradedCount > 0 ? "graded" : "submitted";
        }
        return "pending";
    }

    private static String resolveSubmissionStatus(AssignmentSubmissionRecord record) {
        if (Boolean.TRUE.equals(record.getGraded())) {
            return "graded";
        }
        if (Boolean.TRUE.equals(record.getIsLate())) {
            return "late";
        }
        return "submitted";
    }

    private static String fallbackStudentName(Long studentId) {
        return studentId == null ? "学生" : "学生" + studentId;
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
}
