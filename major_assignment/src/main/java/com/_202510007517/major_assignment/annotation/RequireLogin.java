package com._202510007517.major_assignment.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 登录验证注解
 * 标注在Controller方法上，表示该方法需要用户登录后才能访问
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireLogin {
    
    /**
     * 是否需要登录，默认为true
     */
    boolean required() default true;
    
    /**
     * 允许的角色列表，为空表示只需登录即可
     */
    String[] roles() default {};
}
