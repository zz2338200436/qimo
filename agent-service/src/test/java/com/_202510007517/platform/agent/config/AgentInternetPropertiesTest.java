package com._202510007517.platform.agent.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgentInternetPropertiesTest {

    @Test
    void usesReachableDefaultSearchEndpoint() {
        AgentInternetProperties properties = new AgentInternetProperties();

        assertThat(properties.getSearchBaseUrl()).isEqualTo("https://cn.bing.com/search");
    }
}
