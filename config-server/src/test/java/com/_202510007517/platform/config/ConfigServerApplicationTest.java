package com._202510007517.platform.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.environment.PropertySource;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.config.server.native.search-locations=classpath:/config-repo"
})
class ConfigServerApplicationTest {

    @Autowired
    private EnvironmentRepository environmentRepository;

    @Test
    void contextLoadsWithNativeConfigRepository() {
        assertThat(environmentRepository).isNotNull();
    }

    @Test
    void gatewayConfigurationIsServedFromCentralRepository() {
        Environment environment = environmentRepository.findOne("gateway", "default", null);

        assertThat(findPropertyValue(environment, "spring.cloud.gateway.server.webflux.routes[0].id"))
                .contains("auth-route");
        assertThat(findPropertyValue(environment, "spring.cloud.gateway.server.webflux.routes[0].filters[0].name"))
                .contains("CircuitBreaker");
        assertThat(findPropertyValue(environment, "gateway.rate-limit.routes.ai-route.replenish-rate"))
                .contains("2");
        assertThat(findPropertyValue(environment, "resilience4j.circuitbreaker.instances.exam-service-student-submit.failure-rate-threshold"))
                .contains("30");
    }

    @Test
    void commonConfigurationIsServedToEveryApplication() {
        Environment environment = environmentRepository.findOne("auth-service", "default", null);

        assertThat(findPropertyValue(environment, "eureka.client.service-url.defaultZone"))
                .contains("${EUREKA_SERVER_URL:http://localhost:8761/eureka/}");
        assertThat(findPropertyValue(environment, "management.endpoints.web.exposure.include"))
                .contains("health,info,prometheus");
    }

    private static Optional<String> findPropertyValue(Environment environment, String key) {
        return environment.getPropertySources().stream()
                .map(PropertySource::getSource)
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .filter(source -> source.containsKey(key))
                .map(source -> source.get(key))
                .map(Object::toString)
                .findFirst();
    }
}
