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
    static final String GENERATED_QUESTIONS = "GENERATED_QUESTIONS";
    static final String FIRST_AVAILABLE_CLASS_FOR_TEST = "FIRST_AVAILABLE_CLASS_FOR_TEST";

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
        applyGeneratedQuestionDraftContext(slots, context);
        applySelectedQuestionContext(slots, context);
        applyRandomContext(slots, message);
        applyTestRandomTargetContext(slots, message);
        applyDefaultMaxScore(slots);
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

    private void applyGeneratedQuestionDraftContext(Map<String, Object> slots, Map<String, Object> context) {
        Object draftValue = context.get("questionDraft");
        if (!(draftValue instanceof Map<?, ?> draft)) {
            return;
        }
        Object questions = draft.get("questions");
        if (!(questions instanceof Iterable<?> iterable) || !iterable.iterator().hasNext()) {
            return;
        }
        slots.putIfAbsent("selectionMode", GENERATED_QUESTIONS);
        slots.putIfAbsent("questions", questions);
        putLongIfMissing(slots, "courseId", draft.get("courseId"));
        putLongIfMissing(slots, "classId", draft.get("classId"));
        putStringIfMissing(slots, "title", draft.get("title"));
        String studentContent = formatStudentQuestionContent(iterable);
        if (hasValue(studentContent)) {
            slots.put("content", studentContent);
        } else {
            putStringIfMissing(slots, "content", draft.get("content"));
        }
        if (!hasValue(slots.get("content"))) {
            putStringIfMissing(slots, "content", draft.get("topic"));
        }
    }

    private String formatStudentQuestionContent(Iterable<?> questions) {
        List<String> blocks = new ArrayList<>();
        int index = 1;
        for (Object item : questions) {
            if (!(item instanceof Map<?, ?> question)) {
                continue;
            }
            Object content = firstValue(question, "content", "questionText", "title");
            if (!hasValue(content)) {
                content = "暂无题目内容";
            }
            StringBuilder block = new StringBuilder();
            block.append(index).append(". ").append(content);
            List<String> options = stringList(question.get("options"));
            for (int i = 0; i < options.size(); i++) {
                block.append('\n')
                        .append((char) ('A' + i))
                        .append(". ")
                        .append(options.get(i));
            }
            blocks.add(block.toString());
            index++;
        }
        if (blocks.isEmpty()) {
            return "";
        }
        return "题目如下：\n" + String.join("\n\n", blocks);
    }

    private Object firstValue(Map<?, ?> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (hasValue(value)) {
                return value;
            }
        }
        return null;
    }

    private List<String> stringList(Object value) {
        if (!(value instanceof Iterable<?> iterable)) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (Object item : iterable) {
            if (hasValue(item)) {
                values.add(String.valueOf(item));
            }
        }
        return values;
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

    private void applyTestRandomTargetContext(Map<String, Object> slots, String message) {
        String text = message == null ? "" : message.replaceAll("\\s+", "");
        if (!containsAny(text, "随机", "随便")) {
            return;
        }
        if (!containsAny(text, "班级", "课程")) {
            return;
        }
        if (!containsAny(text, "发布", "发到", "布置")) {
            return;
        }
        if (!containsAny(text, "测试", "试一下", "验证")) {
            return;
        }
        removeGenericPublishTarget(slots);
        if (hasValue(slots.get("courseId"))
                || hasValue(slots.get("courseName"))
                || hasValue(slots.get("classId"))
                || hasValue(slots.get("className"))) {
            return;
        }
        slots.putIfAbsent("targetSelectionMode", FIRST_AVAILABLE_CLASS_FOR_TEST);
    }

    private void removeGenericPublishTarget(Map<String, Object> slots) {
        if (isGenericPublishTarget(slots.get("className"))) {
            slots.remove("className");
        }
        if (isGenericPublishTarget(slots.get("courseName"))) {
            slots.remove("courseName");
        }
    }

    private boolean isGenericPublishTarget(Object value) {
        if (!hasValue(value)) {
            return false;
        }
        String text = String.valueOf(value).replaceAll("\\s+", "");
        return "发布".equals(text)
                || "发布作业".equals(text)
                || "发".equals(text)
                || "布置".equals(text);
    }

    private void applyDefaultMaxScore(Map<String, Object> slots) {
        if (!GENERATED_QUESTIONS.equals(String.valueOf(slots.get("selectionMode")))) {
            return;
        }
        slots.putIfAbsent("maxScore", 100);
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

    private void putStringIfMissing(Map<String, Object> slots, String key, Object value) {
        if (hasValue(slots.get(key)) || !hasValue(value)) {
            return;
        }
        slots.put(key, String.valueOf(value));
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

    private boolean containsAny(String text, String... terms) {
        for (String term : terms) {
            if (text.contains(term)) {
                return true;
            }
        }
        return false;
    }
}
