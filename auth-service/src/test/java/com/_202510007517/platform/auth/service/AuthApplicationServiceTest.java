package com._202510007517.platform.auth.service;

import com._202510007517.platform.auth.domain.AuthCredential;
import com._202510007517.platform.auth.repository.AuthCredentialRepository;
import com._202510007517.platform.common.exception.BusinessException;
import com._202510007517.platform.common.exception.RemoteClientException;
import com._202510007517.platform.common.exception.RemoteServerException;
import com._202510007517.platform.user.api.dto.UserProfileDTO;
import com._202510007517.platform.user.api.feign.UserFeignClient;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthApplicationServiceTest {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void changePasswordByUserIdVerifiesCurrentPasswordAndUpdatesCredential() {
        FakeCredentialRepository repository = new FakeCredentialRepository(passwordEncoder.encode("oldPass1"));
        AuthApplicationService service = new AuthApplicationService(
                repository, mock(com._202510007517.platform.user.api.feign.UserFeignClient.class),
                mock(CaptchaService.class), mock(JwtTokenService.class), passwordEncoder);

        service.changePasswordByUserId(42L, "oldPass1", "newPass2");

        assertThat(repository.updatedUserId).isEqualTo(42L);
        assertThat(passwordEncoder.matches("newPass2", repository.updatedPasswordHash)).isTrue();
    }

    @Test
    void changePasswordByUserIdRejectsWrongCurrentPassword() {
        FakeCredentialRepository repository = new FakeCredentialRepository(passwordEncoder.encode("oldPass1"));
        AuthApplicationService service = new AuthApplicationService(
                repository, mock(com._202510007517.platform.user.api.feign.UserFeignClient.class),
                mock(CaptchaService.class), mock(JwtTokenService.class), passwordEncoder);

        assertThatThrownBy(() -> service.changePasswordByUserId(42L, "badPass1", "newPass2"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("当前密码错误");
        assertThat(repository.updatedPasswordHash).isNull();
    }

    @Test
    void loginReturnsServiceUnavailableWhenUserServiceFails() {
        FakeCredentialRepository repository = new FakeCredentialRepository(passwordEncoder.encode("Teach1234"));
        CaptchaService captchaService = mock(CaptchaService.class);
        when(captchaService.verify("captcha-key", "ABCD")).thenReturn(true);
        UserFeignClient userFeignClient = mock(UserFeignClient.class);
        when(userFeignClient.getProfile(7L)).thenThrow(new RuntimeException("user-service unavailable"));

        AuthApplicationService service = new AuthApplicationService(
                repository, userFeignClient, captchaService, mock(JwtTokenService.class), passwordEncoder);

        assertServiceUnavailable(() -> service.login("student42", "Teach1234", "ABCD", "captcha-key"));
    }

    @Test
    void refreshReturnsServiceUnavailableWhenUserServiceFails() {
        UserFeignClient userFeignClient = mock(UserFeignClient.class);
        when(userFeignClient.getProfile(7L)).thenReturn(profile(7L, "student42"));
        when(userFeignClient.getRoles(7L)).thenThrow(new RuntimeException("user-service unavailable"));
        JwtTokenService jwtTokenService = mock(JwtTokenService.class);
        when(jwtTokenService.readRefreshContext("refresh-token"))
                .thenReturn(new JwtTokenService.RefreshContext(7L, "STUDENT"));

        AuthApplicationService service = new AuthApplicationService(
                mock(AuthCredentialRepository.class), userFeignClient, mock(CaptchaService.class), jwtTokenService, passwordEncoder);

        assertServiceUnavailable(() -> service.refresh("refresh-token"));
    }

    @Test
    void switchRoleReturnsServiceUnavailableWhenUserServiceFails() {
        UserFeignClient userFeignClient = mock(UserFeignClient.class);
        when(userFeignClient.getProfile(7L)).thenThrow(new RuntimeException("user-service unavailable"));
        JwtTokenService jwtTokenService = mock(JwtTokenService.class);
        when(jwtTokenService.verifyAccessToken("access-token"))
                .thenReturn(new AuthTokenClaims(
                        7L,
                        "student42",
                        List.of("STUDENT"),
                        "STUDENT",
                        "jti-1",
                        "access",
                        Instant.now().plusSeconds(60)
                ));

        AuthApplicationService service = new AuthApplicationService(
                mock(AuthCredentialRepository.class), userFeignClient, mock(CaptchaService.class), jwtTokenService, passwordEncoder);

        assertServiceUnavailable(() -> service.switchRole("access-token", "STUDENT"));
    }

    @Test
    void meReturnsServiceUnavailableWhenUserServiceFails() {
        UserFeignClient userFeignClient = mock(UserFeignClient.class);
        when(userFeignClient.getProfile(7L)).thenThrow(new RuntimeException("user-service unavailable"));
        JwtTokenService jwtTokenService = mock(JwtTokenService.class);
        when(jwtTokenService.verifyAccessToken("access-token"))
                .thenReturn(new AuthTokenClaims(
                        7L,
                        "student42",
                        List.of("STUDENT"),
                        "STUDENT",
                        "jti-1",
                        "access",
                        Instant.now().plusSeconds(60)
                ));

        AuthApplicationService service = new AuthApplicationService(
                mock(AuthCredentialRepository.class), userFeignClient, mock(CaptchaService.class), jwtTokenService, passwordEncoder);

        assertServiceUnavailable(() -> service.me("access-token"));
    }

    @Test
    void loginPropagatesRemoteClientExceptionFromUserService() {
        FakeCredentialRepository repository = new FakeCredentialRepository(passwordEncoder.encode("Teach1234"));
        CaptchaService captchaService = mock(CaptchaService.class);
        when(captchaService.verify("captcha-key", "ABCD")).thenReturn(true);
        UserFeignClient userFeignClient = mock(UserFeignClient.class);
        when(userFeignClient.getProfile(7L))
                .thenThrow(new RemoteClientException(404, "用户不存在", "{\"message\":\"用户不存在\"}"));

        AuthApplicationService service = new AuthApplicationService(
                repository, userFeignClient, captchaService, mock(JwtTokenService.class), passwordEncoder);

        assertThatThrownBy(() -> service.login("student42", "Teach1234", "ABCD", "captcha-key"))
                .isInstanceOfSatisfying(RemoteClientException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo(404);
                    assertThat(ex.getMessage()).contains("用户不存在");
                });
    }

    private void assertServiceUnavailable(ThrowingCall call) {
        Throwable thrown = catchThrowable(call::run);
        assertThat(thrown)
                .isInstanceOfSatisfying(RemoteServerException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo(503);
                    assertThat(ex.getMessage()).contains("user-service 暂不可用");
                });
    }

    private UserProfileDTO profile(Long userId, String username) {
        UserProfileDTO profile = new UserProfileDTO();
        profile.setId(userId);
        profile.setUsername(username);
        profile.setName("Student");
        profile.setEmail("student@example.com");
        profile.setPhone("13812345678");
        return profile;
    }

    @FunctionalInterface
    private interface ThrowingCall {
        void run();
    }

    private static class FakeCredentialRepository implements AuthCredentialRepository {
        private final String passwordHash;
        private Long updatedUserId;
        private String updatedPasswordHash;

        private FakeCredentialRepository(String passwordHash) {
            this.passwordHash = passwordHash;
        }

        @Override
        public Optional<AuthCredential> findByUsername(String username) {
            AuthCredential credential = new AuthCredential();
            credential.setUserId(7L);
            credential.setUsername(username);
            credential.setPasswordHash(passwordHash);
            credential.setEnabled(true);
            return Optional.of(credential);
        }

        @Override
        public Optional<AuthCredential> findByUserId(Long userId) {
            AuthCredential credential = new AuthCredential();
            credential.setUserId(userId);
            credential.setUsername("student42");
            credential.setPasswordHash(passwordHash);
            credential.setEnabled(true);
            return Optional.of(credential);
        }

        @Override
        public void updatePassword(Long userId, String encodedPassword) {
            updatedUserId = userId;
            updatedPasswordHash = encodedPassword;
        }
    }
}
