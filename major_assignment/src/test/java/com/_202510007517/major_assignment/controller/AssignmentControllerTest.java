package com._202510007517.major_assignment.controller;

import com._202510007517.major_assignment.annotation.RequireLogin;
import com._202510007517.major_assignment.constants.RoleConstants;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class AssignmentControllerTest {

    @Test
    void controllerRequiresTeacherLoginAtClassLevel() {
        RequireLogin requireLogin = AssignmentController.class.getAnnotation(RequireLogin.class);

        assertThat(requireLogin).isNotNull();
        assertThat(requireLogin.roles()).containsExactly(RoleConstants.TEACHER);
    }

    @Test
    void noMethodShouldRepeatRequireLoginAnnotationOnceClassLevelGuardExists() {
        for (Method method : AssignmentController.class.getDeclaredMethods()) {
            if (!method.getName().startsWith("lambda$")) {
                assertThat(method.getAnnotation(RequireLogin.class))
                        .as("method %s should rely on class-level teacher guard", method.getName())
                        .isNull();
            }
        }
    }
}
