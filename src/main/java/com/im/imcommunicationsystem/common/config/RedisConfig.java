package com.im.imcommunicationsystem.common.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis配置类
 * 配置Redis连接、序列化和缓存管理
 * 
 * @author IM Team
 * @version 1.0
 * @since 2024-01-01
 */
@Configuration
@EnableCaching
public class RedisConfig {

    /**
     * 配置RedisTemplate
     * 设置键值序列化方式
     * 
     * @param connectionFactory Redis连接工厂
     * @return RedisTemplate实例
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 使用Jackson2JsonRedisSerializer来序列化和反序列化redis的value值
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);

        // 使用StringRedisSerializer来序列化和反序列化redis的key值
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();

        // 设置key和value的序列化规则
        template.setKeySerializer(stringRedisSerializer);
        template.setValueSerializer(jackson2JsonRedisSerializer);
        template.setHashKeySerializer(stringRedisSerializer);
        template.setHashValueSerializer(jackson2JsonRedisSerializer);

        // 禁用事务支持，避免与Spring事务冲突
        template.setEnableTransactionSupport(false);
        template.afterPropertiesSet();

        return template;
    }

    /**
     * 配置缓存管理器
     * 设置缓存的默认过期时间和序列化方式
     * 
     * @param connectionFactory Redis连接工厂
     * @return CacheManager实例
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // 配置序列化
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);

        // 配置缓存
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30)) // 设置缓存过期时间为30分钟
                .serializeKeysWith(org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair.fromSerializer(jackson2JsonRedisSerializer))
                .disableCachingNullValues(); // 不缓存空值

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }

    /**
     * Redis缓存使用说明：
     * 
     * 1. 用户会话缓存：
     *    - Key: "user:session:{userId}"
     *    - Value: 用户会话信息
     *    - TTL: 24小时
     * 
     * 2. 验证码缓存：
     *    - Key: "verification:code:{email}:{type}"
     *    - Value: 验证码信息
     *    - TTL: 5分钟
     * 
     * 3. 用户在线状态：
     *    - Key: "user:online:{userId}"
     *    - Value: 在线状态信息
     *    - TTL: 30分钟
     * 
     * 4. 群组信息缓存：
     *    - Key: "group:info:{groupId}"
     *    - Value: 群组详细信息
     *    - TTL: 1小时
     * 
     * 5. 消息缓存：
     *    - Key: "message:recent:{conversationId}"
     *    - Value: 最近消息列表
     *    - TTL: 10分钟
     * 
     * 6. 文件上传临时信息：
     *    - Key: "file:upload:{uploadId}"
     *    - Value: 上传进度和状态
     *    - TTL: 1小时
     */

}