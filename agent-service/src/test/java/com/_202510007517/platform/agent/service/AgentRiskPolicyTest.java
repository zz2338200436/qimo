package com._202510007517.platform.agent.service;

import com._202510007517.platform.agent.model.AgentIntent;
import com._202510007517.platform.agent.model.AgentRiskLevel;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class AgentRiskPolicyTest {

    private final AgentRiskPolicy policy = new AgentRiskPolicy();

    @ParameterizedTest
    @MethodSource("riskMappings")
    void classifiesEveryIntentRiskLevel(AgentIntent intent, AgentRiskLevel expectedRiskLevel) {
        assertThat(policy.riskLevel(intent)).isEqualTo(expectedRiskLevel);
    }

    @ParameterizedTest
    @MethodSource("riskMappings")
    void requiresSecondConfirmationOnlyForCriticalRisk(AgentIntent intent, AgentRiskLevel expectedRiskLevel) {
        assertThat(policy.requiresSecondConfirmation(intent))
                .isEqualTo(expectedRiskLevel == AgentRiskLevel.CRITICAL);
    }

    private static Stream<Arguments> riskMappings() {
        return Stream.of(
                Arguments.of(AgentIntent.PUBLISH_ASSIGNMENT, AgentRiskLevel.MEDIUM),
                Arguments.of(AgentIntent.UPDATE_ASSIGNMENT, AgentRiskLevel.HIGH),
                Arguments.of(AgentIntent.DELETE_ASSIGNMENT, AgentRiskLevel.CRITICAL),
                Arguments.of(AgentIntent.GRADE_ASSIGNMENT, AgentRiskLevel.HIGH),
                Arguments.of(AgentIntent.PUBLISH_EXAM, AgentRiskLevel.MEDIUM),
                Arguments.of(AgentIntent.UPDATE_EXAM, AgentRiskLevel.HIGH),
                Arguments.of(AgentIntent.DELETE_EXAM, AgentRiskLevel.CRITICAL),
                Arguments.of(AgentIntent.GRADE_EXAM, AgentRiskLevel.HIGH),
                Arguments.of(AgentIntent.SUBMIT_ASSIGNMENT, AgentRiskLevel.MEDIUM),
                Arguments.of(AgentIntent.SUBMIT_EXAM, AgentRiskLevel.MEDIUM),
                Arguments.of(AgentIntent.QUERY_PENDING_ASSIGNMENTS, AgentRiskLevel.LOW),
                Arguments.of(AgentIntent.QUERY_COURSES, AgentRiskLevel.LOW),
                Arguments.of(AgentIntent.QUERY_COURSE_DETAIL, AgentRiskLevel.LOW),
                Arguments.of(AgentIntent.QUERY_CLASSES, AgentRiskLevel.LOW),
                Arguments.of(AgentIntent.QUERY_CLASS_DETAIL, AgentRiskLevel.LOW),
                Arguments.of(AgentIntent.QUERY_ASSIGNMENTS, AgentRiskLevel.LOW),
                Arguments.of(AgentIntent.QUERY_ASSIGNMENT_DETAIL, AgentRiskLevel.LOW),
                Arguments.of(AgentIntent.QUERY_ASSIGNMENT_SUBMISSIONS, AgentRiskLevel.LOW),
                Arguments.of(AgentIntent.QUERY_EXAMS, AgentRiskLevel.LOW),
                Arguments.of(AgentIntent.QUERY_EXAM_DETAIL, AgentRiskLevel.LOW),
                Arguments.of(AgentIntent.QUERY_EXAM_SUBMISSIONS, AgentRiskLevel.LOW),
                Arguments.of(AgentIntent.QUERY_SCORES, AgentRiskLevel.LOW),
                Arguments.of(AgentIntent.QUERY_NOTIFICATIONS, AgentRiskLevel.LOW),
                Arguments.of(AgentIntent.QUERY_UNREAD_NOTIFICATION_COUNT, AgentRiskLevel.LOW),
                Arguments.of(AgentIntent.MARK_ALL_NOTIFICATIONS_READ, AgentRiskLevel.MEDIUM),
                Arguments.of(AgentIntent.MARK_NOTIFICATION_READ, AgentRiskLevel.MEDIUM),
                Arguments.of(AgentIntent.DELETE_NOTIFICATION, AgentRiskLevel.CRITICAL),
                Arguments.of(AgentIntent.DELETE_ALL_READ_NOTIFICATIONS, AgentRiskLevel.CRITICAL),
                Arguments.of(AgentIntent.SEND_NOTIFICATION, AgentRiskLevel.MEDIUM),
                Arguments.of(AgentIntent.SEND_BATCH_NOTIFICATION, AgentRiskLevel.CRITICAL),
                Arguments.of(AgentIntent.QUERY_STUDENT_STATS, AgentRiskLevel.LOW),
                Arguments.of(AgentIntent.QUERY_STUDY_TIME_DISTRIBUTION, AgentRiskLevel.LOW),
                Arguments.of(AgentIntent.QUERY_TEACHER_DASHBOARD, AgentRiskLevel.LOW),
                Arguments.of(AgentIntent.QUERY_LEARNING_SUMMARY, AgentRiskLevel.LOW),
                Arguments.of(AgentIntent.QUERY_SCORE_TREND, AgentRiskLevel.LOW),
                Arguments.of(AgentIntent.QUERY_KNOWLEDGE_MASTERY, AgentRiskLevel.LOW),
                Arguments.of(AgentIntent.QUERY_RAG_KNOWLEDGE, AgentRiskLevel.LOW),
                Arguments.of(AgentIntent.GENERATE_QUESTIONS, AgentRiskLevel.LOW),
                Arguments.of(AgentIntent.GENERATE_EXAM, AgentRiskLevel.MEDIUM),
                Arguments.of(AgentIntent.GENERATE_LEARNING_SUGGESTIONS, AgentRiskLevel.LOW),
                Arguments.of(AgentIntent.UNKNOWN, AgentRiskLevel.LOW)
        );
    }
}
