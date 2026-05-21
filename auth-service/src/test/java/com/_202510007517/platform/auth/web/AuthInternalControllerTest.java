package com._202510007517.platform.auth.web;

import com._202510007517.platform.auth.service.AuthTokenClaims;
import com._202510007517.platform.auth.service.AuthApplicationService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthInternalControllerTest {

    @Test
    void exposesInternalContextByUserId() throws Exception {
        AuthApplicationService service = mock(AuthApplicationService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AuthInternalController(service)).build();

        mockMvc.perform(get("/internal/auth/context/{userId}", 42L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(42));
    }

    @Test
    void introspectReturnsActiveClaims() throws Exception {
        AuthApplicationService service = mock(AuthApplicationService.class);
        Instant expiresAt = Instant.parse("2026-09-01T10:00:00Z");
        when(service.bearer("Bearer access-token")).thenReturn("access-token");
        when(service.introspect("access-token")).thenReturn(new AuthTokenClaims(
                42L,
                "student42",
                List.of("STUDENT"),
                "STUDENT",
                "jti-42",
                "Bearer",
                expiresAt
        ));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AuthInternalController(service)).build();

        mockMvc.perform(post("/internal/auth/introspect")
                        .header("Authorization", "Bearer access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.userId").value(42))
                .andExpect(jsonPath("$.subject").value("student42"))
                .andExpect(jsonPath("$.roles[0]").value("STUDENT"))
                .andExpect(jsonPath("$.activeRole").value("STUDENT"))
                .andExpect(jsonPath("$.jti").value("jti-42"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresAt").isNumber());

        verify(service).bearer("Bearer access-token");
        verify(service).introspect("access-token");
    }

    @Test
    void introspectReturnsInactiveWhenTokenCannotBeVerified() throws Exception {
        AuthApplicationService service = mock(AuthApplicationService.class);
        when(service.bearer("Bearer bad-token")).thenReturn("bad-token");
        when(service.introspect("bad-token")).thenThrow(new RuntimeException("token expired"));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AuthInternalController(service)).build();

        mockMvc.perform(post("/internal/auth/introspect")
                        .header("Authorization", "Bearer bad-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.reason").value("token expired"));
    }

    @Test
    void exposesRevokeByUserForInternalConsumers() throws Exception {
        AuthApplicationService service = mock(AuthApplicationService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AuthInternalController(service)).build();

        mockMvc.perform(post("/internal/auth/revoke-by-user/{userId}", 42L))
                .andExpect(status().isOk());

        verify(service).revokeByUser(42L);
    }

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
