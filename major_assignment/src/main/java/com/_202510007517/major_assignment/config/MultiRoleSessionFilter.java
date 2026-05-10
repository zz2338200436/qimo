package com._202510007517.major_assignment.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.List;
import java.util.stream.Collectors;

/**
 * 多角色 Session 过滤器
 * <p>
 * 职责（对齐 Design §7.1 会话一致性不变量，R2.2 / R2.3）：
 * </p>
 * <ol>
 *   <li>从 Cookie 中识别当前请求的 Session，从 Redis 读出 {@link MultiRoleSessionManager.SessionData}。</li>
 *   <li>命中有效会话时，<b>同步写入三处上下文，任何一处的 userId 都必须与其它两处一致</b>：
 *     <ul>
 *       <li>{@link SecurityContextHolder} 写入 {@code UsernamePasswordAuthenticationToken}</li>
 *       <li>{@code request.setAttribute(}{@link #CURRENT_USER_ATTR}{@code , ...)} 作为 BaseController 唯一入口的数据源</li>
 *       <li>{@link HttpSession} 中以同一键 {@value #CURRENT_USER_ATTR} 写入相同的 {@link AuthUser}</li>
 *     </ul>
 *   </li>
 *   <li>未命中或无效会话时，<b>保持匿名</b>：不得向 {@link SecurityContextHolder} 写入任何已认证 {@link Authentication}
 *       （R2.3）。</li>
 *   <li>将 {@code userId / role / uri / method} 写入 {@link MDC}，并在 {@code finally} 中<b>仅移除本 Filter 追加的键</b>。
 *       {@code traceId} 由更外层的 {@link TraceIdFilter}（{@link org.springframework.core.Ordered#HIGHEST_PRECEDENCE}）
 *       写入并最终通过 {@link MDC#clear()} 清理，确保线程池复用时上下文彻底清空（Design §7.2）。</li>
 * </ol>
 * <p>
 * <b>幂等性（R2 Idempotence）</b>：若 {@link SecurityContextHolder} 当前请求已包含 "已认证" 主体，
 * 本 Filter 直接放行，不再重复查 Redis，也不覆写 context。
 * </p>
 */
@Component
@Order(1)  // 确保在 Spring Security 之前执行
public class MultiRoleSessionFilter implements Filter {

    /**
     * {@code request.setAttribute} 与 {@link HttpSession#setAttribute} 使用的统一键名。
     * <b>必须</b>与 {@code BaseController.getCurrentUser(HttpServletRequest)} 的读取键保持一致。
     */
    public static final String CURRENT_USER_ATTR = "CURRENT_USER";

    // MDC 字段名（对齐 Design §7.2）
    private static final String MDC_USER_ID = "userId";
    private static final String MDC_ROLE = "role";
    private static final String MDC_URI = "uri";
    private static final String MDC_METHOD = "method";

    @Autowired
    private MultiRoleSessionManager sessionManager;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String requestPath = httpRequest.getRequestURI();

        // 跳过静态资源和公开接口
        if (shouldSkip(requestPath)) {
            chain.doFilter(request, response);
            return;
        }

        try {
            // MDC 基础字段：uri / method。traceId 由外层的 TraceIdFilter 写入，此处不再覆写。
            MDC.put(MDC_URI, requestPath);
            MDC.put(MDC_METHOD, httpRequest.getMethod());

            // —— R2 幂等性：若已存在已认证 Authentication，直接放行，不重复写入 —— //
            Authentication existing = SecurityContextHolder.getContext().getAuthentication();
            if (isAuthenticated(existing)) {
                writeMdcFromAuth(existing);
                chain.doFilter(request, response);
                return;
            }

            // —— 解析会话 —— //
            String cookieName = sessionManager.getSessionCookieNameByPath(requestPath);
            String sessionId = sessionManager.getSessionId(httpRequest, cookieName);
            MultiRoleSessionManager.SessionData sessionData = null;
            if (sessionId != null) {
                sessionData = sessionManager.getSessionData(sessionId, cookieName);
            }

            if (isValidSession(sessionData)) {
                // 命中有效会话 —— 三处同步写入（R2.2、Design §7.1 三处一致）
                AuthUser authUser = AuthUser.of(sessionData.getUserId(), sessionData.getRoles());

                // 1) request attribute —— BaseController 唯一入口
                httpRequest.setAttribute(CURRENT_USER_ATTR, authUser);
                // 兼容字段（过渡期，后续下线）
                httpRequest.setAttribute("SESSION_DATA", sessionData);
                httpRequest.setAttribute("SESSION_ID", sessionId);
                httpRequest.setAttribute("SESSION_COOKIE_NAME", cookieName);

                // 2) Spring Security Authentication
                Authentication authentication = toAuthentication(authUser);
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(authentication);
                SecurityContextHolder.setContext(context);

                // 3) HttpSession 使用同一键写入同一 AuthUser 对象
                HttpSession session = httpRequest.getSession(true);
                session.setAttribute(CURRENT_USER_ATTR, authUser);
                // 兼容旧字段（过渡期）
                session.setAttribute("userId", authUser.getUserId());
                session.setAttribute("roles", authUser.getRoles());
                session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

                // 刷新 Redis 过期时间
                sessionManager.updateSessionData(sessionId, cookieName, sessionData);

                // MDC
                MDC.put(MDC_USER_ID, String.valueOf(authUser.getUserId()));
                MDC.put(MDC_ROLE, joinRoles(authUser.getRoles()));
            } else {
                // 无效会话 —— 保持匿名，绝不回退写入已认证主体（R2.3）
                // 注意：此处不调用 SecurityContextHolder.setContext / setAuthentication，
                // 也不向 request/session 写入 CURRENT_USER，以确保三处同时为空。
                SecurityContextHolder.clearContext();
            }

            chain.doFilter(request, response);
        } finally {
            // 仅移除本 Filter 追加的键，保留外层 TraceIdFilter 写入的 traceId。
            // 最终统一清理由 TraceIdFilter.finally 中的 MDC.clear() 负责（Design §7.2 "Filter 结束前 MDC.clear()"）。
            MDC.remove(MDC_USER_ID);
            MDC.remove(MDC_ROLE);
            MDC.remove(MDC_URI);
            MDC.remove(MDC_METHOD);
        }
    }

    /**
     * 判断 {@link Authentication} 是否已完成认证，排除 {@link AnonymousAuthenticationToken}。
     */
    private boolean isAuthenticated(Authentication auth) {
        return auth != null
                && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken);
    }

    private void writeMdcFromAuth(Authentication auth) {
        MDC.put(MDC_USER_ID, String.valueOf(auth.getName()));
        String role = auth.getAuthorities().stream()
                .map(Object::toString)
                .collect(Collectors.joining(","));
        MDC.put(MDC_ROLE, role);
    }

    /**
     * 有效会话的最小判定：非空且 userId 非空。
     * 其它形式（null / 过期 / 反序列化失败 / 字段缺失）都视为无效，保持匿名。
     */
    private boolean isValidSession(MultiRoleSessionManager.SessionData data) {
        return data != null && data.getUserId() != null;
    }

    private Authentication toAuthentication(AuthUser authUser) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        if (authUser.getRoles() != null) {
            for (String role : authUser.getRoles()) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
            }
        }
        return new UsernamePasswordAuthenticationToken(
                String.valueOf(authUser.getUserId()),
                null,
                authorities
        );
    }

    private String joinRoles(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return "";
        }
        return String.join(",", roles);
    }

    /**
     * 判断是否应该跳过 Session 处理
     */
    private boolean shouldSkip(String requestPath) {
        return requestPath.startsWith("/static/") ||
               requestPath.startsWith("/webjars/") ||
               requestPath.startsWith("/components/") ||
               requestPath.startsWith("/lib/") ||
               requestPath.startsWith("/fonts/") ||
               requestPath.endsWith(".html") ||
               requestPath.endsWith(".js") ||
               requestPath.endsWith(".css") ||
               requestPath.endsWith(".png") ||
               requestPath.endsWith(".jpg") ||
               requestPath.endsWith(".jpeg") ||
               requestPath.endsWith(".gif") ||
               requestPath.endsWith(".svg") ||
               requestPath.endsWith(".ico");
    }

    /**
     * 当前请求的认证主体快照，写入 request attribute / HttpSession 的同一键 {@value #CURRENT_USER_ATTR}。
     * <p>
     * 不可变（不暴露 setter），保证三处读取到的对象状态一致。
     * </p>
     */
    public static final class AuthUser implements java.io.Serializable {
        private static final long serialVersionUID = 1L;

        private final Long userId;
        private final List<String> roles;

        private AuthUser(Long userId, List<String> roles) {
            this.userId = userId;
            this.roles = roles == null
                    ? List.of()
                    : List.copyOf(roles);
        }

        public static AuthUser of(Long userId, List<String> roles) {
            return new AuthUser(userId, roles);
        }

        public Long getUserId() {
            return userId;
        }

        public List<String> getRoles() {
            return roles;
        }
    }
}
