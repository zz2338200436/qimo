package com._202510007517.major_assignment.arch;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DeploymentScaffoldTest {

    private static final List<String> RUNTIME_SERVICES = List.of(
            "registry-server",
            "gateway",
            "auth-service",
            "user-service",
            "course-service",
            "assignment-service",
            "exam-service",
            "analysis-service",
            "notification-service",
            "ai-service");

    private static final List<String> REQUIRED_COMPOSE_SERVICES = List.of(
            "mysql",
            "redis",
            "rabbitmq",
            "prometheus",
            "grafana",
            "registry-server",
            "gateway",
            "auth-service",
            "user-service",
            "course-service",
            "assignment-service",
            "exam-service",
            "analysis-service",
            "notification-service",
            "ai-service");

    @Test
    void runtime_services_must_have_dockerfiles_and_helm_charts() throws IOException {
        Path repoRoot = repoRoot();
        List<String> violations = new ArrayList<>();

        for (String service : RUNTIME_SERVICES) {
            Path dockerfile = repoRoot.resolve(service).resolve("Dockerfile");
            if (!Files.isRegularFile(dockerfile)) {
                violations.add("Missing Dockerfile: " + normalize(repoRoot, dockerfile));
                continue;
            }

            String content = Files.readString(dockerfile);
            if (!content.contains("maven:3.9-eclipse-temurin-17")) {
                violations.add("Dockerfile must use Maven builder image: " + normalize(repoRoot, dockerfile));
            }
            if (!content.contains("eclipse-temurin:17-jre-jammy")) {
                violations.add("Dockerfile must use JRE runtime image: " + normalize(repoRoot, dockerfile));
            }
            if (!content.contains("HEALTHCHECK")) {
                violations.add("Dockerfile must define HEALTHCHECK: " + normalize(repoRoot, dockerfile));
            }
            if (!content.contains("/actuator/health")) {
                violations.add("Dockerfile healthcheck must target /actuator/health: " + normalize(repoRoot, dockerfile));
            }

            Path chartYaml = repoRoot.resolve("deploy/charts").resolve(service).resolve("Chart.yaml");
            if (!Files.isRegularFile(chartYaml)) {
                violations.add("Missing Helm chart: " + normalize(repoRoot, chartYaml));
            }
        }

        Path umbrellaChart = repoRoot.resolve("deploy/charts/target-platform/Chart.yaml");
        if (!Files.isRegularFile(umbrellaChart)) {
            violations.add("Missing umbrella Helm chart: " + normalize(repoRoot, umbrellaChart));
        }

        Path infraChart = repoRoot.resolve("deploy/charts/platform-infra/Chart.yaml");
        if (!Files.isRegularFile(infraChart)) {
            violations.add("Missing infra Helm chart: " + normalize(repoRoot, infraChart));
        }

        for (String env : List.of("dev", "test", "prod")) {
            Path valuesFile = repoRoot.resolve("deploy/charts/target-platform").resolve("values-" + env + ".yaml");
            if (!Files.isRegularFile(valuesFile)) {
                violations.add("Missing environment values file: " + normalize(repoRoot, valuesFile));
            }
        }

        assertTrue(violations.isEmpty(), () -> "Deployment scaffold gaps:%n" + String.join(System.lineSeparator(), violations));
    }

    @Test
    void root_compose_must_aggregate_platform_stack() throws IOException {
        Path repoRoot = repoRoot();
        Path composeFile = repoRoot.resolve("docker-compose.yml");
        assertTrue(Files.isRegularFile(composeFile),
                () -> "Missing root docker-compose.yml at " + normalize(repoRoot, composeFile));

        String compose = Files.readString(composeFile);
        Set<String> missing = new LinkedHashSet<>();
        for (String service : REQUIRED_COMPOSE_SERVICES) {
            if (!compose.contains(service + ":")) {
                missing.add(service);
            }
        }

        assertTrue(missing.isEmpty(), () -> "docker-compose.yml is missing services: " + missing);
    }

    private static String normalize(Path repoRoot, Path path) {
        return repoRoot.relativize(path).toString().replace('\\', '/');
    }

    private static Path repoRoot() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null) {
            if (Files.isDirectory(current.resolve("deploy"))
                    && Files.isRegularFile(current.resolve("pom.xml"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate repository root from " + System.getProperty("user.dir"));
    }
}
