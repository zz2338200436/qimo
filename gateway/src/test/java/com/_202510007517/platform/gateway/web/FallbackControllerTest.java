package com._202510007517.platform.gateway.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebFluxTest(FallbackController.class)
class FallbackControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void postFallbackReturnsServiceUnavailableJson() {
        webTestClient.post()
                .uri("/_fallback/exam-service")
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.data.service").isEqualTo("exam-service")
                .jsonPath("$.code").isEqualTo(503);
    }

    @Test
    void putRegistryFallbackReturnsGatewayTimeoutJson() {
        webTestClient.put()
                .uri("/_fallback/registry/exam-service")
                .exchange()
                .expectStatus().isEqualTo(504)
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.data.service").isEqualTo("exam-service")
                .jsonPath("$.code").isEqualTo(504);
    }
}
