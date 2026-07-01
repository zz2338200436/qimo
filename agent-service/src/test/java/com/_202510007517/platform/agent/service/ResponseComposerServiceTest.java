package com._202510007517.platform.agent.service;

import com._202510007517.platform.agent.model.PlannerDecision;
import com._202510007517.platform.exam.api.dto.ExamDTO;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ResponseComposerServiceTest {

    private final ResponseComposerService service = new ResponseComposerService();

    @Test
    void buildsHumanReadableSuccessReplyFromToolResult() {
        String reply = service.composeToolReply(
                PlannerDecision.toolCall("publish_assignment", Map.of("className", "云计算技术1班"),
                        "latest_generated_questions", "publish generated questions"),
                Map.of(
                        "status", "EXECUTED",
                        "title", "Java课堂练习",
                        "className", "云计算技术1班",
                        "questionCount", 5
                ),
                List.of()
        );

        assertThat(reply).contains("云计算技术1班").contains("Java课堂练习");
    }

    @Test
    void keepsStructuredFailureReasonVisible() {
        String reply = service.composeToolReply(
                PlannerDecision.toolCall("publish_assignment", Map.of("className", "云计算技术1班"),
                        "latest_generated_questions", "publish generated questions"),
                Map.of(
                        "status", "FAILED",
                        "message", "未找到班级“云计算技术1班”"
                ),
                List.of()
        );

        assertThat(reply).contains("未找到班级");
    }

    @Test
    void fallsBackWhenToolMessageIsNull() {
        Map<String, Object> toolResult = new LinkedHashMap<>();
        toolResult.put("status", "EXECUTED");
        toolResult.put("message", null);
        toolResult.put("count", 5);

        String reply = service.composeToolReply(
                PlannerDecision.toolCall("generate_questions", Map.of(), null, "generate questions"),
                toolResult,
                List.of()
        );

        assertThat(reply).isEqualTo("操作已处理。");
    }

    @Test
    void rendersClassNamesWhenQueryClassesReturnsOnlyStructuredData() {
        Map<String, Object> classOne = new LinkedHashMap<>();
        classOne.put("id", 11L);
        classOne.put("className", "软件 2301");
        classOne.put("studentCount", 32);

        Map<String, Object> classTwo = new LinkedHashMap<>();
        classTwo.put("id", 12L);
        classTwo.put("className", "软件 2302");
        classTwo.put("studentCount", 30);

        String reply = service.composeToolReply(
                PlannerDecision.toolCall("query_classes", Map.of(), null, "query classes"),
                Map.of(
                        "status", "EXECUTED",
                        "classes", new ArrayList<>(List.of(classOne, classTwo))
                ),
                List.of()
        );

        assertThat(reply)
                .contains("软件 2301")
                .contains("软件 2302")
                .doesNotContain("操作已处理");
    }

    @Test
    void rendersAssignmentSummaryWhenQueryAssignmentsReturnsStudentScores() {
        Map<String, Object> assignmentOne = new LinkedHashMap<>();
        assignmentOne.put("relatedId", 101L);
        assignmentOne.put("title", "数据库作业一");
        assignmentOne.put("courseName", "数据库原理");
        assignmentOne.put("score", 92);
        assignmentOne.put("totalScore", 100);

        Map<String, Object> assignmentTwo = new LinkedHashMap<>();
        assignmentTwo.put("relatedId", 102L);
        assignmentTwo.put("title", "Java Web 作业");
        assignmentTwo.put("courseName", "Java Web");
        assignmentTwo.put("score", 88);
        assignmentTwo.put("totalScore", 100);

        String reply = service.composeToolReply(
                PlannerDecision.toolCall("query_assignments", Map.of(), null, "query assignments"),
                Map.of(
                        "status", "EXECUTED",
                        "assignments", new ArrayList<>(List.of(assignmentOne, assignmentTwo))
                ),
                List.of()
        );

        assertThat(reply)
                .contains("数据库作业一")
                .contains("Java Web 作业")
                .doesNotContain("操作已处理");
    }

    @Test
    void rendersExamSummaryWhenQueryExamsReturnsStudentScores() {
        Map<String, Object> examOne = new LinkedHashMap<>();
        examOne.put("relatedId", 201L);
        examOne.put("title", "数据库期中测试");
        examOne.put("courseName", "数据库原理");
        examOne.put("score", 86);
        examOne.put("totalScore", 100);

        Map<String, Object> examTwo = new LinkedHashMap<>();
        examTwo.put("relatedId", 202L);
        examTwo.put("title", "Java Web 阶段测验");
        examTwo.put("courseName", "Java Web");
        examTwo.put("score", 91);
        examTwo.put("totalScore", 100);

        String reply = service.composeToolReply(
                PlannerDecision.toolCall("query_exams", Map.of(), null, "query exams"),
                Map.of(
                        "status", "EXECUTED",
                        "exams", new ArrayList<>(List.of(examOne, examTwo))
                ),
                List.of()
        );

        assertThat(reply)
                .contains("数据库期中测试")
                .contains("Java Web 阶段测验")
                .doesNotContain("操作已处理");
    }

    @Test
    void rendersExamScheduleMarkdownWhenQueryExamsReturnsExamDtos() {
        List<ExamDTO> exams = new ArrayList<>();
        for (int i = 1; i <= 11; i++) {
            ExamDTO exam = new ExamDTO();
            exam.setTitle("考试 " + i);
            exam.setCourseName("课程 " + i);
            exam.setStartTime("2026-07-" + String.format("%02d", i) + " 09:00");
            exam.setEndTime("2026-07-" + String.format("%02d", i) + " 11:00");
            exam.setStatus("待参加");
            exams.add(exam);
        }

        String reply = service.composeToolReply(
                PlannerDecision.toolCall("query_exams", Map.of(), null, "query exams"),
                Map.of(
                        "status", "EXECUTED",
                        "exams", exams
                ),
                List.of()
        );

        assertThat(reply)
                .contains("查到以下考试安排信息，共 **11 条记录**。")
                .contains("先为你展示前 **10 条**：")
                .contains("1. **考试 1**")
                .contains("- 考试时间：2026-07-01 09:00 至 2026-07-01 11:00")
                .contains("- 所属课程：课程 1")
                .contains("- 考试状态：待参加")
                .contains("10. **考试 10**")
                .contains("其余 **1 条** 请在考试列表中查看。")
                .doesNotContain("ExamDTO@");
    }

    @Test
    void rendersInternetSearchResultsWithCleanTitlesSnippetsAndUrls() {
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("title", "Spring Cloud Gateway");
        source.put("url", "https://spring.io/projects/spring-cloud-gateway");
        source.put("snippet", "This project provides a library for building an API Gateway on top of Spring.");

        String reply = service.composeToolReply(
                PlannerDecision.toolCall("internet_search", Map.of("query", "Spring Cloud Gateway"), null, "internet search"),
                Map.of(
                        "status", "EXECUTED",
                        "query", "Spring Cloud Gateway",
                        "results", new ArrayList<>(List.of(source)),
                        "message", "联网搜索完成。"
                ),
                List.of()
        );

        assertThat(reply)
                .contains("Spring Cloud Gateway：This project provides a library for building an API Gateway on top of Spring.")
                .contains("**来源（Spring Cloud Gateway）**")
                .contains("1. [Spring Cloud Gateway](https://spring.io/projects/spring-cloud-gateway)")
                .contains("   - 摘要：This project provides a library")
                .doesNotContain("搜索结果里提到")
                .doesNotContain("解释：")
                .doesNotContain("链接：https://spring.io/projects/spring-cloud-gateway");
    }

    @Test
    void internetSearchReplyAnswersLatestVersionBeforeListingSources() {
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("title", "Spring Boot 3.5.7");
        source.put("url", "https://spring.io/projects/spring-boot");
        source.put("snippet", "Spring Boot 3.5.7 is the latest stable line on the official project page.");

        String reply = service.composeToolReply(
                PlannerDecision.toolCall("internet_search", Map.of("query", "Spring Boot最新版本是多少"), null, "internet search"),
                Map.of(
                        "status", "EXECUTED",
                        "query", "Spring Boot最新版本是多少",
                        "results", new ArrayList<>(List.of(source)),
                        "message", "联网搜索完成。"
                ),
                List.of()
        );

        assertThat(reply)
                .contains("Spring Boot最新版本看起来是 3.5.7。")
                .contains("**来源（Spring Boot最新版本是多少）**")
                .contains("1. [Spring Boot 3.5.7](https://spring.io/projects/spring-boot)")
                .doesNotContain("解释：");
    }

    @Test
    void internetSearchReplySummarizesCookingStyleQueriesBeforeListingSources() {
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("title", "西红柿炒鸡蛋做法");
        source.put("url", "https://example.com/tomato-egg");
        source.put("snippet", "先把鸡蛋打散炒至凝固盛出，再下番茄炒出汁水，加入盐和少许糖调味，最后倒回鸡蛋翻炒均匀即可。");

        String reply = service.composeToolReply(
                PlannerDecision.toolCall("internet_search", Map.of("query", "西红柿炒鸡蛋怎么做"), null, "internet search"),
                Map.of(
                        "status", "EXECUTED",
                        "query", "西红柿炒鸡蛋怎么做",
                        "results", new ArrayList<>(List.of(source)),
                        "message", "联网搜索完成。"
                ),
                List.of()
        );

        assertThat(reply)
                .contains("西红柿炒鸡蛋一般可以")
                .contains("先把鸡蛋打散炒至凝固盛出")
                .contains("最后倒回鸡蛋翻炒均匀即可")
                .contains("**来源（西红柿炒鸡蛋怎么做）**")
                .contains("1. [西红柿炒鸡蛋做法](https://example.com/tomato-egg)")
                .contains("   - 摘要：先把鸡蛋打散炒至凝固盛出")
                .doesNotContain("解释：");
    }

    @Test
    void internetSearchReplyAnswersTodayWeekdayDirectlyBeforeListingSources() {
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("title", "今天星期几");
        source.put("url", "https://example.com/calendar");
        source.put("snippet", "日历网主要提供在线日历查询、农历、节日、节气等内容。");

        String reply = service.composeToolReply(
                PlannerDecision.toolCall("internet_search", Map.of("query", "今天星期几"), null, "internet search"),
                Map.of(
                        "status", "EXECUTED",
                        "query", "今天星期几",
                        "results", new ArrayList<>(List.of(source)),
                        "message", "联网搜索完成。"
                ),
                List.of()
        );

        assertThat(reply)
                .contains("今天是" + chineseWeekday(LocalDate.now().getDayOfWeek()) + "。")
                .contains("**来源（今天星期几）**")
                .contains("1. [今天星期几](https://example.com/calendar)")
                .doesNotContain("搜索结果里提到")
                .doesNotContain("解释：");
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
