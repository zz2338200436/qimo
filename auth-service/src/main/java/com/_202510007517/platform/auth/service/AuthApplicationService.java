package com._202510007517.platform.auth.service;

import com._202510007517.platform.auth.domain.AuthCredential;
import com._202510007517.platform.auth.repository.AuthCredentialRepository;
import com._202510007517.platform.auth.controller.dto.AuthUserDTO;
import com._202510007517.platform.auth.controller.dto.LoginResponseDTO;
import com._202510007517.platform.common.exception.BusinessException;
import com._202510007517.platform.common.exception.ForbiddenException;
import com._202510007517.platform.common.exception.RemoteClientException;
import com._202510007517.platform.common.exception.RemoteServerException;
import com._202510007517.platform.common.exception.UnauthorizedException;
import com._202510007517.platform.user.api.dto.UserProfileDTO;
import com._202510007517.platform.user.api.feign.UserFeignClient;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

@Service
public class AuthApplicationService {

    private final AuthCredentialRepository credentialRepository;
    private final UserFeignClient userFeignClient;
    private final CaptchaService captchaService;
    private final JwtTokenService jwtTokenService;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthApplicationService(AuthCredentialRepository credentialRepository,
                                  UserFeignClient userFeignClient,
                                  CaptchaService captchaService,
                                  JwtTokenService jwtTokenService,
                                  BCryptPasswordEncoder passwordEncoder) {
        this.credentialRepository = credentialRepository;
        this.userFeignClient = userFeignClient;
        this.captchaService = captchaService;
        this.jwtTokenService = jwtTokenService;
        this.passwordEncoder = passwordEncoder;
    }

    public LoginResponseDTO login(String username, String password, String captcha, String captchaKey) {
        if (!captchaService.verify(captchaKey, captcha)) {
            throw new BusinessException("验证码错误或已过期", 400);
        }
        AuthCredential credential = credentialRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("用户名或密码错误"));
        if (!credential.isEnabled()) {
            throw new ForbiddenException("账号已被禁用");
        }
        if (!passwordEncoder.matches(password, credential.getPasswordHash())) {
            throw new UnauthorizedException("用户名或密码错误");
        }

        UserProfileDTO profile = loadUserProfile(credential.getUserId());
        List<String> roles = normalizeRoles(loadUserRoles(credential.getUserId()));
        String activeRole = roles.isEmpty() ? "STUDENT" : roles.get(0);
        TokenPair tokenPair = jwtTokenService.issueTokenPair(credential.getUserId(), username, roles, activeRole);
        return toLoginResponse(profile, roles, activeRole, tokenPair);
    }

    public LoginResponseDTO refresh(String refreshToken) {
        JwtTokenService.RefreshContext context = jwtTokenService.readRefreshContext(refreshToken);
        UserProfileDTO profile = loadUserProfile(context.userId());
        List<String> roles = normalizeRoles(loadUserRoles(context.userId()));
        TokenPair tokenPair = jwtTokenService.refresh(refreshToken, profile.getUsername(), roles);
        return toLoginResponse(profile, roles, context.activeRole(), tokenPair);
    }

    public LoginResponseDTO switchRole(String accessToken, String targetRole) {
        AuthTokenClaims claims = jwtTokenService.verifyAccessToken(accessToken);
        List<String> roles = normalizeRoles(claims.roles());
        String normalizedTarget = normalizeRole(targetRole);
        if (!roles.contains(normalizedTarget)) {
            throw new ForbiddenException("目标角色不在当前用户角色集合内");
        }
        jwtTokenService.revokeAccessToken(accessToken);
        UserProfileDTO profile = loadUserProfile(claims.userId());
        TokenPair tokenPair = jwtTokenService.issueTokenPair(claims.userId(), profile.getUsername(), roles, normalizedTarget);
        return toLoginResponse(profile, roles, normalizedTarget, tokenPair);
    }

    public AuthUserDTO me(String accessToken) {
        AuthTokenClaims claims = jwtTokenService.verifyAccessToken(accessToken);
        UserProfileDTO profile = loadUserProfile(claims.userId());
        return toUser(profile, normalizeRoles(claims.roles()), claims.activeRole());
    }

    public void logout(String accessToken, String refreshToken) {
        jwtTokenService.revokeAccessToken(accessToken);
        jwtTokenService.deleteRefreshToken(refreshToken);
    }

    public void changePassword(String accessToken, String currentPassword, String newPassword) {
        AuthTokenClaims claims = jwtTokenService.verifyAccessToken(accessToken);
        changePasswordByUserId(claims.userId(), currentPassword, newPassword);
        jwtTokenService.revokeAccessToken(accessToken);
    }

    public void changePasswordByUserId(Long userId, String currentPassword, String newPassword) {
        AuthCredential credential = credentialRepository.findByUserId(userId)
                .orElseThrow(() -> new UnauthorizedException("用户凭证不存在"));
        if (!passwordEncoder.matches(currentPassword, credential.getPasswordHash())) {
            throw new BusinessException("当前密码错误", 400);
        }
        if (passwordEncoder.matches(newPassword, credential.getPasswordHash())) {
            throw new BusinessException("新密码不能与当前密码相同", 400);
        }
        if (!isPasswordStrong(newPassword)) {
            throw new BusinessException("密码强度不足，需同时包含字母和数字", 400);
        }
        credentialRepository.updatePassword(userId, passwordEncoder.encode(newPassword));
    }

    public AuthTokenClaims introspect(String accessToken) {
        return jwtTokenService.verifyAccessToken(accessToken);
    }

    public void revokeByUser(Long userId) {
        jwtTokenService.revokeByUser(userId);
    }

    public String bearer(String authorization) {
        return jwtTokenService.extractBearer(authorization);
    }

    private UserProfileDTO loadUserProfile(Long userId) {
        return invokeUserService(() -> userFeignClient.getProfile(userId));
    }

    private List<String> loadUserRoles(Long userId) {
        return invokeUserService(() -> userFeignClient.getRoles(userId));
    }

    private <T> T invokeUserService(Supplier<T> action) {
        try {
            return action.get();
        } catch (RemoteClientException | RemoteServerException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new RemoteServerException(503, "user-service 暂不可用", "{\"service\":\"user-service\"}");
        }
    }

    private LoginResponseDTO toLoginResponse(UserProfileDTO profile, List<String> roles,
                                             String activeRole, TokenPair tokenPair) {
        LoginResponseDTO response = new LoginResponseDTO();
        response.setAccessToken(tokenPair.accessToken());
        response.setRefreshToken(tokenPair.refreshToken());
        response.setTokenType(tokenPair.tokenType());
        response.setExpiresAt(tokenPair.expiresAt());
        response.setUser(toUser(profile, roles, activeRole));
        return response;
    }

    private AuthUserDTO toUser(UserProfileDTO profile, List<String> roles, String activeRole) {
        AuthUserDTO user = new AuthUserDTO();
        user.setId(profile.getId());
        user.setUsername(profile.getUsername());
        user.setName(profile.getName());
        user.setEmail(maskEmail(profile.getEmail()));
        user.setPhone(maskPhone(profile.getPhone()));
        user.setRoles(roles);
        user.setActiveRole(activeRole);
        return user;
    }

    private List<String> normalizeRoles(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return List.of("STUDENT");
        }
        return roles.stream().map(this::normalizeRole).distinct().toList();
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "STUDENT";
        }
        return role.trim().toUpperCase(Locale.ROOT);
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@", 2);
        String prefix = parts[0].length() <= 2 ? parts[0].charAt(0) + "*" : parts[0].substring(0, 2) + "***";
        return prefix + "@" + parts[1];
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private boolean isPasswordStrong(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        boolean hasLetter = password.chars().anyMatch(Character::isLetter);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        return hasLetter && hasDigit;
    }
}
