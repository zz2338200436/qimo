package com._202510007517.platform.agent.service;

import com._202510007517.platform.agent.domain.AgentSessionEntity;
import com._202510007517.platform.assignment.api.dto.AssignmentDTO;
import com._202510007517.platform.course.api.dto.CourseDTO;
import com._202510007517.platform.exam.api.dto.ExamDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AgentArtifactService {
    public static final String CUSTOM_SESSION_TITLE_KEY = "custom_session_title";

    private final ObjectMapper objectMapper;

    public AgentArtifactService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Map<String, SessionArtifact> loadArtifacts(AgentSessionEntity session) {
        String json = session.getArtifactsJson();
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException ex) {
            return Map.of();
        }
    }

    public void saveArtifacts(AgentSessionEntity session, Map<String, SessionArtifact> artifacts) {
        session.setArtifactsJson(writeJson(artifacts));
    }

    public void saveToolArtifacts(AgentSessionEntity session, String toolName, Map<String, Object> toolResult) {
        if (toolName == null || toolResult == null || toolResult.isEmpty()) {
            return;
        }
        Map<String, SessionArtifact> artifacts = new LinkedHashMap<>(loadArtifacts(session));
        if ("generate_questions".equals(toolName)) {
            persistGeneratedQuestionsArtifact(artifacts, toolResult);
        }
        if ("publish_assignment".equals(toolName)) {
            persistLatestPublishedAssignmentArtifact(artifacts, toolResult);
        }
        if ("publish_exam".equals(toolName)) {
            persistLatestPublishedExamArtifact(artifacts, toolResult);
        }
        if ("create_course".equals(toolName)) {
            persistLatestCreatedCourseArtifact(artifacts, toolResult);
        }
        if ("create_class".equals(toolName)) {
            persistLatestCreatedClassArtifact(artifacts, toolResult);
        }
        saveArtifacts(session, artifacts);
    }

    public Map<String, Object> summarizeArtifacts(Map<String, SessionArtifact> artifacts) {
        if (artifacts == null || artifacts.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> summary = new LinkedHashMap<>();
        SessionArtifact customTitle = artifacts.get(CUSTOM_SESSION_TITLE_KEY);
        if (customTitle != null && customTitle.payload() != null) {
            Object title = customTitle.payload().get("title");
            if (title != null && !String.valueOf(title).isBlank()) {
                summary.put("customTitle", String.valueOf(title).trim());
            }
        }
        SessionArtifact latestGenerated = artifacts.get("latest_generated_questions");
        if (latestGenerated != null) {
            summary.put("latestGeneratedArtifactKey", latestGenerated.key());
            summary.put("latestGeneratedArtifactType", latestGenerated.type());
            Object payload = latestGenerated.payload();
            if (payload instanceof Map<?, ?> payloadMap) {
                summary.put("latestGeneratedTitle", payloadMap.get("title"));
                Object questions = payloadMap.get("questions");
                if (questions instanceof List<?> list) {
                    summary.put("latestGeneratedQuestionCount", list.size());
                }
            }
        }
        SessionArtifact latestPublishedAssignment = artifacts.get("latest_published_assignment");
        if (latestPublishedAssignment != null) {
            summary.put("latestPublishedAssignmentArtifactKey", latestPublishedAssignment.key());
            Object payload = latestPublishedAssignment.payload();
            if (payload instanceof Map<?, ?> payloadMap) {
                summary.put("latestPublishedAssignmentId", payloadMap.get("assignmentId"));
                summary.put("latestPublishedAssignmentTitle", payloadMap.get("title"));
            }
        }
        summarizeLatestEntity(summary, artifacts.get("latest_published_exam"),
                "latestPublishedExam", "examId", "title");
        summarizeLatestEntity(summary, artifacts.get("latest_created_course"),
                "latestCreatedCourse", "courseId", "courseName");
        summarizeLatestEntity(summary, artifacts.get("latest_created_class"),
                "latestCreatedClass", "classId", "className");
        return summary;
    }

    private void persistGeneratedQuestionsArtifact(Map<String, SessionArtifact> artifacts, Map<String, Object> toolResult) {
        Object questions = toolResult.get("questions");
        if (!(questions instanceof List<?> list) || list.isEmpty()) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("selectionMode", AgentAssignmentDraftService.GENERATED_QUESTIONS);
        copyIfPresent(payload, toolResult, "title");
        copyIfPresent(payload, toolResult, "topic");
        copyIfPresent(payload, toolResult, "content");
        payload.put("questions", questions);
        if (!payload.containsKey("content")) {
            payload.put("content", formatGeneratedQuestions(list));
        }
        if (!payload.containsKey("title")) {
            Object topic = toolResult.get("topic");
            if (topic != null && !String.valueOf(topic).isBlank()) {
                payload.put("title", topic + "课堂练习");
            } else {
                payload.put("title", "随机练习课堂练习");
            }
        }
        String now = LocalDateTime.now().toString();
        artifacts.put("latest_generated_questions", new SessionArtifact(
                "generated_questions",
                "latest_generated_questions",
                payload,
                now,
                now
        ));
    }

    private void persistLatestPublishedAssignmentArtifact(Map<String, SessionArtifact> artifacts,
                                                          Map<String, Object> toolResult) {
        Object assignment = toolResult.get("assignment");
        Map<String, Object> payload = new LinkedHashMap<>();
        if (assignment instanceof AssignmentDTO dto) {
            if (dto.getId() != null) {
                payload.put("assignmentId", dto.getId());
            }
            copyIfPresent(payload, Map.of(
                    "title", dto.getTitle(),
                    "courseId", dto.getCourseId()
            ), "title");
            if (dto.getCourseId() != null) {
                payload.put("courseId", dto.getCourseId());
            }
        } else if (assignment instanceof Map<?, ?> assignmentMap) {
            Object assignmentId = assignmentMap.get("id");
            if (assignmentId != null && !String.valueOf(assignmentId).isBlank()) {
                payload.put("assignmentId", assignmentId);
            }
            copyIfPresent(payload, castMap(assignmentMap), "title");
            Object courseId = assignmentMap.get("courseId");
            if (courseId != null && !String.valueOf(courseId).isBlank()) {
                payload.put("courseId", courseId);
            }
        }
        if (payload.isEmpty()) {
            return;
        }
        String now = LocalDateTime.now().toString();
        artifacts.put("latest_published_assignment", new SessionArtifact(
                "assignment",
                "latest_published_assignment",
                payload,
                now,
                now
        ));
    }

    private void persistLatestPublishedExamArtifact(Map<String, SessionArtifact> artifacts,
                                                    Map<String, Object> toolResult) {
        Object exam = toolResult.get("exam");
        Map<String, Object> payload = new LinkedHashMap<>();
        if (exam instanceof ExamDTO dto) {
            if (dto.getId() != null) {
                payload.put("examId", dto.getId());
            }
            if (dto.getCourseId() != null) {
                payload.put("courseId", dto.getCourseId());
            }
            copyNonBlank(payload, "title", dto.getTitle());
            copyNonBlank(payload, "courseName", dto.getCourseName());
        } else if (exam instanceof Map<?, ?> examMap) {
            copyIfPresent(payload, castMap(examMap), "examId");
            copyIfPresent(payload, castMap(examMap), "id", "examId");
            copyIfPresent(payload, castMap(examMap), "courseId");
            copyIfPresent(payload, castMap(examMap), "title");
            copyIfPresent(payload, castMap(examMap), "courseName");
        }
        persistEntityArtifact(artifacts, "latest_published_exam", "exam", payload);
    }

    private void persistLatestCreatedCourseArtifact(Map<String, SessionArtifact> artifacts,
                                                    Map<String, Object> toolResult) {
        Object course = toolResult.get("course");
        Map<String, Object> payload = new LinkedHashMap<>();
        if (course instanceof CourseDTO dto) {
            if (dto.getId() != null) {
                payload.put("courseId", dto.getId());
            }
            copyNonBlank(payload, "courseName", dto.getCourseName());
            copyNonBlank(payload, "courseCode", dto.getCourseCode());
        } else if (course instanceof Map<?, ?> courseMap) {
            copyIfPresent(payload, castMap(courseMap), "courseId");
            copyIfPresent(payload, castMap(courseMap), "id", "courseId");
            copyIfPresent(payload, castMap(courseMap), "courseName");
            copyIfPresent(payload, castMap(courseMap), "courseCode");
        }
        persistEntityArtifact(artifacts, "latest_created_course", "course", payload);
    }

    private void persistLatestCreatedClassArtifact(Map<String, SessionArtifact> artifacts,
                                                   Map<String, Object> toolResult) {
        Map<String, Object> payload = new LinkedHashMap<>();
        Object classId = toolResult.get("classId");
        if (classId != null && !String.valueOf(classId).isBlank()) {
            payload.put("classId", classId);
        }
        copyIfPresent(payload, toolResult, "className");
        persistEntityArtifact(artifacts, "latest_created_class", "class", payload);
    }

    private String formatGeneratedQuestions(List<?> questions) {
        StringBuilder builder = new StringBuilder("题目如下：");
        for (int index = 0; index < questions.size(); index++) {
            builder.append(System.lineSeparator()).append(index + 1).append(". ");
            Object question = questions.get(index);
            if (question instanceof Map<?, ?> questionMap) {
                Object content = questionMap.get("content");
                builder.append(content == null ? "" : String.valueOf(content));
            } else {
                builder.append(String.valueOf(question));
            }
        }
        return builder.toString();
    }

    private void copyIfPresent(Map<String, Object> target, Map<String, Object> source, String key) {
        Object value = source.get(key);
        if (value != null && !String.valueOf(value).isBlank()) {
            target.put(key, value);
        }
    }

    private void copyIfPresent(Map<String, Object> target, Map<String, Object> source, String sourceKey, String targetKey) {
        Object value = source.get(sourceKey);
        if (value != null && !String.valueOf(value).isBlank()) {
            target.put(targetKey, value);
        }
    }

    private void copyNonBlank(Map<String, Object> target, String key, Object value) {
        if (value != null && !String.valueOf(value).isBlank()) {
            target.put(key, value);
        }
    }

    private void persistEntityArtifact(Map<String, SessionArtifact> artifacts, String artifactKey, String type,
                                       Map<String, Object> payload) {
        if (payload.isEmpty()) {
            return;
        }
        String now = LocalDateTime.now().toString();
        artifacts.put(artifactKey, new SessionArtifact(type, artifactKey, payload, now, now));
    }

    private void summarizeLatestEntity(Map<String, Object> summary,
                                       SessionArtifact artifact,
                                       String summaryPrefix,
                                       String idKey,
                                       String titleKey) {
        if (artifact == null) {
            return;
        }
        summary.put(summaryPrefix + "ArtifactKey", artifact.key());
        Object payload = artifact.payload();
        if (payload instanceof Map<?, ?> payloadMap) {
            summary.put(summaryPrefix + "Id", payloadMap.get(idKey));
            summary.put(summaryPrefix + "Title", payloadMap.get(titleKey));
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Map<?, ?> source) {
        return (Map<String, Object>) source;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize agent artifact payload.", ex);
        }
    }
}
