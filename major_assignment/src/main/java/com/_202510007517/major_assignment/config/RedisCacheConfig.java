package com._202510007517.major_assignment.config;

import com._202510007517.major_assignment.constants.CacheConstants;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis 缓存配置。
 * <p>
 * 每个缓存名称必须在此处显式声明 TTL（R5.2），禁止依赖默认 1 小时。
 * 新增缓存名称时，同时需要在 {@link CacheConstants} 声明名称和 *_TTL 常量，
 * 并在此 {@code cacheManager} 的 initial configurations map 中注册。
 * </p>
 */
@Configuration
@EnableCaching
public class RedisCacheConfig {

    /**
     * 兜底 TTL：未在 {@link #cacheManager(RedisConnectionFactory)} 中显式声明的缓存名
     * 将落到此配置。设置为短 TTL（5 分钟）而不是 1 小时，确保忘记声明的缓存
     * "fail-safe short"，不会长期漂移。
     */
    private static final Duration FALLBACK_TTL = Duration.ofSeconds(CacheConstants.SHORT_TTL);

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 使用Jackson2JsonRedisSerializer来序列化和反序列化redis的value值
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        mapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(mapper, Object.class);

        // 使用StringRedisSerializer来序列化和反序列化redis的key值
        RedisSerializer<String> stringSerializer = new StringRedisSerializer();

        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(serializer);
        template.afterPropertiesSet();

        return template;
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // 配置Jackson序列化器
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        mapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(mapper, Object.class);

        // 公共的 key/value 序列化 + 禁止缓存 null（R5.2）
        RedisCacheConfiguration baseConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .disableCachingNullValues();

        // 兜底配置：未显式声明 TTL 的缓存名命中此值，采用短 TTL 避免长期漂移
        RedisCacheConfiguration defaults = baseConfig.entryTtl(FALLBACK_TTL);

        // 每个 legacy 缓存名都必须显式声明 TTL（R5.2 禁止默认）
        Map<String, RedisCacheConfiguration> perCacheConfigs = new HashMap<>();
        perCacheConfigs.put(CacheConstants.USERS,
                baseConfig.entryTtl(Duration.ofSeconds(CacheConstants.USER_PROFILE_TTL)));
        perCacheConfigs.put(CacheConstants.USER_ROLES,
                baseConfig.entryTtl(Duration.ofSeconds(CacheConstants.USER_ROLES_TTL)));
        perCacheConfigs.put(CacheConstants.COURSES,
                baseConfig.entryTtl(Duration.ofSeconds(CacheConstants.COURSE_LIST_TTL)));
        perCacheConfigs.put(CacheConstants.ASSIGNMENTS,
                baseConfig.entryTtl(Duration.ofSeconds(CacheConstants.SHORT_TTL)));
        perCacheConfigs.put(CacheConstants.EXAMS,
                baseConfig.entryTtl(Duration.ofSeconds(CacheConstants.SHORT_TTL)));
        perCacheConfigs.put(CacheConstants.KNOWLEDGE_POINTS,
                baseConfig.entryTtl(Duration.ofSeconds(CacheConstants.KP_MASTERY_TTL)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaults)
                .withInitialCacheConfigurations(perCacheConfigs)
                .build();
    }
}
