package com._202510007517.platform.agent.service;

import com._202510007517.platform.agent.domain.AgentAuditLogEntity;
import com._202510007517.platform.agent.model.AgentIntent;
import com._202510007517.platform.agent.repository.AgentAuditLogRepository;
import org.springframework.stereotype.Service;

@Service
public class AgentAuditService {
    private final AgentAuditLogRepository auditLogRepository;
    private final AgentDataMaskingPolicy dataMaskingPolicy;

    public AgentAuditService(AgentAuditLogRepository auditLogRepository,
                             AgentDataMaskingPolicy dataMaskingPolicy) {
        this.auditLogRepository = auditLogRepository;
        this.dataMaskingPolicy = dataMaskingPolicy;
    }

    public void recordActionExecution(Long actionId, Long userId, String userRole, AgentIntent intent,
                                      boolean success, String errorMessage) {
        AgentAuditLogEntity auditLog = new AgentAuditLogEntity();
        auditLog.setActionId(actionId);
        auditLog.setUserId(userId);
        auditLog.setUserRole(userRole);
        auditLog.setOperation(intent.name());
        auditLog.setTargetService(targetService(intent));
        auditLog.setTargetResource("agent_action:" + actionId);
        auditLog.setSuccess(success);
        auditLog.setErrorMessage(dataMaskingPolicy.maskText(errorMessage));
        auditLogRepository.save(auditLog);
    }

    String targetService(AgentIntent intent) {
        return switch (intent) {
            case PUBLISH_ASSIGNMENT, UPDATE_ASSIGNMENT, DELETE_ASSIGNMENT, GRADE_ASSIGNMENT, SUBMIT_ASSIGNMENT,
                    QUERY_PENDING_ASSIGNMENTS, QUERY_ASSIGNMENTS, QUERY_ASSIGNMENT_DETAIL,
                    QUERY_ASSIGNMENT_SUBMISSIONS -> "assignment-service";
            case CREATE_COURSE, UPDATE_COURSE, DELETE_COURSE, CREATE_CLASS, QUERY_COURSES, QUERY_COURSE_DETAIL,
                    QUERY_CLASSES, QUERY_CLASS_DETAIL, QUERY_KNOWLEDGE_POINTS -> "course-service";
            case PUBLISH_EXAM, UPDATE_EXAM, DELETE_EXAM, GRADE_EXAM, SUBMIT_EXAM, QUERY_EXAMS, QUERY_EXAM_DETAIL,
                    QUERY_EXAM_SUBMISSIONS, QUERY_SCORES -> "exam-service";
            case GENERATE_QUESTIONS, GENERATE_EXAM, GENERATE_LEARNING_SUGGESTIONS, QUERY_QUESTION_BANK -> "ai-service";
            case QUERY_NOTIFICATIONS, QUERY_UNREAD_NOTIFICATION_COUNT, MARK_ALL_NOTIFICATIONS_READ,
                    MARK_NOTIFICATION_READ, DELETE_NOTIFICATION, DELETE_ALL_READ_NOTIFICATIONS,
                    SEND_NOTIFICATION, SEND_BATCH_NOTIFICATION -> "notification-service";
            case QUERY_STUDENT_STATS, QUERY_STUDY_TIME_DISTRIBUTION, QUERY_TEACHER_DASHBOARD,
                    QUERY_LEARNING_SUMMARY, QUERY_SCORE_TREND, QUERY_KNOWLEDGE_MASTERY -> "analysis-service";
            case QUERY_RAG_KNOWLEDGE, UNKNOWN -> "agent-service";
        };
    }
}
