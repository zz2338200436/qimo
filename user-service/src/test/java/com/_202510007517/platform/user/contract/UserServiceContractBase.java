package com._202510007517.platform.user.contract;

import com._202510007517.platform.user.api.dto.StudentProfileDTO;
import com._202510007517.platform.user.api.dto.UpdateStudentProfileDTO;
import com._202510007517.platform.user.api.dto.UpdateUserProfileDTO;
import com._202510007517.platform.user.api.dto.UserProfileDTO;
import com._202510007517.platform.user.api.dto.UserRolesDTO;
import com._202510007517.platform.user.service.UserApplicationService;
import com._202510007517.platform.user.controller.UserController;
import com._202510007517.platform.user.controller.UserInternalController;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class UserServiceContractBase {

    private UserApplicationService userApplicationService;

    @BeforeEach
    void setup() {
        userApplicationService = mock(UserApplicationService.class);
        stubUserContracts();
        RestAssuredMockMvc.standaloneSetup(
                new UserController(userApplicationService),
                new UserInternalController(userApplicationService)
        );
    }

    private void stubUserContracts() {
        UserProfileDTO teacher = userProfile("teacher7", "教师七", "teacher7@example.com", "13900000007");
        when(userApplicationService.getProfile(7L)).thenReturn(teacher);
        when(userApplicationService.getProfileByUsername("teacher7")).thenReturn(teacher);
        when(userApplicationService.getRoles(7L)).thenReturn(List.of("TEACHER"));

        UserRolesDTO roles = new UserRolesDTO();
        roles.setUserId(7L);
        roles.setRoles(List.of("TEACHER"));
        when(userApplicationService.getRolesDetail(7L)).thenReturn(roles);

        UserProfileDTO updatedTeacher = userProfile("teacher7", "教师新名", "teacher7@example.com", "13900000007");
        when(userApplicationService.updateProfile(eq(7L), any(UpdateUserProfileDTO.class))).thenReturn(updatedTeacher);

        StudentProfileDTO student = studentProfile("学生四二", "student42@example.com", "13800000042", "/avatars/42.png");
        when(userApplicationService.getStudentProfile(42L)).thenReturn(student);

        StudentProfileDTO updatedStudent = studentProfile("学生新名", "new42@example.com", "13800000042", "/avatars/42-new.png");
        when(userApplicationService.updateStudentProfile(eq(42L), any(UpdateStudentProfileDTO.class))).thenReturn(updatedStudent);

        when(userApplicationService.listByIds(List.of(7L, 42L))).thenReturn(List.of(teacher, userProfile("student42", "学生四二", "student42@example.com", "13800000042")));
    }

    private static UserProfileDTO userProfile(String username, String name, String email, String phone) {
        UserProfileDTO dto = new UserProfileDTO();
        dto.setId(username.startsWith("teacher") ? 7L : 42L);
        dto.setUsername(username);
        dto.setName(name);
        dto.setEmail(email);
        dto.setPhone(phone);
        dto.setRoles(List.of(username.startsWith("teacher") ? "TEACHER" : "STUDENT"));
        return dto;
    }

    private static StudentProfileDTO studentProfile(String realName, String email, String phone, String avatar) {
        StudentProfileDTO dto = new StudentProfileDTO();
        dto.setStudentId(42L);
        dto.setUsername("student42");
        dto.setRealName(realName);
        dto.setEmail(email);
        dto.setPhone(phone);
        dto.setAvatar(avatar);
        dto.setClassName("未知班级");
        dto.setRoles(List.of("STUDENT"));
        return dto;
    }
}
