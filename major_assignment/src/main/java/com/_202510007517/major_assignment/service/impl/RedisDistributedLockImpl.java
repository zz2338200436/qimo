package com._202510007517.major_assignment.service.impl;

import com._202510007517.major_assignment.constants.CacheConstants;
import com._202510007517.major_assignment.service.DistributedLock;
import com._202510007517.major_assignment.service.LockAcquisitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 基于 Redis {@code SET NX PX} + Lua 解锁脚本的 {@link DistributedLock} 实现（R5.4 / Task 8.5）。
 *
 * <p>实现要点：</p>
 * <ul>
 *   <li>加锁：{@code redisTemplate.opsForValue().setIfAbsent(key, token, ttl)}，
 *       token 为 {@link UUID#randomUUID()} 的字符串表示，确保跨线程/跨进程唯一。</li>
 *   <li>解锁：Lua 脚本原子比对 {@code GET key == token} 后再 {@code DEL}，避免误删。</li>
 *   <li>token 归属：通过 {@link ThreadLocal}{@code <Map<String, String>>} 保存"本线程持有的
 *       锁 key → token"映射，从而让同一线程的 {@link #unlock} 能找回对应 token。
 *       这是由于 {@link DistributedLock} 接口签名不暴露 token 所作的权衡：同一线程内
 *       {@code tryLock} → 业务 → {@code unlock} 的调用链安全且无须改动接口。</li>
 *   <li>Key 格式：{@code LOCK:{domain}:{resourceId}}，前缀引用 {@link CacheConstants#LOCK_PREFIX}。</li>
 * </ul>
 */
@Service
public class RedisDistributedLockImpl implements DistributedLock {

    private static final Logger log = LoggerFactory.getLogger(RedisDistributedLockImpl.class);

    /**
     * Lua 原子解锁脚本：仅当 GET 回来的 value 等于调用方 token 时才 DEL。
     * 返回值 1 = 释放成功，0 = 未持锁 / token 不匹配。
     */
    private static final String UNLOCK_LUA =
            "if redis.call('GET', KEYS[1]) == ARGV[1] then "
            + "return redis.call('DEL', KEYS[1]) "
            + "else return 0 end";

    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT =
            new DefaultRedisScript<>(UNLOCK_LUA, Long.class);

    /**
     * 本线程当前持有的锁 token：key = 完整 Redis lock key，value = token。
     */
    private static final ThreadLocal<Map<String, String>> HELD_TOKENS =
            ThreadLocal.withInitial(HashMap::new);

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisDistributedLockImpl(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean tryLock(String domain, String resourceId, Duration ttl) {
        validateDomainAndResource(domain, resourceId);
        validateTtl(ttl);

        String key = buildKey(domain, resourceId);
        String token = UUID.randomUUID().toString();

        Boolean ok = redisTemplate.opsForValue().setIfAbsent(key, token, ttl);
        boolean acquired = Boolean.TRUE.equals(ok);
        if (acquired) {
            HELD_TOKENS.get().put(key, token);
            log.debug("分布式锁获取成功 key={}", key);
        } else {
            log.debug("分布式锁获取失败（已被持有）key={}", key);
        }
        return acquired;
    }

    @Override
    public void unlock(String domain, String resourceId) {
        validateDomainAndResource(domain, resourceId);

        String key = buildKey(domain, resourceId);
        Map<String, String> localMap = HELD_TOKENS.get();
        String token = localMap.remove(key);
        if (token == null) {
            // 当前线程未持有该锁：不做任何远程调用（防误删核心约束）
            log.debug("unlock 跳过：本线程未持有锁 key={}", key);
            cleanupIfEmpty(localMap);
            return;
        }

        Long result = redisTemplate.execute(
                UNLOCK_SCRIPT,
                Collections.singletonList(key),
                token);
        if (result == null || result == 0L) {
            // 锁可能已自然过期或被其他逻辑清理，记录但不抛异常
            log.warn("unlock 未删除任何 key（锁可能已过期）key={}", key);
        } else {
            log.debug("分布式锁释放成功 key={}", key);
        }
        cleanupIfEmpty(localMap);
    }

    @Override
    public <T> T withLock(String domain, String resourceId, Duration ttl, Supplier<T> action) {
        validateDomainAndResource(domain, resourceId);
        validateTtl(ttl);
        if (action == null) {
            throw new IllegalArgumentException("action 不能为空");
        }

        if (!tryLock(domain, resourceId, ttl)) {
            throw new LockAcquisitionException(
                    "无法获取分布式锁: " + buildKey(domain, resourceId));
        }
        try {
            return action.get();
        } finally {
            unlock(domain, resourceId);
        }
    }

    /**
     * 构造锁的完整 Redis key，固定格式 {@code LOCK:{domain}:{resourceId}}。
     * <p>输出恒满足正则 {@code ^LOCK:[^:]+:.+$}（Property P9）。</p>
     */
    private static String buildKey(String domain, String resourceId) {
        return CacheConstants.LOCK_PREFIX + ":" + domain + ":" + resourceId;
    }

    private static void validateDomainAndResource(String domain, String resourceId) {
        if (domain == null || domain.isEmpty()) {
            throw new IllegalArgumentException("domain 不能为空");
        }
        if (domain.indexOf(':') >= 0) {
            throw new IllegalArgumentException("domain 不能包含冒号: " + domain);
        }
        if (resourceId == null || resourceId.isEmpty()) {
            throw new IllegalArgumentException("resourceId 不能为空");
        }
    }

    private static void validateTtl(Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("ttl 必须为正数: " + ttl);
        }
    }

    private static void cleanupIfEmpty(Map<String, String> localMap) {
        if (localMap.isEmpty()) {
            HELD_TOKENS.remove();
        }
    }
}
