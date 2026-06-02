package com._202510007517.major_assignment.aspect;

import com._202510007517.major_assignment.annotation.RequireLogin;
import com._202510007517.major_assignment.constants.RoleConstants;
import com._202510007517.major_assignment.exception.ForbiddenException;
import com._202510007517.major_assignment.exception.UnauthorizedException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthenticationAspectTest {

    private final AuthenticationAspect aspect = new AuthenticationAspect();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void classLevelRequireLogin_allows_authenticated_teacher() throws Throwable {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("teacher7", "n/a", RoleConstants.ROLE_TEACHER)
        );

        ProceedingJoinPoint joinPoint = mockJoinPoint(ClassLevelProtectedController.class, "dashboard");
        when(joinPoint.proceed()).thenReturn("ok");

        Object result = aspect.checkLogin(joinPoint);

        assertEquals("ok", result);
        verify(joinPoint).proceed();
    }

    @Test
    void classLevelRequireLogin_rejects_missing_teacher_role() throws Throwable {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("student42", "n/a", RoleConstants.ROLE_STUDENT)
        );

        ProceedingJoinPoint joinPoint = mockJoinPoint(ClassLevelProtectedController.class, "dashboard");

        assertThrows(ForbiddenException.class, () -> aspect.checkLogin(joinPoint));
    }

    @Test
    void methodLevelRequireLogin_rejects_anonymous_request() throws Throwable {
        ProceedingJoinPoint joinPoint = mockJoinPoint(MethodLevelProtectedController.class, "profile");

        assertThrows(UnauthorizedException.class, () -> aspect.checkLogin(joinPoint));
    }

    private ProceedingJoinPoint mockJoinPoint(Class<?> declaringClass, String methodName) throws NoSuchMethodException {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        Method method = declaringClass.getDeclaredMethod(methodName);

        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        return joinPoint;
    }

    @RequireLogin(roles = {RoleConstants.TEACHER})
    private static final class ClassLevelProtectedController {
        void dashboard() {
        }
    }

    private static final class MethodLevelProtectedController {
        @RequireLogin
        void profile() {
        }
    }
}
