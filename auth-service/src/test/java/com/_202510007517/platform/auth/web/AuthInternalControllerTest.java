package com._202510007517.platform.auth.web;

import com._202510007517.platform.auth.service.AuthApplicationService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthInternalControllerTest {

    @Test
    void exposesInternalChangePasswordByUserId() throws Exception {
        AuthApplicationService service = mock(AuthApplicationService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AuthInternalController(service)).build();
        ArgumentCaptor<String> currentPassword = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> newPassword = ArgumentCaptor.forClass(String.class);

        mockMvc.perform(post("/internal/auth/users/{userId}/change-password", 42L)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"oldPass1","newPassword":"newPass2"}
                                """))
                .andExpect(status().isOk());

        verify(service).changePasswordByUserId(org.mockito.ArgumentMatchers.eq(42L),
                currentPassword.capture(), newPassword.capture());
        assertThat(currentPassword.getValue()).isEqualTo("oldPass1");
        assertThat(newPassword.getValue()).isEqualTo("newPass2");
    }
}
