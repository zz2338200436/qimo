package com._202510007517.major_assignment.config;

import com._202510007517.platform.common.web.CommonTraceConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class GatewayJwtBridgeFilterTest {

    private final GatewayJwtBridgeFilter filter = new GatewayJwtBridgeFilter();

    @AfterEach
    void tearDown() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    void bridgesGatewayHeadersIntoSessionAndSecurityContext() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CommonTraceConstants.USER_ID_HEADER, "42");
        request.addHeader(CommonTraceConstants.ROLES_HEADER, "STUDENT");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        Object currentUser = request.getAttribute(MultiRoleSessionFilter.CURRENT_USER_ATTR);
        assertThat(currentUser).isInstanceOf(MultiRoleSessionFilter.AuthUser.class);
        MultiRoleSessionFilter.AuthUser authUser = (MultiRoleSessionFilter.AuthUser) currentUser;
        assertThat(authUser.getUserId()).isEqualTo(42L);
        assertThat(authUser.getRoles()).containsExactly("STUDENT");

        assertThat(request.getSession(false)).isNotNull();
        assertThat(request.getSession(false).getAttribute("userId")).isEqualTo(42L);
        assertThat(request.getSession(false).getAttribute("roles")).isEqualTo(java.util.List.of("STUDENT"));
        assertThat(org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication())
                .isNotNull();
        assertThat(org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().isAuthenticated())
                .isTrue();
        verify(chain).doFilter(request, response);
    }

    @Test
    void skipsBridgeWhenGatewayUserHeaderMissing() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(request.getAttribute(MultiRoleSessionFilter.CURRENT_USER_ATTR)).isNull();
        assertThat(request.getSession(false)).isNull();
        assertThat(org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }
}
