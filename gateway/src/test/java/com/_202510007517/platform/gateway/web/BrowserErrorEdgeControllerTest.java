package com._202510007517.platform.gateway.web;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BrowserErrorEdgeControllerTest {

    @Test
    void reportBrowserErrorReturnsSuccessEnvelopeAndGeneratedId() {
        BrowserErrorEdgeController controller = new BrowserErrorEdgeController(new BrowserErrorEdgeStore());
        BrowserErrorEdgeStore.BrowserErrorPayload payload = new BrowserErrorEdgeStore.BrowserErrorPayload();
        payload.setErrorType("JavaScript Error");
        payload.setErrorMessage("boom");
        payload.setPageUrl("http://localhost:5500/teacher-courses.html");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");

        var response = controller.reportBrowserError(payload, "7", request);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("错误日志上报成功");
        assertThat(response.getData()).isEqualTo(1L);
    }

    @Test
    void browserErrorListReturnsLegacyCompatiblePageEnvelope() {
        BrowserErrorEdgeStore store = new BrowserErrorEdgeStore();
        BrowserErrorEdgeStore.BrowserErrorPayload payload = new BrowserErrorEdgeStore.BrowserErrorPayload();
        payload.setErrorType("JavaScript Error");
        payload.setErrorMessage("boom");
        payload.setPageUrl("http://localhost:5500/teacher-courses.html");
        store.save(payload);
        BrowserErrorEdgeController controller = new BrowserErrorEdgeController(store);

        var response = controller.getBrowserErrorList(Map.of(), 1, 10);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("获取错误日志列表成功");
        assertThat(response.getData()).containsKeys("content", "totalElements", "totalPages");
        assertThat((Integer) response.getData().get("totalElements")).isEqualTo(1);
        assertThat((Integer) response.getData().get("totalPages")).isEqualTo(1);
        @SuppressWarnings("unchecked")
        List<BrowserErrorEdgeStore.BrowserErrorPayload> content =
                (List<BrowserErrorEdgeStore.BrowserErrorPayload>) response.getData().get("content");
        assertThat(content).hasSize(1);
        assertThat(content.get(0).getErrorMessage()).isEqualTo("boom");
    }
}
