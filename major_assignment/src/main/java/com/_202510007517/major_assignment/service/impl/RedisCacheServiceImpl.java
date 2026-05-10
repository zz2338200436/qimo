package com._202510007517.major_assignment.service.impl;

import com._202510007517.major_assignment.service.RedisCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

@Service
public class RedisCacheServiceImpl implements RedisCacheService {

    private static final String KEY_PREFIX = "MAJOR_ASSIGNMENT:";
    private static final long DEFAULT_EXPIRE_TIME = 3600; // 默认过期时间1小时
    private static final TimeUnit DEFAULT_TIME_UNIT = TimeUnit.SECONDS;
    private static final long RANDOM_EXPIRE_RANGE = 1800; // 随机过期时间范围，用于防止缓存雪崩

    // 用于缓存穿透防护的布隆过滤器替代方案：使用Redis集合存储所有可能的键
    private static final String BLOOM_FILTER_KEY = KEY_PREFIX + "BLOOM_FILTER";
    
    // 用于缓存击穿防护的互斥锁
    private final Map<String, Object> locks = new ConcurrentHashMap<>();
    
    // 缓存命中率统计
    private final AtomicLong hitCount = new AtomicLong(0);
    private final AtomicLong missCount = new AtomicLong(0);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public <T> T get(String key) {
        String fullKey = buildFullKey(key);
        T value = (T) redisTemplate.opsForValue().get(fullKey);
        if (value != null) {
            hitCount.incrementAndGet();
            return value;
        } else {
            missCount.incrementAndGet();
            return null;
        }
    }

    @Override
    public <T> T getOrLoad(String key, Supplier<T> supplier, long expireTime, TimeUnit timeUnit) {
        // 1. 尝试从缓存获取
        String fullKey = buildFullKey(key);
        T value = (T) redisTemplate.opsForValue().get(fullKey);
        if (value != null) {
            hitCount.incrementAndGet();
            return value;
        }

        // 2. 获取互斥锁，防止缓存击穿
        Object lock = locks.computeIfAbsent(key, k -> new Object());
        synchronized (lock) {
            // 双重检查，防止重复加载
            value = (T) redisTemplate.opsForValue().get(fullKey);
            if (value != null) {
                hitCount.incrementAndGet();
                return value;
            }

            // 3. 从数据源加载数据
            value = supplier.get();
            if (value != null) {
                // 4. 设置缓存（传入原始key，set方法会自己构建fullKey）
                set(key, value, expireTime, timeUnit);
            }
        }

        // 释放锁
        locks.remove(key);
        return value;
    }

    @Override
    public void set(String key, Object value, long expireTime, TimeUnit timeUnit) {
        String fullKey = buildFullKey(key);
        redisTemplate.opsForValue().set(fullKey, value, expireTime, timeUnit);
        // 添加到布隆过滤器
        addToBloomFilter(key);
    }

    @Override
    public void set(String key, Object value) {
        set(key, value, DEFAULT_EXPIRE_TIME, DEFAULT_TIME_UNIT);
    }

    @Override
    public void delete(String key) {
        String fullKey = buildFullKey(key);
        redisTemplate.delete(fullKey);
        // 从布隆过滤器移除
        removeFromBloomFilter(key);
    }

    @Override
    public void delete(String... keys) {
        List<String> fullKeys = new ArrayList<>(keys.length);
        for (String key : keys) {
            fullKeys.add(buildFullKey(key));
            removeFromBloomFilter(key);
        }
        redisTemplate.delete(fullKeys);
    }

    @Override
    public void expire(String key, long expireTime, TimeUnit timeUnit) {
        String fullKey = buildFullKey(key);
        redisTemplate.expire(fullKey, expireTime, timeUnit);
    }

    @Override
    public boolean exists(String key) {
        String fullKey = buildFullKey(key);
        Boolean exists = redisTemplate.hasKey(fullKey);
        return exists != null && exists;
    }

    @Override
    public boolean setIfAbsent(String key, Object value, long expireTime, TimeUnit timeUnit) {
        String fullKey = buildFullKey(key);
        Boolean result = redisTemplate.opsForValue().setIfAbsent(fullKey, value, expireTime, timeUnit);
        if (result != null && result) {
            addToBloomFilter(key);
        }
        return result != null && result;
    }

    @Override
    public long getHitCount() {
        return hitCount.get();
    }

    @Override
    public long getMissCount() {
        return missCount.get();
    }

    @Override
    public double getHitRate() {
        long total = hitCount.get() + missCount.get();
        return total == 0 ? 0 : (double) hitCount.get() / total;
    }

    @Override
    public void resetStatistics() {
        hitCount.set(0);
        missCount.set(0);
    }

    /**
     * 构建完整的缓存键
     */
    private String buildFullKey(String key) {
        return KEY_PREFIX + key;
    }

    /**
     * 检查键是否可能存在于缓存中（布隆过滤器替代方案）
     */
    private boolean mightContain(String key) {
        // 使用Redis集合作为简单的布隆过滤器替代方案
        Boolean result = redisTemplate.opsForSet().isMember(BLOOM_FILTER_KEY, key);
        return result != null && result;
    }

    /**
     * 将键添加到布隆过滤器
     */
    private void addToBloomFilter(String key) {
        redisTemplate.opsForSet().add(BLOOM_FILTER_KEY, key);
    }

    /**
     * 从布隆过滤器移除键
     */
    private void removeFromBloomFilter(String key) {
        redisTemplate.opsForSet().remove(BLOOM_FILTER_KEY, key);
    }
}
