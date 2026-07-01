package com._202510007517.platform.agent.service;

import com._202510007517.platform.agent.model.AgentIntent;
import com._202510007517.platform.agent.model.AgentRiskLevel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class InternetIntentPolicyTest {

    private final AgentRiskPolicy riskPolicy = new AgentRiskPolicy();
    private final AgentConfirmationPolicy confirmationPolicy = new AgentConfirmationPolicy(riskPolicy);
    private final AgentPermissionPolicy permissionPolicy = new AgentPermissionPolicy();

    @Test
    void treatsInternetIntentsAsLowRiskReadOnlyOperations() {
        assertThat(riskPolicy.riskLevel(AgentIntent.INTERNET_SEARCH)).isEqualTo(AgentRiskLevel.LOW);
        assertThat(riskPolicy.riskLevel(AgentIntent.READ_WEB_PAGE)).isEqualTo(AgentRiskLevel.LOW);
        assertThat(confirmationPolicy.requiresConfirmation(AgentIntent.INTERNET_SEARCH)).isFalse();
        assertThat(confirmationPolicy.requiresConfirmation(AgentIntent.READ_WEB_PAGE)).isFalse();
        assertThat(confirmationPolicy.requiresSecondConfirmation(AgentIntent.INTERNET_SEARCH)).isFalse();
        assertThat(confirmationPolicy.requiresSecondConfirmation(AgentIntent.READ_WEB_PAGE)).isFalse();
    }

    @Test
    void allowsTeachersStudentsAndAdminsToUseInternetIntents() {
        for (String role : new String[]{"TEACHER", "STUDENT", "ADMIN"}) {
            assertThatCode(() -> permissionPolicy.assertAllowed(7L, role, AgentIntent.INTERNET_SEARCH))
                    .as(role + " internet search")
                    .doesNotThrowAnyException();
            assertThatCode(() -> permissionPolicy.assertAllowed(7L, role, AgentIntent.READ_WEB_PAGE))
                    .as(role + " web page read")
                    .doesNotThrowAnyException();
        }
    }
}
