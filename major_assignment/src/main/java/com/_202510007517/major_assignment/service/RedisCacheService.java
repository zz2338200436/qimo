package com._202510007517.major_assignment.service;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public interface RedisCacheService {

    /**
     * 获取缓存值
     * @param key 缓存键
     * @param <T> 值类型
     * @return 缓存值
     */
    <T> T get(String key);

    /**
     * 获取缓存值，如果不存在则从数据源加载并缓存
     * @param key 缓存键
     * @param supplier 数据源加载函数
     * @param expireTime 过期时间
     * @param timeUnit 时间单位
     * @param <T> 值类型
     * @return 缓存值
     */
    <T> T getOrLoad(String key, Supplier<T> supplier, long expireTime, TimeUnit timeUnit);

    /**
     * 设置缓存值
     * @param key 缓存键
     * @param value 缓存值
     * @param expireTime 过期时间
     * @param timeUnit 时间单位
     */
    void set(String key, Object value, long expireTime, TimeUnit timeUnit);

    /**
     * 设置缓存值，使用默认过期时间
     * @param key 缓存键
     * @param value 缓存值
     */
    void set(String key, Object value);

    /**
     * 删除缓存
     * @param key 缓存键
     */
    void delete(String key);

    /**
     * 批量删除缓存
     * @param keys 缓存键数组
     */
    void delete(String... keys);

    /**
     * 更新缓存过期时间
     * @param key 缓存键
     * @param expireTime 过期时间
     * @param timeUnit 时间单位
     */
    void expire(String key, long expireTime, TimeUnit timeUnit);

    /**
     * 检查缓存是否存在
     * @param key 缓存键
     * @return 是否存在
     */
    boolean exists(String key);

    /**
     * 原子性地设置缓存值，仅当键不存在时
     * @param key 缓存键
     * @param value 缓存值
     * @param expireTime 过期时间
     * @param timeUnit 时间单位
     * @return 是否设置成功
     */
    boolean setIfAbsent(String key, Object value, long expireTime, TimeUnit timeUnit);

    /**
     * 获取缓存命中次数
     * @return 命中次数
     */
    long getHitCount();

    /**
     * 获取缓存未命中次数
     * @return 未命中次数
     */
    long getMissCount();

    /**
     * 计算缓存命中率
     * @return 命中率
     */
    double getHitRate();

    /**
     * 重置缓存统计信息
     */
    void resetStatistics();
}
