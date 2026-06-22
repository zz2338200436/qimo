package com._202510007517.platform.agent.service;

import com._202510007517.platform.agent.model.AgentIntent;
import com._202510007517.platform.agent.model.RecognizedIntent;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class RuleBasedIntentRecognitionService implements IntentRecognitionService {

    private static final Pattern TITLE_PATTERN = Pattern.compile("标题(?:是|为|[:：])?\\s*[“\"《]?([^”\"》，,。；;]+)[”\"》]?");
    private static final Pattern CONTENT_PATTERN = Pattern.compile("内容是(.+?)(?:[，,。；;](?:类型|学生|课程|相关)(?:ID)?是?.*)?$");
    private static final Pattern MAX_SCORE_PATTERN = Pattern.compile("满分\\s*(\\d+)|(\\d+)\\s*分(?!钟)");
    private static final Pattern TIME_TAKEN_PATTERN = Pattern.compile("用时\\s*(\\d+)\\s*分钟?");
    private static final Pattern DURATION_PATTERN = Pattern.compile("(?:时长|考试时长)\\s*(\\d+)\\s*分钟?");
    private static final Pattern START_TIME_PATTERN = Pattern.compile("(?:开始时间|开考时间|考试开始时间)(?:是)?([^，,。；;]+)");
    private static final Pattern END_TIME_PATTERN = Pattern.compile("(?:结束时间|考试结束时间)(?:是)?([^，,。；;]+)");
    private static final Pattern DUE_DATE_PATTERN = Pattern.compile("截止(?:时间)?(?:是)?([^，,。；;]+)|([^，,。；;]+?)截止");
    private static final Pattern COURSE_PATTERN = Pattern.compile("给([^，,。；;]*?)课程");
    private static final Pattern COURSE_NAME_PATTERN = Pattern.compile("课程(?:名称|名)(?:是|为)?([^，,。；;]+)");
    private static final Pattern KNOWLEDGE_POINT_COURSE_NAME_PATTERN = Pattern.compile("查询([^，,。；;]+?)课程知识点");
    private static final Pattern COURSE_BOOK_TITLE_PATTERN = Pattern.compile("《\\s*([^》]+?)\\s*》[^，,。；;]*?(?:课程|这门课|课下)");
    private static final Pattern COURSE_CODE_PATTERN = Pattern.compile("课程(?:代码|编号)(?:是|为)?([A-Za-z0-9_-]+)");
    private static final Pattern CREDIT_PATTERN = Pattern.compile("学分\\s*(\\d+)");
    private static final Pattern TOTAL_HOURS_PATTERN = Pattern.compile("总学时\\s*(\\d+)");
    private static final Pattern CLASS_NAME_PATTERN = Pattern.compile("班级(?:名称|名)?(?:是|为)?([^，,。；;]+)");
    private static final Pattern COLLOQUIAL_CLASS_NAME_PATTERN = Pattern.compile("开(?:设)?(?:一个|个)?([^，,。；;]+?)班");
    private static final Pattern NATURAL_CLASS_NAME_PATTERN = Pattern.compile("([\\u4e00-\\u9fa5A-Za-z0-9]+(?:\\d{2,4}级)?\\d+班)");
    private static final Pattern YEAR_PATTERN = Pattern.compile("年级(?:是|为)?\\s*([0-9]{4}|[0-9]{2})");
    private static final Pattern COLLOQUIAL_YEAR_PATTERN = Pattern.compile("([0-9]{4}|[0-9]{2})级");
    private static final Pattern SEMESTER_PATTERN = Pattern.compile(
            "((?:\\d{4}\\s*[-/]\\s*\\d{4}\\s*[-/]\\s*[1-4])(?=\\s*学期)|(?:\\d{4}\\s*(?:春|秋))(?:学期)?|(?:第?[一二三四五六七八九十]+学期)|(?:春季|秋季)学期)");
    private static final Pattern CAPACITY_PATTERN = Pattern.compile("(?:容量|人数上限)\\s*(\\d+)");
    private static final Pattern COLLOQUIAL_CAPACITY_PATTERN = Pattern.compile("最多\\s*(\\d+)\\s*人");
    private static final Pattern MAJOR_ID_PATTERN = Pattern.compile("专业(?:ID)?\\s*(\\d+)");
    private static final Pattern SUBMIT_ASSIGNMENT_PATTERN = Pattern.compile("提交([^，,。；;]*?作业)");
    private static final Pattern SUBMIT_EXAM_PATTERN = Pattern.compile("提交([^，,。；;]*?(?:考试|试卷))");
    private static final Pattern ANSWERS_PATTERN = Pattern.compile("(?:答案|答题内容)(?:是|为)?(.+)$");
    private static final Pattern ASSIGNMENT_ID_PATTERN = Pattern.compile("作业(?:ID)?\\s*(\\d+)");
    private static final Pattern EXAM_ID_PATTERN = Pattern.compile("(?:考试|试卷)(?:ID)?\\s*(\\d+)");
    private static final Pattern STUDENT_ID_PATTERN = Pattern.compile("学生(?:ID)?\\s*(\\d+)");
    private static final Pattern STUDENT_IDS_PATTERN = Pattern.compile("学生(?:ID)?\\s*([\\d\\s,，、]+)");
    private static final Pattern COURSE_ID_PATTERN = Pattern.compile("课程(?:ID)?\\s*(\\d+)");
    private static final Pattern NOTIFICATION_ID_PATTERN = Pattern.compile("通知(?:ID)?\\s*(\\d+)");
    private static final Pattern NOTIFICATION_TYPE_PATTERN = Pattern.compile("类型是([^，,。；;]+)");
    private static final Pattern QUESTION_COUNT_PATTERN = Pattern.compile("(\\d+)\\s*道|([一二三四五六七八九十两])\\s*道");

    @Override
    public RecognizedIntent recognize(String message) {
        String text = message == null ? "" : message.trim();
        if (text.isBlank()) {
            return unknown();
        }
        Map<String, Object> slots = extractCommonSlots(text);
        AgentIntent intent = detectIntent(text);
        double confidence = intent == AgentIntent.UNKNOWN ? 0.2 : 0.9;
        return new RecognizedIntent(intent, confidence, slots);
    }

    private AgentIntent detectIntent(String text) {
        if (containsAny(text, "发布作业", "布置作业", "创建作业")) {
            return AgentIntent.PUBLISH_ASSIGNMENT;
        }
        if (containsAny(text, "更新作业", "修改作业", "编辑作业")) {
            return AgentIntent.UPDATE_ASSIGNMENT;
        }
        if (containsAny(text, "删除作业", "移除作业")) {
            return AgentIntent.DELETE_ASSIGNMENT;
        }
        if (containsAny(text, "批改作业", "给作业打分", "作业评分", "作业打分")) {
            return AgentIntent.GRADE_ASSIGNMENT;
        }
        if (containsAny(text, "发布考试", "创建考试", "发布试卷", "创建试卷")) {
            return AgentIntent.PUBLISH_EXAM;
        }
        if (containsAny(text, "更新考试", "修改考试", "编辑考试", "更新试卷", "修改试卷", "编辑试卷")) {
            return AgentIntent.UPDATE_EXAM;
        }
        if (containsAny(text, "删除考试", "移除考试", "删除试卷", "移除试卷")) {
            return AgentIntent.DELETE_EXAM;
        }
        if (containsAny(text, "批改考试", "给考试打分", "考试评分", "考试打分",
                "批改试卷", "给试卷打分", "试卷评分", "试卷打分")) {
            return AgentIntent.GRADE_EXAM;
        }
        if (containsAny(text, "考试详情", "试卷详情", "考试明细", "试卷明细")) {
            return AgentIntent.QUERY_EXAM_DETAIL;
        }
        if (containsAny(text, "考试提交记录", "考试提交列表", "考试提交情况", "试卷提交记录", "试卷提交列表")
                || (containsAny(text, "考试", "试卷") && containsAny(text, "提交记录", "提交列表", "提交情况"))) {
            return AgentIntent.QUERY_EXAM_SUBMISSIONS;
        }
        if (containsAny(text, "待提交作业", "没交的作业", "未提交作业")) {
            return AgentIntent.QUERY_PENDING_ASSIGNMENTS;
        }
        if (containsAny(text, "作业提交记录", "作业提交列表", "作业提交情况", "作业提交")
                || (text.contains("作业") && containsAny(text, "提交记录", "提交列表", "提交情况"))) {
            return AgentIntent.QUERY_ASSIGNMENT_SUBMISSIONS;
        }
        if (containsAny(text, "作业详情", "作业明细")) {
            return AgentIntent.QUERY_ASSIGNMENT_DETAIL;
        }
        if (containsAny(text, "提交") && text.contains("作业")
                && !containsAny(text, "待提交", "未提交", "没交")) {
            return AgentIntent.SUBMIT_ASSIGNMENT;
        }
        if ((containsAny(text, "提交") && containsAny(text, "考试", "试卷")) || containsAny(text, "交卷")) {
            return AgentIntent.SUBMIT_EXAM;
        }
        if (containsAny(text, "课程详情", "课程明细")) {
            return AgentIntent.QUERY_COURSE_DETAIL;
        }
        if (containsAny(text, "创建课程", "新增课程", "添加课程", "开设课程")) {
            return AgentIntent.CREATE_COURSE;
        }
        if (containsAny(text, "更新课程", "修改课程", "编辑课程")) {
            return AgentIntent.UPDATE_COURSE;
        }
        if (containsAny(text, "删除课程", "移除课程")) {
            return AgentIntent.DELETE_COURSE;
        }
        if (containsAny(text, "创建班级", "新增班级", "添加班级")
                || (containsAny(text, "开班", "开一个", "开个", "开设") && text.contains("班"))) {
            return AgentIntent.CREATE_CLASS;
        }
        if (containsAny(text, "教师仪表盘", "班级学情概览", "教师看板", "教学仪表盘")) {
            return AgentIntent.QUERY_TEACHER_DASHBOARD;
        }
        if (containsAny(text, "学生学习汇总", "班级学习汇总", "学习汇总", "学情汇总")) {
            return AgentIntent.QUERY_LEARNING_SUMMARY;
        }
        if (containsAny(text, "成绩趋势", "分数趋势", "得分趋势")) {
            return AgentIntent.QUERY_SCORE_TREND;
        }
        if (containsAny(text, "知识点掌握", "掌握情况", "掌握度") && containsAny(text, "学生", "课程")) {
            return AgentIntent.QUERY_KNOWLEDGE_MASTERY;
        }
        if (containsAny(text, "题库") && containsAny(text, "有什么", "有哪些", "查询", "查看", "题目", "知识点", "数量")) {
            return AgentIntent.QUERY_QUESTION_BANK;
        }
        if (text.contains("知识点")
                && containsAny(text, "查看", "查询", "有哪些", "列表")
                && !containsAny(text, "掌握", "掌握情况", "掌握度")) {
            return AgentIntent.QUERY_KNOWLEDGE_POINTS;
        }
        if (containsAny(text, "发送通知", "发通知", "发送消息", "发消息")
                && containsAny(text, "批量", "多个", "多名", "群发")
                && extractStudentIds(text).size() > 1) {
            return AgentIntent.SEND_BATCH_NOTIFICATION;
        }
        if (containsAny(text, "发送通知", "发通知", "发送消息", "发消息")) {
            return AgentIntent.SEND_NOTIFICATION;
        }
        if (containsAny(text, "班级详情", "班级明细")) {
            return AgentIntent.QUERY_CLASS_DETAIL;
        }
        if (containsAny(text, "班级") && containsAny(text, "查看", "查询", "有哪些", "列表")) {
            return AgentIntent.QUERY_CLASSES;
        }
        if (containsAny(text, "作业") && containsAny(text, "查看", "查询", "列表")) {
            return AgentIntent.QUERY_ASSIGNMENTS;
        }
        if (text.contains("作业") && containsAny(text, "有哪些")) {
            return AgentIntent.QUERY_ASSIGNMENTS;
        }
        if (containsAny(text, "考试", "试卷") && containsAny(text, "查看", "查询", "列表")) {
            return AgentIntent.QUERY_EXAMS;
        }
        if (containsAny(text, "考试", "试卷") && containsAny(text, "有哪些")) {
            return AgentIntent.QUERY_EXAMS;
        }
        if (containsAny(text, "课程") && containsAny(text, "查看", "查询", "有哪些", "列表")) {
            return AgentIntent.QUERY_COURSES;
        }
        if (containsAny(text, "成绩", "分数", "查分") && containsAny(text, "查看", "查询", "我的", "列表")) {
            return AgentIntent.QUERY_SCORES;
        }
        if (containsAny(text, "通知", "消息") && containsAny(text, "未读")
                && containsAny(text, "数量", "数", "多少", "几个")) {
            return AgentIntent.QUERY_UNREAD_NOTIFICATION_COUNT;
        }
        if (containsAny(text, "通知", "消息") && isMarkAllNotificationsReadCommand(text)) {
            return AgentIntent.MARK_ALL_NOTIFICATIONS_READ;
        }
        if (containsAny(text, "通知", "消息") && isMarkNotificationReadCommand(text)) {
            return AgentIntent.MARK_NOTIFICATION_READ;
        }
        if (containsAny(text, "通知", "消息") && isDeleteAllReadNotificationsCommand(text)) {
            return AgentIntent.DELETE_ALL_READ_NOTIFICATIONS;
        }
        if (containsAny(text, "通知", "消息") && containsAny(text, "删除", "移除")) {
            return AgentIntent.DELETE_NOTIFICATION;
        }
        if (containsAny(text, "通知", "消息") && containsAny(text, "查看", "查询", "未读")) {
            return AgentIntent.QUERY_NOTIFICATIONS;
        }
        if (containsAny(text, "学习统计", "学习数据", "学情")) {
            return AgentIntent.QUERY_STUDENT_STATS;
        }
        if (containsAny(text, "学习时间分布", "学习时长分布", "学习时间统计", "学习时长统计")) {
            return AgentIntent.QUERY_STUDY_TIME_DISTRIBUTION;
        }
        if (containsAny(text, "学习建议", "复习建议")) {
            return AgentIntent.GENERATE_LEARNING_SUGGESTIONS;
        }
        if (containsAny(text, "生成", "出") && containsAny(text, "题", "题目", "选择题")) {
            return AgentIntent.GENERATE_QUESTIONS;
        }
        if (containsAny(text, "生成", "创建") && containsAny(text, "试卷", "模拟试卷")) {
            return AgentIntent.GENERATE_EXAM;
        }
        if (isRagKnowledgeQuestion(text)) {
            return AgentIntent.QUERY_RAG_KNOWLEDGE;
        }
        return AgentIntent.UNKNOWN;
    }

    private Map<String, Object> extractCommonSlots(String text) {
        Map<String, Object> slots = new LinkedHashMap<>();
        putIfFound(slots, "title", TITLE_PATTERN, text);
        putIfFound(slots, "content", CONTENT_PATTERN, text);
        putCourseNameIfFound(slots, text);
        putKnowledgePointCourseNameIfFound(slots, text);
        putIfAbsentFound(slots, "courseName", COURSE_PATTERN, text);
        putIfAbsentFound(slots, "courseName", COURSE_BOOK_TITLE_PATTERN, text);
        putIfFound(slots, "courseCode", COURSE_CODE_PATTERN, text);
        putIfFound(slots, "className", CLASS_NAME_PATTERN, text);
        putIfAbsentFound(slots, "className", COLLOQUIAL_CLASS_NAME_PATTERN, text);
        putIfAbsentFound(slots, "className", NATURAL_CLASS_NAME_PATTERN, text);
        putIfFound(slots, "assignmentTitle", SUBMIT_ASSIGNMENT_PATTERN, text);
        putIfFound(slots, "examTitle", SUBMIT_EXAM_PATTERN, text);
        putLongIfFound(slots, "assignmentId", ASSIGNMENT_ID_PATTERN, text);
        putLongIfFound(slots, "examId", EXAM_ID_PATTERN, text);
        Matcher maxScore = MAX_SCORE_PATTERN.matcher(text);
        if (maxScore.find()) {
            String value = maxScore.group(1) != null ? maxScore.group(1) : maxScore.group(2);
            slots.put("maxScore", Integer.parseInt(value));
        }
        Matcher timeTaken = TIME_TAKEN_PATTERN.matcher(text);
        if (timeTaken.find()) {
            slots.put("timeTaken", Integer.parseInt(timeTaken.group(1)));
        }
        putIntegerIfFound(slots, "credit", CREDIT_PATTERN, text);
        putIntegerIfFound(slots, "totalHours", TOTAL_HOURS_PATTERN, text);
        putIfFound(slots, "semester", SEMESTER_PATTERN, text);
        putIfFound(slots, "year", YEAR_PATTERN, text);
        putIfAbsentFound(slots, "year", COLLOQUIAL_YEAR_PATTERN, text);
        putIntegerIfFound(slots, "capacity", CAPACITY_PATTERN, text);
        putIntegerIfAbsentFound(slots, "capacity", COLLOQUIAL_CAPACITY_PATTERN, text);
        putLongIfFound(slots, "majorId", MAJOR_ID_PATTERN, text);
        Matcher duration = DURATION_PATTERN.matcher(text);
        if (duration.find()) {
            slots.put("duration", Integer.parseInt(duration.group(1)));
        }
        putIfFound(slots, "startTime", START_TIME_PATTERN, text);
        putIfFound(slots, "endTime", END_TIME_PATTERN, text);
        putLongIfFound(slots, "studentId", STUDENT_ID_PATTERN, text);
        List<Long> studentIds = extractStudentIds(text);
        if (studentIds.size() > 1) {
            slots.put("studentIds", studentIds);
        }
        putLongIfFound(slots, "courseId", COURSE_ID_PATTERN, text);
        putLongIfFound(slots, "notificationId", NOTIFICATION_ID_PATTERN, text);
        putIfFound(slots, "type", NOTIFICATION_TYPE_PATTERN, text);
        Matcher dueDate = DUE_DATE_PATTERN.matcher(text);
        if (dueDate.find()) {
            String value = dueDate.group(1) != null ? dueDate.group(1) : dueDate.group(2);
            if (value != null && !value.isBlank()) {
                slots.put("dueDate", value.trim());
            }
        }
        Matcher answers = ANSWERS_PATTERN.matcher(text);
        if (answers.find()) {
            Map<String, String> parsedAnswers = parseAnswers(answers.group(1));
            if (!parsedAnswers.isEmpty()) {
                slots.put("answers", parsedAnswers);
            }
        }
        extractQuestionGenerationSlots(slots, text);
        return slots;
    }

    private static void extractQuestionGenerationSlots(Map<String, Object> slots, String text) {
        if (!(containsAny(text, "生成", "出") && containsAny(text, "题", "题目", "选择题"))) {
            return;
        }

        Matcher countMatcher = QUESTION_COUNT_PATTERN.matcher(text);
        if (countMatcher.find()) {
            String arabic = countMatcher.group(1);
            if (arabic != null) {
                slots.put("count", Integer.parseInt(arabic));
            } else {
                Integer count = chineseNumber(countMatcher.group(2));
                if (count != null) {
                    slots.put("count", count);
                }
            }
        }

        if (containsAny(text, "简单难度", "简单", "easy", "入门")) {
            slots.put("difficulty", "简单");
        } else if (containsAny(text, "中等难度", "中等", "medium", "中级")) {
            slots.put("difficulty", "中等");
        } else if (containsAny(text, "困难难度", "困难", "hard", "高级")) {
            slots.put("difficulty", "困难");
        }

        if (text.contains("选择题")) {
            slots.put("type", "SINGLE_CHOICE");
        } else if (text.contains("判断题")) {
            slots.put("type", "TRUE_FALSE");
        } else if (text.contains("填空题")) {
            slots.put("type", "FILL_BLANK");
        } else if (containsAny(text, "简答题", "问答题")) {
            slots.put("type", "SHORT_ANSWER");
        }

        String topic = text
                .replaceAll("[0-9]+\\s*道", "")
                .replaceAll("[一二三四五六七八九十两]\\s*道", "")
                .replace("随机", "")
                .replace("帮我", "")
                .replace("请", "")
                .replace("生成", "")
                .replace("出", "")
                .replace("中等难度", "")
                .replace("简单难度", "")
                .replace("困难难度", "")
                .replace("中等", "")
                .replace("简单", "")
                .replace("困难", "")
                .replace("选择题", "")
                .replace("判断题", "")
                .replace("填空题", "")
                .replace("简答题", "")
                .replace("问答题", "")
                .replace("课堂练习题", "")
                .replace("课堂练习", "")
                .replace("练习题", "")
                .replace("题目", "")
                .replace("题", "")
                .replaceAll("\\s+", "")
                .trim();
        if (!topic.isBlank()) {
            slots.put("topic", topic);
        }
    }

    private static void putIfFound(Map<String, Object> slots, String key, Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            String value = matcher.group(1).trim();
            if (!value.isBlank()) {
                slots.put(key, value);
            }
        }
    }

    private static void putIfAbsentFound(Map<String, Object> slots, String key, Pattern pattern, String text) {
        if (!slots.containsKey(key)) {
            putIfFound(slots, key, pattern, text);
        }
    }

    private static void putLongIfFound(Map<String, Object> slots, String key, Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            slots.put(key, Long.valueOf(matcher.group(1)));
        }
    }

    private static void putIntegerIfFound(Map<String, Object> slots, String key, Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            slots.put(key, Integer.valueOf(matcher.group(1)));
        }
    }

    private static void putIntegerIfAbsentFound(Map<String, Object> slots, String key, Pattern pattern, String text) {
        if (!slots.containsKey(key)) {
            putIntegerIfFound(slots, key, pattern, text);
        }
    }

    private static void putCourseNameIfFound(Map<String, Object> slots, String text) {
        Matcher matcher = COURSE_NAME_PATTERN.matcher(text);
        if (matcher.find()) {
            String value = matcher.group(1).trim();
            if (!value.isBlank() && !value.matches("\\d+")) {
                slots.put("courseName", value);
            }
        }
    }

    private static void putKnowledgePointCourseNameIfFound(Map<String, Object> slots, String text) {
        if (slots.containsKey("courseName") || !text.contains("知识点")) {
            return;
        }
        Matcher matcher = KNOWLEDGE_POINT_COURSE_NAME_PATTERN.matcher(text);
        if (matcher.find()) {
            String value = matcher.group(1).trim();
            if (!value.isBlank() && !value.matches("\\d+")) {
                slots.put("courseName", value);
            }
        }
    }

    private static boolean containsAny(String text, String... terms) {
        for (String term : terms) {
            if (text.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isMarkAllNotificationsReadCommand(String text) {
        if (!containsAny(text, "全部", "所有", "全都")) {
            return false;
        }
        if (containsAny(text, "标为已读", "设为已读", "改为已读", "置为已读", "标记为已读", "全部已读", "全都已读")) {
            return true;
        }
        return containsAny(text, "读完", "读过")
                && containsAny(text, "标为", "设为", "改为", "置为", "标记", "处理");
    }

    private static boolean isMarkNotificationReadCommand(String text) {
        if (!containsAny(text, "标为已读", "设为已读", "改为已读", "置为已读", "标记为已读")) {
            return false;
        }
        return NOTIFICATION_ID_PATTERN.matcher(text).find()
                || !containsAny(text, "全部", "所有", "全都");
    }

    private static boolean isDeleteAllReadNotificationsCommand(String text) {
        return containsAny(text, "删除", "移除", "清理", "清空")
                && containsAny(text, "已读")
                && containsAny(text, "全部", "所有", "全都");
    }

    private static boolean isRagKnowledgeQuestion(String text) {
        if (containsBusinessActionOrLiveData(text)) {
            return false;
        }
        return containsAny(text,
                "什么是", "解释", "说明", "如何理解", "平台怎么", "学生如何", "教师如何",
                "怎么使用AI助手", "怎么使用 AI 助手", "服务注册", "配置中心", "网关", "RAG");
    }

    private static boolean containsBusinessActionOrLiveData(String text) {
        return containsAny(text,
                "删除", "发布", "批改", "提交", "发送通知", "发通知", "多少分", "名单",
                "有哪些作业", "有哪些考试", "查询作业", "查询考试", "课程ID", "作业ID", "考试ID", "通知ID");
    }

    private static List<Long> extractStudentIds(String text) {
        Matcher matcher = STUDENT_IDS_PATTERN.matcher(text);
        if (!matcher.find()) {
            return List.of();
        }
        List<Long> ids = new ArrayList<>();
        Matcher numberMatcher = Pattern.compile("\\d+").matcher(matcher.group(1));
        while (numberMatcher.find()) {
            ids.add(Long.valueOf(numberMatcher.group()));
        }
        return ids;
    }

    private static Map<String, String> parseAnswers(String rawAnswers) {
        Map<String, String> answers = new LinkedHashMap<>();
        if (rawAnswers == null || rawAnswers.isBlank()) {
            return answers;
        }
        String[] pairs = rawAnswers.split("[，,；;]");
        for (String pair : pairs) {
            String[] parts = pair.trim().split("[:：=]", 2);
            if (parts.length == 2 && !parts[0].isBlank() && !parts[1].isBlank()) {
                answers.put(parts[0].trim(), parts[1].trim());
            }
        }
        return answers;
    }

    private static Integer chineseNumber(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return switch (value.trim()) {
            case "一" -> 1;
            case "二", "两" -> 2;
            case "三" -> 3;
            case "四" -> 4;
            case "五" -> 5;
            case "六" -> 6;
            case "七" -> 7;
            case "八" -> 8;
            case "九" -> 9;
            case "十" -> 10;
            default -> null;
        };
    }

    private static RecognizedIntent unknown() {
        return new RecognizedIntent(AgentIntent.UNKNOWN, 0.0, Map.of());
    }
}
