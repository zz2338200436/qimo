package com._202510007517.major_assignment.service.impl;

import com._202510007517.major_assignment.service.RedisCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class RedisCacheServiceImplTest {

    @Autowired
    private RedisCacheService redisCacheService;

    private static final String TEST_KEY = "test_key";
    private static final String TEST_VALUE = "test_value";
    private static final String NON_EXISTENT_KEY = "non_existent_key";

    @BeforeEach
    public void setUp() {
        // 测试前清除缓存并重置统计信息
        redisCacheService.delete(TEST_KEY);
        redisCacheService.resetStatistics();
    }

    @Test
    public void testSetAndGet() {
        // 测试设置缓存
        redisCacheService.set(TEST_KEY, TEST_VALUE);
        // 测试获取缓存
        String value = redisCacheService.get(TEST_KEY);
        assertEquals(TEST_VALUE, value);
        // 测试缓存命中统计
        assertEquals(1, redisCacheService.getHitCount());
        assertEquals(0, redisCacheService.getMissCount());
    }

    @Test
    public void testSetWithExpireTime() throws InterruptedException {
        // 测试设置带过期时间的缓存
        redisCacheService.set(TEST_KEY, TEST_VALUE, 1, TimeUnit.SECONDS);
        // 立即获取
        assertNotNull(redisCacheService.get(TEST_KEY));
        // 等待过期
        Thread.sleep(1500);
        // 过期后获取
        assertNull(redisCacheService.get(TEST_KEY));
        // 测试缓存统计
        assertEquals(1, redisCacheService.getHitCount());
        assertEquals(1, redisCacheService.getMissCount());
    }

    @Test
    public void testGetNonExistentKey() {
        // 测试获取不存在的缓存
        String value = redisCacheService.get(NON_EXISTENT_KEY);
        assertNull(value);
        // 测试缓存未命中统计
        assertEquals(0, redisCacheService.getHitCount());
        assertEquals(1, redisCacheService.getMissCount());
    }

    @Test
    public void testDelete() {
        // 设置缓存
        redisCacheService.set(TEST_KEY, TEST_VALUE);
        // 验证缓存存在
        assertNotNull(redisCacheService.get(TEST_KEY));
        // 删除缓存
        redisCacheService.delete(TEST_KEY);
        // 验证缓存不存在
        assertNull(redisCacheService.get(TEST_KEY));
    }

    @Test
    public void testDeleteMultipleKeys() {
        // 设置多个缓存
        String key1 = "key1";
        String key2 = "key2";
        String key3 = "key3";
        redisCacheService.set(key1, "value1");
        redisCacheService.set(key2, "value2");
        redisCacheService.set(key3, "value3");
        
        // 验证缓存存在
        assertNotNull(redisCacheService.get(key1));
        assertNotNull(redisCacheService.get(key2));
        assertNotNull(redisCacheService.get(key3));
        
        // 批量删除缓存
        redisCacheService.delete(key1, key2, key3);
        
        // 验证缓存不存在
        assertNull(redisCacheService.get(key1));
        assertNull(redisCacheService.get(key2));
        assertNull(redisCacheService.get(key3));
    }

    @Test
    public void testGetOrLoad() {
        // 测试getOrLoad方法，缓存不存在时从supplier加载
        String value = redisCacheService.getOrLoad(TEST_KEY, () -> TEST_VALUE, 3600, TimeUnit.SECONDS);
        assertEquals(TEST_VALUE, value);
        
        // 测试缓存命中
        value = redisCacheService.get(TEST_KEY);
        assertEquals(TEST_VALUE, value);
        
        // 测试缓存统计
        assertEquals(1, redisCacheService.getHitCount());
        assertEquals(0, redisCacheService.getMissCount());
    }

    @Test
    public void testExists() {
        // 测试不存在的键
        assertFalse(redisCacheService.exists(TEST_KEY));
        
        // 测试存在的键
        redisCacheService.set(TEST_KEY, TEST_VALUE);
        assertTrue(redisCacheService.exists(TEST_KEY));
    }

    @Test
    public void testSetIfAbsent() {
        // 测试不存在的键
        boolean result = redisCacheService.setIfAbsent(TEST_KEY, TEST_VALUE, 3600, TimeUnit.SECONDS);
        assertTrue(result);
        assertEquals(TEST_VALUE, redisCacheService.get(TEST_KEY));
        
        // 测试已存在的键
        result = redisCacheService.setIfAbsent(TEST_KEY, "new_value", 3600, TimeUnit.SECONDS);
        assertFalse(result);
        assertEquals(TEST_VALUE, redisCacheService.get(TEST_KEY));
    }

    @Test
    public void testCacheStatistics() {
        // 测试缓存统计重置
        redisCacheService.resetStatistics();
        assertEquals(0, redisCacheService.getHitCount());
        assertEquals(0, redisCacheService.getMissCount());
        assertEquals(0, redisCacheService.getHitRate());
        
        // 测试缓存命中
        redisCacheService.set(TEST_KEY, TEST_VALUE);
        redisCacheService.get(TEST_KEY);
        redisCacheService.get(TEST_KEY);
        assertEquals(2, redisCacheService.getHitCount());
        assertEquals(0, redisCacheService.getMissCount());
        assertEquals(1.0, redisCacheService.getHitRate());
        
        // 测试缓存未命中
        redisCacheService.get(NON_EXISTENT_KEY);
        assertEquals(2, redisCacheService.getHitCount());
        assertEquals(1, redisCacheService.getMissCount());
        assertEquals(2.0 / 3.0, redisCacheService.getHitRate(), 0.001);
    }

    @Test
    public void testExpire() throws InterruptedException {
        // 设置缓存
        redisCacheService.set(TEST_KEY, TEST_VALUE);
        // 更新过期时间
        redisCacheService.expire(TEST_KEY, 1, TimeUnit.SECONDS);
        // 立即获取
        assertNotNull(redisCacheService.get(TEST_KEY));
        // 等待过期
        Thread.sleep(1500);
        // 过期后获取
        assertNull(redisCacheService.get(TEST_KEY));
    }
}
