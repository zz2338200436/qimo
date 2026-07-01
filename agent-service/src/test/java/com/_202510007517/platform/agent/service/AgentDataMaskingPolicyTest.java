package com._202510007517.platform.agent.service;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgentDataMaskingPolicyTest {

    private final AgentDataMaskingPolicy policy = new AgentDataMaskingPolicy();

    @Test
    void masksSensitiveTextFragments() {
        String masked = policy.maskText("手机号13812345678，邮箱student@example.com，身份证110101199001011234");

        assertThat(masked).contains("138****5678");
        assertThat(masked).contains("s***@example.com");
        assertThat(masked).contains("110101********1234");
        assertThat(masked).doesNotContain("13812345678", "student@example.com", "110101199001011234");
    }

    @Test
    void masksCredentialLikeTextFragments() {
        String masked = policy.maskText("password=abc123 token: sk-secret-value apiKey=xyz");

        assertThat(masked).contains("password=***");
        assertThat(masked).contains("token: ***");
        assertThat(masked).contains("apiKey=***");
        assertThat(masked).doesNotContain("abc123", "sk-secret-value", "xyz");
    }

    @Test
    void recursivelyMasksMetadataValues() {
        Map<String, Object> masked = policy.maskMetadata(Map.of(
                "content", "联系13812345678",
                "nested", Map.of("email", "student@example.com"),
                "items", List.of("token=abc", 42)
        ));

        assertThat(masked.get("content")).isEqualTo("联系138****5678");
        assertThat(masked.get("nested")).isEqualTo(Map.of("email", "s***@example.com"));
        assertThat(masked.get("items")).isEqualTo(List.of("token=***", 42));
    }

    @Test
    void masksStructuredCredentialMetadataKeys() {
        Map<String, Object> masked = policy.maskMetadata(Map.of(
                "token", "secret-token",
                "nested", Map.of("password", "abc123", "note", "保留普通文本"),
                "items", List.of(Map.of("apiKey", "key-123"), 42)
        ));

        assertThat(masked.get("token")).isEqualTo("***");
        assertThat(masked.get("nested")).isEqualTo(Map.of("password", "***", "note", "保留普通文本"));
        assertThat(masked.get("items")).isEqualTo(List.of(Map.of("apiKey", "***"), 42));
    }

    @Test
    void masksJsonLikeCredentialTextFragments() {
        String masked = policy.maskText("{\"token\":\"secret-token\",\"password\":\"abc123\"}");

        assertThat(masked).contains("\"token\":\"***\"");
        assertThat(masked).contains("\"password\":\"***\"");
        assertThat(masked).doesNotContain("secret-token", "abc123");
    }
}
