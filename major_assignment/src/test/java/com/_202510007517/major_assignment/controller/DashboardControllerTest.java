package com._202510007517.major_assignment.controller;

import com._202510007517.major_assignment.client.UserServiceProfileClient;
import com._202510007517.major_assignment.entity.dto.ResponseResult;
import com._202510007517.major_assignment.entity.dto.StudentDashboardDTO;
import com._202510007517.major_assignment.mapper.StudentMapper;
import com._202510007517.major_assignment.service.StudentService;
import com._202510007517.major_assignment.service.UserService;
import com._202510007517.platform.user.api.dto.StudentProfileDTO;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DashboardControllerTest {

    @Test
    void getStudentPerformanceUsesUserServiceToVerifyStudentExists() {
        DashboardController controller = new DashboardController();
        UserService userService = mock(UserService.class);
        StudentService studentService = mock(StudentService.class);
        UserServiceProfileClient userServiceProfileClient = mock(UserServiceProfileClient.class);
        StudentDashboardDTO dashboard = new StudentDashboardDTO();
        dashboard.setCourseCount(3);
        StudentProfileDTO profile = new StudentProfileDTO();
        profile.setStudentId(42L);

        ReflectionTestUtils.setField(controller, "userService", userService);
        ReflectionTestUtils.setField(controller, "studentService", studentService);
        ReflectionTestUtils.setField(controller, "studentMapper", mock(StudentMapper.class));
        ReflectionTestUtils.setField(controller, "userServiceProfileClient", userServiceProfileClient);
        when(userServiceProfileClient.getStudentProfile(42L)).thenReturn(Optional.of(profile));
        when(studentService.getStudentPerformance(42L)).thenReturn(dashboard);

        ResponseResult<StudentDashboardDTO> response = controller.getStudentPerformance(42L, loggedInSession(9L));

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().getCourseCount()).isEqualTo(3);
        verifyNoInteractions(userService);
    }

    private static HttpSession loggedInSession(Long userId) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", userId);
        return session;
    }
}
