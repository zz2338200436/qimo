package com._202510007517.major_assignment.controller;

import com._202510007517.major_assignment.config.MultiRoleSessionFilter;
import com._202510007517.major_assignment.config.MultiRoleSessionFilter.AuthUser;
import com._202510007517.major_assignment.config.MultiRoleSessionManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

/**
 * Controller 公共基类，提供<b>唯一</b>登录态读取入口。
 * <p>
 * 对齐 Design §7.1 / R2.1：所有 Controller 必须通过
 * {@link #getCurrentUser(HttpServletRequest)} /
 * {@link #getCurrentUserId(HttpServletRequest)} /
 * {@link #getCurrentRoles(HttpServletRequest)} 读取登录态；禁止直接读取 {@link HttpSession} 属性。
 * </p>
 * <p>
 * 数据源是 {@link MultiRoleSessionFilter} 在每次请求进入时写入的 request 属性
 * {@link MultiRoleSessionFilter#CURRENT_USER_ATTR}。Filter 保证了 request / {@code SecurityContextHolder}
 * / {@code HttpSession} 三处同步一致（R2.2 不变量）。
 * </p>
 */
@RestController
public abstract class BaseController {

    // ============================================================
    // 新入口 —— 唯一推荐路径（R2.1）
    // ============================================================

    /**
     * 获取当前请求的登录用户快照。未登录返回 {@code null}。
     */
    protected AuthUser getCurrentUser(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        Object attr = request.getAttribute(MultiRoleSessionFilter.CURRENT_USER_ATTR);
        if (attr instanceof AuthUser) {
            return (AuthUser) attr;
        }
        return null;
    }

    /**
     * 获取当前请求的登录用户 ID。未登录返回 {@code null}。
     * <p>
     * 仅从 request attribute 读取，保证与 {@link MultiRoleSessionFilter} 写入的三处上下文同源。
     * </p>
     */
    protected Long getCurrentUserId(HttpServletRequest request) {
        AuthUser user = getCurrentUser(request);
        return user != null ? user.getUserId() : null;
    }

    /**
     * 获取当前用户的角色列表（不可变副本）。未登录返回空列表。
     */
    protected List<String> getCurrentRoles(HttpServletRequest request) {
        AuthUser user = getCurrentUser(request);
        return user != null ? user.getRoles() : Collections.emptyList();
    }

    /**
     * 检查当前请求是否已登录。
     */
    protected boolean isLoggedIn(HttpServletRequest request) {
        return getCurrentUserId(request) != null;
    }

    // ============================================================
    // 兼容入口 —— 已弃用（阶段 1 过渡期保留，禁止新增调用）
    //
    // 历史上存在 (HttpSession) 重载，依赖 Filter 向标准 Session 写入 userId/roles。
    // 由于 {@link MultiRoleSessionFilter} 现在会把 AuthUser 以统一键写入 HttpSession，
    // 这些方法继续可用，但新代码<b>必须</b>使用基于 {@link HttpServletRequest} 的重载。
    //
    // 计划在阶段 2 Auth_Service 剥离后（Spring Cloud 迁移）统一移除。
    // ============================================================

    /**
     * @deprecated 请改用 {@link #getCurrentUserId(HttpServletRequest)}，
     *     基于请求属性作为唯一登录态入口（R2.1）。
     */
    // TODO: migrate to getCurrentUserId(HttpServletRequest) once all callers updated
    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated(since = "1.0", forRemoval = true)
    protected Long getCurrentUserId(HttpSession session) {
        if (session == null) {
            return null;
        }
        // 1) 首选：Filter 写入的统一键 CURRENT_USER
        Object current = session.getAttribute(MultiRoleSessionFilter.CURRENT_USER_ATTR);
        if (current instanceof AuthUser) {
            return ((AuthUser) current).getUserId();
        }
        // 2) 回退：历史字段 userId（过渡期兼容）
        Object userIdAttr = session.getAttribute("userId");
        if (userIdAttr instanceof Long) {
            return (Long) userIdAttr;
        }
        if (userIdAttr instanceof Number) {
            return ((Number) userIdAttr).longValue();
        }
        return null;
    }

    /**
     * @deprecated 请改用 {@link #isLoggedIn(HttpServletRequest)}（R2.1）。
     */
    // TODO: migrate to isLoggedIn(HttpServletRequest)
    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated(since = "1.0", forRemoval = true)
    protected boolean isLoggedIn(HttpSession session) {
        return getCurrentUserId(session) != null;
    }

    /**
     * @deprecated 请改用 {@link #getCurrentRoles(HttpServletRequest)}（R2.1）。
     */
    // TODO: migrate to getCurrentRoles(HttpServletRequest)
    @SuppressWarnings({"DeprecatedIsStillUsed", "unchecked"})
    @Deprecated(since = "1.0", forRemoval = true)
    protected List<String> getCurrentUserRoles(HttpServletRequest request) {
        // 优先走新入口
        List<String> roles = getCurrentRoles(request);
        if (!roles.isEmpty()) {
            return roles;
        }
        // 过渡期兼容：若上游未经过 Filter（例如单测或静态路径），回退 Session 读取
        if (request != null) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                Object current = session.getAttribute(MultiRoleSessionFilter.CURRENT_USER_ATTR);
                if (current instanceof AuthUser) {
                    return ((AuthUser) current).getRoles();
                }
                Object legacy = session.getAttribute("roles");
                if (legacy instanceof List) {
                    return (List<String>) legacy;
                }
            }
        }
        return Collections.emptyList();
    }

    /**
     * @deprecated 阶段 1 过渡期遗留，后续直接通过 {@link HttpServletRequest} 参数获取 request 即可。
     */
    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated(since = "1.0", forRemoval = true)
    protected HttpServletRequest getRequestFromSession(HttpSession session) {
        if (session != null) {
            Object requestObj = session.getAttribute("HTTP_REQUEST");
            if (requestObj instanceof HttpServletRequest) {
                return (HttpServletRequest) requestObj;
            }
        }
        return null;
    }

    // ============================================================
    // 分页参数兜底（R5.5 / Design §7.5）
    // ============================================================

    /** 单页条数默认值。 */
    protected static final int DEFAULT_PAGE_SIZE = 10;

    /** 单页条数上限，对齐 Requirement 5.5。 */
    protected static final int MAX_PAGE_SIZE = 100;

    /**
     * 将 {@code pageSize} 夹紧到合法区间 {@code [1, 100]}。
     *
     * <p>第一道防线是 {@link com._202510007517.major_assignment.entity.dto.PageRequestDTO}
     * 上的 {@code @Min(1)} / {@code @Max(100)}，Controller 用 {@code @Valid} 触发
     * {@link org.springframework.web.bind.MethodArgumentNotValidException}，
     * 由 {@code GlobalExceptionHandler} 映射为 HTTP 400。</p>
     *
     * <p>该方法是第二道兜底：未走 {@code @Valid} 的入口（例如直接使用
     * {@code @RequestParam Integer size}）仍可通过它将越界值夹紧到 {@code [1, 100]}，
     * 防止意外全表扫描（Requirement 5.5）。</p>
     *
     * @param pageSize 调用方传入的单页条数
     * @return {@code pageSize <= 0} 时返回默认 {@link #DEFAULT_PAGE_SIZE}；
     *         {@code pageSize > 100} 时返回 {@link #MAX_PAGE_SIZE}；否则原样返回。
     */
    protected int clampPageSize(int pageSize) {
        if (pageSize <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        if (pageSize > MAX_PAGE_SIZE) {
            return MAX_PAGE_SIZE;
        }
        return pageSize;
    }

    /**
     * 将 {@code pageNum} 夹紧到合法区间 {@code [1, +∞)}。
     *
     * <p>与 {@link #clampPageSize(int)} 配套，确保页码至少为 1（Requirement 5.5 同源约束）。</p>
     *
     * @param pageNum 调用方传入的页码
     * @return {@code pageNum <= 0} 时返回 {@code 1}；否则原样返回。
     */
    protected int clampPageNum(int pageNum) {
        return pageNum <= 0 ? 1 : pageNum;
    }

    // ============================================================
    // 历史字段兼容：部分旧代码仍传入 HttpServletRequest 但期望从 Session 中读取。
    // 新入口已统一改为 request attribute，这里保留常量供测试与兼容引用。
    // ============================================================

    /**
     * @deprecated 仅供过渡期兼容引用，等价于 {@link MultiRoleSessionFilter#CURRENT_USER_ATTR}。
     */
    @Deprecated(since = "1.0", forRemoval = true)
    protected static final String SESSION_DATA_ATTR = "SESSION_DATA";

    // 引用 MultiRoleSessionManager.SessionData 仅为类型可见；新代码不应直接使用。
    @SuppressWarnings("unused")
    private static final Class<?> LEGACY_SESSION_DATA_TYPE = MultiRoleSessionManager.SessionData.class;
}
