package com._202510007517.platform.common.feign;

import com._202510007517.platform.common.exception.RemoteClientException;
import com._202510007517.platform.common.exception.RemoteServerException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.Util;
import feign.codec.ErrorDecoder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class GlobalFeignErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultDecoder = new Default();
    private final ObjectMapper objectMapper;

    public GlobalFeignErrorDecoder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Exception decode(String methodKey, Response response) {
        int status = response.status();
        String bodyJson = readBody(response);
        String message = extractMessage(bodyJson, response.reason(), methodKey, status);

        if (status >= 400 && status < 500) {
            return new RemoteClientException(status, message, bodyJson);
        }
        if (status >= 500 && status < 600) {
            return new RemoteServerException(status, message, bodyJson);
        }
        return defaultDecoder.decode(methodKey, response);
    }

    private String readBody(Response response) {
        if (response.body() == null) {
            return "";
        }
        try {
            return Util.toString(response.body().asReader(StandardCharsets.UTF_8));
        } catch (IOException ex) {
            return "";
        }
    }

    private String extractMessage(String bodyJson, String reason, String methodKey, int status) {
        if (bodyJson != null && !bodyJson.isBlank()) {
            try {
                JsonNode root = objectMapper.readTree(bodyJson);
                JsonNode messageNode = root.get("message");
                if (messageNode != null && !messageNode.isNull() && !messageNode.asText().isBlank()) {
                    return messageNode.asText();
                }
            } catch (IOException ignored) {
                // ignore invalid JSON and fall back to reason/default message
            }
        }
        if (reason != null && !reason.isBlank()) {
            return reason;
        }
        return "Remote call failed: " + methodKey + " (" + status + ")";
    }
}
