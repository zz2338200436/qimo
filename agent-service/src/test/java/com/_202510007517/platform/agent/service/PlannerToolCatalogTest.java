package com._202510007517.platform.agent.service;

import com._202510007517.platform.agent.model.AgentIntent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlannerToolCatalogTest {

    private final PlannerToolCatalog catalog = new PlannerToolCatalog();

    @Test
    void resolvesPublishAssignmentToolNameToLegacyIntent() {
        assertThat(catalog.resolve("publish_assignment")).isEqualTo(AgentIntent.PUBLISH_ASSIGNMENT);
    }

    @Test
    void exposesStableGenerateQuestionsToolName() {
        assertThat(catalog.resolve("generate_questions")).isEqualTo(AgentIntent.GENERATE_QUESTIONS);
    }

    @Test
    void exposesStableInternetToolNames() {
        assertThat(catalog.resolve("internet_search")).isEqualTo(AgentIntent.INTERNET_SEARCH);
        assertThat(catalog.resolve("read_web_page")).isEqualTo(AgentIntent.READ_WEB_PAGE);
    }

    @Test
    void rejectsUnknownToolName() {
        assertThatThrownBy(() -> catalog.resolve("not_real_tool"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No planner tool registered");
    }
}
