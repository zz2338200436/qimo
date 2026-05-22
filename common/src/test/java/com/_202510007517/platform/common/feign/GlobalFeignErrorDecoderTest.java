package com._202510007517.platform.common.feign;

import com._202510007517.platform.common.exception.RemoteClientException;
import com._202510007517.platform.common.exception.RemoteServerException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Request;
import feign.Response;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class GlobalFeignErrorDecoderTest {

    private final GlobalFeignErrorDecoder decoder = new GlobalFeignErrorDecoder(new ObjectMapper());

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 429, 499})
    void client_errors_must_decode_to_remote_client_exception(int status) {
        Exception decoded = decoder.decode("UserClient#getUser",
                response(status, "Client Error", "{\"message\":\"client-failure\"}"));

        RemoteClientException ex = assertInstanceOf(RemoteClientException.class, decoded);
        assertEquals(status, ex.getCode());
        assertEquals("client-failure", ex.getMessage());
    }

    @ParameterizedTest
    @ValueSource(ints = {500, 502, 503, 599})
    void server_errors_must_decode_to_remote_server_exception(int status) {
        Exception decoded = decoder.decode("UserClient#getUser",
                response(status, "Server Error", "{\"message\":\"server-failure\"}"));

        RemoteServerException ex = assertInstanceOf(RemoteServerException.class, decoded);
        assertEquals(status, ex.getCode());
        assertEquals("server-failure", ex.getMessage());
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 500})
    void decoder_must_fall_back_to_reason_when_body_is_not_json(int status) {
        Exception decoded = decoder.decode("UserClient#getUser",
                response(status, "Fallback Reason", "not-json"));

        if (status < 500) {
            RemoteClientException ex = assertInstanceOf(RemoteClientException.class, decoded);
            assertEquals(status, ex.getCode());
            assertEquals("Fallback Reason", ex.getMessage());
        } else {
            RemoteServerException ex = assertInstanceOf(RemoteServerException.class, decoded);
            assertEquals(status, ex.getCode());
            assertEquals("Fallback Reason", ex.getMessage());
        }
    }

    private static Response response(int status, String reason, String body) {
        return Response.builder()
                .status(status)
                .reason(reason)
                .request(Request.create(
                        Request.HttpMethod.GET,
                        "http://example.test/api/users/42",
                        Collections.emptyMap(),
                        null,
                        StandardCharsets.UTF_8,
                        null))
                .headers(Collections.emptyMap())
                .body(body, StandardCharsets.UTF_8)
                .build();
    }
}
