package com._202510007517.platform.agent.service;

import com._202510007517.platform.agent.model.AgentIntent;
import com._202510007517.platform.agent.model.PlannerDecision;
import com._202510007517.platform.agent.model.PlannerMode;
import com._202510007517.platform.agent.model.RecognizedIntent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FallbackPlannerServiceTest {

    private final FallbackPlannerService service = new FallbackPlannerService(
            message -> new RecognizedIntent(AgentIntent.UNKNOWN, 0.2, Map.of()),
            new PlannerToolCatalog());

    @Test
    void doesNotTreatSendBackToMePhrasesAsGeneratedQuestionPublishFollowUps() {
        for (String message : List.of(
                "把前面生成的五道题目重新发给我",
                "前面那五道题再发我一下",
                "给我看看前面生成的五道题",
                "重新显示前面生成的题目")) {
            PlannerDecision decision = service.plan(contextWithGeneratedQuestions(message));

            assertThat(decision.mode())
                    .as(message)
                    .isEqualTo(PlannerMode.ANSWER);
            assertThat(decision.plannerReason())
                    .as(message)
                    .isEqualTo("legacy_unknown");
        }
    }

    @Test
    void doesNotTreatTypedPublishPhrasesAsGeneratedQuestionPublishFollowUps() {
        PlannerDecision decision = service.plan(contextWithGeneratedQuestions("把生成的题目发到云计算技术1班"));

        assertThat(decision.mode()).isEqualTo(PlannerMode.ANSWER);
        assertThat(decision.plannerReason()).isEqualTo("legacy_unknown");
    }

    @Test
    void treatsToolbarPublishCommandAsGeneratedQuestionPublishFollowUp() {
        PlannerDecision decision = service.plan(contextWithGeneratedQuestions(
                "发布给班级",
                Map.of(
                        "agentCommand", "发布给班级",
                        "agentCommandSource", "toolbar"
                )));

        assertThat(decision.mode()).isEqualTo(PlannerMode.TOOL_CALL);
        assertThat(decision.toolName()).isEqualTo("publish_assignment");
        assertThat(decision.artifactRef()).isEqualTo("latest_generated_questions");
    }

    private PlannerContext contextWithGeneratedQuestions(String message) {
        return contextWithGeneratedQuestions(message, Map.of());
    }

    private PlannerContext contextWithGeneratedQuestions(String message, Map<String, Object> pageContext) {
        return new PlannerContext(
                7L,
                "TEACHER",
                null,
                message,
                List.of(),
                Map.of(
                        "latest_generated_questions",
                        new SessionArtifact(
                                "generated_questions",
                                "latest_generated_questions",
                                Map.of(
                                        "title", "Java课堂练习",
                                        "content", "题目如下：\n1. 题目A",
                                        "questions", List.of(Map.of("content", "题目A"))
                                ),
                                "2026-06-29T15:00:00",
                                "2026-06-29T15:00:00"
                        )
                ),
                pageContext,
                null
        );
    }
}
