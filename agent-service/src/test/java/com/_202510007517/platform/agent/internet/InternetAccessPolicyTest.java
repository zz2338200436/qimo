package com._202510007517.platform.agent.internet;

import com._202510007517.platform.agent.config.AgentInternetProperties;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InternetAccessPolicyTest {

    @Test
    void allowsPublicHttpAndHttpsUris() {
        InternetAccessPolicy policy = new InternetAccessPolicy(new AgentInternetProperties());

        assertThatCode(() -> policy.validate(URI.create("https://93.184.216.34/docs")))
                .doesNotThrowAnyException();
        assertThatCode(() -> policy.validate(URI.create("http://93.184.216.34/docs")))
                .doesNotThrowAnyException();
    }

    @Test
    void blocksLocalAndPrivateNetworkTargets() {
        InternetAccessPolicy policy = new InternetAccessPolicy(new AgentInternetProperties());

        for (String url : new String[]{
                "http://localhost:8080/admin",
                "http://127.0.0.1:8080/admin",
                "http://10.0.0.5/internal",
                "http://192.168.1.2/internal",
                "http://172.16.0.3/internal",
                "http://169.254.169.254/latest/meta-data"
        }) {
            assertThatThrownBy(() -> policy.validate(URI.create(url)))
                    .as(url)
                    .isInstanceOf(InternetAccessDeniedException.class);
        }
    }

    @Test
    void blocksHostnamesResolvingToPrivateNetworkTargets() throws Exception {
        InternetAccessPolicy policy = new InternetAccessPolicy(
                new AgentInternetProperties(),
                host -> new InetAddress[]{InetAddress.getByName("127.0.0.1")});

        assertThatThrownBy(() -> policy.validate(URI.create("https://example.com/internal")))
                .isInstanceOf(InternetAccessDeniedException.class)
                .hasMessageContaining("禁止访问");
    }

    @Test
    void blocksUnsupportedSchemesAndDomainsOutsideAllowList() {
        AgentInternetProperties properties = new AgentInternetProperties();
        properties.setAllowedDomains(java.util.List.of("example.com"));
        InternetAccessPolicy policy = new InternetAccessPolicy(properties);

        assertThatThrownBy(() -> policy.validate(URI.create("file:///etc/passwd")))
                .isInstanceOf(InternetAccessDeniedException.class);
        assertThatThrownBy(() -> policy.validate(URI.create("https://spring.io/projects")))
                .isInstanceOf(InternetAccessDeniedException.class)
                .hasMessageContaining("不在允许访问域名列表");
    }
}
