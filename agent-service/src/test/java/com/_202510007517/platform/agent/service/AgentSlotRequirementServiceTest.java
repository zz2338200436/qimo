package com._202510007517.platform.agent.service;

import com._202510007517.platform.agent.model.AgentIntent;
import com._202510007517.platform.agent.model.RecognizedIntent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgentSlotRequirementServiceTest {

    private final AgentSlotRequirementService service = new AgentSlotRequirementService();

    @Test
    void doesNotReportUpstreamMissingKeyWhenSlotIsPresent() {
        RecognizedIntent intent = new RecognizedIntent(
                AgentIntent.PUBLISH_ASSIGNMENT,
                0.93,
                Map.of(
                        "courseName", "Java企业开发",
                        "title", "Spring Cloud实验",
                        "dueDate", "2026-06-13 22:00",
                        "maxScore", 100
                ),
                List.of("maxScore")
        );

        List<String> missingSlots = service.missingSlots(intent, "给Java企业开发课程发布作业，标题是Spring Cloud实验，截止今晚十点，满分100");

        assertThat(missingSlots).isEmpty();
    }

    @Test
    void doesNotReportUpstreamMissingLabelWhenEquivalentSlotIsPresent() {
        RecognizedIntent intent = new RecognizedIntent(
                AgentIntent.PUBLISH_ASSIGNMENT,
                0.93,
                Map.of(
                        "courseName", "Java企业开发",
                        "title", "Spring Cloud实验",
                        "dueDate", "2026-06-13 22:00",
                        "maxScore", 100
                ),
                List.of("满分")
        );

        List<String> missingSlots = service.missingSlots(intent, "给Java企业开发课程发布作业，标题是Spring Cloud实验，截止今晚十点，满分100");

        assertThat(missingSlots).isEmpty();
    }

    @Test
    void requiresNotificationIdForMarkNotificationRead() {
        RecognizedIntent intent = new RecognizedIntent(
                AgentIntent.MARK_NOTIFICATION_READ,
                0.9,
                Map.of(),
                List.of()
        );

        List<String> missingSlots = service.missingSlots(intent, "把这条通知标为已读");

        assertThat(missingSlots).containsExactly("notificationId");
    }

    @Test
    void requiresCourseNameCodeCreditAndTotalHoursForCreateCourse() {
        RecognizedIntent intent = new RecognizedIntent(
                AgentIntent.CREATE_COURSE,
                0.9,
                Map.of("courseName", "分布式框架技术", "courseCode", "DFT101"),
                List.of()
        );

        List<String> missingSlots = service.missingSlots(intent, "创建课程，课程名称是分布式框架技术，课程代码是DFT101");

        assertThat(missingSlots).containsExactly("学分", "总学时");
    }

    @Test
    void requiresCourseIdNameCodeCreditAndTotalHoursForUpdateCourse() {
        RecognizedIntent intent = new RecognizedIntent(
                AgentIntent.UPDATE_COURSE,
                0.9,
                Map.of("courseName", "高级分布式框架技术", "courseCode", "DFT201", "credit", 4),
                List.of()
        );

        List<String> missingSlots = service.missingSlots(intent, "更新课程，课程名称是高级分布式框架技术");

        assertThat(missingSlots).containsExactly("课程ID", "总学时");
    }

    @Test
    void requiresCourseIdForDeleteCourse() {
        RecognizedIntent intent = new RecognizedIntent(
                AgentIntent.DELETE_COURSE,
                0.9,
                Map.of(),
                List.of()
        );

        List<String> missingSlots = service.missingSlots(intent, "删除这门课程");

        assertThat(missingSlots).containsExactly("课程ID");
    }

    @Test
    void requiresClassNameYearAndCapacityForCreateClass() {
        RecognizedIntent intent = new RecognizedIntent(
                AgentIntent.CREATE_CLASS,
                0.9,
                Map.of("className", "软件2301"),
                List.of()
        );

        List<String> missingSlots = service.missingSlots(intent, "创建班级，班级名称是软件2301");

        assertThat(missingSlots).containsExactly("年级", "容量");
    }

    @Test
    void requiresNotificationIdForDeleteNotification() {
        RecognizedIntent intent = new RecognizedIntent(
                AgentIntent.DELETE_NOTIFICATION,
                0.9,
                Map.of(),
                List.of()
        );

        List<String> missingSlots = service.missingSlots(intent, "删除这条通知");

        assertThat(missingSlots).containsExactly("notificationId");
    }

    @Test
    void acceptsNotificationIdForDeleteNotification() {
        RecognizedIntent intent = new RecognizedIntent(
                AgentIntent.DELETE_NOTIFICATION,
                0.9,
                Map.of("notificationId", 9L),
                List.of("notificationId")
        );

        List<String> missingSlots = service.missingSlots(intent, "删除通知ID 9");

        assertThat(missingSlots).isEmpty();
    }

    @Test
    void requiresStudentTitleContentAndTypeForSendNotification() {
        RecognizedIntent intent = new RecognizedIntent(
                AgentIntent.SEND_NOTIFICATION,
                0.9,
                Map.of("studentId", 42L, "title", "开课通知"),
                List.of()
        );

        List<String> missingSlots = service.missingSlots(intent, "给学生ID 42发送通知，标题是开课通知");

        assertThat(missingSlots).containsExactly("content", "type");
    }

    @Test
    void requiresStudentIdsTitleContentAndTypeForSendBatchNotification() {
        RecognizedIntent intent = new RecognizedIntent(
                AgentIntent.SEND_BATCH_NOTIFICATION,
                0.9,
                Map.of("studentIds", List.of(42L, 43L), "title", "开课通知"),
                List.of()
        );

        List<String> missingSlots = service.missingSlots(intent, "给学生ID 42,43批量发送通知，标题是开课通知");

        assertThat(missingSlots).containsExactly("content", "type");
    }

    @Test
    void reportsInvalidNumericSlotShape() {
        RecognizedIntent intent = new RecognizedIntent(
                AgentIntent.DELETE_ASSIGNMENT,
                0.9,
                Map.of("assignmentId", "abc"),
                List.of()
        );

        List<String> missingSlots = service.missingSlots(intent, "删除作业abc");

        assertThat(missingSlots).containsExactly("作业ID格式不正确");
    }

    @Test
    void reportsInvalidBatchStudentIdsShape() {
        RecognizedIntent intent = new RecognizedIntent(
                AgentIntent.SEND_BATCH_NOTIFICATION,
                0.9,
                Map.of(
                        "studentIds", List.of(42L, "abc"),
                        "title", "开课通知",
                        "content", "请按时上课",
                        "type", "course"
                ),
                List.of()
        );

        List<String> missingSlots = service.missingSlots(intent, "给学生ID 42,abc批量发送通知");

        assertThat(missingSlots).containsExactly("studentIds格式不正确");
    }

    @Test
    void reportsInvalidExamAnswersShape() {
        RecognizedIntent intent = new RecognizedIntent(
                AgentIntent.SUBMIT_EXAM,
                0.9,
                Map.of("examId", 8L, "answers", "1:A,2:B"),
                List.of()
        );

        List<String> missingSlots = service.missingSlots(intent, "提交考试，答案是1:A,2:B");

        assertThat(missingSlots).containsExactly("答题内容格式不正确");
    }

    @Test
    void buildPromptExplainsHowToDisambiguateCourse() {
        String prompt = service.buildPrompt(AgentIntent.PUBLISH_ASSIGNMENT, List.of("更明确的课程"));

        assertThat(prompt).isEqualTo("还需要补充课程标识，例如课程名+学期+班级，或直接提供课程ID，我才能继续处理发布作业。");
    }

    @Test
    void buildPromptDeduplicatesEquivalentMissingLabels() {
        String prompt = service.buildPrompt(
                AgentIntent.PUBLISH_ASSIGNMENT,
                List.of("title", "maxScore", "标题", "满分", "截止时间"));

        assertThat(prompt).isEqualTo("还需要补充标题、满分、截止时间，我才能继续处理发布作业。");
    }
}
