package com._202510007517.major_assignment.config;

import com._202510007517.platform.common.web.CommonTraceConstants;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 过渡期桥接过滤器：
 * 将 Gateway 注入的 JWT 身份透传头同步到单体的 request/session/security context，
 * 让 legacy-route 下的受保护接口在 JWT-only 环境中也能读取到登录态。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class GatewayJwtBridgeFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        Authentication existing = SecurityContextHolder.getContext().getAuthentication();
        if (isAuthenticated(existing)
                && httpRequest.getAttribute(MultiRoleSessionFilter.CURRENT_USER_ATTR) instanceof MultiRoleSessionFilter.AuthUser) {
            chain.doFilter(request, response);
            return;
        }

        String userIdHeader = httpRequest.getHeader(CommonTraceConstants.USER_ID_HEADER);
        if (userIdHeader == null || userIdHeader.isBlank()) {
            chain.doFilter(request, response);
            return;
        }

        Long userId;
        try {
            userId = Long.valueOf(userIdHeader.trim());
        } catch (NumberFormatException ex) {
            chain.doFilter(request, response);
            return;
        }

        List<String> roles = parseRoles(httpRequest.getHeader(CommonTraceConstants.ROLES_HEADER));
        MultiRoleSessionFilter.AuthUser authUser = MultiRoleSessionFilter.AuthUser.of(userId, roles);

        httpRequest.setAttribute(MultiRoleSessionFilter.CURRENT_USER_ATTR, authUser);
        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(MultiRoleSessionFilter.CURRENT_USER_ATTR, authUser);
        session.setAttribute("userId", userId);
        session.setAttribute("roles", roles);

        Authentication authentication = toAuthentication(authUser);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

        chain.doFilter(request, response);
    }

    private boolean isAuthenticated(Authentication auth) {
        return auth != null
                && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken);
    }

    private List<String> parseRoles(String rolesHeader) {
        if (rolesHeader == null || rolesHeader.isBlank()) {
            return List.of("STUDENT");
        }
        return Arrays.stream(rolesHeader.split(","))
                .map(String::trim)
                .filter(role -> !role.isEmpty())
                .map(String::toUpperCase)
                .toList();
    }

    private Authentication toAuthentication(MultiRoleSessionFilter.AuthUser authUser) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        for (String role : authUser.getRoles()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
        }
        return new UsernamePasswordAuthenticationToken(String.valueOf(authUser.getUserId()), null, authorities);
    }
}
