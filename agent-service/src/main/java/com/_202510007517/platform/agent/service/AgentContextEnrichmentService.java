package com._202510007517.platform.agent.service;

import com._202510007517.platform.agent.model.AgentIntent;
import com._202510007517.platform.agent.model.RecognizedIntent;
import com._202510007517.platform.assignment.api.dto.AssignmentStudentScoreDTO;
import com._202510007517.platform.assignment.api.feign.AssignmentFeignClient;
import com._202510007517.platform.course.api.dto.CourseAssignmentDTO;
import com._202510007517.platform.course.api.dto.CourseDTO;
import com._202510007517.platform.course.api.feign.CourseFeignClient;
import com._202510007517.platform.exam.api.dto.ExamDTO;
import com._202510007517.platform.exam.api.feign.ExamFeignClient;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class AgentContextEnrichmentService {

    private final CourseFeignClient courseClient;
    private final AssignmentFeignClient assignmentClient;
    private final ExamFeignClient examClient;

    public AgentContextEnrichmentService(CourseFeignClient courseClient,
                                         AssignmentFeignClient assignmentClient,
                                         ExamFeignClient examClient) {
        this.courseClient = courseClient;
        this.assignmentClient = assignmentClient;
        this.examClient = examClient;
    }

    public RecognizedIntent enrich(Long userId, String userRole, RecognizedIntent recognizedIntent) {
        recognizedIntent = enrichAssignmentContext(userId, userRole, recognizedIntent);
        recognizedIntent = enrichExamContext(userId, userRole, recognizedIntent);
        return enrichCourseContext(userId, userRole, recognizedIntent);
    }

    private RecognizedIntent enrichCourseContext(Long userId, String userRole, RecognizedIntent recognizedIntent) {
        if (!needsCourseResolution(recognizedIntent.intent()) || !"TEACHER".equalsIgnoreCase(userRole)) {
            return recognizedIntent;
        }
        Map<String, Object> slots = recognizedIntent.slots();
        if (hasValue(slots.get("courseId"))) {
            return recognizedIntent;
        }

        if (recognizedIntent.intent() == AgentIntent.QUERY_COURSE_DETAIL && !hasValue(slots.get("courseName"))) {
            List<CourseDTO> teacherCourses = safeList(courseClient.listTeacherCourses(userId, null, null, null, null));
            if (teacherCourses.size() == 1) {
                return withResolvedCourse(recognizedIntent, teacherCourses.get(0));
            }
            return recognizedIntent;
        }

        if (!hasValue(slots.get("courseName"))) {
            return recognizedIntent;
        }

        String courseName = String.valueOf(slots.get("courseName"));
        String semester = asStringOrNull(slots.get("semester"));
        String className = asStringOrNull(slots.get("className"));
        List<CourseDTO> matches = findCourseMatches(userId, courseName, semester, className);
        if (matches.size() == 1) {
            return withResolvedCourse(recognizedIntent, matches.get(0));
        }
        if (matches.isEmpty()) {
            return withMissingSlot(recognizedIntent, "可识别课程");
        }
        return withMissingSlot(recognizedIntent, "更明确的课程");
    }

    private RecognizedIntent enrichAssignmentContext(Long userId, String userRole, RecognizedIntent recognizedIntent) {
        if (recognizedIntent.intent() != AgentIntent.SUBMIT_ASSIGNMENT || !"STUDENT".equalsIgnoreCase(userRole)) {
            return recognizedIntent;
        }
        Map<String, Object> slots = recognizedIntent.slots();
        if (hasValue(slots.get("assignmentId")) || !hasValue(slots.get("assignmentTitle"))) {
            return recognizedIntent;
        }

        String assignmentTitle = String.valueOf(slots.get("assignmentTitle"));
        List<AssignmentMatch> matches = findAssignmentMatches(userId, assignmentTitle);
        if (matches.size() == 1) {
            Map<String, Object> enrichedSlots = new LinkedHashMap<>(slots);
            AssignmentMatch assignment = matches.get(0);
            enrichedSlots.put("assignmentId", assignment.id());
            if (hasValue(assignment.title())) {
                enrichedSlots.put("resolvedAssignmentTitle", assignment.title());
            }
            return new RecognizedIntent(recognizedIntent.intent(), recognizedIntent.confidence(),
                    enrichedSlots, recognizedIntent.missingSlots());
        }
        if (matches.isEmpty()) {
            return withMissingSlot(recognizedIntent, "可识别作业");
        }
        return withMissingSlot(recognizedIntent, "更明确的作业");
    }

    private RecognizedIntent enrichExamContext(Long userId, String userRole, RecognizedIntent recognizedIntent) {
        if (recognizedIntent.intent() != AgentIntent.SUBMIT_EXAM || !"STUDENT".equalsIgnoreCase(userRole)) {
            return recognizedIntent;
        }
        Map<String, Object> slots = recognizedIntent.slots();
        if (hasValue(slots.get("examId")) || !hasValue(slots.get("examTitle"))) {
            return recognizedIntent;
        }

        String examTitle = String.valueOf(slots.get("examTitle"));
        List<ExamDTO> matches = findExamMatches(userId, examTitle);
        if (matches.size() == 1) {
            Map<String, Object> enrichedSlots = new LinkedHashMap<>(slots);
            ExamDTO exam = matches.get(0);
            enrichedSlots.put("examId", exam.getId());
            if (hasValue(exam.getTitle())) {
                enrichedSlots.put("resolvedExamTitle", exam.getTitle());
            }
            return new RecognizedIntent(recognizedIntent.intent(), recognizedIntent.confidence(),
                    enrichedSlots, recognizedIntent.missingSlots());
        }
        if (matches.isEmpty()) {
            return withMissingSlot(recognizedIntent, "可识别考试");
        }
        return withMissingSlot(recognizedIntent, "更明确的考试");
    }

    private List<CourseDTO> findCourseMatches(Long teacherId, String courseName, String semester, String className) {
        String needle = normalize(courseName);
        List<CourseDTO> matches = safeList(courseClient.listTeacherCourses(teacherId, null, null, null, null)).stream()
                .filter(course -> matches(needle, course.getCourseName()) || matches(needle, course.getCourseCode()))
                .toList();
        if (matches.size() <= 1) {
            return matches;
        }

        List<CourseDTO> assignmentFiltered = filterByAssignments(teacherId, matches, semester, className);
        if (!assignmentFiltered.isEmpty()) {
            return assignmentFiltered;
        }

        if (hasValue(semester)) {
            List<CourseDTO> semesterFiltered = matches.stream()
                    .filter(course -> matches(normalize(semester), course.getSemester()))
                    .toList();
            if (!semesterFiltered.isEmpty()) {
                return semesterFiltered;
            }
        }
        return matches;
    }

    private List<AssignmentMatch> findAssignmentMatches(Long studentId, String assignmentTitle) {
        String needle = normalize(assignmentTitle);
        List<AssignmentMatch> pendingMatches = pendingAssignments(studentId).stream()
                .filter(assignment -> matches(needle, assignment.title()))
                .toList();
        if (!pendingMatches.isEmpty()) {
            return pendingMatches;
        }
        return assignmentClient.listStudentScores(studentId).stream()
                .map(assignment -> new AssignmentMatch(assignment.getRelatedId(), assignment.getTitle()))
                .filter(assignment -> matches(needle, assignment.title()))
                .toList();
    }

    private List<ExamDTO> findExamMatches(Long studentId, String examTitle) {
        String needle = normalize(examTitle);
        return examClient.listByStudent(studentId).stream()
                .filter(exam -> matches(needle, exam.getTitle()))
                .toList();
    }

    private List<AssignmentMatch> pendingAssignments(Long studentId) {
        Map<String, Object> page = assignmentClient.listStudentAssignments(
                studentId, 1, 100, "dueDate", "DESC", null, false, true);
        if (page == null || !(page.get("content") instanceof Iterable<?> assignments)) {
            return List.of();
        }
        List<AssignmentMatch> matches = new ArrayList<>();
        for (Object assignment : assignments) {
            if (assignment instanceof Map<?, ?> assignmentMap) {
                Long id = asLongOrNull(assignmentMap.get("id"));
                Object title = assignmentMap.get("title");
                if (id != null && hasValue(title)) {
                    matches.add(new AssignmentMatch(id, String.valueOf(title)));
                }
            }
        }
        return matches;
    }

    private boolean matches(String needle, String candidate) {
        if (!hasValue(candidate)) {
            return false;
        }
        String normalized = normalize(candidate);
        return normalized.equals(needle) || normalized.contains(needle) || needle.contains(normalized);
    }

    private List<CourseDTO> filterByAssignments(Long teacherId, List<CourseDTO> courseMatches,
                                                String semester, String className) {
        if (!hasValue(semester) && !hasValue(className)) {
            return List.of();
        }
        List<CourseAssignmentDTO> assignments = safeList(courseClient.listCourseAssignments(teacherId, null, null));
        if (assignments.isEmpty()) {
            return List.of();
        }
        Set<Long> candidateCourseIds = courseMatches.stream()
                .map(CourseDTO::getId)
                .filter(this::hasValue)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        List<CourseAssignmentDTO> filteredAssignments = assignments.stream()
                .filter(assignment -> candidateCourseIds.contains(assignment.getCourseId()))
                .filter(assignment -> !hasValue(semester) || matches(normalize(semester), assignment.getSemester()))
                .filter(assignment -> !hasValue(className) || matches(normalize(className), assignment.getClassName()))
                .toList();
        if (filteredAssignments.isEmpty()) {
            return List.of();
        }
        Set<Long> matchedCourseIds = filteredAssignments.stream()
                .map(CourseAssignmentDTO::getCourseId)
                .filter(this::hasValue)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        return courseMatches.stream()
                .filter(course -> matchedCourseIds.contains(course.getId()))
                .toList();
    }

    private RecognizedIntent withMissingSlot(RecognizedIntent recognizedIntent, String missingSlot) {
        List<String> missingSlots = new ArrayList<>(recognizedIntent.missingSlots());
        if (!missingSlots.contains(missingSlot)) {
            missingSlots.add(missingSlot);
        }
        return new RecognizedIntent(recognizedIntent.intent(), recognizedIntent.confidence(),
                recognizedIntent.slots(), missingSlots);
    }

    private RecognizedIntent withResolvedCourse(RecognizedIntent recognizedIntent, CourseDTO course) {
        Map<String, Object> enrichedSlots = new LinkedHashMap<>(recognizedIntent.slots());
        enrichedSlots.put("courseId", course.getId());
        if (hasValue(course.getCourseName())) {
            enrichedSlots.put("resolvedCourseName", course.getCourseName());
        }
        return new RecognizedIntent(recognizedIntent.intent(), recognizedIntent.confidence(),
                enrichedSlots, recognizedIntent.missingSlots());
    }

    private boolean needsCourseResolution(AgentIntent intent) {
        return switch (intent) {
            case PUBLISH_ASSIGNMENT, UPDATE_ASSIGNMENT, PUBLISH_EXAM, UPDATE_EXAM, QUERY_COURSE_DETAIL,
                    QUERY_KNOWLEDGE_POINTS -> true;
            default -> false;
        };
    }

    private boolean hasValue(Object value) {
        return value != null && !String.valueOf(value).isBlank();
    }

    private String normalize(String value) {
        return value == null ? "" : value.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }

    private Long asLongOrNull(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }

    private String asStringOrNull(Object value) {
        if (!hasValue(value)) {
            return null;
        }
        return String.valueOf(value);
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private record AssignmentMatch(Long id, String title) {
    }
}
