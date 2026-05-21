package com._202510007517.platform.auth.web;

import com._202510007517.platform.auth.service.AuthApplicationService;
import com._202510007517.platform.auth.service.CaptchaService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthCompatibilityControllerTest {

    @Test
    void publicCaptchaCompatibilityEndpointReturnsImageAndCaptchaKeyHeader() throws Exception {
        CaptchaService captchaService = mock(CaptchaService.class);
        when(captchaService.createCaptcha()).thenReturn(new CaptchaService.CaptchaImage("captcha-key", new byte[]{1, 2, 3}));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new PublicCaptchaCompatibilityController(captchaService)).build();

        mockMvc.perform(get("/api/public/captcha"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Captcha-Key", "captcha-key"))
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(content().bytes(new byte[]{1, 2, 3}));
    }

    @Test
    void authNotificationSettingsCompatibilityEndpointReturnsSuccessEnvelope() throws Exception {
        AuthApplicationService authService = mock(AuthApplicationService.class);
        CaptchaService captchaService = mock(CaptchaService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService, captchaService)).build();

        mockMvc.perform(put("/api/auth/notification-settings")
                        .header("Authorization", "Bearer test-token")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "emailNotifications": true,
                                  "assignmentNotifications": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("通知设置保存成功"))
                .andExpect(jsonPath("$.data.persisted").value(false))
                .andExpect(jsonPath("$.data.mode").value("compatibility-placeholder"));

        verifyNoInteractions(authService, captchaService);
    }

    @Test
    void studentChangePasswordCompatibilityEndpointKeepsLegacyEnvelope() throws Exception {
        AuthApplicationService authService = mock(AuthApplicationService.class);
        when(authService.bearer("Bearer access-token")).thenReturn("access-token");
        CaptchaService captchaService = mock(CaptchaService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService, captchaService)).build();

        mockMvc.perform(post("/api/student/change-password")
                        .header("Authorization", "Bearer access-token")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "oldPass1",
                                  "newPassword": "newPass2",
                                  "confirmPassword": "newPass2"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("密码更新成功"))
                .andExpect(jsonPath("$.data").value(true));

        verify(authService).changePassword("access-token", "oldPass1", "newPass2");
        verifyNoInteractions(captchaService);
    }

    @Test
    void studentChangePasswordCompatibilityEndpointRejectsMismatchedConfirmationBeforeServiceCall() throws Exception {
        AuthApplicationService authService = mock(AuthApplicationService.class);
        CaptchaService captchaService = mock(CaptchaService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService, captchaService)).build();

        mockMvc.perform(post("/api/student/change-password")
                        .header("Authorization", "Bearer access-token")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "oldPass1",
                                  "newPassword": "newPass2",
                                  "confirmPassword": "newPass3"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("两次输入的密码不一致"))
                .andExpect(jsonPath("$.code").value(400));

        verifyNoInteractions(authService, captchaService);
    }
}
