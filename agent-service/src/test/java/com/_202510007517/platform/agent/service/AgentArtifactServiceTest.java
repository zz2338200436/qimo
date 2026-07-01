package com._202510007517.platform.agent.service;

import com._202510007517.platform.agent.domain.AgentSessionEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgentArtifactServiceTest {

    private final AgentArtifactService service = new AgentArtifactService(new ObjectMapper());

    @Test
    void storesGeneratedQuestionsAsLatestArtifact() {
        AgentSessionEntity session = new AgentSessionEntity();

        service.saveToolArtifacts(session, "generate_questions", Map.of(
                "title", "Java课堂练习",
                "questions", List.of(Map.of("content", "题目1")),
                "content", "题目如下：\n1. 题目1"
        ));

        assertThat(session.getArtifactsJson()).contains("latest_generated_questions");
        Map<String, SessionArtifact> artifacts = service.loadArtifacts(session);
        assertThat(artifacts).containsKey("latest_generated_questions");
        assertThat(artifacts.get("latest_generated_questions").type()).isEqualTo("generated_questions");
        assertThat(String.valueOf(artifacts.get("latest_generated_questions").payload().get("title")))
                .isEqualTo("Java课堂练习");
    }
}
