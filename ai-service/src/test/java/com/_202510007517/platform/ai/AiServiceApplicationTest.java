package com._202510007517.platform.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

@SpringBootTest(
        classes = AiServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "eureka.client.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "spring.datasource.url=jdbc:h2:mem:ai-app;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.datasource.driver-class-name=org.h2.Driver"
        })
class AiServiceApplicationTest {

    @Autowired
    private Environment environment;

    @Test
    void startsWithAiServiceIdentity() {
        assertThat(environment.getProperty("spring.application.name")).isEqualTo("ai-service");
        assertThat(environment.getProperty("server.port", Integer.class)).isEqualTo(8088);
    }
}
