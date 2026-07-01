package com._202510007517.major_assignment.controller;

import com._202510007517.major_assignment.annotation.RequireLogin;
import com._202510007517.major_assignment.constants.RoleConstants;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AnalysisControllerTest {

    @Test
    void controllerRequiresTeacherLoginAtClassLevel() {
        RequireLogin requireLogin = AnalysisController.class.getAnnotation(RequireLogin.class);

        assertThat(requireLogin).isNotNull();
        assertThat(requireLogin.roles()).containsExactly(RoleConstants.TEACHER);
    }
}
