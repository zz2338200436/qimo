package com._202510007517.platform.agent.service;

import com._202510007517.platform.agent.api.dto.AgentActionPreviewDTO;
import com._202510007517.platform.agent.model.AgentIntent;
import com._202510007517.platform.agent.model.RecognizedIntent;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class AgentActionMapper {

    public static final String SECOND_CONFIRMATION_PHRASE = "确认执行";

    private final AgentRiskPolicy riskPolicy;
    private final AgentConfirmationPolicy confirmationPolicy;

    public AgentActionMapper(AgentRiskPolicy riskPolicy, AgentConfirmationPolicy confirmationPolicy) {
        this.riskPolicy = riskPolicy;
        this.confirmationPolicy = confirmationPolicy;
    }

    public AgentActionPreviewDTO toPreview(RecognizedIntent recognizedIntent) {
        AgentActionPreviewDTO preview = new AgentActionPreviewDTO();
        Map<String, Object> previewPayload = new LinkedHashMap<>(recognizedIntent.slots());
        addDerivedPreviewFields(recognizedIntent.intent(), previewPayload);
        preview.setIntent(recognizedIntent.intent().name());
        preview.setRiskLevel(riskPolicy.riskLevel(recognizedIntent.intent()).name());
        preview.setTitle(title(recognizedIntent.intent()));
        preview.setSummary(summary(recognizedIntent.intent(), previewPayload));
        preview.setPreview(previewPayload);
        boolean requiresSecondConfirmation = requiresSecondConfirmation(recognizedIntent.intent());
        preview.setRequiresSecondConfirmation(requiresSecondConfirmation);
        if (requiresSecondConfirmation) {
            preview.setSecondConfirmationPhrase(SECOND_CONFIRMATION_PHRASE);
            preview.setSecondConfirmationPrompt("该操作风险较高，请输入“" + SECOND_CONFIRMATION_PHRASE + "”完成二级确认。");
        }
        return preview;
    }

    private boolean requiresSecondConfirmation(AgentIntent intent) {
        return confirmationPolicy.requiresSecondConfirmation(intent);
    }

    private void addDerivedPreviewFields(AgentIntent intent, Map<String, Object> previewPayload) {
        if (intent == AgentIntent.SEND_BATCH_NOTIFICATION) {
            Object studentIds = previewPayload.get("studentIds");
            if (studentIds instanceof Iterable<?> iterable) {
                int count = 0;
                for (Object ignored : iterable) {
                    count++;
                }
                previewPayload.put("recipientCount", count);
            }
        }
    }

    private String title(AgentIntent intent) {
        return switch (intent) {
            case PUBLISH_ASSIGNMENT -> "发布作业";
            case UPDATE_ASSIGNMENT -> "更新作业";
            case DELETE_ASSIGNMENT -> "删除作业";
            case GRADE_ASSIGNMENT -> "批改作业";
            case PUBLISH_EXAM -> "发布考试";
            case UPDATE_EXAM -> "更新考试";
            case DELETE_EXAM -> "删除考试";
            case GRADE_EXAM -> "批改考试";
            case SUBMIT_ASSIGNMENT -> "提交作业";
            case SUBMIT_EXAM -> "提交考试";
            case GENERATE_EXAM -> "生成试卷";
            case GENERATE_QUESTIONS -> "生成题目";
            case GENERATE_LEARNING_SUGGESTIONS -> "生成学习建议";
            case QUERY_QUESTION_BANK -> "查询题库";
            case QUERY_PENDING_ASSIGNMENTS -> "查询待提交作业";
            case CREATE_COURSE -> "创建课程";
            case UPDATE_COURSE -> "更新课程";
            case DELETE_COURSE -> "删除课程";
            case CREATE_CLASS -> "创建班级";
            case QUERY_COURSES -> "查询课程";
            case QUERY_COURSE_DETAIL -> "查询课程详情";
            case QUERY_CLASSES -> "查询班级";
            case QUERY_CLASS_DETAIL -> "查询班级详情";
            case QUERY_ASSIGNMENTS -> "查询作业";
            case QUERY_ASSIGNMENT_DETAIL -> "查询作业详情";
            case QUERY_ASSIGNMENT_SUBMISSIONS -> "查询作业提交记录";
            case QUERY_EXAMS -> "查询考试";
            case QUERY_EXAM_DETAIL -> "查询考试详情";
            case QUERY_EXAM_SUBMISSIONS -> "查询考试提交记录";
            case QUERY_SCORES -> "查询成绩";
            case QUERY_NOTIFICATIONS -> "查询通知";
            case QUERY_UNREAD_NOTIFICATION_COUNT -> "查询未读通知数量";
            case MARK_ALL_NOTIFICATIONS_READ -> "全部通知标为已读";
            case MARK_NOTIFICATION_READ -> "通知标为已读";
            case DELETE_NOTIFICATION -> "删除通知";
            case DELETE_ALL_READ_NOTIFICATIONS -> "删除全部已读通知";
            case SEND_NOTIFICATION -> "发送通知";
            case SEND_BATCH_NOTIFICATION -> "批量发送通知";
            case QUERY_STUDENT_STATS -> "查询学习统计";
            case QUERY_STUDY_TIME_DISTRIBUTION -> "查询学习时间分布";
            case QUERY_TEACHER_DASHBOARD -> "查询教师仪表盘";
            case QUERY_LEARNING_SUMMARY -> "查询学习汇总";
            case QUERY_SCORE_TREND -> "查询成绩趋势";
            case QUERY_KNOWLEDGE_POINTS -> "查询课程知识点";
            case QUERY_KNOWLEDGE_MASTERY -> "查询知识点掌握情况";
            case QUERY_RAG_KNOWLEDGE -> "查询知识库";
            case UNKNOWN -> "暂不支持的操作";
        };
    }

    private String summary(AgentIntent intent, Map<String, Object> slots) {
        Object title = slots.get("title");
        Object courseName = slots.get("courseName");
        Object assignmentTitle = slots.get("assignmentTitle");
        Object examTitle = slots.get("examTitle");
        if (title != null) {
            return title(intent) + ": " + title;
        }
        if (courseName != null && (intent == AgentIntent.CREATE_COURSE || intent == AgentIntent.UPDATE_COURSE)) {
            return title(intent) + ": " + courseName;
        }
        Object className = slots.get("className");
        if (className != null && intent == AgentIntent.CREATE_CLASS) {
            return title(intent) + ": " + className;
        }
        if (assignmentTitle != null) {
            return title(intent) + ": " + assignmentTitle;
        }
        if (examTitle != null) {
            return title(intent) + ": " + examTitle;
        }
        return title(intent);
    }
}
