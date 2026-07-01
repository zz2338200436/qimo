package com._202510007517.major_assignment.aspect;

import com._202510007517.major_assignment.annotation.RequireLogin;
import com._202510007517.major_assignment.exception.ForbiddenException;
import com._202510007517.major_assignment.exception.UnauthorizedException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 认证切面
 * <p>
 * 统一处理登录验证和权限检查。
 * </p>
 * <p>
 * 对齐 R2.1 / R2.2 / Design §7.1：切面<b>只读</b>
 * {@link SecurityContextHolder} 中由 {@code MultiRoleSessionFilter} 写入的 {@link Authentication}，
 * 不再自行查 Session / Redis，避免双轨读取导致的登录态不一致。
 * </p>
 */
@Aspect
@Component
@Order(1)
public class AuthenticationAspect {

    /**
     * 拦截所有带有 @RequireLogin 注解的方法
     */
    @Around("@annotation(com._202510007517.major_assignment.annotation.RequireLogin)")
    public Object checkLogin(ProceedingJoinPoint joinPoint) throws Throwable {
        RequireLogin requireLogin = resolveRequireLogin(joinPoint);
        if (requireLogin == null || !requireLogin.required()) {
            return joinPoint.proceed();
        }

        return enforceLogin(joinPoint, requireLogin);
    }

    @Around("@within(com._202510007517.major_assignment.annotation.RequireLogin)")
    public Object checkLoginAtTypeLevel(ProceedingJoinPoint joinPoint) throws Throwable {
        RequireLogin requireLogin = resolveRequireLogin(joinPoint);
        if (requireLogin == null || !requireLogin.required()) {
            return joinPoint.proceed();
        }

        return enforceLogin(joinPoint, requireLogin);
    }

    private Object enforceLogin(ProceedingJoinPoint joinPoint, RequireLogin requireLogin) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        if (method == null) {
            return joinPoint.proceed();
        }

        // 仅从 SecurityContextHolder 读取登录态（R2.1）
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!isAuthenticated(auth)) {
            throw new UnauthorizedException();
        }

        // 角色权限校验
        String[] requiredRoles = requireLogin.roles();
        if (requiredRoles.length > 0) {
            List<String> userRoles = extractRoles(auth);
            if (userRoles.isEmpty()) {
                throw new ForbiddenException();
            }
            boolean hasRole = Arrays.stream(requiredRoles)
                    .anyMatch(role -> userRoles.contains(role.toUpperCase()));
            if (!hasRole) {
                throw new ForbiddenException();
            }
        }

        return joinPoint.proceed();
    }

    private RequireLogin resolveRequireLogin(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequireLogin requireLogin = method.getAnnotation(RequireLogin.class);

        if (requireLogin != null) {
            return requireLogin;
        }

        Class<?> declaringClass = method.getDeclaringClass();
        if (declaringClass != null) {
            return declaringClass.getAnnotation(RequireLogin.class);
        }
        return null;
    }

    private boolean isAuthenticated(Authentication auth) {
        return auth != null
                && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken);
    }

    /**
     * 从 {@link Authentication} 中提取角色名集合。
     * 约定由 {@code MultiRoleSessionFilter} 以 {@code ROLE_<name>} 形式写入 authority。
     */
    private List<String> extractRoles(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.startsWith("ROLE_") ? a.substring("ROLE_".length()) : a)
                .map(String::toUpperCase)
                .collect(Collectors.toList());
    }
}
