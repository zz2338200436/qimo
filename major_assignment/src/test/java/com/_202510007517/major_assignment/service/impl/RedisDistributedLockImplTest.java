package com._202510007517.major_assignment.service.impl;

import com._202510007517.major_assignment.service.LockAcquisitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 纯单元测试（Mockito），不依赖 Spring / 真实 Redis。
 *
 * <p>验证 {@link RedisDistributedLockImpl} 的 key 格式、token 机制、
 * Lua 解锁调用以及参数校验，覆盖 Task 8.5 的所有行为要点。</p>
 */
@ExtendWith(MockitoExtension.class)
class RedisDistributedLockImplTest {

    @Mock
    RedisTemplate<String, Object> redisTemplate;

    @Mock
    ValueOperations<String, Object> valueOps;

    RedisDistributedLockImpl lock;

    @BeforeEach
    void setUp() {
        lock = new RedisDistributedLockImpl(redisTemplate);
    }

    // -----------------------------------------------------------------
    // 1. tryLock —— 成功路径 & Key 格式
    // -----------------------------------------------------------------

    @Test
    void tryLock_returnsTrue_andUsesExpectedKey_whenSetIfAbsentSucceeds() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), any(), any(Duration.class))).thenReturn(true);

        boolean acquired = lock.tryLock("order", "123", Duration.ofSeconds(5));

        assertTrue(acquired);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> tokenCaptor = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(valueOps).setIfAbsent(keyCaptor.capture(), tokenCaptor.capture(), ttlCaptor.capture());
        assertEquals("LOCK:order:123", keyCaptor.getValue());
        assertEquals(Duration.ofSeconds(5), ttlCaptor.getValue());
        assertTrue(tokenCaptor.getValue() instanceof String);
        assertFalse(((String) tokenCaptor.getValue()).isEmpty());
    }

    // -----------------------------------------------------------------
    // 2. tryLock —— 竞争失败路径
    // -----------------------------------------------------------------

    @Test
    void tryLock_returnsFalse_whenSetIfAbsentReturnsFalse() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), any(), any(Duration.class))).thenReturn(false);

        assertFalse(lock.tryLock("order", "123", Duration.ofSeconds(5)));
    }

    @Test
    void tryLock_returnsFalse_whenSetIfAbsentReturnsNull() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), any(), any(Duration.class))).thenReturn(null);

        assertFalse(lock.tryLock("order", "123", Duration.ofSeconds(5)));
    }

    // -----------------------------------------------------------------
    // 3. unlock —— 同线程 tryLock 成功后，Lua 脚本被调用且 token 匹配
    // -----------------------------------------------------------------

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void unlock_afterSuccessfulTryLock_executesLuaWithStoredToken() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        ArgumentCaptor<Object> tokenCaptor = ArgumentCaptor.forClass(Object.class);
        when(valueOps.setIfAbsent(anyString(), tokenCaptor.capture(), any(Duration.class)))
                .thenReturn(true);
        when(redisTemplate.execute((RedisScript) any(), anyList(), any())).thenReturn(1L);

        assertTrue(lock.tryLock("order", "123", Duration.ofSeconds(5)));
        String storedToken = (String) tokenCaptor.getValue();

        lock.unlock("order", "123");

        verify(redisTemplate).execute(
                (RedisScript) any(),
                eq(Collections.singletonList("LOCK:order:123")),
                eq(storedToken));
    }

    // -----------------------------------------------------------------
    // 4. unlock —— 未持锁时是 no-op
    // -----------------------------------------------------------------

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void unlock_withoutPriorTryLock_isNoOp() {
        lock.unlock("order", "never-locked");

        verify(redisTemplate, never()).execute((RedisScript) any(), anyList(), any());
    }

    // -----------------------------------------------------------------
    // 5. withLock —— 成功路径返回 action 结果 + 解锁
    // -----------------------------------------------------------------

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void withLock_returnsActionResult_andReleasesLock() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), any(), any(Duration.class))).thenReturn(true);
        when(redisTemplate.execute((RedisScript) any(), anyList(), any())).thenReturn(1L);

        String result = lock.withLock("order", "42", Duration.ofSeconds(2), () -> "OK");

        assertEquals("OK", result);
        verify(redisTemplate).execute(
                (RedisScript) any(),
                eq(Collections.singletonList("LOCK:order:42")),
                any());
    }

    // -----------------------------------------------------------------
    // 6. withLock —— action 抛异常时仍然解锁，异常向上传播
    // -----------------------------------------------------------------

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void withLock_propagatesActionException_andStillUnlocks() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), any(), any(Duration.class))).thenReturn(true);
        when(redisTemplate.execute((RedisScript) any(), anyList(), any())).thenReturn(1L);

        AtomicBoolean called = new AtomicBoolean(false);
        IllegalStateException boom = assertThrows(IllegalStateException.class, () ->
                lock.withLock("order", "42", Duration.ofSeconds(2), () -> {
                    called.set(true);
                    throw new IllegalStateException("业务炸了");
                }));

        assertTrue(called.get());
        assertEquals("业务炸了", boom.getMessage());
        verify(redisTemplate).execute(
                (RedisScript) any(),
                eq(Collections.singletonList("LOCK:order:42")),
                any());
    }

    // -----------------------------------------------------------------
    // 7. withLock —— tryLock 失败时抛 LockAcquisitionException, 不执行 action
    // -----------------------------------------------------------------

    @Test
    void withLock_throwsLockAcquisitionException_whenTryLockFails_andDoesNotRunAction() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), any(), any(Duration.class))).thenReturn(false);

        AtomicBoolean invoked = new AtomicBoolean(false);
        LockAcquisitionException ex = assertThrows(LockAcquisitionException.class, () ->
                lock.withLock("order", "42", Duration.ofMillis(500), () -> {
                    invoked.set(true);
                    return "不应被调用";
                }));

        assertFalse(invoked.get());
        assertTrue(ex.getMessage().contains("LOCK:order:42"));
    }

    // -----------------------------------------------------------------
    // 8. 参数校验 —— null / 空 / 非正 TTL
    // -----------------------------------------------------------------

    @Test
    void tryLock_rejectsNullDomain() {
        // lenient: 参数校验分支不会触达 opsForValue，但 setUp 级别的 Mock 未使用不应报错
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        assertThrows(IllegalArgumentException.class,
                () -> lock.tryLock(null, "x", Duration.ofSeconds(1)));
    }

    @Test
    void tryLock_rejectsEmptyDomain() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        assertThrows(IllegalArgumentException.class,
                () -> lock.tryLock("", "x", Duration.ofSeconds(1)));
    }

    @Test
    void tryLock_rejectsNullResourceId() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        assertThrows(IllegalArgumentException.class,
                () -> lock.tryLock("d", null, Duration.ofSeconds(1)));
    }

    @Test
    void tryLock_rejectsEmptyResourceId() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        assertThrows(IllegalArgumentException.class,
                () -> lock.tryLock("d", "", Duration.ofSeconds(1)));
    }

    @Test
    void tryLock_rejectsNullTtl() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        assertThrows(IllegalArgumentException.class,
                () -> lock.tryLock("d", "r", null));
    }

    @Test
    void tryLock_rejectsZeroTtl() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        assertThrows(IllegalArgumentException.class,
                () -> lock.tryLock("d", "r", Duration.ZERO));
    }

    @Test
    void tryLock_rejectsNegativeTtl() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        assertThrows(IllegalArgumentException.class,
                () -> lock.tryLock("d", "r", Duration.ofSeconds(-1)));
    }

    @Test
    void withLock_rejectsNullAction() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        assertThrows(IllegalArgumentException.class,
                () -> lock.withLock("d", "r", Duration.ofSeconds(1), null));
    }

    @Test
    void buildKey_matchesExpectedFormat_forTypicalInputs() {
        // 不直接调用 private helper，通过 tryLock 的捕获 key 来验证格式不变量
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), any(), any(Duration.class))).thenReturn(true);

        lock.tryLock("user-service", "u-42", Duration.ofSeconds(1));

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOps).setIfAbsent(keyCaptor.capture(), any(), any(Duration.class));
        String key = keyCaptor.getValue();
        assertEquals("LOCK:user-service:u-42", key);
        assertTrue(key.matches("^LOCK:[^:]+:.+$"),
                "锁 Key 必须满足 LOCK:{domain}:{resourceId} 不变量, 实际=" + key);
    }

    @Test
    void unlock_argumentValidation() {
        assertThrows(IllegalArgumentException.class, () -> lock.unlock(null, "r"));
        assertThrows(IllegalArgumentException.class, () -> lock.unlock("", "r"));
        assertThrows(IllegalArgumentException.class, () -> lock.unlock("d", null));
        assertThrows(IllegalArgumentException.class, () -> lock.unlock("d", ""));
    }

    @Test
    void unlock_usesExpectedKeyFormat() {
        // 此测试确认 unlock 所构造的 key 与 tryLock 一致（可通过 List 捕获）
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), any(), any(Duration.class))).thenReturn(true);
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any())).thenReturn(1L);

        lock.tryLock("payment", "tx-9", Duration.ofSeconds(3));
        lock.unlock("payment", "tx-9");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> keysCaptor = ArgumentCaptor.forClass(List.class);
        verify(redisTemplate).execute(any(RedisScript.class), keysCaptor.capture(), any());
        assertEquals(Collections.singletonList("LOCK:payment:tx-9"), keysCaptor.getValue());
    }
}
