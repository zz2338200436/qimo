package com._202510007517.platform.user.web;

import com._202510007517.platform.user.api.dto.StudentProfileDTO;
import com._202510007517.platform.user.api.dto.UpdateStudentProfileDTO;
import com._202510007517.platform.user.service.UserApplicationService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserInternalControllerTest {

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
}
