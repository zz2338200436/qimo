package com._202510007517.platform.user.web;

import com._202510007517.platform.user.api.dto.StudentProfileDTO;
import com._202510007517.platform.user.api.dto.UpdateStudentProfileDTO;
import com._202510007517.platform.user.api.dto.UpdateUserProfileDTO;
import com._202510007517.platform.user.api.dto.UserProfileDTO;
import com._202510007517.platform.user.api.dto.UserRolesDTO;
import com._202510007517.platform.user.service.UserApplicationService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserInternalControllerTest {

    @Test
    void exposesUserProfileByIdForInternalConsumers() throws Exception {
        UserApplicationService service = mock(UserApplicationService.class);
        when(service.getProfile(42L)).thenReturn(userProfile(42L, "student42"));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new UserInternalController(service)).build();

        mockMvc.perform(get("/internal/users/{userId}", 42L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.username").value("student42"))
                .andExpect(jsonPath("$.roles[0]").value("STUDENT"));

        verify(service).getProfile(42L);
    }

    @Test
    void exposesUserProfileUpdateForInternalConsumers() throws Exception {
        UserApplicationService service = mock(UserApplicationService.class);
        ArgumentCaptor<UpdateUserProfileDTO> requestCaptor = ArgumentCaptor.forClass(UpdateUserProfileDTO.class);
        when(service.updateProfile(eq(42L), requestCaptor.capture())).thenReturn(userProfile(42L, "student42"));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new UserInternalController(service)).build();

        mockMvc.perform(put("/internal/users/{userId}", 42L)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name":"学生四二","email":"new42@example.com","phone":"13800000042","avatar":"/avatar.png"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.username").value("student42"));

        UpdateUserProfileDTO request = requestCaptor.getValue();
        assertThat(request.getName()).isEqualTo("学生四二");
        assertThat(request.getEmail()).isEqualTo("new42@example.com");
        assertThat(request.getPhone()).isEqualTo("13800000042");
        assertThat(request.getAvatar()).isEqualTo("/avatar.png");
    }

    @Test
    void exposesUserProfileByUsernameForInternalConsumers() throws Exception {
        UserApplicationService service = mock(UserApplicationService.class);
        when(service.getProfileByUsername("student42")).thenReturn(userProfile(42L, "student42"));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new UserInternalController(service)).build();

        mockMvc.perform(get("/internal/users/by-username/{username}", "student42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.username").value("student42"));

        verify(service).getProfileByUsername("student42");
    }

    @Test
    void exposesRolesForInternalConsumers() throws Exception {
        UserApplicationService service = mock(UserApplicationService.class);
        when(service.getRoles(42L)).thenReturn(List.of("STUDENT", "TEACHER"));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new UserInternalController(service)).build();

        mockMvc.perform(get("/internal/users/{userId}/roles", 42L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("STUDENT"))
                .andExpect(jsonPath("$[1]").value("TEACHER"));

        verify(service).getRoles(42L);
    }

    @Test
    void exposesRolesDetailForInternalConsumers() throws Exception {
        UserApplicationService service = mock(UserApplicationService.class);
        UserRolesDTO roles = new UserRolesDTO();
        roles.setUserId(42L);
        roles.setRoles(List.of("STUDENT"));
        when(service.getRolesDetail(42L)).thenReturn(roles);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new UserInternalController(service)).build();

        mockMvc.perform(get("/internal/users/{userId}/roles-detail", 42L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(42))
                .andExpect(jsonPath("$.roles[0]").value("STUDENT"));

        verify(service).getRolesDetail(42L);
    }

    @Test
    void exposesListByIdsForInternalConsumers() throws Exception {
        UserApplicationService service = mock(UserApplicationService.class);
        when(service.listByIds(List.of(42L, 43L))).thenReturn(List.of(
                userProfile(42L, "student42"),
                userProfile(43L, "student43")
        ));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new UserInternalController(service)).build();

        mockMvc.perform(get("/internal/users")
                        .param("ids", "42", "43"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(42))
                .andExpect(jsonPath("$[1].username").value("student43"));

        verify(service).listByIds(List.of(42L, 43L));
    }

    @Test
    void exposesStudentProfileForInternalConsumers() throws Exception {
        UserApplicationService service = mock(UserApplicationService.class);
        StudentProfileDTO profile = new StudentProfileDTO();
        profile.setStudentId(42L);
        profile.setUsername("student42");
        profile.setRealName("学生四二");
        profile.setClassName("未知班级");
        profile.setRoles(List.of("STUDENT"));
        when(service.getStudentProfile(42L)).thenReturn(profile);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new UserInternalController(service)).build();

        mockMvc.perform(get("/internal/users/{studentId}/student-profile", 42L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(42))
                .andExpect(jsonPath("$.username").value("student42"))
                .andExpect(jsonPath("$.realName").value("学生四二"))
                .andExpect(jsonPath("$.className").value("未知班级"))
                .andExpect(jsonPath("$.roles[0]").value("STUDENT"));
    }

    @Test
    void exposesStudentProfileUpdateForInternalConsumers() throws Exception {
        UserApplicationService service = mock(UserApplicationService.class);
        StudentProfileDTO profile = new StudentProfileDTO();
        profile.setStudentId(42L);
        profile.setRealName("新名字");
        profile.setEmail("new42@example.com");
        ArgumentCaptor<UpdateStudentProfileDTO> requestCaptor = ArgumentCaptor.forClass(UpdateStudentProfileDTO.class);
        when(service.updateStudentProfile(org.mockito.ArgumentMatchers.eq(42L), requestCaptor.capture())).thenReturn(profile);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new UserInternalController(service)).build();

        mockMvc.perform(put("/internal/users/{studentId}/student-profile", 42L)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"realName":"新名字","email":"new42@example.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(42))
                .andExpect(jsonPath("$.realName").value("新名字"))
                .andExpect(jsonPath("$.email").value("new42@example.com"));

        assertThat(requestCaptor.getValue().getRealName()).isEqualTo("新名字");
        assertThat(requestCaptor.getValue().getEmail()).isEqualTo("new42@example.com");
    }

    private static UserProfileDTO userProfile(Long id, String username) {
        UserProfileDTO profile = new UserProfileDTO();
        profile.setId(id);
        profile.setUsername(username);
        profile.setName("学生四二");
        profile.setEmail(username + "@example.com");
        profile.setPhone("13800000042");
        profile.setRoles(List.of("STUDENT"));
        return profile;
    }
}
