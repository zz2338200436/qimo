package com._202510007517.platform.agent.service;

import com._202510007517.platform.agent.model.AgentIntent;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class AgentConfirmationPolicyTest {

    private final AgentConfirmationPolicy policy = new AgentConfirmationPolicy(new AgentRiskPolicy());

    @ParameterizedTest
    @MethodSource("confirmationMappings")
    void classifiesEveryIntentConfirmationRequirement(AgentIntent intent, boolean expectedRequiresConfirmation) {
        assertThat(policy.requiresConfirmation(intent)).isEqualTo(expectedRequiresConfirmation);
    }

    @ParameterizedTest
    @MethodSource("secondConfirmationMappings")
    void requiresSecondConfirmationOnlyForCriticalActions(AgentIntent intent, boolean expectedRequiresSecondConfirmation) {
        assertThat(policy.requiresSecondConfirmation(intent)).isEqualTo(expectedRequiresSecondConfirmation);
    }

    private static Stream<Arguments> confirmationMappings() {
        return Stream.of(
                Arguments.of(AgentIntent.PUBLISH_ASSIGNMENT, true),
                Arguments.of(AgentIntent.UPDATE_ASSIGNMENT, true),
                Arguments.of(AgentIntent.DELETE_ASSIGNMENT, true),
                Arguments.of(AgentIntent.GRADE_ASSIGNMENT, true),
                Arguments.of(AgentIntent.PUBLISH_EXAM, true),
                Arguments.of(AgentIntent.UPDATE_EXAM, true),
                Arguments.of(AgentIntent.DELETE_EXAM, true),
                Arguments.of(AgentIntent.GRADE_EXAM, true),
                Arguments.of(AgentIntent.SUBMIT_ASSIGNMENT, true),
                Arguments.of(AgentIntent.SUBMIT_EXAM, true),
                Arguments.of(AgentIntent.QUERY_PENDING_ASSIGNMENTS, false),
                Arguments.of(AgentIntent.QUERY_COURSES, false),
                Arguments.of(AgentIntent.QUERY_COURSE_DETAIL, false),
                Arguments.of(AgentIntent.QUERY_CLASSES, false),
                Arguments.of(AgentIntent.QUERY_CLASS_DETAIL, false),
                Arguments.of(AgentIntent.QUERY_ASSIGNMENTS, false),
                Arguments.of(AgentIntent.QUERY_ASSIGNMENT_DETAIL, false),
                Arguments.of(AgentIntent.QUERY_ASSIGNMENT_SUBMISSIONS, false),
                Arguments.of(AgentIntent.QUERY_EXAMS, false),
                Arguments.of(AgentIntent.QUERY_EXAM_DETAIL, false),
                Arguments.of(AgentIntent.QUERY_EXAM_SUBMISSIONS, false),
                Arguments.of(AgentIntent.QUERY_SCORES, false),
                Arguments.of(AgentIntent.QUERY_NOTIFICATIONS, false),
                Arguments.of(AgentIntent.QUERY_UNREAD_NOTIFICATION_COUNT, false),
                Arguments.of(AgentIntent.MARK_ALL_NOTIFICATIONS_READ, true),
                Arguments.of(AgentIntent.MARK_NOTIFICATION_READ, true),
                Arguments.of(AgentIntent.DELETE_NOTIFICATION, true),
                Arguments.of(AgentIntent.DELETE_ALL_READ_NOTIFICATIONS, true),
                Arguments.of(AgentIntent.SEND_NOTIFICATION, true),
                Arguments.of(AgentIntent.SEND_BATCH_NOTIFICATION, true),
                Arguments.of(AgentIntent.QUERY_STUDENT_STATS, false),
                Arguments.of(AgentIntent.QUERY_STUDY_TIME_DISTRIBUTION, false),
                Arguments.of(AgentIntent.QUERY_TEACHER_DASHBOARD, false),
                Arguments.of(AgentIntent.QUERY_LEARNING_SUMMARY, false),
                Arguments.of(AgentIntent.QUERY_SCORE_TREND, false),
                Arguments.of(AgentIntent.QUERY_KNOWLEDGE_MASTERY, false),
                Arguments.of(AgentIntent.QUERY_RAG_KNOWLEDGE, false),
                Arguments.of(AgentIntent.GENERATE_QUESTIONS, false),
                Arguments.of(AgentIntent.GENERATE_EXAM, true),
                Arguments.of(AgentIntent.GENERATE_LEARNING_SUGGESTIONS, false),
                Arguments.of(AgentIntent.UNKNOWN, false)
        );
    }

    private static Stream<Arguments> secondConfirmationMappings() {
        return confirmationMappings()
                .map(argument -> Arguments.of(
                        argument.get()[0],
                        argument.get()[0] == AgentIntent.DELETE_ASSIGNMENT
                                || argument.get()[0] == AgentIntent.DELETE_EXAM
                                || argument.get()[0] == AgentIntent.DELETE_NOTIFICATION
                                || argument.get()[0] == AgentIntent.DELETE_ALL_READ_NOTIFICATIONS
                                || argument.get()[0] == AgentIntent.SEND_BATCH_NOTIFICATION
                ));
    }
}
