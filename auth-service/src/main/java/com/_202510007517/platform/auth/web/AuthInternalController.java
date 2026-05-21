package com._202510007517.platform.auth.web;

import com._202510007517.platform.auth.api.dto.AuthContextDTO;
import com._202510007517.platform.auth.api.dto.TokenIntrospectionDTO;
import com._202510007517.platform.auth.service.AuthApplicationService;
import com._202510007517.platform.auth.service.AuthTokenClaims;
import com._202510007517.platform.auth.web.dto.ChangePasswordRequestDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/auth")
public class AuthInternalController {

    private final AuthApplicationService authService;

    public AuthInternalController(AuthApplicationService authService) {
        this.authService = authService;
    }

    @GetMapping("/context/{userId}")
    public AuthContextDTO context(@PathVariable Long userId) {
        AuthContextDTO context = new AuthContextDTO();
        context.setUserId(userId);
        return context;
    }

    @PostMapping("/introspect")
    public TokenIntrospectionDTO introspect(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        try {
            AuthTokenClaims claims = authService.introspect(authService.bearer(authorization));
            TokenIntrospectionDTO dto = new TokenIntrospectionDTO();
            dto.setActive(true);
            dto.setUserId(claims.userId());
            dto.setSubject(claims.subject());
            dto.setRoles(claims.roles());
            dto.setActiveRole(claims.activeRole());
            dto.setJti(claims.jti());
            dto.setTokenType(claims.tokenType());
            dto.setExpiresAt(claims.expiresAt());
            return dto;
        } catch (RuntimeException ex) {
            TokenIntrospectionDTO dto = new TokenIntrospectionDTO();
            dto.setActive(false);
            dto.setReason(ex.getMessage());
            return dto;
        }
    }

    @PostMapping("/revoke-by-user/{userId}")
    public void revokeByUser(@PathVariable Long userId) {
        authService.revokeByUser(userId);
    }

    @PostMapping("/users/{userId}/change-password")
    public void changePassword(@PathVariable Long userId,
                               @RequestBody @Valid ChangePasswordRequestDTO request) {
        authService.changePasswordByUserId(userId, request.getCurrentPassword(), request.getNewPassword());
    }
}
