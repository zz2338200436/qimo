package com._202510007517.platform.agent.tool;

import com._202510007517.platform.agent.model.AgentIntent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ToolRegistryTest {

    private final ToolRegistry registry = new ToolRegistry(List.of(
            new AssignmentPublishTool(null, null),
            new AssignmentUpdateTool(null),
            new AssignmentDeleteTool(null),
            new AssignmentGradeTool(null),
            new PendingAssignmentQueryTool(null),
            new AssignmentQueryTool(null),
            new AssignmentDetailLookupTool(null),
            new AssignmentSubmissionLookupTool(null),
            new AssignmentSubmitTool(null),
            new ExamPublishTool(null, null),
            new ExamUpdateTool(null),
            new ExamDeleteTool(null),
            new ExamGradeTool(null),
            new ExamQueryTool(null),
            new ExamDetailLookupTool(null),
            new ExamSubmissionLookupTool(null),
            new ExamSubmitTool(null),
            new ScoreQueryTool(null),
            new CourseCreateTool(null),
            new CourseUpdateTool(null),
            new CourseDeleteTool(null),
            new CourseQueryTool(null),
            new CourseDetailLookupTool(null, null),
            new ClassCreateTool(null),
            new ClassLookupTool(null),
            new ClassDetailLookupTool(null),
            new NotificationQueryTool(null),
            new NotificationUnreadCountTool(null),
            new NotificationMarkAllReadTool(null),
            new NotificationMarkReadTool(null),
            new NotificationDeleteTool(null),
            new NotificationDeleteAllReadTool(null),
            new TeacherSendNotificationTool(null),
            new TeacherSendBatchNotificationTool(null),
            new LearningStatsQueryTool(null),
            new StudyTimeDistributionQueryTool(null),
            new TeacherDashboardQueryTool(null),
            new LearningSummaryQueryTool(null),
            new ScoreTrendQueryTool(null),
            new KnowledgePointQueryTool(null),
            new KnowledgeMasteryQueryTool(null),
            new GenerateQuestionsTool(null),
            new GenerateExamTool(null),
            new GenerateLearningSuggestionsTool(null)
    ));

    @Test
    void resolvesAssignmentPublishToolByIntent() {
        AgentTool tool = registry.resolve(AgentIntent.PUBLISH_ASSIGNMENT);

        assertThat(tool.intent()).isEqualTo(AgentIntent.PUBLISH_ASSIGNMENT);
    }

    @Test
    void resolvesAssignmentSubmitToolByIntent() {
        AgentTool tool = registry.resolve(AgentIntent.SUBMIT_ASSIGNMENT);

        assertThat(tool.intent()).isEqualTo(AgentIntent.SUBMIT_ASSIGNMENT);
    }

    @Test
    void resolvesAssignmentUpdateToolByIntent() {
        AgentTool tool = registry.resolve(AgentIntent.UPDATE_ASSIGNMENT);

        assertThat(tool.intent()).isEqualTo(AgentIntent.UPDATE_ASSIGNMENT);
    }

    @Test
    void resolvesAssignmentDeleteToolByIntent() {
        AgentTool tool = registry.resolve(AgentIntent.DELETE_ASSIGNMENT);

        assertThat(tool.intent()).isEqualTo(AgentIntent.DELETE_ASSIGNMENT);
    }

    @Test
    void resolvesAssignmentGradeToolByIntent() {
        AgentTool tool = registry.resolve(AgentIntent.GRADE_ASSIGNMENT);

        assertThat(tool.intent()).isEqualTo(AgentIntent.GRADE_ASSIGNMENT);
    }

    @Test
    void resolvesPendingAssignmentQueryToolByIntent() {
        AgentTool tool = registry.resolve(AgentIntent.QUERY_PENDING_ASSIGNMENTS);

        assertThat(tool.intent()).isEqualTo(AgentIntent.QUERY_PENDING_ASSIGNMENTS);
    }

    @Test
    void resolvesAssignmentDetailLookupToolByIntent() {
        AgentTool tool = registry.resolve(AgentIntent.QUERY_ASSIGNMENT_DETAIL);

        assertThat(tool.intent()).isEqualTo(AgentIntent.QUERY_ASSIGNMENT_DETAIL);
    }

    @Test
    void resolvesAssignmentSubmissionLookupToolByIntent() {
        AgentTool tool = registry.resolve(AgentIntent.QUERY_ASSIGNMENT_SUBMISSIONS);

        assertThat(tool.intent()).isEqualTo(AgentIntent.QUERY_ASSIGNMENT_SUBMISSIONS);
    }

    @Test
    void resolvesAssignmentQueryToolByIntent() {
        AgentTool tool = registry.resolve(AgentIntent.QUERY_ASSIGNMENTS);

        assertThat(tool.intent()).isEqualTo(AgentIntent.QUERY_ASSIGNMENTS);
    }

    @Test
    void resolvesExamPublishToolByIntent() {
        AgentTool tool = registry.resolve(AgentIntent.PUBLISH_EXAM);

        assertThat(tool.intent()).isEqualTo(AgentIntent.PUBLISH_EXAM);
    }

    @Test
    void resolvesExamUpdateToolByIntent() {
        AgentTool tool = registry.resolve(AgentIntent.UPDATE_EXAM);

        assertThat(tool.intent()).isEqualTo(AgentIntent.UPDATE_EXAM);
    }

    @Test
    void resolvesExamDeleteToolByIntent() {
        AgentTool tool = registry.resolve(AgentIntent.DELETE_EXAM);

        assertThat(tool.intent()).isEqualTo(AgentIntent.DELETE_EXAM);
    }

    @Test
    void resolvesExamGradeToolByIntent() {
        AgentTool tool = registry.resolve(AgentIntent.GRADE_EXAM);

        assertThat(tool.intent()).isEqualTo(AgentIntent.GRADE_EXAM);
    }

    @Test
    void resolvesExamDetailLookupToolByIntent() {
        AgentTool tool = registry.resolve(AgentIntent.QUERY_EXAM_DETAIL);

        assertThat(tool.intent()).isEqualTo(AgentIntent.QUERY_EXAM_DETAIL);
    }

    @Test
    void resolvesExamSubmissionLookupToolByIntent() {
        AgentTool tool = registry.resolve(AgentIntent.QUERY_EXAM_SUBMISSIONS);

        assertThat(tool.intent()).isEqualTo(AgentIntent.QUERY_EXAM_SUBMISSIONS);
    }

    @Test
    void resolvesExamQueryToolByIntent() {
        AgentTool tool = registry.resolve(AgentIntent.QUERY_EXAMS);

        assertThat(tool.intent()).isEqualTo(AgentIntent.QUERY_EXAMS);
    }

    @Test
    void resolvesExamSubmitToolByIntent() {
        AgentTool tool = registry.resolve(AgentIntent.SUBMIT_EXAM);

        assertThat(tool.intent()).isEqualTo(AgentIntent.SUBMIT_EXAM);
    }

    @Test
    void resolvesScoreQueryToolByIntent() {
        AgentTool tool = registry.resolve(AgentIntent.QUERY_SCORES);

        assertThat(tool.intent()).isEqualTo(AgentIntent.QUERY_SCORES);
    }

    @Test
    void resolvesCourseDetailLookupToolByIntent() {
        AgentTool tool = registry.resolve(AgentIntent.QUERY_COURSE_DETAIL);

        assertThat(tool.intent()).isEqualTo(AgentIntent.QUERY_COURSE_DETAIL);
    }

    @Test
    void resolvesCourseQueryToolByIntent() {
        AgentTool tool = registry.resolve(AgentIntent.QUERY_COURSES);

        assertThat(tool.intent()).isEqualTo(AgentIntent.QUERY_COURSES);
    }

    @Test
    void resolvesCourseCreateToolByIntent() {
        AgentTool tool = registry.resolve(AgentIntent.CREATE_COURSE);

        assertThat(tool.intent()).isEqualTo(AgentIntent.CREATE_COURSE);
    }

    @Test
    void resolvesCourseUpdateToolByIntent() {
        AgentTool tool = registry.resolve(AgentIntent.UPDATE_COURSE);

        assertThat(tool.intent()).isEqualTo(AgentIntent.UPDATE_COURSE);
    }

    @Test
    void resolvesCourseDeleteToolByIntent() {
        AgentTool tool = registry.resolve(AgentIntent.DELETE_COURSE);

        assertThat(tool.intent()).isEqualTo(AgentIntent.DELETE_COURSE);
    }

    @Test
    void resolvesClassLookupToolByIntent() {
        AgentTool tool = registry.resolve(AgentIntent.QUERY_CLASSES);

        assertThat(tool.intent()).isEqualTo(AgentIntent.QUERY_CLASSES);
    }

    @Test
    void resolvesClassCreateToolByIntent() {
        AgentTool tool = registry.resolve(AgentIntent.CREATE_CLASS);

        assertThat(tool.intent()).isEqualTo(AgentIntent.CREATE_CLASS);
    }

    @Test
    void resolvesClassDetailLookupToolByIntent() {
        AgentTool tool = registry.resolve(AgentIntent.QUERY_CLASS_DETAIL);

        assertThat(tool.intent()).isEqualTo(AgentIntent.QUERY_CLASS_DETAIL);
    }

    @Test
    void resolvesNotificationQueryToolByIntent() {
        AgentTool tool = registry.resolve(AgentIntent.QUERY_NOTIFICATIONS);

        assertThat(tool.intent()).isEqualTo(AgentIntent.QUERY_NOTIFICATIONS);
    }

    @Test
    void resolvesNotificationUnreadCountToolByIntent() {
        AgentTool tool = registry.resolve(AgentIntent.QUERY_UNREAD_NOTIFICATION_COUNT);

        assertThat(tool.intent()).isEqualTo(AgentIntent.QUERY_UNREAD_NOTIFICATION_COUNT);
    }

    @Test
    void resolvesNotificationMarkAllReadToolByIntent() {
        AgentTool tool = registry.resolve(AgentIntent.MARK_ALL_NOTIFICATIONS_READ);

        assertThat(tool.intent()).isEqualTo(AgentIntent.MARK_ALL_NOTIFICATIONS_READ);
    }

    @Test
    void resolvesNotificationMarkReadToolByIntent() {
        AgentTool tool = registry.resolve(AgentIntent.MARK_NOTIFICATION_READ);

        assertThat(tool.intent()).isEqualTo(AgentIntent.MARK_NOTIFICATION_READ);
    }

    @Test
    void resolvesTeacherSendNotificationToolByIntent() {
        AgentTool tool = registry.resolve(AgentIntent.SEND_NOTIFICATION);

        assertThat(tool.intent()).isEqualTo(AgentIntent.SEND_NOTIFICATION);
    }

    @Test
    void resolvesTeacherSendBatchNotificationToolByIntent() {
        AgentTool tool = registry.resolve(AgentIntent.SEND_BATCH_NOTIFICATION);

        assertThat(tool.intent()).isEqualTo(AgentIntent.SEND_BATCH_NOTIFICATION);
    }

    @Test
    void resolvesNotificationDeleteToolByIntent() {
        AgentTool tool = registry.resolve(AgentIntent.DELETE_NOTIFICATION);

        assertThat(tool.intent()).isEqualTo(AgentIntent.DELETE_NOTIFICATION);
    }

    @Test
    void resolvesNotificationDeleteAllReadToolByIntent() {
        AgentTool tool = registry.resolve(AgentIntent.DELETE_ALL_READ_NOTIFICATIONS);

        assertThat(tool.intent()).isEqualTo(AgentIntent.DELETE_ALL_READ_NOTIFICATIONS);
    }

    @Test
    void resolvesLearningStatsQueryToolByIntent() {
        AgentTool tool = registry.resolve(AgentIntent.QUERY_STUDENT_STATS);

        assertThat(tool.intent()).isEqualTo(AgentIntent.QUERY_STUDENT_STATS);
    }

    @Test
    void resolvesStudyTimeDistributionQueryToolByIntent() {
        AgentTool tool = registry.resolve(AgentIntent.QUERY_STUDY_TIME_DISTRIBUTION);

        assertThat(tool.intent()).isEqualTo(AgentIntent.QUERY_STUDY_TIME_DISTRIBUTION);
    }

    @Test
    void resolvesTeacherDashboardQueryToolByIntent() {
        AgentTool tool = registry.resolve(AgentIntent.QUERY_TEACHER_DASHBOARD);

        assertThat(tool.intent()).isEqualTo(AgentIntent.QUERY_TEACHER_DASHBOARD);
    }

    @Test
    void resolvesLearningSummaryQueryToolByIntent() {
        AgentTool tool = registry.resolve(AgentIntent.QUERY_LEARNING_SUMMARY);

        assertThat(tool.intent()).isEqualTo(AgentIntent.QUERY_LEARNING_SUMMARY);
    }

    @Test
    void resolvesScoreTrendQueryToolByIntent() {
        AgentTool tool = registry.resolve(AgentIntent.QUERY_SCORE_TREND);

        assertThat(tool.intent()).isEqualTo(AgentIntent.QUERY_SCORE_TREND);
    }

    @Test
    void resolvesKnowledgeMasteryQueryToolByIntent() {
        AgentTool tool = registry.resolve(AgentIntent.QUERY_KNOWLEDGE_MASTERY);

        assertThat(tool.intent()).isEqualTo(AgentIntent.QUERY_KNOWLEDGE_MASTERY);
    }

    @Test
    void resolvesKnowledgePointQueryToolByIntent() {
        AgentTool tool = registry.resolve(AgentIntent.QUERY_KNOWLEDGE_POINTS);

        assertThat(tool.intent()).isEqualTo(AgentIntent.QUERY_KNOWLEDGE_POINTS);
    }

    @Test
    void resolvesGenerateQuestionsToolByIntent() {
        AgentTool tool = registry.resolve(AgentIntent.GENERATE_QUESTIONS);

        assertThat(tool.intent()).isEqualTo(AgentIntent.GENERATE_QUESTIONS);
    }

    @Test
    void resolvesGenerateExamToolByIntent() {
        AgentTool tool = registry.resolve(AgentIntent.GENERATE_EXAM);

        assertThat(tool.intent()).isEqualTo(AgentIntent.GENERATE_EXAM);
    }

    @Test
    void resolvesGenerateLearningSuggestionsToolByIntent() {
        AgentTool tool = registry.resolve(AgentIntent.GENERATE_LEARNING_SUGGESTIONS);

        assertThat(tool.intent()).isEqualTo(AgentIntent.GENERATE_LEARNING_SUGGESTIONS);
    }

    @Test
    void publishToolReportsMissingRequiredSlotsBeforeRemoteCall() {
        AssignmentPublishTool tool = new AssignmentPublishTool(null, null);

        Map<String, Object> result = tool.execute(7L, "TEACHER", Map.of("title", "Spring Cloud实验"));

        assertThat(result).containsEntry("status", "VALIDATION_FAILED");
    }
}
