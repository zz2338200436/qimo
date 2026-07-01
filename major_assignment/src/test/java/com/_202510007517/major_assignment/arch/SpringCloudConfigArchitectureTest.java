package com._202510007517.major_assignment.arch;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpringCloudConfigArchitectureTest {

    private static final List<String> CONFIG_CLIENT_MODULES = List.of(
            "gateway",
            "auth-service",
            "user-service",
            "course-service",
            "assignment-service",
            "exam-service",
            "analysis-service",
            "notification-service",
            "ai-service",
            "legacy-adapter"
    );

    private static final List<String> LOAD_BALANCED_RUNTIME_MODULES = List.of(
            "registry-server",
            "config-server",
            "gateway",
            "auth-service",
            "user-service",
            "course-service",
            "assignment-service",
            "exam-service",
            "analysis-service",
            "notification-service",
            "ai-service",
            "legacy-adapter"
    );

    private static final List<String> OBSERVED_SERVICE_NAMES = List.of(
            "registry-server",
            "config-server",
            "gateway",
            "auth-service",
            "user-service",
            "course-service",
            "assignment-service",
            "exam-service",
            "analysis-service",
            "notification-service",
            "ai-service",
            "legacy-adapter"
    );

    private static final List<String> STATEFUL_INFRASTRUCTURE_SERVICES = List.of(
            "mysql",
            "redis",
            "rabbitmq"
    );

    private static final List<String> SPRING_BOOT_DOCKER_SERVICES = List.of(
            "registry-server",
            "config-server",
            "gateway",
            "auth-service",
            "user-service",
            "course-service",
            "assignment-service",
            "exam-service",
            "analysis-service",
            "notification-service",
            "ai-service",
            "legacy-adapter",
            "legacy-monolith"
    );

    private static final Map<String, String> DOCKERFILE_MODULE_BY_SERVICE = Map.of(
            "legacy-monolith", "major_assignment"
    );

    @Test
    void aggregateBuildMustIncludeConfigServerModule() throws IOException {
        Path repoRoot = repoRoot();
        String pom = Files.readString(repoRoot.resolve("pom.xml"));

        assertTrue(pom.contains("<module>config-server</module>"),
                "Aggregate build must include the config-server module");
    }

    @Test
    void configServerMustExposeNativeConfigRepository() throws IOException {
        Path repoRoot = repoRoot();

        assertAll(
                () -> assertTrue(Files.exists(repoRoot.resolve("config-server/pom.xml")),
                        "config-server module must have a Maven descriptor"),
                () -> assertTrue(Files.exists(repoRoot.resolve("config-server/src/main/java/com/_202510007517/platform/config/ConfigServerApplication.java")),
                        "config-server module must declare its Spring Boot entry point"),
                () -> assertTrue(Files.exists(repoRoot.resolve("config-server/src/main/resources/config-repo/application.yml")),
                        "config-server must publish common configuration from config-repo/application.yml"),
                () -> assertTrue(Files.exists(repoRoot.resolve("config-server/src/main/resources/config-repo/gateway.yml")),
                        "config-server must publish gateway-specific configuration")
        );
    }

    @Test
    void configClientsMustImportConfigServerAndKeepLocalFallbacks() throws IOException {
        Path repoRoot = repoRoot();

        for (String module : CONFIG_CLIENT_MODULES) {
            Path application = repoRoot.resolve(module).resolve("src/main/resources/application.yml");
            String yaml = Files.readString(application);

            assertTrue(yaml.contains("optional:configserver:${CONFIG_SERVER_URL:http://localhost:8888}"),
                    () -> module + " must import the optional config server");
            assertTrue(yaml.contains("optional:classpath:application-common.yml"),
                    () -> module + " must keep the local optional classpath fallback");
        }
    }

    @Test
    void configClientsMustDisableConfigServerInTestsAndFailFastInDockerAndProd() throws IOException {
        Path repoRoot = repoRoot();

        for (String module : CONFIG_CLIENT_MODULES) {
            Path application = repoRoot.resolve(module).resolve("src/main/resources/application.yml");
            String yaml = Files.readString(application);

            assertTrue(yaml.contains("on-profile: test"), () -> module + " must declare a test profile block");
            assertTrue(yaml.contains("enabled: false"), () -> module + " must disable Spring Cloud Config in tests");
            assertTrue(yaml.contains("on-profile: docker | prod"),
                    () -> module + " must declare Docker/production Config Server behavior");
            assertTrue(yaml.contains("- configserver:${CONFIG_SERVER_URL:http://localhost:8888}"),
                    () -> module + " must fail fast when Config Server is unavailable in Docker/production");
            assertTrue(yaml.contains("fail-fast: true"), () -> module + " must enable Config Client fail-fast");
            assertTrue(yaml.contains("max-attempts: 6"), () -> module + " must retry Config Server startup races");
        }
    }

    @Test
    void sharedConfigurationMustLiveInConfigRepositoryInsteadOfEveryClient() throws IOException {
        Path repoRoot = repoRoot();
        String commonConfig = Files.readString(repoRoot.resolve("config-server/src/main/resources/config-repo/application.yml"));

        assertTrue(commonConfig.contains("eureka:"), "Config repository must own shared Eureka settings");
        assertTrue(commonConfig.contains("management:"), "Config repository must own shared Actuator settings");

        for (String module : CONFIG_CLIENT_MODULES) {
            String yaml = Files.readString(repoRoot.resolve(module).resolve("src/main/resources/application.yml"));

            assertFalse(yaml.contains("\neureka:"), () -> module + " must not duplicate shared Eureka settings");
            assertFalse(yaml.contains("\nmanagement:"), () -> module + " must not duplicate shared Actuator settings");
        }
    }

    @Test
    void gatewayRoutesMustLiveInConfigRepositoryWithCurrentWebfluxPrefix() throws IOException {
        Path repoRoot = repoRoot();
        String centralGatewayConfig = Files.readString(repoRoot.resolve("config-server/src/main/resources/config-repo/gateway.yml"));
        String localGatewayConfig = Files.readString(repoRoot.resolve("gateway/src/main/resources/application.yml"));

        assertTrue(centralGatewayConfig.contains("webflux:"),
                "gateway.yml must use the current Spring Cloud Gateway WebFlux namespace");
        assertTrue(centralGatewayConfig.contains("auth-route"),
                "gateway route definitions must be served from the Config Server repository");
        assertTrue(centralGatewayConfig.contains("CircuitBreaker"),
                "centralized gateway routes must retain circuit breaker filters");
        assertFalse(centralGatewayConfig.contains("\n      routes:"),
                "gateway.yml must not use the deprecated spring.cloud.gateway.routes prefix");
        assertFalse(localGatewayConfig.contains("auth-route"),
                "gateway local application.yml must not own the full route table");
        assertFalse(localGatewayConfig.contains("\n      routes:"),
                "gateway local application.yml must not use the deprecated Spring Cloud Gateway routes prefix");
        assertFalse(localGatewayConfig.contains("\n  rate-limit:"),
                "gateway local application.yml must not duplicate centralized rate-limit settings");
        assertFalse(localGatewayConfig.contains("\nresilience4j:"),
                "gateway local application.yml must not duplicate centralized circuit breaker settings");
    }

    @Test
    void sensitiveKeysMustBeExternalizedFromYamlConfiguration() throws IOException {
        Path repoRoot = repoRoot();
        List<Path> yamlFiles = Files.walk(repoRoot)
                .filter(path -> Files.isRegularFile(path)
                        && (path.getFileName().toString().endsWith(".yml")
                        || path.getFileName().toString().endsWith(".yaml")))
                .filter(path -> !path.toString().contains("\\target\\"))
                .toList();

        for (Path yamlFile : yamlFiles) {
            String yaml = Files.readString(yamlFile);
            assertFalse(yaml.contains("-----BEGIN PRIVATE KEY-----"),
                    () -> repoRoot.relativize(yamlFile) + " must not contain a PEM private key");
        }

        List<Path> runtimeConfigFiles = yamlFiles.stream()
                .filter(path -> path.toString().contains("\\src\\main\\resources\\"))
                .toList();

        for (Path yamlFile : runtimeConfigFiles) {
            String yaml = Files.readString(yamlFile);
            assertFalse(yaml.contains("dev_only_pwd"),
                    () -> repoRoot.relativize(yamlFile) + " must not use the old shared dev_only_pwd fallback");
            assertFalse(yaml.contains("${DB_PASSWORD:root}"),
                    () -> repoRoot.relativize(yamlFile) + " must not fall back to the root database password");
        }
    }

    @Test
    void prometheusMustScrapeEveryPlatformService() throws IOException {
        Path repoRoot = repoRoot();
        String prometheus = Files.readString(repoRoot.resolve("deploy/prometheus/prometheus.yml"));

        for (String serviceName : OBSERVED_SERVICE_NAMES) {
            assertTrue(prometheus.contains("service: " + serviceName),
                    () -> "Prometheus must include a target labelled " + serviceName);
        }
    }

    @Test
    void loadBalancedRuntimeModulesMustUseCaffeineCache() throws IOException {
        Path repoRoot = repoRoot();

        for (String module : LOAD_BALANCED_RUNTIME_MODULES) {
            String pom = Files.readString(repoRoot.resolve(module).resolve("pom.xml"));

            assertTrue(pom.contains("<artifactId>caffeine</artifactId>"),
                    () -> module + " must include Caffeine so Spring Cloud LoadBalancer does not use the default cache");
        }
    }

    @Test
    void dockerComposeMustStartConfigServerBeforeGatewayAndClients() throws IOException {
        Path repoRoot = repoRoot();
        Map<String, Object> compose = readYaml(repoRoot.resolve("docker-compose.yml"));

        @SuppressWarnings("unchecked")
        Map<String, Object> services = (Map<String, Object>) compose.get("services");

        assertTrue(services.containsKey("config-server"),
                "docker-compose.yml must define a config-server service");
        for (String service : STATEFUL_INFRASTRUCTURE_SERVICES) {
            assertServiceHasHealthcheck(services, service);
        }
        for (String service : SPRING_BOOT_DOCKER_SERVICES) {
            assertDockerfileHasActuatorHealthcheck(repoRoot, service);
        }

        assertServiceDependsOnHealthy(services, "config-server", "registry-server");
        assertServiceDependsOnHealthy(services, "gateway", "config-server");
        assertServiceDependsOnHealthy(services, "gateway", "registry-server");
        assertServiceDependsOnHealthy(services, "gateway", "redis");
        assertServiceDependsOnHealthy(services, "legacy-adapter", "config-server");
        assertServiceDependsOnHealthy(services, "legacy-adapter", "mysql");
        for (String module : CONFIG_CLIENT_MODULES) {
            assertServiceDeclaresConfigServerUrl(services, module);
            assertServiceActivatesDockerProfile(services, module);
            assertServiceDependsOnHealthy(services, module, "config-server");
            assertServiceDependsOnHealthy(services, module, "registry-server");
        }
    }

    @SuppressWarnings("unchecked")
    private static void assertServiceHasHealthcheck(Map<String, Object> services, String service) {
        Map<String, Object> definition = (Map<String, Object>) services.get(service);
        assertNotNull(definition, () -> "docker-compose.yml must define " + service);

        assertTrue(definition.containsKey("healthcheck"),
                () -> service + " must define a Docker healthcheck");
    }

    @SuppressWarnings("unchecked")
    private static void assertServiceDependsOnHealthy(Map<String, Object> services, String service, String dependency) {
        Map<String, Object> definition = (Map<String, Object>) services.get(service);
        assertNotNull(definition, () -> "docker-compose.yml must define " + service);
        Object dependsOn = definition.get("depends_on");

        assertTrue(dependsOn instanceof Map<?, ?> dependsOnMap,
                () -> service + " must use conditional depends_on entries");
        Map<?, ?> dependsOnMap = (Map<?, ?>) dependsOn;
        Object dependencyDefinition = dependsOnMap.get(dependency);
        assertTrue(dependencyDefinition instanceof Map<?, ?> dependencyMap
                        && "service_healthy".equals(dependencyMap.get("condition")),
                () -> service + " must wait for healthy " + dependency);
    }

    private static void assertDockerfileHasActuatorHealthcheck(Path repoRoot, String service) throws IOException {
        String module = DOCKERFILE_MODULE_BY_SERVICE.getOrDefault(service, service);
        String dockerfile = Files.readString(repoRoot.resolve(module).resolve("Dockerfile"));

        assertTrue(dockerfile.contains("HEALTHCHECK"),
                () -> service + " Dockerfile must define a Docker healthcheck");
        assertTrue(dockerfile.contains("/actuator/health"),
                () -> service + " Dockerfile healthcheck must use the Actuator health endpoint");
    }

    @SuppressWarnings("unchecked")
    private static void assertServiceDeclaresConfigServerUrl(Map<String, Object> services, String service) {
        Map<String, Object> definition = (Map<String, Object>) services.get(service);
        assertNotNull(definition, () -> "docker-compose.yml must define " + service);
        Map<String, Object> environment = environment(definition);

        assertTrue(environment.containsKey("CONFIG_SERVER_URL"),
                () -> service + " must declare CONFIG_SERVER_URL for Docker deployments");
    }

    @SuppressWarnings("unchecked")
    private static void assertServiceActivatesDockerProfile(Map<String, Object> services, String service) {
        Map<String, Object> definition = (Map<String, Object>) services.get(service);
        assertNotNull(definition, () -> "docker-compose.yml must define " + service);
        Map<String, Object> environment = environment(definition);

        assertTrue("docker".equals(environment.get("SPRING_PROFILES_ACTIVE")),
                () -> service + " must activate the docker profile");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> environment(Map<String, Object> definition) {
        Object rawEnvironment = definition.get("environment");
        if (rawEnvironment instanceof Map<?, ?> environmentMap) {
            return (Map<String, Object>) environmentMap;
        }
        if (rawEnvironment instanceof List<?> environmentList) {
            return environmentList.stream()
                    .map(Object::toString)
                    .map(entry -> entry.split("=", 2))
                    .collect(Collectors.toMap(
                            parts -> parts[0],
                            parts -> parts.length > 1 ? parts[1] : "",
                            (first, second) -> second,
                            LinkedHashMap::new));
        }
        return Map.of();
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
