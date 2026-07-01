package com._202510007517.platform.agent.service;

import com._202510007517.platform.agent.model.AgentIntent;
import com._202510007517.platform.agent.model.RecognizedIntent;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class AgentSlotRequirementService {

    public List<String> missingSlots(RecognizedIntent recognizedIntent, String message) {
        Set<String> missing = new LinkedHashSet<>(recognizedIntent.missingSlots());
        Map<String, Object> slots = recognizedIntent.slots();
        AgentIntent intent = recognizedIntent.intent();
        removeSatisfiedMissingSlots(missing, slots);

        switch (intent) {
            case PUBLISH_ASSIGNMENT -> {
                requirePublishTarget(missing, slots);
                require(missing, slots, "标题", "title");
                requireDueDate(missing, slots, message);
                require(missing, slots, "满分", "maxScore");
                requireNumberIfPresent(missing, slots, "课程ID", "courseId");
                requireNumberIfPresent(missing, slots, "班级ID", "classId");
                requireNumberIfPresent(missing, slots, "满分", "maxScore");
            }
            case UPDATE_ASSIGNMENT -> {
                require(missing, slots, "作业ID", "assignmentId");
                requireAny(missing, slots, "课程", "courseId", "courseName");
                require(missing, slots, "标题", "title");
                requireDueDate(missing, slots, message);
                require(missing, slots, "满分", "maxScore");
                requireNumberIfPresent(missing, slots, "作业ID", "assignmentId");
                requireNumberIfPresent(missing, slots, "课程ID", "courseId");
                requireNumberIfPresent(missing, slots, "满分", "maxScore");
            }
            case DELETE_ASSIGNMENT, QUERY_ASSIGNMENT_DETAIL, QUERY_ASSIGNMENT_SUBMISSIONS -> {
                require(missing, slots, "作业ID", "assignmentId");
                requireNumberIfPresent(missing, slots, "作业ID", "assignmentId");
            }
            case SUBMIT_ASSIGNMENT -> {
                requireAny(missing, slots, "作业", "assignmentId", "assignmentTitle");
                require(missing, slots, "提交内容", "content");
                requireNumberIfPresent(missing, slots, "作业ID", "assignmentId");
            }
            case GRADE_ASSIGNMENT, GRADE_EXAM -> {
                require(missing, slots, "提交记录ID", "submissionId");
                requireAny(missing, slots, "分数", "score", "maxScore");
                requireNumberIfPresent(missing, slots, "提交记录ID", "submissionId");
                requireNumberIfPresent(missing, slots, "分数", "score");
                requireNumberIfPresent(missing, slots, "满分", "maxScore");
            }
            case PUBLISH_EXAM -> {
                requireAny(missing, slots, "课程", "courseId", "courseName");
                require(missing, slots, "标题", "title");
                require(missing, slots, "开始时间", "startTime");
                require(missing, slots, "结束时间", "endTime");
                require(missing, slots, "考试时长", "duration");
                requireNumberIfPresent(missing, slots, "课程ID", "courseId");
                requireNumberIfPresent(missing, slots, "考试时长", "duration");
            }
            case CREATE_COURSE -> {
                require(missing, slots, "课程名称", "courseName");
                require(missing, slots, "课程代码", "courseCode");
                require(missing, slots, "学分", "credit");
                require(missing, slots, "总学时", "totalHours");
                requireNumberIfPresent(missing, slots, "学分", "credit");
                requireNumberIfPresent(missing, slots, "总学时", "totalHours");
            }
            case UPDATE_COURSE -> {
                require(missing, slots, "课程ID", "courseId");
                require(missing, slots, "课程名称", "courseName");
                require(missing, slots, "课程代码", "courseCode");
                require(missing, slots, "学分", "credit");
                require(missing, slots, "总学时", "totalHours");
                requireNumberIfPresent(missing, slots, "课程ID", "courseId");
                requireNumberIfPresent(missing, slots, "学分", "credit");
                requireNumberIfPresent(missing, slots, "总学时", "totalHours");
            }
            case DELETE_COURSE -> {
                require(missing, slots, "课程ID", "courseId");
                requireNumberIfPresent(missing, slots, "课程ID", "courseId");
            }
            case CREATE_CLASS -> {
                require(missing, slots, "班级名称", "className");
                require(missing, slots, "年级", "year");
                require(missing, slots, "容量", "capacity");
                requireNumberIfPresent(missing, slots, "容量", "capacity");
                requireNumberIfPresent(missing, slots, "课程ID", "courseId");
                requireNumberIfPresent(missing, slots, "专业ID", "majorId");
            }
            case UPDATE_EXAM -> {
                require(missing, slots, "考试ID", "examId");
                requireAny(missing, slots, "课程", "courseId", "courseName");
                require(missing, slots, "标题", "title");
                require(missing, slots, "开始时间", "startTime");
                require(missing, slots, "结束时间", "endTime");
                require(missing, slots, "考试时长", "duration");
                requireNumberIfPresent(missing, slots, "考试ID", "examId");
                requireNumberIfPresent(missing, slots, "课程ID", "courseId");
                requireNumberIfPresent(missing, slots, "考试时长", "duration");
            }
            case DELETE_EXAM, QUERY_EXAM_DETAIL, QUERY_EXAM_SUBMISSIONS -> {
                require(missing, slots, "考试ID", "examId");
                requireNumberIfPresent(missing, slots, "考试ID", "examId");
            }
            case SUBMIT_EXAM -> {
                requireAny(missing, slots, "考试", "examId", "examTitle");
                require(missing, slots, "答题内容", "answers");
                requireNumberIfPresent(missing, slots, "考试ID", "examId");
                requireNumberIfPresent(missing, slots, "考试用时", "timeTaken");
                requireMapIfPresent(missing, slots, "答题内容", "answers");
            }
            case QUERY_COURSE_DETAIL -> {
                require(missing, slots, "课程ID", "courseId");
                requireNumberIfPresent(missing, slots, "课程ID", "courseId");
            }
            case QUERY_CLASS_DETAIL -> {
                require(missing, slots, "班级ID", "classId");
                requireNumberIfPresent(missing, slots, "班级ID", "classId");
            }
            case QUERY_KNOWLEDGE_MASTERY -> {
                if (hasAny(slots, "studentId")) {
                    require(missing, slots, "课程ID", "courseId");
                }
                requireNumberIfPresent(missing, slots, "学生ID", "studentId");
                requireNumberIfPresent(missing, slots, "课程ID", "courseId");
            }
            case QUERY_KNOWLEDGE_POINTS -> {
                requireAny(missing, slots, "课程", "courseId", "courseName");
                requireNumberIfPresent(missing, slots, "课程ID", "courseId");
            }
            case INTERNET_SEARCH -> require(missing, slots, "搜索关键词", "query");
            case READ_WEB_PAGE -> require(missing, slots, "网页地址", "url");
            case MARK_NOTIFICATION_READ, DELETE_NOTIFICATION -> {
                require(missing, slots, "notificationId", "notificationId");
                requireNumberIfPresent(missing, slots, "notificationId", "notificationId");
            }
            case SEND_NOTIFICATION -> {
                require(missing, slots, "studentId", "studentId");
                require(missing, slots, "title", "title");
                require(missing, slots, "content", "content");
                require(missing, slots, "type", "type");
                requireNumberIfPresent(missing, slots, "studentId", "studentId");
                requireNumberIfPresent(missing, slots, "relatedId", "relatedId");
            }
            case SEND_BATCH_NOTIFICATION -> {
                require(missing, slots, "studentIds", "studentIds");
                require(missing, slots, "title", "title");
                require(missing, slots, "content", "content");
                require(missing, slots, "type", "type");
                requireNumberListIfPresent(missing, slots, "studentIds", "studentIds");
            }
            case GENERATE_QUESTIONS -> {
                removeQuestionGenerationHints(missing);
                requireQuestionTopic(missing, slots, message);
            }
            default -> {
                return new ArrayList<>(missing);
            }
        }
        return new ArrayList<>(missing);
    }

    private void removeSatisfiedMissingSlots(Set<String> missing, Map<String, Object> slots) {
        removeIfSatisfied(missing, slots, "课程或班级", "courseId", "courseName", "classId", "className");
        removeIfSatisfied(missing, slots, "课程", "courseId", "courseName");
        removeIfSatisfied(missing, slots, "班级", "classId", "className");
        removeIfSatisfied(missing, slots, "作业", "assignmentId", "assignmentTitle");
        removeIfSatisfied(missing, slots, "考试", "examId", "examTitle");
        removeIfSatisfied(missing, slots, "标题", "title");
        removeIfSatisfied(missing, slots, "满分", "maxScore");
        removeIfSatisfied(missing, slots, "分数", "score", "maxScore");
        removeIfSatisfied(missing, slots, "截止时间", "dueDate");
        removeIfSatisfied(missing, slots, "提交内容", "content");
        removeIfSatisfied(missing, slots, "答题内容", "answers");
        removeIfSatisfied(missing, slots, "开始时间", "startTime");
        removeIfSatisfied(missing, slots, "结束时间", "endTime");
        removeIfSatisfied(missing, slots, "考试时长", "duration");
        removeIfSatisfied(missing, slots, "课程名称", "courseName");
        removeIfSatisfied(missing, slots, "课程代码", "courseCode");
        removeIfSatisfied(missing, slots, "学分", "credit");
        removeIfSatisfied(missing, slots, "总学时", "totalHours");
        removeIfSatisfied(missing, slots, "班级名称", "className");
        removeIfSatisfied(missing, slots, "年级", "year");
        removeIfSatisfied(missing, slots, "容量", "capacity");
        removeIfSatisfied(missing, slots, "作业ID", "assignmentId");
        removeIfSatisfied(missing, slots, "考试ID", "examId");
        removeIfSatisfied(missing, slots, "提交记录ID", "submissionId");
        removeIfSatisfied(missing, slots, "课程ID", "courseId");
        removeIfSatisfied(missing, slots, "班级ID", "classId");
        removeIfSatisfied(missing, slots, "学生ID", "studentId");
        removeIfSatisfied(missing, slots, "studentIds", "studentIds");
        removeIfSatisfied(missing, slots, "notificationId", "notificationId");
        removeIfSatisfied(missing, slots, "title", "title");
        removeIfSatisfied(missing, slots, "content", "content");
        removeIfSatisfied(missing, slots, "type", "type");
    }

    private void removeIfSatisfied(Set<String> missing, Map<String, Object> slots, String label, String... keys) {
        if (hasAny(slots, keys)) {
            missing.remove(label);
            for (String key : keys) {
                missing.remove(key);
            }
        }
    }

    public String buildPrompt(AgentIntent intent, List<String> missingSlots) {
        if (intent == AgentIntent.PUBLISH_ASSIGNMENT && missingSlots.size() == 1
                && missingSlots.contains("课程或班级")) {
            return "我已准备好作业内容、标题、截止时间和满分。还需要选择发布课程或班级。";
        }
        if (intent == AgentIntent.GENERATE_QUESTIONS && missingSlots.contains("主题")) {
            return "还需要补充出题主题，我才能继续生成题目。请按这个顺序补充：1. 课程名或知识点，例如“Java基础”或“服务注册与发现”；2. 可选难度，例如“中等”；3. 可选数量和题型，例如“10道选择题”。也可以直接说：“随机生成10道中等题目”。";
        }
        return "还需要补充" + missingSlots.stream()
                .map(this::describeMissingSlot)
                .distinct()
                .collect(java.util.stream.Collectors.joining("、"))
                + "，我才能继续处理" + displayName(intent) + "。";
    }

    private String describeMissingSlot(String missingSlot) {
        return switch (missingSlot) {
            case "更明确的课程", "可识别课程" -> "课程标识，例如课程名+学期+班级，或直接提供课程ID";
            case "更明确的作业", "可识别作业" -> "作业标识，例如作业标题或作业ID";
            case "更明确的考试", "可识别考试" -> "考试标识，例如考试标题或考试ID";
            case "课程或班级" -> "发布课程或班级";
            case "title" -> "标题";
            case "maxScore" -> "满分";
            case "dueDate" -> "截止时间";
            case "courseId", "courseName" -> "课程";
            case "classId", "className" -> "班级";
            case "assignmentId", "assignmentTitle" -> "作业";
            case "examId", "examTitle" -> "考试";
            case "主题" -> "出题主题（课程名或知识点）";
            default -> missingSlot;
        };
    }

    private void requireDueDate(Set<String> missing, Map<String, Object> slots, String message) {
        if (!hasAny(slots, "dueDate") && (message == null || !message.contains("截止"))) {
            missing.add("截止时间");
        }
    }

    private void require(Set<String> missing, Map<String, Object> slots, String label, String key) {
        if (!hasAny(slots, key)) {
            missing.add(label);
        }
    }

    private void requireAny(Set<String> missing, Map<String, Object> slots, String label, String... keys) {
        if (!hasAny(slots, keys)) {
            missing.add(label);
        }
    }

    private void requirePublishTarget(Set<String> missing, Map<String, Object> slots) {
        if (!hasAny(slots, "courseId", "courseName", "classId", "className")) {
            missing.add("课程或班级");
        }
    }

    private void removeQuestionGenerationHints(Set<String> missing) {
        missing.remove("topic");
        missing.remove("difficulty");
        missing.remove("count");
        missing.remove("type");
    }

    private void requireQuestionTopic(Set<String> missing, Map<String, Object> slots, String message) {
        if (isRandomQuestionRequest(message, slots)) {
            return;
        }
        Object topic = slots.get("topic");
        if (topic instanceof String text && !text.isBlank() && !isGenericQuestionTopic(text)) {
            return;
        }
        if (message != null && !message.isBlank() && !isGenericQuestionTopic(message)) {
            return;
        }
        missing.add("主题");
    }

    private boolean isRandomQuestionRequest(String message, Map<String, Object> slots) {
        String text = message == null ? "" : message;
        if (!text.contains("随机")) {
            return false;
        }
        Object topic = slots.get("topic");
        return !(topic instanceof String topicText) || topicText.isBlank() || isGenericQuestionTopic(topicText);
    }

    private boolean hasAny(Map<String, Object> slots, String... keys) {
        for (String key : keys) {
            Object value = slots.get(key);
            if (value != null && !String.valueOf(value).isBlank()) {
                return true;
            }
        }
        return false;
    }

    private void requireNumberIfPresent(Set<String> missing, Map<String, Object> slots, String label, String key) {
        Object value = slots.get(key);
        if (value == null || String.valueOf(value).isBlank() || isNumericValue(value)) {
            return;
        }
        missing.add(label + "格式不正确");
    }

    private void requireNumberListIfPresent(Set<String> missing, Map<String, Object> slots, String label, String key) {
        Object value = slots.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            return;
        }
        if (value instanceof List<?> values && !values.isEmpty() && values.stream().allMatch(this::isNumericValue)) {
            return;
        }
        missing.add(label + "格式不正确");
    }

    private void requireMapIfPresent(Set<String> missing, Map<String, Object> slots, String label, String key) {
        Object value = slots.get(key);
        if (value == null || String.valueOf(value).isBlank() || value instanceof Map<?, ?>) {
            return;
        }
        missing.add(label + "格式不正确");
    }

    private boolean isNumericValue(Object value) {
        if (value instanceof Number) {
            return true;
        }
        if (value instanceof String text) {
            return text.matches("\\d+");
        }
        return false;
    }

    private boolean isGenericQuestionTopic(String value) {
        String text = value == null ? "" : value.trim();
        if (text.isBlank()) {
            return true;
        }
        String normalized = text
                .replaceAll("\\d+", "")
                .replaceAll("[一二三四五六七八九十百千两]+道", "")
                .replace("随机", "")
                .replace("生成", "")
                .replace("帮我", "")
                .replace("课堂", "")
                .replace("练习", "")
                .replace("练习题", "")
                .replace("题目", "")
                .replace("题", "")
                .replace("选择", "")
                .replace("选择题", "")
                .replace("中等难度", "")
                .replace("简单难度", "")
                .replace("困难难度", "")
                .replace("中等", "")
                .replace("简单", "")
                .replace("困难", "")
                .replace("容易", "")
                .replace("高难度", "")
                .replace("低难度", "")
                .replaceAll("\\s+", "");
        return normalized.isBlank() || "综合".equals(normalized) || "综合练习".equals(text.trim());
    }

    private String displayName(AgentIntent intent) {
        return switch (intent) {
            case PUBLISH_ASSIGNMENT -> "发布作业";
            case UPDATE_ASSIGNMENT -> "更新作业";
            case DELETE_ASSIGNMENT -> "删除作业";
            case GRADE_ASSIGNMENT -> "批改作业";
            case PUBLISH_EXAM -> "发布考试";
            case CREATE_COURSE -> "创建课程";
            case UPDATE_COURSE -> "更新课程";
            case DELETE_COURSE -> "删除课程";
            case CREATE_CLASS -> "创建班级";
            case UPDATE_EXAM -> "更新考试";
            case DELETE_EXAM -> "删除考试";
            case GRADE_EXAM -> "批改考试";
            case SUBMIT_ASSIGNMENT -> "提交作业";
            case SUBMIT_EXAM -> "提交考试";
            case QUERY_COURSE_DETAIL -> "查询课程详情";
            case QUERY_CLASS_DETAIL -> "查询班级详情";
            case QUERY_EARLY_WARNINGS -> "查询学情预警";
            case QUERY_KNOWLEDGE_POINTS -> "查询课程知识点";
            case QUERY_KNOWLEDGE_MASTERY -> "查询知识点掌握情况";
            case QUERY_ASSIGNMENT_DETAIL -> "查询作业详情";
            case QUERY_ASSIGNMENT_SUBMISSIONS -> "查询作业提交记录";
            case QUERY_EXAM_DETAIL -> "查询考试详情";
            case QUERY_EXAM_SUBMISSIONS -> "查询考试提交记录";
            case MARK_NOTIFICATION_READ -> "单条通知标为已读";
            case DELETE_NOTIFICATION -> "删除通知";
            case SEND_NOTIFICATION -> "发送通知";
            case SEND_BATCH_NOTIFICATION -> "批量发送通知";
            case GENERATE_QUESTIONS -> "生成题目";
            case INTERNET_SEARCH -> "联网搜索";
            case READ_WEB_PAGE -> "读取网页";
            default -> "这个请求";
        };
    }
}
