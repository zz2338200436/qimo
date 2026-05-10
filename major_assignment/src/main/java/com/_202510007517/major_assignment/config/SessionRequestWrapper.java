package com._202510007517.major_assignment.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpSession;

/**
 * Session 请求包装器
 * 用于在 Controller 中通过 HttpSession 也能访问到自定义 Session 数据
 */
public class SessionRequestWrapper extends HttpServletRequestWrapper {
    
    private final MultiRoleSessionManager.SessionData sessionData;
    
    public SessionRequestWrapper(HttpServletRequest request, MultiRoleSessionManager.SessionData sessionData) {
        super(request);
        this.sessionData = sessionData;
    }
    
    @Override
    public HttpSession getSession(boolean create) {
        HttpSession session = super.getSession(create);
        if (session != null && sessionData != null) {
            // 将自定义 Session 数据同步到标准 Session 中
            session.setAttribute("userId", sessionData.getUserId());
            session.setAttribute("roles", sessionData.getRoles());
        }
        return session;
    }
}

