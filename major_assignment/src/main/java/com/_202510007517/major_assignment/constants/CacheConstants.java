package com._202510007517.major_assignment.constants;

/**
 * 缓存常量类
 * <p>
 * 统一管理缓存名称、Redis Key 命名空间、分布式锁前缀以及各缓存项的显式 TTL。
 * 所有 {@code @Cacheable}/{@code @CacheEvict} 注解必须引用此类定义的常量，
 * 禁止使用字面量硬编码（R5.2）。
 * </p>
 *
 * <h2>命名空间规约（与 {@code .kiro/specs/spring-cloud-migration/design.md §7.5 / §Data Models §3.1} 对齐）</h2>
 * <ul>
 *   <li>{@link #SESSION_NAMESPACE} —— 多角色业务会话的 Redis key 前缀（{@code SESSION:*})，
 *       由 {@code MultiRoleSessionManager} 使用。</li>
 *   <li>{@link #CAPTCHA_NAMESPACE} —— 验证码图片校验值的 Redis key 前缀（{@code CAPTCHA:IMG:*})，
 *       与业务会话前缀 {@link #SESSION_NAMESPACE} 严格隔离，避免串 key（R2.5 不变量）。</li>
 *   <li>{@link #LOCK_PREFIX} —— 分布式锁前缀（{@code LOCK:{domain}:{resourceId}})，R5.4。</li>
 * </ul>
 *
 * <h2>§Data Models §3.1 缓存命名空间与 TTL 矩阵</h2>
 * <pre>
 * +---------------------+----------------------+--------+-------------------------------+------------------+
 * | 缓存名              | Key 表达式            | TTL    | 失效策略                       | 所属服务          |
 * +---------------------+----------------------+--------+-------------------------------+------------------+
 * | USER:PROFILE        | #userId              | 10 min | 更新用户信息时 @CacheEvict     | User_Service     |
 * | USER:ROLES          | #userId              | 10 min | 修改角色时失效                 | User_Service     |
 * | COURSE:LIST         | #teacherId           |  5 min | 课程 CRUD 时失效               | Course_Service   |
 * | COURSE:DETAIL       | #courseId            |  5 min | 同上                           | Course_Service   |
 * | KP:MASTERY          | #studentId:#courseId | 30 min | Exam/Assignment 事件触发失效   | Analysis_Service |
 * | SCORE:TREND         | #studentId           | 30 min | 同上                           | Analysis_Service |
 * | AUTH:JTI_BLACKLIST  | #jti                 | 动态   | TTL = token 剩余有效期         | Auth_Service     |
 * | AUTH:REFRESH        | #rt                  |  7 天  | 登出 / 刷新时失效              | Auth_Service     |
 * | CAPTCHA:IMG         | #sessionKey          |  2 min | 验证后失效；前缀与会话隔离     | Auth_Service     |
 * +---------------------+----------------------+--------+-------------------------------+------------------+
 * </pre>
 *
 * <h2>兼容性说明</h2>
 * <p>
 * 单体阶段沿用 {@link #USERS}、{@link #USER_ROLES}、{@link #COURSES}、{@link #ASSIGNMENTS}、
 * {@link #EXAMS}、{@link #KNOWLEDGE_POINTS} 等旧缓存名以保证现有
 * {@code @Cacheable}/{@code @CacheEvict} 注解不破坏；
 * 阶段 2/3 剥离微服务时切换到 {@link #USER_PROFILE}、{@link #USER_ROLES_NS}、
 * {@link #COURSE_LIST}、{@link #COURSE_DETAIL} 等新命名空间。
 * 两套命名空间在同一 Redis 实例内不相互覆盖。
 * </p>
 */
public final class CacheConstants {

    private CacheConstants() {
        // 防止实例化
    }

    // ============================================================
    // Redis Key 命名空间（R2.5 / R5.2 / Design §7.5）
    // 规则：所有 Redis key 必须从此处显式引用前缀，禁止硬编码字面量。
    //      不同命名空间之间不得相互包含或前缀共享，保证业务会话、
    //      验证码、分布式锁互不串 key。
    // ============================================================

    /**
     * 业务会话 Redis key 前缀。
     * 实际 key 形如 {@code SESSION:<cookieName>:<sessionId>}。
     */
    public static final String SESSION_NAMESPACE = "SESSION:";

    /**
     * 验证码图片校验值 Redis key 前缀。
     * 实际 key 形如 {@code CAPTCHA:IMG:<sessionKey>}，
     * 与 {@link #SESSION_NAMESPACE} 完全隔离（R2.5 不变量）。
     */
    public static final String CAPTCHA_NAMESPACE = "CAPTCHA:IMG:";

    /**
     * 分布式锁前缀，固定格式 {@code LOCK:<domain>:<resourceId>}。
     */
    public static final String LOCK_PREFIX = "LOCK";

    // ============================================================
    // Spring Cache 缓存名称 —— 单体兼容命名（legacy）
    // 说明：阶段 1 治理时为保证现有 @Cacheable / @CacheEvict 注解不破坏，
    //      旧命名保留；阶段 2/3 剥离时切换到下方 "§3.1 矩阵命名"。
    // ============================================================

    public static final String USERS = "users";
    public static final String USER_ROLES = "userRoles";
    public static final String COURSES = "courses";
    public static final String ASSIGNMENTS = "assignments";
    public static final String EXAMS = "exams";
    public static final String KNOWLEDGE_POINTS = "knowledgePoints";

    // ============================================================
    // Spring Cache 缓存名称 —— §Data Models §3.1 矩阵命名（forward-compatible）
    // 格式：CACHE:{domain}:{resource}，TTL 由下方同名 *_TTL 常量显式声明。
    // ============================================================

    /**
     * 用户画像缓存。
     * <ul>
     *   <li>cacheName: {@code USER:PROFILE}</li>
     *   <li>key 表达式: {@code #userId}</li>
     *   <li>TTL: 10 min ({@link #USER_PROFILE_TTL} = 600 s)</li>
     *   <li>失效策略: 更新用户信息时 {@code @CacheEvict}</li>
     *   <li>所属服务: User_Service</li>
     * </ul>
     */
    public static final String USER_PROFILE = "USER:PROFILE";

    /**
     * 用户角色缓存（§3.1 规范命名，与 legacy {@link #USER_ROLES} 并存）。
     * <ul>
     *   <li>cacheName: {@code USER:ROLES}</li>
     *   <li>key 表达式: {@code #userId}</li>
     *   <li>TTL: 10 min ({@link #USER_ROLES_TTL} = 600 s)</li>
     *   <li>失效策略: 修改角色时失效</li>
     *   <li>所属服务: User_Service</li>
     * </ul>
     */
    public static final String USER_ROLES_NS = "USER:ROLES";

    /**
     * 教师课程列表缓存。
     * <ul>
     *   <li>cacheName: {@code COURSE:LIST}</li>
     *   <li>key 表达式: {@code #teacherId}</li>
     *   <li>TTL: 5 min ({@link #COURSE_LIST_TTL} = 300 s)</li>
     *   <li>失效策略: 课程 CRUD 时失效</li>
     *   <li>所属服务: Course_Service</li>
     * </ul>
     */
    public static final String COURSE_LIST = "COURSE:LIST";

    /**
     * 课程详情缓存。
     * <ul>
     *   <li>cacheName: {@code COURSE:DETAIL}</li>
     *   <li>key 表达式: {@code #courseId}</li>
     *   <li>TTL: 5 min ({@link #COURSE_DETAIL_TTL} = 300 s)</li>
     *   <li>失效策略: 课程 CRUD 时失效</li>
     *   <li>所属服务: Course_Service</li>
     * </ul>
     */
    public static final String COURSE_DETAIL = "COURSE:DETAIL";

    /**
     * 知识点掌握度缓存。
     * <ul>
     *   <li>cacheName: {@code KP:MASTERY}</li>
     *   <li>key 表达式: {@code #studentId:#courseId}</li>
     *   <li>TTL: 30 min ({@link #KP_MASTERY_TTL} = 1800 s)</li>
     *   <li>失效策略: Exam / Assignment 事件触发失效</li>
     *   <li>所属服务: Analysis_Service</li>
     * </ul>
     */
    public static final String KP_MASTERY = "KP:MASTERY";

    /**
     * 成绩趋势缓存。
     * <ul>
     *   <li>cacheName: {@code SCORE:TREND}</li>
     *   <li>key 表达式: {@code #studentId}</li>
     *   <li>TTL: 30 min ({@link #SCORE_TREND_TTL} = 1800 s)</li>
     *   <li>失效策略: Exam / Assignment 事件触发失效</li>
     *   <li>所属服务: Analysis_Service</li>
     * </ul>
     */
    public static final String SCORE_TREND = "SCORE:TREND";

    /**
     * JWT 黑名单（Redis 直写，不走 Spring Cache 抽象）。
     * <ul>
     *   <li>key 前缀: {@code AUTH:JTI_BLACKLIST}</li>
     *   <li>key 表达式: {@code #jti}</li>
     *   <li>TTL: 动态，由调用方传入 = token 剩余有效期（自然过期即自动清理）</li>
     *   <li>失效策略: 到期自动过期；登出 / 强制吊销时显式写入</li>
     *   <li>所属服务: Auth_Service</li>
     * </ul>
     */
    public static final String AUTH_JTI_BLACKLIST = "AUTH:JTI_BLACKLIST";

    /**
     * Refresh Token 存储（Redis 直写）。
     * <ul>
     *   <li>key 前缀: {@code AUTH:REFRESH}</li>
     *   <li>key 表达式: {@code #rt}</li>
     *   <li>TTL: 7 天 ({@link #AUTH_REFRESH_TTL} = 604800 s)</li>
     *   <li>失效策略: 登出 / 刷新滚动时失效</li>
     *   <li>所属服务: Auth_Service</li>
     * </ul>
     */
    public static final String AUTH_REFRESH = "AUTH:REFRESH";

    /**
     * 验证码缓存名（§3.1 规范命名）。
     * <ul>
     *   <li>cacheName: {@code CAPTCHA:IMG}</li>
     *   <li>key 表达式: {@code #sessionKey}</li>
     *   <li>TTL: 2 min ({@link #CAPTCHA_TTL} = 120 s)</li>
     *   <li>失效策略: 验证后立即失效</li>
     *   <li>所属服务: Auth_Service</li>
     *   <li>不变量: 与 {@link #SESSION_NAMESPACE} 前缀严格隔离（R2.5）。</li>
     * </ul>
     *
     * @see #CAPTCHA_NAMESPACE 具体 Redis key 前缀（含结尾冒号）
     */
    public static final String CAPTCHA_IMG = "CAPTCHA:IMG";

    // ============================================================
    // 缓存过期时间（秒）—— legacy 通用常量，保留以兼容旧代码
    // ============================================================

    public static final long DEFAULT_TTL = 3600;
    public static final long SHORT_TTL = 300;
    public static final long LONG_TTL = 86400;

    /**
     * 验证码过期时间（秒），设计默认 2 分钟。
     */
    public static final long CAPTCHA_TTL = 120;

    // ============================================================
    // 缓存过期时间（秒）—— §Data Models §3.1 矩阵显式 TTL
    // 与上方 forward-compatible 缓存名一一对应。
    // ============================================================

    /** {@link #USER_PROFILE} TTL = 10 min。 */
    public static final long USER_PROFILE_TTL = 600L;

    /** {@link #USER_ROLES_NS} TTL = 10 min。 */
    public static final long USER_ROLES_TTL = 600L;

    /** {@link #COURSE_LIST} TTL = 5 min。 */
    public static final long COURSE_LIST_TTL = 300L;

    /** {@link #COURSE_DETAIL} TTL = 5 min。 */
    public static final long COURSE_DETAIL_TTL = 300L;

    /** {@link #KP_MASTERY} TTL = 30 min。 */
    public static final long KP_MASTERY_TTL = 1800L;

    /** {@link #SCORE_TREND} TTL = 30 min。 */
    public static final long SCORE_TREND_TTL = 1800L;

    /**
     * {@link #AUTH_JTI_BLACKLIST} TTL = 动态（由调用方按 token 剩余有效期传入）。
     * 本常量仅用作"未提供显式 TTL 时的安全上限"兜底：24 小时。
     */
    public static final long AUTH_JTI_BLACKLIST_MAX_TTL = 86400L;

    /** {@link #AUTH_REFRESH} TTL = 7 天。 */
    public static final long AUTH_REFRESH_TTL = 7L * 24L * 60L * 60L; // 604800

    // ============================================================
    // R2.5 验证码与业务会话命名空间隔离不变量
    // ============================================================

    static {
        // 编译/加载期断言：两个前缀互不包含，避免 Redis key 串用。
        if (SESSION_NAMESPACE.startsWith(CAPTCHA_NAMESPACE)
                || CAPTCHA_NAMESPACE.startsWith(SESSION_NAMESPACE)) {
            throw new IllegalStateException(
                    "CacheConstants 违反 R2.5 不变量：SESSION_NAMESPACE 与 CAPTCHA_NAMESPACE 前缀相互包含"
                            + "（SESSION=" + SESSION_NAMESPACE + ", CAPTCHA=" + CAPTCHA_NAMESPACE + "）");
        }
    }
}
