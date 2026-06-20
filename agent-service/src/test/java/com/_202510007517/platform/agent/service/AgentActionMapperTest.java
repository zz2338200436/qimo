package com._202510007517.platform.agent.service;

import com._202510007517.platform.agent.model.AgentIntent;
import com._202510007517.platform.agent.model.RecognizedIntent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgentActionMapperTest {

    private final AgentRiskPolicy riskPolicy = new AgentRiskPolicy();
    private final AgentConfirmationPolicy confirmationPolicy = new AgentConfirmationPolicy(riskPolicy);
    private final AgentActionMapper mapper = new AgentActionMapper(riskPolicy, confirmationPolicy);

    @Test
    void buildsHighRiskPreviewWithoutSecondConfirmationPrompt() {
        var preview = mapper.toPreview(new RecognizedIntent(
                AgentIntent.UPDATE_ASSIGNMENT,
                0.9,
                Map.of("assignmentId", 88L, "title", "微服务实验")
        ));

        assertThat(preview.getIntent()).isEqualTo("UPDATE_ASSIGNMENT");
        assertThat(preview.getRiskLevel()).isEqualTo("HIGH");
        assertThat(preview.getTitle()).isEqualTo("更新作业");
        assertThat(preview.getSummary()).isEqualTo("更新作业: 微服务实验");
        assertThat(preview.getRequiresSecondConfirmation()).isFalse();
        assertThat(preview.getSecondConfirmationPhrase()).isNull();
    }

    @Test
    void buildsCriticalPreviewWithSecondConfirmationPrompt() {
        var preview = mapper.toPreview(new RecognizedIntent(
                AgentIntent.DELETE_ASSIGNMENT,
                0.9,
                Map.of("assignmentId", 88L)
        ));

        assertThat(preview.getRiskLevel()).isEqualTo("CRITICAL");
        assertThat(preview.getRequiresSecondConfirmation()).isTrue();
        assertThat(preview.getSecondConfirmationPhrase()).isEqualTo("确认执行");
        assertThat(preview.getSecondConfirmationPrompt()).contains("确认执行");
    }

    @Test
    void addsRecipientCountForBatchNotificationPreview() {
        var preview = mapper.toPreview(new RecognizedIntent(
                AgentIntent.SEND_BATCH_NOTIFICATION,
                0.9,
                Map.of("studentIds", List.of(42L, 43L), "title", "开课通知")
        ));

        assertThat(preview.getRiskLevel()).isEqualTo("CRITICAL");
        assertThat(preview.getRequiresSecondConfirmation()).isTrue();
        assertThat(preview.getPreview()).containsEntry("recipientCount", 2);
    }

    @Test
    void buildsMediumRiskPreviewForCreateCourse() {
        var preview = mapper.toPreview(new RecognizedIntent(
                AgentIntent.CREATE_COURSE,
                0.9,
                Map.of("courseName", "分布式框架技术", "courseCode", "DFT101")
        ));

        assertThat(preview.getIntent()).isEqualTo("CREATE_COURSE");
        assertThat(preview.getRiskLevel()).isEqualTo("MEDIUM");
        assertThat(preview.getTitle()).isEqualTo("创建课程");
        assertThat(preview.getSummary()).isEqualTo("创建课程: 分布式框架技术");
        assertThat(preview.getRequiresSecondConfirmation()).isFalse();
    }

    @Test
    void buildsHighRiskPreviewForUpdateCourse() {
        var preview = mapper.toPreview(new RecognizedIntent(
                AgentIntent.UPDATE_COURSE,
                0.9,
                Map.of("courseId", 101L, "courseName", "高级分布式框架技术")
        ));

        assertThat(preview.getIntent()).isEqualTo("UPDATE_COURSE");
        assertThat(preview.getRiskLevel()).isEqualTo("HIGH");
        assertThat(preview.getTitle()).isEqualTo("更新课程");
        assertThat(preview.getSummary()).isEqualTo("更新课程: 高级分布式框架技术");
        assertThat(preview.getRequiresSecondConfirmation()).isFalse();
    }

    @Test
    void buildsCriticalPreviewForDeleteCourse() {
        var preview = mapper.toPreview(new RecognizedIntent(
                AgentIntent.DELETE_COURSE,
                0.9,
                Map.of("courseId", 101L)
        ));

        assertThat(preview.getIntent()).isEqualTo("DELETE_COURSE");
        assertThat(preview.getRiskLevel()).isEqualTo("CRITICAL");
        assertThat(preview.getTitle()).isEqualTo("删除课程");
        assertThat(preview.getRequiresSecondConfirmation()).isTrue();
        assertThat(preview.getSecondConfirmationPhrase()).isEqualTo("确认执行");
    }

    @Test
    void buildsHighRiskPreviewForCreateClass() {
        var preview = mapper.toPreview(new RecognizedIntent(
                AgentIntent.CREATE_CLASS,
                0.9,
                Map.of("className", "软件2301")
        ));

        assertThat(preview.getIntent()).isEqualTo("CREATE_CLASS");
        assertThat(preview.getRiskLevel()).isEqualTo("HIGH");
        assertThat(preview.getTitle()).isEqualTo("创建班级");
        assertThat(preview.getSummary()).isEqualTo("创建班级: 软件2301");
        assertThat(preview.getRequiresSecondConfirmation()).isFalse();
    }

    @Test
    void buildsLowRiskReadOnlyPreviewShapeWhenUsedDirectly() {
        var preview = mapper.toPreview(new RecognizedIntent(
                AgentIntent.QUERY_LEARNING_SUMMARY,
                0.9,
                Map.of("studentId", 7L)
        ));

        assertThat(preview.getRiskLevel()).isEqualTo("LOW");
        assertThat(preview.getTitle()).isEqualTo("查询学习汇总");
        assertThat(preview.getRequiresSecondConfirmation()).isFalse();
    }
}
