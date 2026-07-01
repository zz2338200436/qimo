package com._202510007517.major_assignment.client;

import com._202510007517.platform.user.api.dto.StudentProfileDTO;
import com._202510007517.platform.user.api.dto.UpdateStudentProfileDTO;
import com._202510007517.platform.user.api.dto.UpdateUserProfileDTO;
import com._202510007517.platform.user.api.dto.UserProfileDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Optional;

@Component
public class UserServiceProfileClient {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceProfileClient.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String userServiceBaseUrl;

    @Autowired
    public UserServiceProfileClient(RestTemplateBuilder restTemplateBuilder,
                                    ObjectMapper objectMapper,
                                    @Value("${platform.user-service.base-url:http://localhost:8082}") String userServiceBaseUrl) {
        this(restTemplateBuilder.build(), objectMapper, userServiceBaseUrl);
    }

    UserServiceProfileClient(RestTemplate restTemplate, String userServiceBaseUrl) {
        this(restTemplate, new ObjectMapper(), userServiceBaseUrl);
    }

    UserServiceProfileClient(RestTemplate restTemplate, ObjectMapper objectMapper, String userServiceBaseUrl) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.userServiceBaseUrl = stripTrailingSlash(userServiceBaseUrl);
    }

    public Optional<StudentProfileDTO> getStudentProfile(Long studentId) {
        String url = studentProfileUrl(studentId);
        try {
            ResponseEntity<JsonNode> response = restTemplate.getForEntity(url, JsonNode.class);
            return readStudentProfile(response);
        } catch (RestClientException ex) {
            logger.warn("调用 User_Service 学生画像失败 studentId={} message={}", studentId, ex.getMessage());
            logger.debug("User_Service 学生画像调用异常详情", ex);
            return Optional.empty();
        } catch (Exception ex) {
            logger.warn("解析 User_Service 学生画像响应失败 studentId={} message={}", studentId, ex.getMessage());
            logger.debug("User_Service 学生画像响应解析异常详情", ex);
            return Optional.empty();
        }
    }

    public Optional<UserProfileDTO> getUserProfile(Long userId) {
        String url = UriComponentsBuilder.fromHttpUrl(userServiceBaseUrl)
                .path("/api/users/{userId}")
                .build(userId)
                .toString();
        try {
            ResponseEntity<JsonNode> response = restTemplate.getForEntity(url, JsonNode.class);
            return readResponseData(response, UserProfileDTO.class);
        } catch (RestClientException ex) {
            logger.warn("调用 User_Service 用户资料失败 userId={} message={}", userId, ex.getMessage());
            logger.debug("User_Service 用户资料调用异常详情", ex);
            return Optional.empty();
        } catch (Exception ex) {
            logger.warn("解析 User_Service 用户资料响应失败 userId={} message={}", userId, ex.getMessage());
            logger.debug("User_Service 用户资料响应解析异常详情", ex);
            return Optional.empty();
        }
    }

    public Optional<UserProfileDTO> updateUserProfile(Long userId, UpdateUserProfileDTO request) {
        String url = UriComponentsBuilder.fromHttpUrl(userServiceBaseUrl)
                .path("/api/users/{userId}")
                .build(userId)
                .toString();
        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url,
                    org.springframework.http.HttpMethod.PUT,
                    new org.springframework.http.HttpEntity<>(request),
                    JsonNode.class);
            return readResponseData(response, UserProfileDTO.class);
        } catch (RestClientException ex) {
            logger.warn("调用 User_Service 更新用户资料失败 userId={} message={}", userId, ex.getMessage());
            logger.debug("User_Service 更新用户资料调用异常详情", ex);
            return Optional.empty();
        } catch (Exception ex) {
            logger.warn("解析 User_Service 更新用户资料响应失败 userId={} message={}", userId, ex.getMessage());
            logger.debug("User_Service 更新用户资料响应解析异常详情", ex);
            return Optional.empty();
        }
    }

    public Optional<StudentProfileDTO> updateStudentProfile(Long studentId, UpdateStudentProfileDTO request) {
        String url = studentProfileUrl(studentId);
        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url,
                    org.springframework.http.HttpMethod.PUT,
                    new org.springframework.http.HttpEntity<>(request),
                    JsonNode.class);
            return readStudentProfile(response);
        } catch (RestClientException ex) {
            logger.warn("调用 User_Service 更新学生画像失败 studentId={} message={}", studentId, ex.getMessage());
            logger.debug("User_Service 更新学生画像调用异常详情", ex);
            return Optional.empty();
        } catch (Exception ex) {
            logger.warn("解析 User_Service 更新学生画像响应失败 studentId={} message={}", studentId, ex.getMessage());
            logger.debug("User_Service 更新学生画像响应解析异常详情", ex);
            return Optional.empty();
        }
    }

    private Optional<StudentProfileDTO> readStudentProfile(ResponseEntity<JsonNode> response) throws com.fasterxml.jackson.core.JsonProcessingException {
        return readResponseData(response, StudentProfileDTO.class);
    }

    private <T> Optional<T> readResponseData(ResponseEntity<JsonNode> response, Class<T> type) throws com.fasterxml.jackson.core.JsonProcessingException {
        JsonNode data = response.getBody() != null ? response.getBody().get("data") : null;
        if (data == null || data.isNull()) {
            return Optional.empty();
        }
        return Optional.of(objectMapper.treeToValue(data, type));
    }

    private String studentProfileUrl(Long studentId) {
        return UriComponentsBuilder.fromHttpUrl(userServiceBaseUrl)
                .path("/api/users/students/{studentId}")
                .build(studentId)
                .toString();
    }

    private static String stripTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "http://localhost:8082";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
