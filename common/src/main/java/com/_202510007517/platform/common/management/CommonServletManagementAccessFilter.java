package com._202510007517.platform.common.management;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class CommonServletManagementAccessFilter extends OncePerRequestFilter {

    private final ManagementEndpointAccessMatcher matcher;

    public CommonServletManagementAccessFilter(ManagementEndpointAccessMatcher matcher) {
        this.matcher = matcher;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !matcher.isProtectedPath(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!matcher.isAllowed(request.getRemoteAddr())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Actuator endpoints are only accessible from trusted networks.");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
