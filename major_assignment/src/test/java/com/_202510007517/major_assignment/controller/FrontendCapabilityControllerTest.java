package com._202510007517.major_assignment.controller;

import com._202510007517.major_assignment.entity.dto.ResponseResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FrontendCapabilityControllerTest {

    @Test
    void returnsStudentAndTeacherCapabilityMaps() {
        FrontendCapabilityController controller = new FrontendCapabilityController();

        ResponseResult<Map<String, Object>> response = controller.getCapabilities();

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).containsKeys("student", "teacher");

        Map<String, Object> student = castMap(response.getData().get("student"));
        Map<String, Object> teacher = castMap(response.getData().get("teacher"));

        assertThat(student).containsEntry("avatarUpload", true);
        assertThat(student).containsEntry("courses", true);
        assertThat(student).containsEntry("notificationSettings", false);
        assertThat(student).containsEntry("privacySettings", false);
        assertThat(teacher).containsEntry("notificationPersistence", false);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }
}
