package com._202510007517.major_assignment.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Spring Security 配置。
 * <p>
 * CSRF 策略（对齐 R2.4 / Design §7.1）：
 * </p>
 * <ul>
 *   <li><b>仅对</b>匿名入口（登录 / 登出 / 注册 / 公开验证码等）放行 CSRF，
 *       因为这些接口无法依赖会话内的 CSRF token（登录之前未建立会话）。</li>
 *   <li>停止对 {@code /api/teacher/**} / {@code /api/student/**} /
 *       {@code /api/knowledge-points/**} / {@code /api/early-warnings/**} /
 *       {@code /api/notifications/**} 的全量 CSRF 忽略 —— 这些接口使用登录态，
 *       应当参与 CSRF 校验。</li>
 *   <li>过渡到 Spring Cloud 微服务后（阶段 2），同源校验由 Gateway / JWT 负责
 *       （{@code JwtAuthenticationFilter} + CORS 白名单），本单体配置将被替换。</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Autowired
    private MultiRoleSessionFilter multiRoleSessionFilter;
    
    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 将 MultiRoleSessionFilter 添加到 Spring Security 过滤器链的最前面
            // 这样它会在所有 Spring Security 过滤器之前执行
            .addFilterBefore(multiRoleSessionFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
            // CSRF：使用 Cookie 承载 token，便于前端读取并回写到请求头。
            //
            // 放行规则（R2.4）：
            //   - /api/auth/**   —— 登录/登出/令牌刷新，无会话前提
            //   - /api/public/** —— 公开资源（包括验证码图片）
            //
            // 说明（Spring Cloud 迁移后由 Gateway + JWT 负责同源校验）：
            //   - /api/teacher/**、/api/student/**、/api/knowledge-points/**、
            //     /api/early-warnings/**、/api/notifications/** 不再全量忽略 CSRF；
            //     前端 axios 需要从 XSRF-TOKEN Cookie 读取并附加 X-XSRF-TOKEN 请求头。
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringRequestMatchers(
                    "/api/auth/**",
                    "/api/public/**"
                )
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/auth/**",
                    "/api/public/**",
                    "/api/errors/**",
                    "/",
                    "/error",
                    // 静态资源与前端页面（按实际目录列举，避免 '**/*.ext' 的 PathPattern 解析报错）
                    "/static/**",
                    "/webjars/**",
                    "/*.html",
                    "/*.js",
                    "/*.css",
                    "/*.png",
                    "/*.jpg",
                    "/*.jpeg",
                    "/*.gif",
                    "/*.svg",
                    "/*.ico",
                    "/components/**",
                    "/lib/**",
                    "/fonts/**"
                ).permitAll()
                .requestMatchers("/api/teacher/**").hasAnyRole("TEACHER")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form.disable())
            .logout(logout -> logout.disable())
            .httpBasic(httpBasic -> httpBasic.disable());
        
        return http.build();
    }
}
