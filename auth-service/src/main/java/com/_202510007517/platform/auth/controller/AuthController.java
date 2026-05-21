package com._202510007517.platform.auth.controller;

import com._202510007517.platform.auth.service.AuthApplicationService;
import com._202510007517.platform.auth.service.CaptchaService;
import com._202510007517.platform.auth.controller.dto.LoginRequestDTO;
import com._202510007517.platform.auth.controller.dto.LoginResponseDTO;
import com._202510007517.platform.auth.controller.dto.LogoutRequestDTO;
import com._202510007517.platform.auth.controller.dto.RefreshRequestDTO;
import com._202510007517.platform.auth.controller.dto.ChangePasswordRequestDTO;
import com._202510007517.platform.auth.controller.dto.SwitchRoleRequestDTO;
import com._202510007517.platform.common.web.ResponseResult;
import jakarta.validation.Valid;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.List;

@RestController
public class AuthController {

    private final AuthApplicationService authService;
    private final CaptchaService captchaService;

    public AuthController(AuthApplicationService authService, CaptchaService captchaService) {
        this.authService = authService;
        this.captchaService = captchaService;
    }

    @GetMapping("/api/auth/captcha")
    public ResponseEntity<byte[]> captcha() {
        CaptchaService.CaptchaImage captcha = captchaService.createCaptcha();
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .cacheControl(CacheControl.noStore())
                .header("X-Captcha-Key", captcha.key())
                .body(captcha.bytes());
    }

    @PostMapping("/api/auth/login")
    public ResponseResult<LoginResponseDTO> login(@RequestBody @Valid LoginRequestDTO request,
                                                  HttpServletResponse response) {
        LoginResponseDTO loginResponse = authService.login(
                request.getUsername(),
                request.getPassword(),
                request.getCaptcha(),
                request.getCaptchaKey()
        );
        clearLegacySessionCookies(response);
        return ResponseResult.success(
                loginResponse,
                "登录成功",
                200
        );
    }

    @PostMapping("/api/auth/refresh")
    public ResponseResult<LoginResponseDTO> refresh(@RequestBody @Valid RefreshRequestDTO request) {
        return ResponseResult.success(authService.refresh(request.getRefreshToken()), "刷新成功", 200);
    }

    @PostMapping("/api/auth/logout")
    public ResponseResult<Map<String, Object>> logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                                                      @RequestBody(required = false) LogoutRequestDTO request) {
        String token = authService.bearer(authorization);
        authService.logout(token, request != null ? request.getRefreshToken() : null);
        return ResponseResult.success(Map.of("revoked", true), "退出成功", 200);
    }

    @PostMapping("/api/auth/switch-role")
    public ResponseResult<LoginResponseDTO> switchRole(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                                                       @RequestBody @Valid SwitchRoleRequestDTO request) {
        return ResponseResult.success(
                authService.switchRole(authService.bearer(authorization), request.getTargetRole()),
                "角色切换成功",
                200
        );
    }

    @GetMapping("/api/auth/me")
    public ResponseResult<?> me(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return ResponseResult.success(authService.me(authService.bearer(authorization)), "获取成功", 200);
    }

    @PostMapping("/api/auth/change-password")
    public ResponseResult<Map<String, Object>> changePassword(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                                                              @RequestBody @Valid ChangePasswordRequestDTO request) {
        authService.changePassword(
                authService.bearer(authorization),
                request.getCurrentPassword(),
                request.getNewPassword()
        );
        return ResponseResult.success(Map.of("changed", true), "密码修改成功，请重新登录", 200);
    }

    @PostMapping("/api/student/change-password")
    public ResponseResult<Boolean> changeStudentPasswordCompatibility(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestBody Map<String, String> request) {
        String currentPassword = request != null ? request.get("currentPassword") : null;
        String newPassword = request != null ? request.get("newPassword") : null;
        String confirmPassword = request != null ? request.get("confirmPassword") : null;
        if (currentPassword == null || currentPassword.isBlank()
                || newPassword == null || newPassword.isBlank()
                || confirmPassword == null || confirmPassword.isBlank()) {
            return ResponseResult.failure("密码不能为空", 400);
        }
        if (!newPassword.equals(confirmPassword)) {
            return ResponseResult.failure("两次输入的密码不一致", 400);
        }
        if (newPassword.length() < 8) {
            return ResponseResult.failure("密码长度至少8位", 400);
        }
        authService.changePassword(authService.bearer(authorization), currentPassword, newPassword);
        return ResponseResult.success(true, "密码更新成功", 200);
    }

    @PutMapping("/api/auth/notification-settings")
    public ResponseResult<Map<String, Object>> saveNotificationSettingsCompatibility() {
        return ResponseResult.success(
                Map.of(
                        "persisted", false,
                        "mode", "compatibility-placeholder"),
                "通知设置保存成功",
                200);
    }

    private void clearLegacySessionCookies(HttpServletResponse response) {
        for (String cookieName : List.of("JSESSIONID", "JSESSIONID_TEACHER", "JSESSIONID_STUDENT", "JSESSIONID_ADMIN")) {
            Cookie cookie = new Cookie(cookieName, "");
            cookie.setPath("/");
            cookie.setHttpOnly(true);
            cookie.setMaxAge(0);
            response.addCookie(cookie);
        }
    }
}
