package com._202510007517.major_assignment.controller;

import com._202510007517.major_assignment.config.MultiRoleSessionFilter;
import com._202510007517.major_assignment.constants.RoleConstants;
import com._202510007517.major_assignment.entity.dto.ResponseResult;
import com._202510007517.major_assignment.mapper.CourseMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AIControllerTest {

    @Test
    void generateQuestionsReturnsNotImplementedForAuthenticatedUser() {
        AIController controller = new AIController(mock(CourseMapper.class));

        ResponseResult<Map<String, Object>> response = controller.generateQuestions(
                Map.of("topic", "Java", "count", 3, "difficulty", "中等"),
                loggedInRequest(7L, RoleConstants.TEACHER));

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getCode()).isEqualTo(501);
        assertThat(response.getMessage()).contains("AI能力已迁移至独立服务");
        assertThat(response.getData()).isNull();
    }

    @Test
    void generateExamReturnsNotImplementedForAuthenticatedUser() {
        AIController controller = new AIController(mock(CourseMapper.class));

        ResponseResult<Map<String, Object>> response = controller.generateExam(
                Map.of("courseName", "高等数学", "totalScore", 100, "duration", 90, "difficulty", "中等"),
                loggedInRequest(7L, RoleConstants.TEACHER));

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getCode()).isEqualTo(501);
        assertThat(response.getMessage()).contains("AI能力已迁移至独立服务");
        assertThat(response.getData()).isNull();
    }

    @Test
    void learningSuggestionsReturnsNotImplementedForAuthenticatedStudent() {
        AIController controller = new AIController(mock(CourseMapper.class));

        ResponseResult<Map<String, Object>> response = controller.getLearningSuggestions(
                Map.of(),
                loggedInRequest(42L, RoleConstants.STUDENT));

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getCode()).isEqualTo(501);
        assertThat(response.getMessage()).contains("AI能力已迁移至独立服务");
        assertThat(response.getData()).isNull();
    }

    @Test
    void generateQuestionsStillRejectsUnauthenticatedRequests() {
        AIController controller = new AIController(mock(CourseMapper.class));

        ResponseResult<Map<String, Object>> response = controller.generateQuestions(
                Map.of("topic", "Java", "count", 3, "difficulty", "中等"),
                new MockHttpServletRequest());

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getCode()).isEqualTo(401);
        assertThat(response.getMessage()).isEqualTo("未授权，请重新登录");
    }

    private static MockHttpServletRequest loggedInRequest(Long userId, String role) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(
                MultiRoleSessionFilter.CURRENT_USER_ATTR,
                MultiRoleSessionFilter.AuthUser.of(userId, List.of(role)));
        return request;
    }
}
