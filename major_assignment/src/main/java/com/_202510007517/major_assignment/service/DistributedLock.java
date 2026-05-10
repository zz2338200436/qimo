package com._202510007517.major_assignment.service;

import com._202510007517.major_assignment.constants.CacheConstants;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * 分布式锁抽象（Task 8.5 / R5.4）。
 *
 * <p>本接口是 Redis 分布式锁的应用层 Facade，满足
 * {@code .kiro/specs/spring-cloud-migration/design.md §7.5 分布式锁使用约定}：
 * 所有跨进程互斥需求必须经由此接口，不得直接使用 {@code SET NX} 散落在业务代码中。
 * 旧版 {@link RedisCacheService} 聚焦于缓存读写，本锁能力作为独立组件封装，
 * 避免 {@code RedisCacheService} 接口继续膨胀（任务描述中"在 RedisCacheService 封装"
 * 解读为在 cache 同包内封装分布式锁组件，而非加方法到 {@code RedisCacheService}）。</p>
 *
 * <h2>Key 约定</h2>
 * <p>所有锁 Key 由实现统一构造为固定格式：
 * <pre>{@code LOCK:{domain}:{resourceId}}</pre>
 * 前缀 {@code "LOCK"} 引用 {@link CacheConstants#LOCK_PREFIX}，
 * 禁止业务侧拼接变体（满足 §7.5 / Property P9 的格式不变量）。</p>
 *
 * <h2>防误删机制</h2>
 * <p>{@link #tryLock(String, String, Duration)} 获取锁时会生成一个调用方唯一的 token
 * （通常为 {@code UUID.randomUUID().toString()}），并以该 token 作为 Redis value 写入。
 * {@link #unlock(String, String)} 通过 Lua 脚本先 {@code GET} 比较 token，
 * 仅当当前调用方仍然持锁时才 {@code DEL}，从而避免以下场景的"误删他人锁"：</p>
 * <ol>
 *   <li>线程 A 获取锁，TTL=2s</li>
 *   <li>线程 A 执行业务超过 2s，锁自动过期</li>
 *   <li>线程 B 获取同一把锁</li>
 *   <li>线程 A 业务执行完成，如果简单 {@code DEL} 将错误删除线程 B 的锁</li>
 *   <li>token 校验使步骤 4 变为 no-op（GET 的 token != A 的 token）</li>
 * </ol>
 *
 * <h2>TTL 契约</h2>
 * <ul>
 *   <li>TTL 必须由调用方显式提供，不设默认。</li>
 *   <li>业务方需保证 {@code action} 执行时间 &lt; TTL；本实现不提供自动续期（watch dog）。</li>
 *   <li>对长耗时任务，调用方应拆分子任务或自行决定是否续期（目前不在本 task 范围）。</li>
 * </ul>
 *
 * <h2>参数校验</h2>
 * <p>{@code domain}/{@code resourceId} 为 null 或空白、{@code ttl} 为 null 或非正、
 * {@code action} 为 null 均抛 {@link IllegalArgumentException}。</p>
 *
 * @see CacheConstants#LOCK_PREFIX
 */
public interface DistributedLock {

    /**
     * 尝试获取一把锁。非阻塞，不等待：竞争失败立即返回 {@code false}。
     *
     * @param domain     业务域（例如 {@code "order"}、{@code "user"}），非空
     * @param resourceId 资源 ID（例如订单号、用户 ID），非空
     * @param ttl        锁持有时长，必须为正 {@link Duration}
     * @return 成功获取返回 {@code true}；否则 {@code false}
     * @throws IllegalArgumentException 参数非法
     */
    boolean tryLock(String domain, String resourceId, Duration ttl);

    /**
     * 释放锁。仅当当前调用方（同线程，之前调用 {@link #tryLock} 成功）仍持有锁时才真正释放；
     * 否则为 no-op（例如锁已超时或未被本线程持有）。
     *
     * @param domain     业务域
     * @param resourceId 资源 ID
     * @throws IllegalArgumentException 参数非法
     */
    void unlock(String domain, String resourceId);

    /**
     * 获取锁 → 执行 {@code action} → finally 释放锁。
     *
     * <p>若 {@link #tryLock} 返回 {@code false}，抛 {@link LockAcquisitionException}，
     * 不执行 {@code action}。若 {@code action} 抛异常，异常向上传播，但仍然在 {@code finally}
     * 中释放锁。</p>
     *
     * @param domain     业务域
     * @param resourceId 资源 ID
     * @param ttl        锁持有时长
     * @param action     临界区动作
     * @param <T>        返回值类型
     * @return {@code action} 的返回值
     * @throws LockAcquisitionException 无法获取锁
     * @throws IllegalArgumentException 参数非法
     */
    <T> T withLock(String domain, String resourceId, Duration ttl, Supplier<T> action);
}
