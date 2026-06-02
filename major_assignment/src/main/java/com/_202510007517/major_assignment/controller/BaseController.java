package com._202510007517.major_assignment.controller;

import com._202510007517.major_assignment.config.MultiRoleSessionFilter;
import com._202510007517.major_assignment.config.MultiRoleSessionFilter.AuthUser;
import com._202510007517.major_assignment.config.MultiRoleSessionManager;
import com._202510007517.major_assignment.entity.Course;
import com._202510007517.major_assignment.entity.dto.PageResult;
import com._202510007517.major_assignment.utils.PageUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
    protected static final int DEFAULT_PAGE_SIZE = PageUtils.DEFAULT_PAGE_SIZE;

    /** 单页条数上限，对齐 Requirement 5.5。 */
    protected static final int MAX_PAGE_SIZE = PageUtils.MAX_PAGE_SIZE;

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
        return PageUtils.clampPageSize(pageSize);
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
        return PageUtils.clampPageNum(pageNum);
    }

    /**
     * 根据总记录数与单页条数计算总页数。
     *
     * <p>空结果集返回 {@code 0}，其余情况至少返回 {@code 1}。</p>
     */
    protected int calculateTotalPages(int totalElements, int pageSize) {
        return PageUtils.calculateTotalPages(totalElements, pageSize);
    }

    /**
     * 将页码夹紧到当前结果集可用范围内。
     *
     * <p>当结果集为空时，统一返回第一页 {@code 1}，避免上层出现负偏移或越界分页。</p>
     */
    protected int boundPageNum(int pageNum, int totalElements, int pageSize) {
        return PageUtils.boundPageNum(pageNum, totalElements, pageSize);
    }

    protected PageUtils.PageWindow resolvePageWindow(int pageNum, int pageSize, int totalElements) {
        return PageUtils.resolvePageWindow(pageNum, pageSize, totalElements);
    }

    /**
     * 构建与 Spring Data Page JSON 结构兼容的分页响应。
     */
    protected Map<String, Object> buildSpringPageResponse(List<?> content, int pageNum, int pageSize, int totalElements) {
        return PageUtils.buildPageResponse(content, pageNum, pageSize, totalElements);
    }

    protected Map<String, Object> buildSpringPageResponseFromInMemoryList(List<?> content, int pageNum, int pageSize) {
        List<?> safeContent = content != null ? content : List.of();
        int totalElements = safeContent.size();
        int safePageSize = clampPageSize(pageSize);
        int safePageNum = boundPageNum(pageNum, totalElements, safePageSize);
        List<?> pagedContent = PageUtils.paginate(safeContent, safePageNum, safePageSize);
        return buildSpringPageResponse(pagedContent, safePageNum, safePageSize, totalElements);
    }

    protected <T> PageResult<T> buildPageResultFromInMemoryList(List<T> content, int pageNum, int pageSize) {
        List<T> safeContent = content != null ? content : List.of();
        int totalElements = safeContent.size();
        int safePageSize = clampPageSize(pageSize);
        int safePageNum = boundPageNum(pageNum, totalElements, safePageSize);
        List<T> pagedContent = PageUtils.paginate(safeContent, safePageNum, safePageSize);
        return PageUtils.buildPageResult(pagedContent, safePageNum, safePageSize, totalElements);
    }

    /**
     * 归一化教师端作业状态筛选值，兼容中英文别名与大小写。
     */
    protected String normalizeAssignmentStatusFilter(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) {
            return null;
        }

        String normalized = rawStatus.trim().toLowerCase(Locale.ROOT);
        switch (normalized) {
            case "pending":
            case "待提交":
            case "待完成":
                return "pending";
            case "submitted":
            case "已提交":
                return "submitted";
            case "graded":
            case "已批改":
            case "已评分":
                return "graded";
            case "closed":
            case "已截止":
            case "已结束":
                return "closed";
            default:
                return normalized;
        }
    }

    /**
     * 根据作业状态筛选值推导 isActive 过滤条件；未知状态回退到调用方原始值。
     */
    protected Boolean resolveAssignmentActiveFilter(String rawStatus, Boolean fallbackIsActive) {
        String normalizedStatus = normalizeAssignmentStatusFilter(rawStatus);
        if (normalizedStatus == null) {
            return fallbackIsActive;
        }

        switch (normalizedStatus) {
            case "pending":
            case "submitted":
            case "graded":
                return true;
            case "closed":
                return false;
            default:
                return fallbackIsActive;
        }
    }

    /**
     * 归一化教师端考试状态筛选值，兼容中英文别名与大小写。
     */
    protected String normalizeExamStatusFilter(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) {
            return null;
        }

        String normalized = rawStatus.trim().toLowerCase(Locale.ROOT);
        switch (normalized) {
            case "upcoming":
            case "即将开始":
            case "scheduled":
                return "upcoming";
            case "ongoing":
            case "进行中":
            case "in_progress":
                return "ongoing";
            case "completed":
            case "已结束":
            case "已完成":
            case "closed":
                return "completed";
            case "graded":
            case "已评分":
            case "reviewed":
                return "graded";
            default:
                return normalized;
        }
    }

    /**
     * 解析教师端传入的课程标识。
     *
     * <p>兼容两种输入：</p>
     * <ol>
     *   <li>纯数字课程 ID，例如 {@code "42"}</li>
     *   <li>课程代码，例如 {@code "CS101"}</li>
     * </ol>
     *
     * <p>课程代码模式下，调用方需传入当前教师可见课程列表用于匹配；
     * 未匹配到时返回 {@code null} 交由上层决定响应。</p>
     */
    protected Long resolveTeacherCourseId(String rawValue, List<Course> teacherCourses) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }

        try {
            return Long.parseLong(rawValue);
        } catch (NumberFormatException ignored) {
            if (teacherCourses == null) {
                return null;
            }
        }

        for (Course course : teacherCourses) {
            if (course != null && course.getCourseCode() != null && course.getCourseCode().equals(rawValue)) {
                return course.getId();
            }
        }
        return null;
    }

    /**
     * 从教师端请求体中解析课程标识。
     *
     * <p>兼容前端可能发送的两种键名：</p>
     * <ul>
     *   <li>{@code courseId}</li>
     *   <li>{@code course_id}</li>
     * </ul>
     *
     * <p>值兼容数字型课程 ID 与字符串课程代码。</p>
     */
    protected Long resolveTeacherCourseIdFromPayload(Map<String, Object> payload, List<Course> teacherCourses) {
        if (payload == null) {
            return null;
        }

        Object rawValue = payload.containsKey("courseId")
                ? payload.get("courseId")
                : payload.get("course_id");
        if (rawValue instanceof Number) {
            return ((Number) rawValue).longValue();
        }
        if (rawValue instanceof String) {
            return resolveTeacherCourseId((String) rawValue, teacherCourses);
        }
        return null;
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
