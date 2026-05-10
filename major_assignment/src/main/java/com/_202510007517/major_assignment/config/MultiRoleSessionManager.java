package com._202510007517.major_assignment.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 多角色 Session 管理器
 * 为不同角色的用户使用不同的 Session Cookie，实现同一浏览器中多角色同时登录
 */
@Component
public class MultiRoleSessionManager {
    
    private static final String TEACHER_SESSION_COOKIE = "JSESSIONID_TEACHER";
    private static final String STUDENT_SESSION_COOKIE = "JSESSIONID_STUDENT";
    private static final String ADMIN_SESSION_COOKIE = "JSESSIONID_ADMIN";
    
    private static final String SESSION_PREFIX = "SESSION:";
    private static final int SESSION_TIMEOUT_MINUTES = 30;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    /**
     * 根据用户角色获取对应的 Session Cookie 名称
     */
    public String getSessionCookieName(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return STUDENT_SESSION_COOKIE;
        }
        
        for (String role : roles) {
            String roleUpper = role.toUpperCase();
            if ("ADMIN".equals(roleUpper) || "SUPER_ADMIN".equals(roleUpper)) {
                return ADMIN_SESSION_COOKIE;
            } else if ("TEACHER".equals(roleUpper)) {
                return TEACHER_SESSION_COOKIE;
            }
        }
        
        return STUDENT_SESSION_COOKIE;
    }
    
    /**
     * 根据请求路径确定应该使用的 Session Cookie 名称
     */
    public String getSessionCookieNameByPath(String requestPath) {
        // 管理员路径
        if (requestPath.startsWith("/api/admin/")) {
            return ADMIN_SESSION_COOKIE;
        }
        
        // 教师路径 - 包括所有教师相关的 API
        if (requestPath.startsWith("/api/teacher/") || 
            requestPath.contains("/teacher/") ||
            requestPath.startsWith("/api/early-warnings/teacher") ||
            requestPath.startsWith("/api/knowledge-points/analysis/teacher")) {
            return TEACHER_SESSION_COOKIE;
        }
        
        // 学生路径
        if (requestPath.startsWith("/api/student/")) {
            return STUDENT_SESSION_COOKIE;
        }
        
        // 对于其他 API 路径，尝试从所有可能的 Cookie 中查找
        // 默认返回学生 Cookie
        return STUDENT_SESSION_COOKIE;
    }
    
    /**
     * 创建 Session 并设置 Cookie
     */
    public String createSession(HttpServletResponse response, List<String> roles, Long userId) {
        String sessionId = UUID.randomUUID().toString();
        String cookieName = getSessionCookieName(roles);
        String redisKey = SESSION_PREFIX + cookieName + ":" + sessionId;
        
        // 存储 Session 数据到 Redis
        SessionData sessionData = new SessionData();
        sessionData.setUserId(userId);
        sessionData.setRoles(roles);
        sessionData.setSessionId(sessionId);
        
        redisTemplate.opsForValue().set(redisKey, sessionData, SESSION_TIMEOUT_MINUTES, TimeUnit.MINUTES);
        
        // 设置 Cookie
        Cookie cookie = new Cookie(cookieName, sessionId);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(SESSION_TIMEOUT_MINUTES * 60); // 秒
        response.addCookie(cookie);
        
        return sessionId;
    }
    
    /**
     * 从请求中获取 Session ID
     */
    public String getSessionId(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
    
    /**
     * 从 Redis 中获取 Session 数据
     */
    @SuppressWarnings("unchecked")
    public SessionData getSessionData(String sessionId, String cookieName) {
        if (sessionId == null) {
            return null;
        }
        
        String redisKey = SESSION_PREFIX + cookieName + ":" + sessionId;
        Object data = redisTemplate.opsForValue().get(redisKey);
        if (!(data instanceof SessionData)) {
            return null;
        }
        SessionData sessionData = (SessionData) data;
        
        // 如果 Session 存在，更新过期时间
        if (sessionData != null) {
            redisTemplate.expire(redisKey, SESSION_TIMEOUT_MINUTES, TimeUnit.MINUTES);
        }
        
        return sessionData;
    }
    
    /**
     * 更新 Session 数据
     */
    public void updateSessionData(String sessionId, String cookieName, SessionData sessionData) {
        String redisKey = SESSION_PREFIX + cookieName + ":" + sessionId;
        redisTemplate.opsForValue().set(redisKey, sessionData, SESSION_TIMEOUT_MINUTES, TimeUnit.MINUTES);
    }
    
    /**
     * 删除 Session
     */
    public void deleteSession(String sessionId, String cookieName) {
        String redisKey = SESSION_PREFIX + cookieName + ":" + sessionId;
        redisTemplate.delete(redisKey);
    }
    
    /**
     * Session 数据类
     */
    public static class SessionData {
        private Long userId;
        private List<String> roles;
        private String sessionId;
        
        public Long getUserId() {
            return userId;
        }
        
        public void setUserId(Long userId) {
            this.userId = userId;
        }
        
        public List<String> getRoles() {
            return roles;
        }
        
        public void setRoles(List<String> roles) {
            this.roles = roles;
        }
        
        public String getSessionId() {
            return sessionId;
        }
        
        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }
    }
}

