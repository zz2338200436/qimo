package com._202510007517.major_assignment.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class AuthServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceClient.class);

    private final RestTemplate restTemplate;
    private final String authServiceBaseUrl;

    @Autowired
    public AuthServiceClient(RestTemplateBuilder restTemplateBuilder,
                             @Value("${platform.auth-service.base-url:http://localhost:8081}") String authServiceBaseUrl) {
        this(restTemplateBuilder.build(), authServiceBaseUrl);
    }

    AuthServiceClient(RestTemplate restTemplate, String authServiceBaseUrl) {
        this.restTemplate = restTemplate;
        this.authServiceBaseUrl = stripTrailingSlash(authServiceBaseUrl);
    }

    public boolean changePassword(Long userId, String currentPassword, String newPassword) {
        String url = UriComponentsBuilder.fromHttpUrl(authServiceBaseUrl)
                .path("/internal/auth/users/{userId}/change-password")
                .build(userId)
                .toString();
        Map<String, String> request = new LinkedHashMap<>();
        request.put("currentPassword", currentPassword);
        request.put("newPassword", newPassword);
        try {
            restTemplate.postForEntity(url, request, Void.class);
            return true;
        } catch (RestClientException ex) {
            logger.warn("调用 Auth_Service 修改密码失败 userId={} message={}", userId, ex.getMessage());
            logger.debug("Auth_Service 修改密码调用异常详情", ex);
            return false;
        }
    }

    private static String stripTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "http://localhost:8081";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
