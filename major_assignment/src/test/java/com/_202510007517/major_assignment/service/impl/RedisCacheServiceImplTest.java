package com._202510007517.major_assignment.service.impl;

import com._202510007517.major_assignment.service.RedisCacheService;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class RedisCacheServiceImplTest {

    @Autowired
    private RedisCacheService redisCacheService;

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    private static final String TEST_VALUE = "test_value";
    private String testKey;
    private String nonExistentKey;

    @BeforeEach
    public void setUp() {
        Assumptions.assumeTrue(isRedisAvailable(), () ->
                "Redis not available at " + redisHost + ":" + redisPort);
        testKey = "test_key_" + UUID.randomUUID();
        nonExistentKey = "non_existent_key_" + UUID.randomUUID();
        // 测试前清除缓存并重置统计信息
        redisCacheService.delete(testKey);
        redisCacheService.delete(nonExistentKey);
        redisCacheService.resetStatistics();
    }

    @Test
    public void testSetAndGet() {
        // 测试设置缓存
        redisCacheService.set(testKey, TEST_VALUE);
        // 测试获取缓存
        String value = redisCacheService.get(testKey);
        assertEquals(TEST_VALUE, value);
        // 测试缓存命中统计
        assertEquals(1, redisCacheService.getHitCount());
        assertEquals(0, redisCacheService.getMissCount());
    }

    @Test
    public void testSetWithExpireTime() throws InterruptedException {
        // 测试设置带过期时间的缓存
        redisCacheService.set(testKey, TEST_VALUE, 1, TimeUnit.SECONDS);
        // 立即获取
        assertNotNull(redisCacheService.get(testKey));
        // 等待过期
        awaitKeyExpiration(testKey, Duration.ofSeconds(5));
        assertNull(redisCacheService.get(testKey));
        // 测试缓存统计
        assertEquals(1, redisCacheService.getHitCount());
        assertEquals(1, redisCacheService.getMissCount());
    }

    @Test
    public void testGetNonExistentKey() {
        // 测试获取不存在的缓存
        String value = redisCacheService.get(nonExistentKey);
        assertNull(value);
        // 测试缓存未命中统计
        assertEquals(0, redisCacheService.getHitCount());
        assertEquals(1, redisCacheService.getMissCount());
    }

    @Test
    public void testDelete() {
        // 设置缓存
        redisCacheService.set(testKey, TEST_VALUE);
        // 验证缓存存在
        assertNotNull(redisCacheService.get(testKey));
        // 删除缓存
        redisCacheService.delete(testKey);
        // 验证缓存不存在
        assertNull(redisCacheService.get(testKey));
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
        String value = redisCacheService.getOrLoad(testKey, () -> TEST_VALUE, 3600, TimeUnit.SECONDS);
        assertEquals(TEST_VALUE, value);
        
        // 测试缓存命中
        value = redisCacheService.get(testKey);
        assertEquals(TEST_VALUE, value);
        
        // 测试缓存统计
        assertEquals(1, redisCacheService.getHitCount());
        assertEquals(0, redisCacheService.getMissCount());
    }

    @Test
    public void testExists() {
        // 测试不存在的键
        assertFalse(redisCacheService.exists(testKey));
        
        // 测试存在的键
        redisCacheService.set(testKey, TEST_VALUE);
        assertTrue(redisCacheService.exists(testKey));
    }

    @Test
    public void testSetIfAbsent() {
        // 测试不存在的键
        boolean result = redisCacheService.setIfAbsent(testKey, TEST_VALUE, 3600, TimeUnit.SECONDS);
        assertTrue(result);
        assertEquals(TEST_VALUE, redisCacheService.get(testKey));
        
        // 测试已存在的键
        result = redisCacheService.setIfAbsent(testKey, "new_value", 3600, TimeUnit.SECONDS);
        assertFalse(result);
        assertEquals(TEST_VALUE, redisCacheService.get(testKey));
    }

    @Test
    public void testCacheStatistics() {
        // 测试缓存统计重置
        redisCacheService.resetStatistics();
        assertEquals(0, redisCacheService.getHitCount());
        assertEquals(0, redisCacheService.getMissCount());
        assertEquals(0, redisCacheService.getHitRate());
        
        // 测试缓存命中
        redisCacheService.set(testKey, TEST_VALUE);
        redisCacheService.get(testKey);
        redisCacheService.get(testKey);
        assertEquals(2, redisCacheService.getHitCount());
        assertEquals(0, redisCacheService.getMissCount());
        assertEquals(1.0, redisCacheService.getHitRate());
        
        // 测试缓存未命中
        redisCacheService.get(nonExistentKey);
        assertEquals(2, redisCacheService.getHitCount());
        assertEquals(1, redisCacheService.getMissCount());
        assertEquals(2.0 / 3.0, redisCacheService.getHitRate(), 0.001);
    }

    @Test
    public void testExpire() throws InterruptedException {
        // 设置缓存
        redisCacheService.set(testKey, TEST_VALUE);
        // 更新过期时间
        redisCacheService.expire(testKey, 1, TimeUnit.SECONDS);
        // 立即获取
        assertNotNull(redisCacheService.get(testKey));
        // 等待过期
        awaitKeyExpiration(testKey, Duration.ofSeconds(5));
        assertNull(redisCacheService.get(testKey));
    }

    private void awaitKeyExpiration(String key, Duration timeout) throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (!redisCacheService.exists(key)) {
                return;
            }
            Thread.sleep(100);
        }
        assertFalse(redisCacheService.exists(key));
    }

    private boolean isRedisAvailable() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(redisHost, redisPort), 1000);
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }
}
