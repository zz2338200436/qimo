package com._202510007517.platform.user.web;

import com._202510007517.platform.common.web.CommonTraceConstants;
import com._202510007517.platform.user.api.dto.StudentProfileDTO;
import com._202510007517.platform.user.api.dto.UpdateStudentProfileDTO;
import com._202510007517.platform.user.api.dto.UpdateUserProfileDTO;
import com._202510007517.platform.user.api.dto.UserProfileDTO;
import com._202510007517.platform.user.service.UserApplicationService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.http.MediaType.APPLICATION_JSON;

class UserControllerTest {

    @Test
    void getUserByIdWrapsProfileInResponseResult() throws Exception {
        UserApplicationService service = mock(UserApplicationService.class);
        UserProfileDTO profile = new UserProfileDTO();
        profile.setId(7L);
        profile.setUsername("teacher7");
        profile.setName("教师七");
        profile.setRoles(List.of("TEACHER"));
        when(service.getProfile(7L)).thenReturn(profile);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new UserController(service)).build();

        mockMvc.perform(get("/api/users/{userId}", 7L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(7))
                .andExpect(jsonPath("$.data.username").value("teacher7"))
                .andExpect(jsonPath("$.data.roles[0]").value("TEACHER"));
    }

    @Test
    void getCurrentUserReadsUserIdFromHeaderAndWrapsProfileInResponseResult() throws Exception {
        UserApplicationService service = mock(UserApplicationService.class);
        UserProfileDTO profile = new UserProfileDTO();
        profile.setId(7L);
        profile.setUsername("teacher7");
        profile.setName("教师七");
        profile.setRoles(List.of("TEACHER"));
        when(service.getProfile(7L)).thenReturn(profile);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new UserController(service)).build();

        mockMvc.perform(get("/api/users/me")
                        .header(CommonTraceConstants.USER_ID_HEADER, "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(7))
                .andExpect(jsonPath("$.data.username").value("teacher7"))
                .andExpect(jsonPath("$.data.roles[0]").value("TEACHER"));
    }

    @Test
    void getStudentByIdWrapsStudentProfileInResponseResult() throws Exception {
        UserApplicationService service = mock(UserApplicationService.class);
        StudentProfileDTO profile = new StudentProfileDTO();
        profile.setStudentId(42L);
        profile.setUsername("student42");
        profile.setRealName("学生四二");
        profile.setClassName("未知班级");
        profile.setRoles(List.of("STUDENT"));
        when(service.getStudentProfile(42L)).thenReturn(profile);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new UserController(service)).build();

        mockMvc.perform(get("/api/users/students/{studentId}", 42L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.studentId").value(42))
                .andExpect(jsonPath("$.data.realName").value("学生四二"))
                .andExpect(jsonPath("$.data.className").value("未知班级"))
                .andExpect(jsonPath("$.data.roles[0]").value("STUDENT"));
    }

    @Test
    void updateStudentByIdWrapsUpdatedStudentProfileInResponseResult() throws Exception {
        UserApplicationService service = mock(UserApplicationService.class);
        StudentProfileDTO profile = new StudentProfileDTO();
        profile.setStudentId(42L);
        profile.setRealName("新名字");
        profile.setEmail("new42@example.com");
        profile.setRoles(List.of("STUDENT"));
        ArgumentCaptor<UpdateStudentProfileDTO> requestCaptor = ArgumentCaptor.forClass(UpdateStudentProfileDTO.class);
        when(service.updateStudentProfile(org.mockito.ArgumentMatchers.eq(42L), requestCaptor.capture())).thenReturn(profile);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new UserController(service)).build();

        mockMvc.perform(put("/api/users/students/{studentId}", 42L)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"realName":"新名字","email":"new42@example.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.studentId").value(42))
                .andExpect(jsonPath("$.data.realName").value("新名字"))
                .andExpect(jsonPath("$.data.email").value("new42@example.com"));

        assertThat(requestCaptor.getValue().getRealName()).isEqualTo("新名字");
        assertThat(requestCaptor.getValue().getEmail()).isEqualTo("new42@example.com");
    }

    @Test
    void updateUserByIdWrapsUpdatedProfileInResponseResult() throws Exception {
        UserApplicationService service = mock(UserApplicationService.class);
        UserProfileDTO profile = new UserProfileDTO();
        profile.setId(7L);
        profile.setUsername("teacher7");
        profile.setName("教师新名");
        profile.setPhone("13900000007");
        profile.setRoles(List.of("TEACHER"));
        ArgumentCaptor<UpdateUserProfileDTO> requestCaptor = ArgumentCaptor.forClass(UpdateUserProfileDTO.class);
        when(service.updateProfile(org.mockito.ArgumentMatchers.eq(7L), requestCaptor.capture())).thenReturn(profile);
        MockMvc mockMvc = standaloneWithStudentCompatibility(service);

        mockMvc.perform(put("/api/users/{userId}", 7L)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name":"教师新名","phone":"13900000007"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(7))
                .andExpect(jsonPath("$.data.name").value("教师新名"))
                .andExpect(jsonPath("$.data.phone").value("13900000007"));

        assertThat(requestCaptor.getValue().getName()).isEqualTo("教师新名");
        assertThat(requestCaptor.getValue().getPhone()).isEqualTo("13900000007");
    }

    @Test
    void updateCurrentUserReadsUserIdFromHeaderAndWrapsUpdatedProfileInResponseResult() throws Exception {
        UserApplicationService service = mock(UserApplicationService.class);
        UserProfileDTO profile = new UserProfileDTO();
        profile.setId(7L);
        profile.setUsername("teacher7");
        profile.setName("教师新名");
        profile.setPhone("13900000007");
        profile.setRoles(List.of("TEACHER"));
        ArgumentCaptor<UpdateUserProfileDTO> requestCaptor = ArgumentCaptor.forClass(UpdateUserProfileDTO.class);
        when(service.updateProfile(org.mockito.ArgumentMatchers.eq(7L), requestCaptor.capture())).thenReturn(profile);
        MockMvc mockMvc = standaloneWithStudentCompatibility(service);

        mockMvc.perform(put("/api/users/me")
                        .header(CommonTraceConstants.USER_ID_HEADER, "7")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name":"教师新名","phone":"13900000007"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(7))
                .andExpect(jsonPath("$.data.name").value("教师新名"))
                .andExpect(jsonPath("$.data.phone").value("13900000007"));

        assertThat(requestCaptor.getValue().getName()).isEqualTo("教师新名");
        assertThat(requestCaptor.getValue().getPhone()).isEqualTo("13900000007");
    }

    @Test
    void legacyStudentProfileReadsCurrentUserHeader() throws Exception {
        UserApplicationService service = mock(UserApplicationService.class);
        StudentProfileDTO profile = studentProfile();
        when(service.getStudentProfile(42L)).thenReturn(profile);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new UserController(service),
                new StudentCompatibilityController(service)).build();

        mockMvc.perform(get("/api/student/profile")
                        .header(CommonTraceConstants.USER_ID_HEADER, "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("获取用户信息成功"))
                .andExpect(jsonPath("$.data.id").value(42))
                .andExpect(jsonPath("$.data.studentId").value(42))
                .andExpect(jsonPath("$.data.username").value("student42"))
                .andExpect(jsonPath("$.data.name").value("学生四二"))
                .andExpect(jsonPath("$.data.className").value("计科 2301"));
    }

    @Test
    void legacyStudentProfileUpdateMapsLegacyNameToStudentProfile() throws Exception {
        UserApplicationService service = mock(UserApplicationService.class);
        StudentProfileDTO profile = studentProfile();
        profile.setRealName("新名字");
        ArgumentCaptor<UpdateStudentProfileDTO> requestCaptor = ArgumentCaptor.forClass(UpdateStudentProfileDTO.class);
        when(service.updateStudentProfile(org.mockito.ArgumentMatchers.eq(42L), requestCaptor.capture())).thenReturn(profile);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new UserController(service),
                new StudentCompatibilityController(service)).build();

        mockMvc.perform(put("/api/student/profile")
                        .header(CommonTraceConstants.USER_ID_HEADER, "42")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name":"新名字","email":"new42@example.com","phone":"13800000042"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("个人资料更新成功"))
                .andExpect(jsonPath("$.data").value(true));

        assertThat(requestCaptor.getValue().getRealName()).isEqualTo("新名字");
        assertThat(requestCaptor.getValue().getEmail()).isEqualTo("new42@example.com");
        assertThat(requestCaptor.getValue().getPhone()).isEqualTo("13800000042");
    }

    @Test
    void legacyStudentSettingsReturnStableDefaultsAndAcceptUpdates() throws Exception {
        UserApplicationService service = mock(UserApplicationService.class);
        when(service.getNotificationSettings(42L)).thenReturn(Map.of(
                "emailNotifications", false,
                "webNotifications", false,
                "assignmentNotifications", false));
        when(service.updateNotificationSettings(org.mockito.ArgumentMatchers.eq(42L), org.mockito.ArgumentMatchers.anyMap()))
                .thenReturn(true);
        when(service.getPrivacySettings(42L)).thenReturn(Map.of(
                "shareProfile", false,
                "shareAchievements", false,
                "dataCollection", false));
        when(service.updatePrivacySettings(org.mockito.ArgumentMatchers.eq(42L), org.mockito.ArgumentMatchers.anyMap()))
                .thenReturn(true);
        MockMvc mockMvc = standaloneWithStudentCompatibility(service);

        mockMvc.perform(get("/api/student/notification-settings")
                        .header(CommonTraceConstants.USER_ID_HEADER, "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.emailNotifications").value(false))
                .andExpect(jsonPath("$.data.assignmentNotifications").value(false));

        mockMvc.perform(put("/api/student/notification-settings")
                        .header(CommonTraceConstants.USER_ID_HEADER, "42")
                        .contentType(APPLICATION_JSON)
                        .content("{\"emailNotifications\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));

        mockMvc.perform(get("/api/student/privacy-settings")
                        .header(CommonTraceConstants.USER_ID_HEADER, "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.shareProfile").value(false))
                .andExpect(jsonPath("$.data.dataCollection").value(false));

        mockMvc.perform(put("/api/student/privacy-settings")
                        .header(CommonTraceConstants.USER_ID_HEADER, "42")
                        .contentType(APPLICATION_JSON)
                        .content("{\"shareProfile\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void legacyStudentAvatarClassAndExportDataKeepOldEnvelope() throws Exception {
        UserApplicationService service = mock(UserApplicationService.class);
        when(service.uploadAvatar(42L, "/avatars/new.png")).thenReturn(true);
        when(service.getStudentClassName(42L)).thenReturn("计科 2301");
        when(service.exportStudentData(42L)).thenReturn(Map.of(
                "basicInfo", Map.of("id", 42L, "username", "student42"),
                "courses", List.of(),
                "learningStats", Map.of(),
                "scores", List.of()));
        MockMvc mockMvc = standaloneWithStudentCompatibility(service);

        mockMvc.perform(post("/api/student/upload-avatar")
                        .header(CommonTraceConstants.USER_ID_HEADER, "42")
                        .contentType(APPLICATION_JSON)
                        .content("{\"avatar\":\"/avatars/new.png\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("头像上传成功"))
                .andExpect(jsonPath("$.data").value(true));

        mockMvc.perform(get("/api/students/42/class")
                        .header(CommonTraceConstants.USER_ID_HEADER, "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("获取班级名称成功"))
                .andExpect(jsonPath("$.data").value("计科 2301"));

        mockMvc.perform(get("/api/student/export-data")
                        .header(CommonTraceConstants.USER_ID_HEADER, "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("数据导出成功"))
                .andExpect(jsonPath("$.data.basicInfo.id").value(42));

        verify(service).uploadAvatar(42L, "/avatars/new.png");
    }

    private static StudentProfileDTO studentProfile() {
        StudentProfileDTO profile = new StudentProfileDTO();
        profile.setStudentId(42L);
        profile.setUsername("student42");
        profile.setRealName("学生四二");
        profile.setEmail("student42@example.com");
        profile.setPhone("13800000042");
        profile.setAvatar("/avatars/42.png");
        profile.setClassName("计科 2301");
        profile.setRoles(List.of("STUDENT"));
        return profile;
    }

    private static MockMvc standaloneWithStudentCompatibility(UserApplicationService service) {
        return MockMvcBuilders.standaloneSetup(
                new UserController(service),
                new StudentCompatibilityController(service)).build();
    }
}
