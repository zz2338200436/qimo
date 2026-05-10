package com._202510007517.major_assignment.controller;

import com._202510007517.major_assignment.constants.CacheConstants;
import com._202510007517.major_assignment.constants.ErrorMessages;
import com._202510007517.major_assignment.constants.RoleConstants;
import com._202510007517.major_assignment.constants.SuccessMessages;
import com._202510007517.major_assignment.entity.User;
import com._202510007517.major_assignment.entity.dto.ChangePasswordDTO;
import com._202510007517.major_assignment.entity.dto.LoginRequestDTO;
import com._202510007517.major_assignment.entity.dto.ResponseResult;
import com._202510007517.major_assignment.service.UserService;
import com._202510007517.major_assignment.utils.LogUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "认证管理", description = "用户认证相关接口")
public class AuthController extends BaseController {
    
    private static final Logger logger = LogUtil.getLogger(AuthController.class);
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;
    
    @Autowired
    private com._202510007517.major_assignment.config.MultiRoleSessionManager sessionManager;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Operation(summary = "用户登录", description = "用户登录接口，需要提供用户名、密码和验证码")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "登录成功"),
        @ApiResponse(responseCode = "400", description = "验证码错误或参数错误"),
        @ApiResponse(responseCode = "401", description = "用户名或密码错误"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/login")
    public ResponseResult<Map<String, Object>> login(@RequestBody @Valid LoginRequestDTO loginRequest, 
                                                      HttpServletRequest request,
                                                      jakarta.servlet.http.HttpServletResponse response) {
        logger.debug("登录请求：username={}", loginRequest.getUsername());
        
        // 获取当前Session（如果存在）
        HttpSession session = request.getSession(false);
        
        try {
            // 先验证验证码（必须在Session失效之前，因为验证码存储在Session中）
            if (session == null) {
                session = request.getSession(true);
            }
            
            if (!isCaptchaValid(loginRequest.getCaptcha(), session)) {
                return ResponseResult.failure(ErrorMessages.CAPTCHA_ERROR, 400);
            }
            User user = userService.findByUsername(loginRequest.getUsername());
            if (user == null) {
                return ResponseResult.failure(ErrorMessages.LOGIN_FAILED, 401);
            }

            if (Boolean.FALSE.equals(user.getEnabled())) {
                return ResponseResult.failure(ErrorMessages.ACCOUNT_DISABLED, 403);
            }

            boolean passwordMatch = bCryptPasswordEncoder.matches(loginRequest.getPassword(), user.getPassword());
            if (!passwordMatch) {
                return ResponseResult.failure(ErrorMessages.LOGIN_FAILED, 401);
            }
            
            List<String> roles = userService.getRolesByUserId(user.getId());
            List<String> rolesUpper = roles != null && !roles.isEmpty() 
                ? roles.stream().map(String::toUpperCase).collect(Collectors.toList())
                : List.of(RoleConstants.STUDENT);

            // 使用多角色 Session 管理器创建 Session（使用不同的 Cookie 名称）
            String sessionId = sessionManager.createSession(response, rolesUpper, user.getId());
            logger.info("创建多角色 Session 成功：userId={}, roles={}, sessionId={}, cookieName={}", 
                user.getId(), rolesUpper, sessionId, sessionManager.getSessionCookieName(rolesUpper));
            
            // 同时设置标准 Session（用于兼容验证码等功能）
            session.setAttribute("userId", user.getId());
            session.setAttribute("roles", rolesUpper);
            // 统一键：与 MultiRoleSessionFilter.CURRENT_USER_ATTR 保持一致（Design §7.1 三处同步）
            session.setAttribute(
                com._202510007517.major_assignment.config.MultiRoleSessionFilter.CURRENT_USER_ATTR,
                com._202510007517.major_assignment.config.MultiRoleSessionFilter.AuthUser.of(user.getId(), rolesUpper)
            );

            // 将认证信息写入SecurityContext，便于后续过滤器识别
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            for (String role : rolesUpper) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
            }
            Authentication authentication = new UsernamePasswordAuthenticationToken(user.getUsername(), null, authorities);
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            // 显式将SecurityContext存入Session，确保后续请求能识别角色
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

            Map<String, Object> userData = Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "name", user.getName(),
                "email", user.getEmail(),
                "phone", user.getPhone(),
                "roles", rolesUpper
            );
            
            logger.info("用户登录成功：userId={}, roles={}", user.getId(), rolesUpper);
            return ResponseResult.success(userData, SuccessMessages.LOGIN_SUCCESS, 200);
        } catch (Exception e) {
            logger.error("登录失败：{}", e.getMessage(), e);
            return ResponseResult.failure("登录失败：" + e.getMessage(), 500);
        }
    }
    
    @Operation(summary = "获取当前用户信息", description = "获取当前登录用户的详细信息")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "401", description = "未授权，请重新登录")
    })
    @GetMapping("/me")
    public ResponseResult<Map<String, Object>> me(HttpServletRequest request) {
        if (!isLoggedIn(request)) {
            return ResponseResult.failure(ErrorMessages.UNAUTHORIZED, 401);
        }
        
        Long userId = getCurrentUserId(request);
        User user = userService.findById(userId);
        
        // 获取用户角色
        List<String> roles = userService.getRolesByUserId(userId);
        List<String> rolesUpper = roles != null && !roles.isEmpty() 
            ? roles.stream().map(String::toUpperCase).collect(Collectors.toList())
            : List.of(RoleConstants.STUDENT);
        
        Map<String, Object> userData = Map.of(
            "id", user.getId(),
            "username", user.getUsername(),
            "name", user.getName(),
            "email", user.getEmail(),
            "phone", user.getPhone(),
            "roles", rolesUpper
        );
        
        return ResponseResult.success(userData, SuccessMessages.GET_USER_INFO_SUCCESS, 200);
    }
    
    @PutMapping("/me")
    public ResponseResult<Map<String, Object>> updateProfile(@RequestBody Map<String, Object> requestData, HttpSession session) {
        if (!isLoggedIn(session)) {
            return ResponseResult.failure(ErrorMessages.UNAUTHORIZED, 401);
        }
        
        try {
            Long userId = getCurrentUserId(session);
            User user = userService.findById(userId);
            
            // 更新用户信息
            String name = (String) requestData.get("name");
            String email = (String) requestData.get("email");
            String phone = (String) requestData.get("phone");
            
            if (name != null) {
                user.setName(name);
            }
            if (email != null) {
                user.setEmail(email);
            }
            if (phone != null) {
                user.setPhone(phone);
            }
            
            userService.update(user);
            
            // 获取更新后的用户信息
            User updatedUser = userService.findById(userId);
            List<String> roles = userService.getRolesByUserId(userId);
            List<String> rolesUpper = roles != null && !roles.isEmpty() 
                ? roles.stream().map(String::toUpperCase).collect(Collectors.toList())
                : List.of(RoleConstants.STUDENT);
            
            Map<String, Object> userData = Map.of(
                "id", updatedUser.getId(),
                "username", updatedUser.getUsername(),
                "name", updatedUser.getName(),
                "email", updatedUser.getEmail(),
                "phone", updatedUser.getPhone(),
                "roles", rolesUpper
            );
            
            return ResponseResult.success(userData, SuccessMessages.PROFILE_UPDATE_SUCCESS, 200);
        } catch (Exception e) {
            logger.error("更新个人信息失败：{}", e.getMessage(), e);
            return ResponseResult.failure("更新个人信息失败：" + e.getMessage(), 500);
        }
    }
    
    @PutMapping("/change-password")
    public ResponseResult<Map<String, Object>> changePassword(@RequestBody @Valid ChangePasswordDTO requestData, HttpSession session) {
        if (!isLoggedIn(session)) {
            return ResponseResult.failure(ErrorMessages.UNAUTHORIZED, 401);
        }
        
        try {
            Long userId = getCurrentUserId(session);
            User user = userService.findById(userId);
            
            // 获取密码数据
            String currentPassword = requestData.getCurrentPassword();
            String newPassword = requestData.getNewPassword();

            if (currentPassword.equals(newPassword)) {
                return ResponseResult.failure(ErrorMessages.PASSWORD_SAME, 400);
            }
            
            // 验证当前密码
            boolean passwordMatch = bCryptPasswordEncoder.matches(currentPassword, user.getPassword());
            if (!passwordMatch) {
                return ResponseResult.failure(ErrorMessages.CURRENT_PASSWORD_ERROR, 400);
            }

            if (!isPasswordStrong(newPassword)) {
                return ResponseResult.failure(ErrorMessages.PASSWORD_WEAK, 400);
            }
            
            // 更新密码
            user.setPassword(bCryptPasswordEncoder.encode(newPassword));
            userService.update(user);
            
            return ResponseResult.success(null, SuccessMessages.PASSWORD_CHANGED, 200);
        } catch (Exception e) {
            logger.error("更新密码失败：{}", e.getMessage(), e);
            return ResponseResult.failure("更新密码失败：" + e.getMessage(), 500);
        }
    }
    
    @PutMapping("/notification-settings")
    public ResponseResult<Map<String, Object>> saveNotificationSettings(@RequestBody Map<String, Object> requestData, HttpSession session) {
        if (!isLoggedIn(session)) {
            return ResponseResult.failure(ErrorMessages.UNAUTHORIZED, 401);
        }
        
        try {
            // 通知设置暂时保存在session中，实际项目中应该保存在数据库表中
            session.setAttribute("emailNotifications", requestData.getOrDefault("emailNotifications", true));
            session.setAttribute("assignmentNotifications", requestData.getOrDefault("assignmentNotifications", true));
            session.setAttribute("examNotifications", requestData.getOrDefault("examNotifications", true));
            session.setAttribute("warningNotifications", requestData.getOrDefault("warningNotifications", true));
            
            return ResponseResult.success(null, SuccessMessages.NOTIFICATION_SETTINGS_SUCCESS, 200);
        } catch (Exception e) {
            logger.error("保存通知设置失败：{}", e.getMessage(), e);
            return ResponseResult.failure("保存通知设置失败：" + e.getMessage(), 500);
        }
    }

    private boolean isCaptchaValid(String captcha, HttpSession session) {
        if (session == null || captcha == null) {
            return false;
        }
        // 验证码以 CacheConstants.CAPTCHA_NAMESPACE 为前缀存储在 Redis，
        // 与业务会话前缀 SESSION:* 严格隔离（R2.5）。
        String redisKey = CacheConstants.CAPTCHA_NAMESPACE + session.getId();
        Object sessionCaptcha = redisTemplate.opsForValue().get(redisKey);
        if (sessionCaptcha == null) {
            return false;
        }
        boolean matched = sessionCaptcha.toString().equalsIgnoreCase(captcha);
        if (matched) {
            // 单次验证码：成功校验后立即失效，避免重放。
            redisTemplate.delete(redisKey);
        }
        return matched;
    }

    private boolean isPasswordStrong(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        boolean hasLetter = password.chars().anyMatch(Character::isLetter);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        return hasLetter && hasDigit;
    }
}