package com._202510007517.platform.agent.service;

import com._202510007517.platform.agent.model.AgentIntent;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

@Component
public class AgentPermissionPolicy {

    private static final Set<AgentIntent> TEACHER_INTENTS = EnumSet.of(
            AgentIntent.PUBLISH_ASSIGNMENT,
            AgentIntent.UPDATE_ASSIGNMENT,
            AgentIntent.DELETE_ASSIGNMENT,
            AgentIntent.GRADE_ASSIGNMENT,
            AgentIntent.QUERY_ASSIGNMENT_DETAIL,
            AgentIntent.QUERY_ASSIGNMENT_SUBMISSIONS,
            AgentIntent.PUBLISH_EXAM,
            AgentIntent.UPDATE_EXAM,
            AgentIntent.DELETE_EXAM,
            AgentIntent.GRADE_EXAM,
            AgentIntent.QUERY_EXAM_DETAIL,
            AgentIntent.QUERY_EXAM_SUBMISSIONS,
            AgentIntent.CREATE_COURSE,
            AgentIntent.UPDATE_COURSE,
            AgentIntent.DELETE_COURSE,
            AgentIntent.CREATE_CLASS,
            AgentIntent.QUERY_COURSES,
            AgentIntent.QUERY_COURSE_DETAIL,
            AgentIntent.QUERY_CLASSES,
            AgentIntent.QUERY_CLASS_DETAIL,
            AgentIntent.QUERY_TEACHER_DASHBOARD,
            AgentIntent.QUERY_LEARNING_SUMMARY,
            AgentIntent.QUERY_SCORE_TREND,
            AgentIntent.QUERY_KNOWLEDGE_POINTS,
            AgentIntent.QUERY_KNOWLEDGE_MASTERY,
            AgentIntent.QUERY_QUESTION_BANK,
            AgentIntent.QUERY_RAG_KNOWLEDGE,
            AgentIntent.GENERATE_QUESTIONS,
            AgentIntent.GENERATE_EXAM,
            AgentIntent.SEND_NOTIFICATION,
            AgentIntent.SEND_BATCH_NOTIFICATION
    );

    private static final Set<AgentIntent> STUDENT_INTENTS = EnumSet.of(
            AgentIntent.SUBMIT_ASSIGNMENT,
            AgentIntent.SUBMIT_EXAM,
            AgentIntent.QUERY_PENDING_ASSIGNMENTS,
            AgentIntent.QUERY_ASSIGNMENTS,
            AgentIntent.QUERY_EXAMS,
            AgentIntent.QUERY_SCORES,
            AgentIntent.QUERY_COURSE_DETAIL,
            AgentIntent.QUERY_NOTIFICATIONS,
            AgentIntent.QUERY_UNREAD_NOTIFICATION_COUNT,
            AgentIntent.MARK_ALL_NOTIFICATIONS_READ,
            AgentIntent.MARK_NOTIFICATION_READ,
            AgentIntent.DELETE_NOTIFICATION,
            AgentIntent.DELETE_ALL_READ_NOTIFICATIONS,
            AgentIntent.QUERY_STUDENT_STATS,
            AgentIntent.QUERY_STUDY_TIME_DISTRIBUTION,
            AgentIntent.QUERY_RAG_KNOWLEDGE,
            AgentIntent.GENERATE_LEARNING_SUGGESTIONS
    );

    public void assertAllowed(Long userId, String userRole, AgentIntent intent) {
        if (userId == null) {
            throw new SecurityException("缺少用户身份，无法执行 Agent 操作。");
        }
        if (!isAllowed(userRole, intent)) {
            throw new SecurityException("当前角色无权执行 Agent 操作: " + intent.name());
        }
    }

    private boolean isAllowed(String userRole, AgentIntent intent) {
        String normalizedRole = normalizeRole(userRole);
        if ("ADMIN".equals(normalizedRole)) {
            return intent != AgentIntent.UNKNOWN;
        }
        if ("TEACHER".equals(normalizedRole)) {
            return TEACHER_INTENTS.contains(intent);
        }
        if ("STUDENT".equals(normalizedRole)) {
            return STUDENT_INTENTS.contains(intent);
        }
        return false;
    }

    private static String normalizeRole(String userRole) {
        if (userRole == null || userRole.isBlank()) {
            return "";
        }
        String role = userRole.trim().toUpperCase(Locale.ROOT);
        if (role.startsWith("ROLE_")) {
            role = role.substring("ROLE_".length());
        }
        return role;
    }
}
