package com._202510007517.platform.agent.service;

import com._202510007517.platform.agent.model.AgentIntent;
import com._202510007517.platform.agent.model.AgentRiskLevel;
import org.springframework.stereotype.Service;

@Service
public class AgentRiskPolicy {

    public AgentRiskLevel riskLevel(AgentIntent intent) {
        return switch (intent) {
            case PUBLISH_ASSIGNMENT, PUBLISH_EXAM, CREATE_COURSE, SUBMIT_ASSIGNMENT, SUBMIT_EXAM, MARK_ALL_NOTIFICATIONS_READ,
                    MARK_NOTIFICATION_READ, SEND_NOTIFICATION,
                    GENERATE_EXAM -> AgentRiskLevel.MEDIUM;
            case UPDATE_ASSIGNMENT, GRADE_ASSIGNMENT, UPDATE_COURSE, CREATE_CLASS, UPDATE_EXAM, GRADE_EXAM -> AgentRiskLevel.HIGH;
            case DELETE_ASSIGNMENT, DELETE_COURSE, DELETE_EXAM, DELETE_NOTIFICATION, DELETE_ALL_READ_NOTIFICATIONS,
                    SEND_BATCH_NOTIFICATION -> AgentRiskLevel.CRITICAL;
            case GENERATE_QUESTIONS, GENERATE_LEARNING_SUGGESTIONS -> AgentRiskLevel.LOW;
            case QUERY_PENDING_ASSIGNMENTS, QUERY_COURSES, QUERY_COURSE_DETAIL, QUERY_CLASSES,
                    QUERY_CLASS_DETAIL, QUERY_ASSIGNMENTS, QUERY_ASSIGNMENT_DETAIL,
                    QUERY_ASSIGNMENT_SUBMISSIONS, QUERY_EXAMS, QUERY_EXAM_DETAIL, QUERY_EXAM_SUBMISSIONS,
                    QUERY_SCORES, QUERY_NOTIFICATIONS, QUERY_UNREAD_NOTIFICATION_COUNT,
                    QUERY_STUDENT_STATS, QUERY_STUDY_TIME_DISTRIBUTION, QUERY_TEACHER_DASHBOARD,
                    QUERY_LEARNING_SUMMARY, QUERY_SCORE_TREND, QUERY_EARLY_WARNINGS,
                    QUERY_KNOWLEDGE_POINTS, QUERY_KNOWLEDGE_MASTERY,
                    QUERY_QUESTION_BANK, QUERY_RAG_KNOWLEDGE, INTERNET_SEARCH, READ_WEB_PAGE,
                    UNKNOWN -> AgentRiskLevel.LOW;
        };
    }

    public boolean requiresSecondConfirmation(AgentIntent intent) {
        return riskLevel(intent) == AgentRiskLevel.CRITICAL;
    }
}
