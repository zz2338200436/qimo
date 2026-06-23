package com._202510007517.platform.agent.service;

import com._202510007517.platform.agent.model.AgentIntent;
import com._202510007517.platform.agent.model.RecognizedIntent;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgentAssignmentDraftServiceTest {

    private final AgentAssignmentDraftService service = new AgentAssignmentDraftService(
            Clock.fixed(Instant.parse("2026-06-23T02:00:00Z"), ZoneId.of("Asia/Shanghai")));

    @Test
    void enrichesSelectedQuestionPublishDraftFromPageContext() {
        RecognizedIntent input = new RecognizedIntent(
                AgentIntent.PUBLISH_ASSIGNMENT,
                0.9,
                Map.of(),
                List.of());

        RecognizedIntent result = service.enrich(input, "把这个题发布到班级", Map.of(
                "page", "teacher-question-bank",
                "selectedQuestionIds", List.of(91022),
                "selectedQuestionScore", 2,
                "selectedQuestionType", "TRUE_FALSE",
                "selectedQuestionContent", "服务注册中心通常保存服务实例的地址、端口和健康状态等信息。",
                "currentCourseId", 12
        ));

        assertThat(result.slots())
                .containsEntry("selectionMode", "SELECTED_QUESTIONS")
                .containsEntry("questionIds", List.of(91022L))
                .containsEntry("courseId", 12L)
                .containsEntry("maxScore", 2)
                .containsEntry("dueDate", "2026-06-30 23:59:59");
        assertThat(String.valueOf(result.slots().get("title"))).contains("服务注册中心");
        assertThat(String.valueOf(result.slots().get("content"))).contains("服务注册中心通常保存");
    }

    @Test
    void enrichesRandomQuestionBankDraftWhenUserSaysAllRandom() {
        RecognizedIntent input = new RecognizedIntent(
                AgentIntent.PUBLISH_ASSIGNMENT,
                0.9,
                Map.of("courseId", 12L),
                List.of());

        RecognizedIntent result = service.enrich(input, "全部随机", Map.of());

        assertThat(result.slots())
                .containsEntry("selectionMode", "RANDOM_QUESTION_BANK")
                .containsEntry("title", "随机题库练习")
                .containsEntry("maxScore", 100)
                .containsEntry("dueDate", "2026-06-30 23:59:59");
    }

    @Test
    void keepsExplicitUserSlotsOverDefaults() {
        RecognizedIntent input = new RecognizedIntent(
                AgentIntent.PUBLISH_ASSIGNMENT,
                0.9,
                Map.of(
                        "title", "自定义标题",
                        "maxScore", 20,
                        "dueDate", "明晚",
                        "courseId", 12L),
                List.of());

        RecognizedIntent result = service.enrich(input, "把这个题发布到班级", Map.of(
                "selectedQuestionIds", List.of(91022),
                "selectedQuestionScore", 2,
                "selectedQuestionContent", "服务注册中心通常保存服务实例的地址、端口和健康状态等信息。"
        ));

        assertThat(result.slots())
                .containsEntry("title", "自定义标题")
                .containsEntry("maxScore", 20)
                .containsEntry("dueDate", "明晚");
    }
}
