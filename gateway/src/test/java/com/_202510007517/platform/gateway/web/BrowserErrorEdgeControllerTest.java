package com._202510007517.platform.gateway.web;

import com._202510007517.platform.common.web.CommonTraceConstants;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@WebFluxTest(BrowserErrorEdgeController.class)
@Import(BrowserErrorEdgeStore.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class BrowserErrorEdgeControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private BrowserErrorEdgeStore store;

    @Test
    void reportBrowserErrorWorksInsideWebFluxGateway() {
        webTestClient.post()
                .uri("/api/errors/browser")
                .header(CommonTraceConstants.USER_ID_HEADER, "7")
                .header("X-Forwarded-For", "127.0.0.1")
                .bodyValue(Map.of(
                        "errorType", "JavaScript Error",
                        "errorMessage", "boom",
                        "pageUrl", "http://localhost:5500/teacher-courses.html"
                ))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.message").isEqualTo("错误日志上报成功")
                .jsonPath("$.data").isEqualTo(1);

        BrowserErrorEdgeStore.BrowserErrorPayload saved = store.findById(1L);
        assertThat(saved.getUserId()).isEqualTo(7L);
        assertThat(saved.getClientIp()).isEqualTo("127.0.0.1");
        assertThat(saved.getSessionId()).isNotBlank();
    }

    @Test
    void browserErrorListReturnsLegacyCompatiblePageEnvelope() {
        BrowserErrorEdgeStore.BrowserErrorPayload payload = new BrowserErrorEdgeStore.BrowserErrorPayload();
        payload.setErrorType("JavaScript Error");
        payload.setErrorMessage("boom");
        payload.setPageUrl("http://localhost:5500/teacher-courses.html");
        store.save(payload);

        webTestClient.get()
                .uri("/api/errors/browser?page=1&size=10")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.message").isEqualTo("获取错误日志列表成功")
                .jsonPath("$.data.totalElements").isEqualTo(1)
                .jsonPath("$.data.totalPages").isEqualTo(1)
                .jsonPath("$.data.content[0].errorMessage").isEqualTo("boom");
    }
}
