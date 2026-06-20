package com._202510007517.platform.agent.service;

import com._202510007517.platform.agent.domain.AgentActionEntity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentIdempotencyServiceTest {

    private final AgentIdempotencyService service = new AgentIdempotencyService();

    @Test
    void generatesNonBlankUniqueActionKeys() {
        String first = service.newActionKey();
        String second = service.newActionKey();

        assertThat(first).isNotBlank();
        assertThat(second).isNotBlank();
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void acceptsMatchingConfirmationKey() {
        AgentActionEntity action = action("idem-1");

        service.assertMatches(action, "idem-1");
    }

    @Test
    void rejectsMismatchedConfirmationKey() {
        AgentActionEntity action = action("idem-1");

        assertThatThrownBy(() -> service.assertMatches(action, "wrong"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("idempotency key mismatch");
    }

    private AgentActionEntity action(String idempotencyKey) {
        AgentActionEntity action = new AgentActionEntity();
        action.setId(11L);
        action.setIdempotencyKey(idempotencyKey);
        return action;
    }
}
