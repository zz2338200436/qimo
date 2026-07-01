package com._202510007517.major_assignment.arch;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrometheusAlertRulesTest {

    private static final Path PROMETHEUS_CONFIG = Path.of("deploy", "prometheus", "prometheus.yml");
    private static final Path ALERT_RULES = Path.of("deploy", "prometheus", "alerts", "platform-alerts.yml");

    @Test
    void prometheus_must_load_stage2_alert_rules() throws IOException {
        Path repoRoot = repoRoot();
        Map<String, Object> prometheus = readYaml(repoRoot.resolve(PROMETHEUS_CONFIG));

        @SuppressWarnings("unchecked")
        List<String> ruleFiles = (List<String>) prometheus.get("rule_files");

        assertTrue(ruleFiles != null && ruleFiles.contains("alerts/platform-alerts.yml"),
                () -> "Prometheus must load stage 2 alert rules from " + ALERT_RULES);
    }

    @Test
    void stage2_alert_rules_must_cover_5xx_ratio_and_open_circuit_breakers() throws IOException {
        Path repoRoot = repoRoot();
        Map<String, Object> alertConfig = readYaml(repoRoot.resolve(ALERT_RULES));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> groups = (List<Map<String, Object>>) alertConfig.get("groups");

        assertTrue(groups != null && !groups.isEmpty(), "Alert rules must define at least one group");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rules = (List<Map<String, Object>>) groups.get(0).get("rules");

        Map<String, Map<String, Object>> indexedRules = rules.stream()
                .collect(java.util.stream.Collectors.toMap(
                        rule -> rule.get("alert").toString(),
                        rule -> rule));

        assertAll(
                () -> assertEquals("1m", indexedRules.get("QimoInstance5xxRateHigh").get("for")),
                () -> assertTrue(indexedRules.get("QimoInstance5xxRateHigh").get("expr").toString().contains("http_server_requests_seconds_count")),
                () -> assertTrue(indexedRules.get("QimoInstance5xxRateHigh").get("expr").toString().contains("5..")),
                () -> assertEquals("30s", indexedRules.get("QimoCircuitBreakerOpen").get("for")),
                () -> assertTrue(indexedRules.get("QimoCircuitBreakerOpen").get("expr").toString().contains("resilience4j_circuitbreaker_state")),
                () -> assertTrue(indexedRules.get("QimoCircuitBreakerOpen").get("expr").toString().contains("state=\"open\""))
        );
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> readYaml(Path path) throws IOException {
        return new Yaml().load(Files.readString(path));
    }

    private static Path repoRoot() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null) {
            if (Files.isDirectory(current.resolve("major_assignment"))
                    && Files.isDirectory(current.resolve("deploy"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate repository root from " + System.getProperty("user.dir"));
    }
}
