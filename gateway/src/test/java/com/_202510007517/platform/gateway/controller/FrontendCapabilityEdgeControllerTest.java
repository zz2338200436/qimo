package com._202510007517.platform.gateway.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebFluxTest(FrontendCapabilityEdgeController.class)
class FrontendCapabilityEdgeControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void returnsStudentAndTeacherCapabilities() {
        webTestClient.get()
                .uri("/api/frontend/capabilities")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.message").isEqualTo("获取前端能力矩阵成功")
                .jsonPath("$.data.student.courses").isEqualTo(true)
                .jsonPath("$.data.student.avatarUpload").isEqualTo(true)
                .jsonPath("$.data.student.notificationSettings").isEqualTo(false)
                .jsonPath("$.data.student.privacySettings").isEqualTo(false)
                .jsonPath("$.data.teacher.notificationPersistence").isEqualTo(false);
    }
}
