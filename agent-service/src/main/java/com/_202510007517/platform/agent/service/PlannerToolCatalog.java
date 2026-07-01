package com._202510007517.platform.agent.service;

import com._202510007517.platform.agent.model.AgentIntent;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class PlannerToolCatalog {

    private final Map<String, AgentIntent> toolToIntent = new LinkedHashMap<>();
    private final Map<AgentIntent, String> intentToTool = new EnumMap<>(AgentIntent.class);

    public PlannerToolCatalog() {
        register("publish_assignment", AgentIntent.PUBLISH_ASSIGNMENT);
        register("update_assignment", AgentIntent.UPDATE_ASSIGNMENT);
        register("delete_assignment", AgentIntent.DELETE_ASSIGNMENT);
        register("grade_assignment", AgentIntent.GRADE_ASSIGNMENT);
        register("generate_questions", AgentIntent.GENERATE_QUESTIONS);
        register("generate_exam", AgentIntent.GENERATE_EXAM);
        register("generate_learning_suggestions", AgentIntent.GENERATE_LEARNING_SUGGESTIONS);
        register("query_question_bank", AgentIntent.QUERY_QUESTION_BANK);
        register("publish_exam", AgentIntent.PUBLISH_EXAM);
        register("update_exam", AgentIntent.UPDATE_EXAM);
        register("delete_exam", AgentIntent.DELETE_EXAM);
        register("grade_exam", AgentIntent.GRADE_EXAM);
        register("submit_assignment", AgentIntent.SUBMIT_ASSIGNMENT);
        register("submit_exam", AgentIntent.SUBMIT_EXAM);
        register("query_pending_assignments", AgentIntent.QUERY_PENDING_ASSIGNMENTS);
        register("create_course", AgentIntent.CREATE_COURSE);
        register("update_course", AgentIntent.UPDATE_COURSE);
        register("delete_course", AgentIntent.DELETE_COURSE);
        register("create_class", AgentIntent.CREATE_CLASS);
        register("query_courses", AgentIntent.QUERY_COURSES);
        register("query_course_detail", AgentIntent.QUERY_COURSE_DETAIL);
        register("query_classes", AgentIntent.QUERY_CLASSES);
        register("query_class_detail", AgentIntent.QUERY_CLASS_DETAIL);
        register("query_assignments", AgentIntent.QUERY_ASSIGNMENTS);
        register("query_assignment_detail", AgentIntent.QUERY_ASSIGNMENT_DETAIL);
        register("query_assignment_submissions", AgentIntent.QUERY_ASSIGNMENT_SUBMISSIONS);
        register("query_exams", AgentIntent.QUERY_EXAMS);
        register("query_exam_detail", AgentIntent.QUERY_EXAM_DETAIL);
        register("query_exam_submissions", AgentIntent.QUERY_EXAM_SUBMISSIONS);
        register("query_scores", AgentIntent.QUERY_SCORES);
        register("query_notifications", AgentIntent.QUERY_NOTIFICATIONS);
        register("query_unread_notification_count", AgentIntent.QUERY_UNREAD_NOTIFICATION_COUNT);
        register("mark_all_notifications_read", AgentIntent.MARK_ALL_NOTIFICATIONS_READ);
        register("mark_notification_read", AgentIntent.MARK_NOTIFICATION_READ);
        register("delete_notification", AgentIntent.DELETE_NOTIFICATION);
        register("delete_all_read_notifications", AgentIntent.DELETE_ALL_READ_NOTIFICATIONS);
        register("send_notification", AgentIntent.SEND_NOTIFICATION);
        register("send_batch_notification", AgentIntent.SEND_BATCH_NOTIFICATION);
        register("query_student_stats", AgentIntent.QUERY_STUDENT_STATS);
        register("query_study_time_distribution", AgentIntent.QUERY_STUDY_TIME_DISTRIBUTION);
        register("query_teacher_dashboard", AgentIntent.QUERY_TEACHER_DASHBOARD);
        register("query_learning_summary", AgentIntent.QUERY_LEARNING_SUMMARY);
        register("query_score_trend", AgentIntent.QUERY_SCORE_TREND);
        register("query_early_warnings", AgentIntent.QUERY_EARLY_WARNINGS);
        register("query_knowledge_points", AgentIntent.QUERY_KNOWLEDGE_POINTS);
        register("query_knowledge_mastery", AgentIntent.QUERY_KNOWLEDGE_MASTERY);
        register("query_rag_knowledge", AgentIntent.QUERY_RAG_KNOWLEDGE);
        register("internet_search", AgentIntent.INTERNET_SEARCH);
        register("read_web_page", AgentIntent.READ_WEB_PAGE);
    }

    public AgentIntent resolve(String toolName) {
        AgentIntent intent = toolToIntent.get(toolName);
        if (intent == null) {
            throw new IllegalArgumentException("No planner tool registered for name: " + toolName);
        }
        return intent;
    }

    public String reverseResolve(AgentIntent intent) {
        return intentToTool.getOrDefault(intent, intent.name().toLowerCase());
    }

    private void register(String toolName, AgentIntent intent) {
        toolToIntent.put(toolName, intent);
        intentToTool.put(intent, toolName);
    }
}
