package com._202510007517.platform.agent.service;

import com._202510007517.platform.agent.model.AgentIntent;
import com._202510007517.platform.agent.model.RecognizedIntent;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class AgentAssignmentDraftService {
    static final String SELECTED_QUESTIONS = "SELECTED_QUESTIONS";
    static final String RANDOM_QUESTION_BANK = "RANDOM_QUESTION_BANK";

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Clock clock;

    public AgentAssignmentDraftService() {
        this(Clock.systemDefaultZone());
    }

    AgentAssignmentDraftService(Clock clock) {
        this.clock = clock;
    }

    public RecognizedIntent enrich(RecognizedIntent intent, String message, Map<String, Object> pageContext) {
        if (intent.intent() != AgentIntent.PUBLISH_ASSIGNMENT) {
            return intent;
        }
        Map<String, Object> context = pageContext == null ? Map.of() : pageContext;
        Map<String, Object> slots = new LinkedHashMap<>(intent.slots());

        applyCourseContext(slots, context);
        applySelectedQuestionContext(slots, context);
        applyRandomContext(slots, message);
        applyDefaultDueDate(slots);

        return new RecognizedIntent(intent.intent(), intent.confidence(), slots, intent.missingSlots());
    }

    private void applyCourseContext(Map<String, Object> slots, Map<String, Object> context) {
        putLongIfMissing(slots, "courseId", context.get("currentCourseId"));
        putLongIfMissing(slots, "classId", context.get("currentClassId"));
        Object questionFilter = context.get("questionFilter");
        if (questionFilter instanceof Map<?, ?> filter) {
            putLongIfMissing(slots, "courseId", filter.get("courseId"));
            putLongIfMissing(slots, "classId", filter.get("classId"));
        }
    }

    private void applySelectedQuestionContext(Map<String, Object> slots, Map<String, Object> context) {
        List<Long> questionIds = longList(context.get("selectedQuestionIds"));
        if (questionIds.isEmpty()) {
            Long singleId = asLong(context.get("selectedQuestionId"));
            if (singleId != null) {
                questionIds = List.of(singleId);
            }
        }
        if (questionIds.isEmpty()) {
            return;
        }
        slots.putIfAbsent("selectionMode", SELECTED_QUESTIONS);
        slots.putIfAbsent("questionIds", questionIds);
        Object content = context.get("selectedQuestionContent");
        if (content != null && !String.valueOf(content).isBlank()) {
            slots.putIfAbsent("content", String.valueOf(content));
            slots.putIfAbsent("title", titleFromQuestion(String.valueOf(content), context.get("selectedQuestionType")));
        }
        Integer score = asInteger(context.get("selectedQuestionScore"));
        if (score != null) {
            slots.putIfAbsent("maxScore", score);
        }
    }

    private void applyRandomContext(Map<String, Object> slots, String message) {
        String text = message == null ? "" : message;
        if (!text.contains("随机") || !text.contains("全部")) {
            return;
        }
        slots.putIfAbsent("selectionMode", RANDOM_QUESTION_BANK);
        slots.putIfAbsent("title", "随机题库练习");
        slots.putIfAbsent("maxScore", 100);
        slots.putIfAbsent("content", "随机题库练习");
    }

    private void applyDefaultDueDate(Map<String, Object> slots) {
        if (!hasValue(slots.get("selectionMode"))) {
            return;
        }
        if (hasValue(slots.get("dueDate"))) {
            return;
        }
        LocalDate dueDate = LocalDate.now(clock).plusDays(7);
        slots.put("dueDate", LocalDateTime.of(dueDate, LocalTime.of(23, 59, 59)).format(DATE_TIME));
    }

    private String titleFromQuestion(String content, Object type) {
        String compact = content.replaceAll("[，。！？；：,.!?;:\\s]+", "");
        String keyword = compact.length() <= 8 ? compact : compact.substring(0, 8);
        String suffix = "TRUE_FALSE".equals(String.valueOf(type)) ? "判断题练习" : "题目练习";
        return keyword + suffix;
    }

    private void putLongIfMissing(Map<String, Object> slots, String key, Object value) {
        if (hasValue(slots.get(key))) {
            return;
        }
        Long parsed = asLong(value);
        if (parsed != null) {
            slots.put(key, parsed);
        }
    }

    private List<Long> longList(Object value) {
        if (!(value instanceof Iterable<?> iterable)) {
            return List.of();
        }
        List<Long> ids = new ArrayList<>();
        for (Object item : iterable) {
            Long id = asLong(item);
            if (id != null) {
                ids.add(id);
            }
        }
        return ids;
    }

    private Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && text.matches("\\d+")) {
            return Long.valueOf(text);
        }
        return null;
    }

    private Integer asInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && text.matches("\\d+")) {
            return Integer.valueOf(text);
        }
        return null;
    }

    private boolean hasValue(Object value) {
        return value != null && !String.valueOf(value).isBlank();
    }
}
