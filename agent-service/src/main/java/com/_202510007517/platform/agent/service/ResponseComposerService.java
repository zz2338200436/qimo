package com._202510007517.platform.agent.service;

import com._202510007517.platform.agent.model.PlannerDecision;
import com._202510007517.platform.assignment.api.dto.AssignmentStudentScoreDTO;
import com._202510007517.platform.course.api.dto.TeacherClassDTO;
import com._202510007517.platform.exam.api.dto.ExamDTO;
import com._202510007517.platform.exam.api.dto.StudentScoreDTO;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ResponseComposerService {

    private static final Pattern VERSION_PATTERN = Pattern.compile("\\b\\d+(?:\\.\\d+){1,3}\\b");

    public String composeDirectAnswer(PlannerDecision decision, List<?> retrievalResults) {
        if (decision.replyDraft() != null && !decision.replyDraft().isBlank()) {
            return decision.replyDraft();
        }
        return "我已经处理了这个请求。";
    }

    public String composeClarification(PlannerDecision decision) {
        if (decision.clarificationPrompt() != null && !decision.clarificationPrompt().isBlank()) {
            return decision.clarificationPrompt();
        }
        return "还需要更多信息，我才能继续处理。";
    }

    public String composeToolReply(PlannerDecision decision,
                                   Map<String, Object> toolResult,
                                   List<?> retrievalResults) {
        if (toolResult == null || toolResult.isEmpty()) {
            return "操作已处理。";
        }
        String status = String.valueOf(toolResult.getOrDefault("status", ""));
        if ("FAILED".equals(status) || "VALIDATION_FAILED".equals(status)) {
            return defaultIfBlank(toolResult.get("message"), "操作执行失败。");
        }
        if ("publish_assignment".equals(decision.toolName())) {
            String className = stringValue(toolResult.get("className"));
            String title = stringValue(toolResult.get("title"));
            if (className != null && title != null) {
                return "已经为你准备好发布作业《" + title + "》到" + className + "。";
            }
        }
        if ("query_classes".equals(decision.toolName())) {
            String classSummary = composeClassListReply(toolResult.get("classes"));
            if (classSummary != null) {
                return classSummary;
            }
        }
        if ("query_assignments".equals(decision.toolName())) {
            String assignmentSummary = composeAssignmentListReply(toolResult.get("assignments"));
            if (assignmentSummary != null) {
                return assignmentSummary;
            }
        }
        if ("query_exams".equals(decision.toolName())) {
            String examSummary = composeExamListReply(toolResult.get("exams"));
            if (examSummary != null) {
                return examSummary;
            }
        }
        if ("internet_search".equals(decision.toolName())) {
            String summary = composeInternetSearchReply(toolResult);
            if (summary != null) {
                return summary;
            }
        }
        if ("read_web_page".equals(decision.toolName())) {
            String summary = composeWebPageReply(toolResult);
            if (summary != null) {
                return summary;
            }
        }
        return defaultIfBlank(toolResult.get("message"), "操作已处理。");
    }

    private String composeInternetSearchReply(Map<String, Object> toolResult) {
        Object resultsValue = toolResult.get("results");
        if (!(resultsValue instanceof List<?> results) || results.isEmpty()) {
            return defaultIfBlank(toolResult.get("message"), "联网搜索完成，但没有找到可展示的来源。");
        }
        List<?> limitedResults = results.stream().limit(5).toList();
        List<String> lines = java.util.stream.IntStream.range(0, limitedResults.size())
                .mapToObj(index -> describeInternetSource(limitedResults.get(index), index + 1))
                .filter(line -> line != null && !line.isBlank())
                .toList();
        if (lines.isEmpty()) {
            return null;
        }
        String query = stringValue(toolResult.get("query"));
        String answer = composeInternetSearchAnswer(query, limitedResults);
        return answer
                + System.lineSeparator()
                + System.lineSeparator()
                + "**来源"
                + (query == null ? "**" : "（" + query + "）**")
                + System.lineSeparator()
                + String.join(System.lineSeparator(), lines);
    }

    private String composeInternetSearchAnswer(String query, List<?> results) {
        String answer = inferInternetAnswer(query, results);
        if (answer != null) {
            return answer;
        }
        if (query != null) {
            return "已完成联网搜索：" + query + "。";
        }
        return "已完成联网搜索。";
    }

    private String inferInternetAnswer(String query, List<?> results) {
        if (query == null) {
            return null;
        }
        String normalizedQuery = query.toLowerCase(Locale.ROOT);
        if (isTodayWeekdayQuery(normalizedQuery)) {
            return "今天是" + chineseWeekday(LocalDate.now().getDayOfWeek()) + "。";
        }
        if (normalizedQuery.contains("最新版本") || normalizedQuery.contains("latest version") || normalizedQuery.contains("版本是多少")) {
            for (Object rawResult : results) {
                if (!(rawResult instanceof Map<?, ?> source)) {
                    continue;
                }
                String title = stringValue(source.get("title"));
                String snippet = stringValue(source.get("snippet"));
                String version = firstVersion(title, snippet);
                if (version != null) {
                    String subject = normalizeVersionSubject(query);
                    return subject + "最新版本看起来是 " + version + "。";
                }
            }
        }
        if (looksLikeHowToQuery(normalizedQuery)) {
            String stepSummary = summarizeHowToSnippet(results);
            if (stepSummary != null) {
                String subject = normalizeHowToSubject(query);
                return subject + "一般可以" + stepSummary;
            }
        }
        String genericSummary = summarizeGenericSearchResults(query, results);
        if (genericSummary != null) {
            return genericSummary;
        }
        return null;
    }

    private String summarizeGenericSearchResults(String query, List<?> results) {
        for (Object rawResult : results) {
            if (!(rawResult instanceof Map<?, ?> source)) {
                continue;
            }
            String title = stringValue(source.get("title"));
            String snippet = stringValue(source.get("snippet"));
            if (title == null && snippet == null) {
                continue;
            }
            if (title != null && snippet != null) {
                String subject = normalizeInternetAnswerSubject(query);
                if (subject == null || subject.equalsIgnoreCase(title)) {
                    return title + "：" + snippet;
                }
                return subject + "：" + snippet;
            }
            if (snippet != null) {
                String subject = normalizeInternetAnswerSubject(query);
                return subject == null ? snippet : subject + "：" + snippet;
            }
            if (title != null) {
                String subject = normalizeInternetAnswerSubject(query);
                return subject == null ? title : subject + "：" + title;
            }
        }
        return null;
    }

    private boolean isTodayWeekdayQuery(String normalizedQuery) {
        return normalizedQuery.contains("今天星期几")
                || normalizedQuery.contains("今天周几")
                || normalizedQuery.contains("今天礼拜几")
                || normalizedQuery.contains("today weekday")
                || normalizedQuery.contains("what day is it today");
    }

    private boolean looksLikeHowToQuery(String normalizedQuery) {
        return normalizedQuery.contains("怎么做")
                || normalizedQuery.contains("如何做")
                || normalizedQuery.contains("做法")
                || normalizedQuery.contains("怎么炒")
                || normalizedQuery.contains("recipe")
                || normalizedQuery.contains("how to");
    }

    private String summarizeHowToSnippet(List<?> results) {
        for (Object rawResult : results) {
            if (!(rawResult instanceof Map<?, ?> source)) {
                continue;
            }
            String snippet = stringValue(source.get("snippet"));
            if (snippet == null) {
                continue;
            }
            String cleaned = snippet
                    .replace('，', ',')
                    .replace('。', ',')
                    .replace('；', ',');
            String[] parts = cleaned.split("\\s*,\\s*");
            List<String> usefulParts = java.util.Arrays.stream(parts)
                    .map(String::trim)
                    .filter(part -> !part.isBlank())
                    .filter(part -> part.length() >= 4)
                    .limit(4)
                    .toList();
            if (!usefulParts.isEmpty()) {
                return String.join("，", usefulParts) + "。";
            }
        }
        return null;
    }

    private String normalizeHowToSubject(String query) {
        String subject = query
                .replace("帮我", "")
                .replace("请", "")
                .replace("联网搜索", "")
                .replace("搜索一下", "")
                .replace("查一下", "")
                .replace("怎么做", "")
                .replace("如何做", "")
                .replace("做法", "")
                .replace("怎么炒", "")
                .trim();
        return subject.isBlank() ? "这个做法" : subject;
    }

    private String normalizeVersionSubject(String query) {
        String subject = query
                .replace("联网搜索", "")
                .replace("帮我", "")
                .replace("请", "")
                .replace("最新版本是多少", "")
                .replace("最新版本是啥", "")
                .replace("最新版本", "")
                .replace("版本是多少", "")
                .replace("版本", "")
                .trim();
        return subject.isBlank() ? "该项目" : subject;
    }

    private String normalizeInternetAnswerSubject(String query) {
        if (query == null) {
            return null;
        }
        String subject = query
                .replace("帮我", "")
                .replace("请", "")
                .replace("联网搜索", "")
                .replace("联网查", "")
                .replace("上网搜索", "")
                .replace("网上搜索", "")
                .replace("网上查", "")
                .replace("搜索一下", "")
                .replace("查一下", "")
                .replace("查找", "")
                .replace("查询", "")
                .replace("关于", "")
                .trim();
        return subject.isBlank() ? null : subject;
    }

    private String firstVersion(String... texts) {
        for (String text : texts) {
            if (text == null || text.isBlank()) {
                continue;
            }
            Matcher matcher = VERSION_PATTERN.matcher(text);
            if (matcher.find()) {
                return matcher.group();
            }
        }
        return null;
    }

    private String describeInternetSource(Object rawSource, int index) {
        if (!(rawSource instanceof Map<?, ?> source)) {
            return stringValue(rawSource);
        }
        String title = firstNonBlank(source.get("title"), source.get("url"));
        String url = stringValue(source.get("url"));
        String snippet = stringValue(source.get("snippet"));
        if (title == null && url == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        String displayTitle = title == null ? "未命名来源" : title;
        if (url != null) {
            builder.append(index)
                    .append(". [")
                    .append(displayTitle)
                    .append("](")
                    .append(url)
                    .append(")");
        } else {
            builder.append(index).append(". ").append(displayTitle);
        }
        if (snippet != null) {
            builder.append(System.lineSeparator())
                    .append("   - 摘要：")
                    .append(snippet);
        }
        return builder.toString();
    }

    private String composeWebPageReply(Map<String, Object> toolResult) {
        String content = stringValue(toolResult.get("content"));
        String title = firstNonBlank(toolResult.get("title"), toolResult.get("url"));
        String url = stringValue(toolResult.get("url"));
        if (content == null) {
            return defaultIfBlank(toolResult.get("message"), "网页读取完成，但没有解析到正文内容。");
        }
        String preview = content.length() > 400 ? content.substring(0, 400).trim() + "..." : content;
        return "网页读取完成："
                + (title == null ? "未命名网页" : title)
                + (url == null ? "" : System.lineSeparator() + "来源：" + url)
                + System.lineSeparator()
                + preview;
    }

    private String composeClassListReply(Object classesValue) {
        if (!(classesValue instanceof List<?> classes) || classes.isEmpty()) {
            return "当前没有查询到班级。";
        }
        List<String> lines = classes.stream()
                .limit(10)
                .map(this::describeClass)
                .filter(line -> line != null && !line.isBlank())
                .collect(Collectors.toList());
        if (lines.isEmpty()) {
            return null;
        }
        String prefix = "查到以下班级：";
        if (classes.size() > 10) {
            return prefix + System.lineSeparator()
                    + String.join(System.lineSeparator(), lines)
                    + System.lineSeparator()
                    + "其余 " + (classes.size() - 10) + " 个班级请在列表中查看。";
        }
        return prefix + System.lineSeparator() + String.join(System.lineSeparator(), lines);
    }

    private String composeAssignmentListReply(Object assignmentsValue) {
        if (!(assignmentsValue instanceof List<?> assignments) || assignments.isEmpty()) {
            return "当前没有查询到作业记录。";
        }
        List<String> lines = assignments.stream()
                .limit(10)
                .map(this::describeAssignment)
                .filter(line -> line != null && !line.isBlank())
                .collect(Collectors.toList());
        if (lines.isEmpty()) {
            return null;
        }
        String prefix = "查到以下作业记录：";
        if (assignments.size() > 10) {
            return prefix + System.lineSeparator()
                    + String.join(System.lineSeparator(), lines)
                    + System.lineSeparator()
                    + "其余 " + (assignments.size() - 10) + " 条请在列表中查看。";
        }
        return prefix + System.lineSeparator() + String.join(System.lineSeparator(), lines);
    }

    private String composeExamListReply(Object examsValue) {
        if (!(examsValue instanceof List<?> exams) || exams.isEmpty()) {
            return "当前没有查询到考试记录。";
        }
        if (shouldRenderExamSchedule(exams)) {
            return composeExamScheduleReply(exams);
        }
        List<String> lines = exams.stream()
                .limit(10)
                .map(this::describeExam)
                .filter(line -> line != null && !line.isBlank())
                .collect(Collectors.toList());
        if (lines.isEmpty()) {
            return null;
        }
        String prefix = "查到以下考试记录：";
        if (exams.size() > 10) {
            return prefix + System.lineSeparator()
                    + String.join(System.lineSeparator(), lines)
                    + System.lineSeparator()
                    + "其余 " + (exams.size() - 10) + " 条请在列表中查看。";
        }
        return prefix + System.lineSeparator() + String.join(System.lineSeparator(), lines);
    }

    private boolean shouldRenderExamSchedule(List<?> exams) {
        return exams.stream().filter(java.util.Objects::nonNull).anyMatch(this::isExamScheduleRecord)
                && exams.stream().filter(java.util.Objects::nonNull).noneMatch(this::isExamScoreRecord);
    }

    private boolean isExamScheduleRecord(Object rawExam) {
        if (rawExam instanceof ExamDTO) {
            return true;
        }
        if (!(rawExam instanceof Map<?, ?> examMap)) {
            return false;
        }
        return firstNonBlank(
                examMap.get("startTime"),
                examMap.get("endTime"),
                examMap.get("status"),
                examMap.get("publishDate"),
                examMap.get("courseName"),
                examMap.get("title"),
                examMap.get("examName"),
                examMap.get("name")
        ) != null;
    }

    private boolean isExamScoreRecord(Object rawExam) {
        if (rawExam instanceof StudentScoreDTO) {
            return true;
        }
        if (!(rawExam instanceof Map<?, ?> examMap)) {
            return false;
        }
        return firstNonBlank(examMap.get("score"), examMap.get("totalScore"), examMap.get("relatedId")) != null;
    }

    private String composeExamScheduleReply(List<?> exams) {
        List<?> limitedExams = exams.stream().limit(10).toList();
        List<String> lines = java.util.stream.IntStream.range(0, limitedExams.size())
                .mapToObj(index -> describeExamSchedule(index + 1, limitedExams.get(index)))
                .filter(line -> line != null && !line.isBlank())
                .toList();
        if (lines.isEmpty()) {
            return null;
        }
        StringBuilder reply = new StringBuilder();
        reply.append("查到以下考试安排信息，共 **").append(exams.size()).append(" 条记录**。")
                .append(System.lineSeparator())
                .append(System.lineSeparator())
                .append("先为你展示前 **").append(limitedExams.size()).append(" 条**：")
                .append(System.lineSeparator())
                .append(System.lineSeparator())
                .append(String.join(System.lineSeparator() + System.lineSeparator(), lines));
        if (exams.size() > limitedExams.size()) {
            reply.append(System.lineSeparator())
                    .append(System.lineSeparator())
                    .append("其余 **").append(exams.size() - limitedExams.size()).append(" 条** 请在考试列表中查看。");
        }
        return reply.toString();
    }

    private String describeClass(Object rawClass) {
        if (rawClass instanceof TeacherClassDTO classDto) {
            return describeClassFields(classDto.getClassName(), classDto.getYear(), classDto.getStudentCount());
        }
        if (!(rawClass instanceof Map<?, ?> classMap)) {
            return stringValue(rawClass);
        }
        String className = firstNonBlank(classMap.get("className"), classMap.get("name"));
        String year = stringValue(classMap.get("year"));
        String studentCount = stringValue(classMap.get("studentCount"));
        return describeClassFields(className, year, studentCount);
    }

    private String describeAssignment(Object rawAssignment) {
        if (rawAssignment instanceof AssignmentStudentScoreDTO assignment) {
            return describeWorkItem(
                    assignment.getTitle(),
                    assignment.getCourseName(),
                    assignment.getScore(),
                    assignment.getTotalScore(),
                    firstNonBlank(assignment.getSubmitDate(), assignment.getCompletedAt()),
                    assignment.getRelatedId(),
                    "作业ID"
            );
        }
        if (!(rawAssignment instanceof Map<?, ?> assignmentMap)) {
            return stringValue(rawAssignment);
        }
        return describeWorkItem(
                firstNonBlank(assignmentMap.get("title"), assignmentMap.get("assignmentTitle")),
                stringValue(assignmentMap.get("courseName")),
                assignmentMap.get("score"),
                assignmentMap.get("totalScore"),
                firstNonBlank(assignmentMap.get("submitDate"), assignmentMap.get("completedAt")),
                firstNonBlank(assignmentMap.get("relatedId"), assignmentMap.get("assignmentId"), assignmentMap.get("id")),
                "作业ID"
        );
    }

    private String describeExam(Object rawExam) {
        if (rawExam instanceof StudentScoreDTO exam) {
            return describeWorkItem(
                    exam.getTitle(),
                    exam.getCourseName(),
                    exam.getScore(),
                    exam.getTotalScore(),
                    exam.getCompletedAt(),
                    exam.getRelatedId(),
                    "考试ID"
            );
        }
        if (!(rawExam instanceof Map<?, ?> examMap)) {
            return stringValue(rawExam);
        }
        return describeWorkItem(
                firstNonBlank(examMap.get("title"), examMap.get("examName"), examMap.get("name")),
                stringValue(examMap.get("courseName")),
                examMap.get("score"),
                firstNonBlank(examMap.get("totalScore"), examMap.get("total")),
                firstNonBlank(examMap.get("submitDate"), examMap.get("completedAt"), examMap.get("examDate")),
                firstNonBlank(examMap.get("relatedId"), examMap.get("examId"), examMap.get("id")),
                "考试ID"
        );
    }

    private String describeExamSchedule(int index, Object rawExam) {
        String title;
        String courseName;
        String startTime;
        String endTime;
        String status;

        if (rawExam instanceof ExamDTO exam) {
            title = exam.getTitle();
            courseName = exam.getCourseName();
            startTime = exam.getStartTime();
            endTime = exam.getEndTime();
            status = exam.getStatus();
        } else if (rawExam instanceof Map<?, ?> examMap) {
            title = firstNonBlank(examMap.get("title"), examMap.get("examName"), examMap.get("name"));
            courseName = stringValue(examMap.get("courseName"));
            startTime = firstNonBlank(examMap.get("startTime"), examMap.get("publishDate"), examMap.get("examDate"));
            endTime = stringValue(examMap.get("endTime"));
            status = stringValue(examMap.get("status"));
        } else {
            String rawValue = stringValue(rawExam);
            if (rawValue == null) {
                return null;
            }
            return index + ". **" + rawValue + "**";
        }

        StringBuilder builder = new StringBuilder();
        builder.append(index).append(". **").append(title == null ? "未命名考试" : title).append("**");

        String timeText = formatExamTime(startTime, endTime);
        if (timeText != null) {
            builder.append(System.lineSeparator()).append("   - 考试时间：").append(timeText);
        }
        if (courseName != null) {
            builder.append(System.lineSeparator()).append("   - 所属课程：").append(courseName);
        }
        if (status != null) {
            builder.append(System.lineSeparator()).append("   - 考试状态：").append(status);
        }
        return builder.toString();
    }

    private String formatExamTime(String startTime, String endTime) {
        if (startTime != null && endTime != null) {
            return startTime + " 至 " + endTime;
        }
        return firstNonBlank(startTime, endTime);
    }

    private String describeWorkItem(Object titleValue,
                                    Object courseNameValue,
                                    Object scoreValue,
                                    Object totalScoreValue,
                                    Object submitDateValue,
                                    Object relatedIdValue,
                                    String idLabel) {
        String title = stringValue(titleValue);
        String courseName = stringValue(courseNameValue);
        String score = stringValue(scoreValue);
        String totalScore = stringValue(totalScoreValue);
        String submitDate = stringValue(submitDateValue);
        String relatedId = stringValue(relatedIdValue);

        StringBuilder line = new StringBuilder("- ");
        line.append(title != null ? title : "未命名记录");
        if (courseName != null) {
            line.append("（").append(courseName).append("）");
        }
        if (score != null) {
            line.append("，得分 ").append(score);
            if (totalScore != null) {
                line.append("/").append(totalScore);
            }
        } else if (totalScore != null) {
            line.append("，总分 ").append(totalScore);
        }
        if (submitDate != null) {
            line.append("，时间 ").append(submitDate);
        }
        if (relatedId != null) {
            line.append("，").append(idLabel).append(" ").append(relatedId);
        }
        return line.toString();
    }

    private String describeClassFields(String className, String year, Object studentCountValue) {
        String studentCount = stringValue(studentCountValue);
        StringBuilder line = new StringBuilder("- ");
        line.append(className != null ? className : "未命名班级");
        if (year != null) {
            line.append("（").append(year).append("级）");
        }
        if (studentCount != null) {
            line.append("，").append(studentCount).append(" 人");
        }
        return line.toString();
    }

    private String firstNonBlank(Object... values) {
        for (Object value : values) {
            String text = stringValue(value);
            if (text != null) {
                return text;
            }
        }
        return null;
    }

    private String stringValue(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        return String.valueOf(value);
    }

    private String defaultIfBlank(Object value, String fallback) {
        String text = stringValue(value);
        return text == null ? fallback : text;
    }

    private String chineseWeekday(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "星期一";
            case TUESDAY -> "星期二";
            case WEDNESDAY -> "星期三";
            case THURSDAY -> "星期四";
            case FRIDAY -> "星期五";
            case SATURDAY -> "星期六";
            case SUNDAY -> "星期日";
        };
    }
}
