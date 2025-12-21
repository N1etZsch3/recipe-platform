package com.n1etzsch3.recipe.framework.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
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
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@EnableCaching
public class RedisConfig {

        /**
         * 创建支持 Java 8 日期时间的 ObjectMapper
         */
        private ObjectMapper createObjectMapper() {
                ObjectMapper om = new ObjectMapper();
                om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
                om.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
                                ObjectMapper.DefaultTyping.NON_FINAL,
                                JsonTypeInfo.As.PROPERTY);
                om.registerModule(new JavaTimeModule());
                return om;
        }

        @Bean
        public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
                RedisTemplate<String, Object> template = new RedisTemplate<>();
                template.setConnectionFactory(connectionFactory);

                // Key 使用 String 序列化
                template.setKeySerializer(new StringRedisSerializer());
                template.setHashKeySerializer(new StringRedisSerializer());

                // Value 使用 JSON 序列化
                Jackson2JsonRedisSerializer<Object> jsonSerializer = new Jackson2JsonRedisSerializer<>(
                                createObjectMapper(), Object.class);

                template.setValueSerializer(jsonSerializer);
                template.setHashValueSerializer(jsonSerializer);
                template.afterPropertiesSet();

                return template;
        }

        @Bean
        public CacheManager cacheManager(RedisConnectionFactory factory) {
                // 使用带 JavaTimeModule 的序列化器
                Jackson2JsonRedisSerializer<Object> jsonSerializer = new Jackson2JsonRedisSerializer<>(
                                createObjectMapper(), Object.class);

                RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofHours(1))
                                .serializeKeysWith(RedisSerializationContext.SerializationPair
                                                .fromSerializer(new StringRedisSerializer()))
                                .serializeValuesWith(RedisSerializationContext.SerializationPair
                                                .fromSerializer(jsonSerializer))
                                .disableCachingNullValues();

                return RedisCacheManager.builder(factory)
                                .cacheDefaults(defaultConfig)
                                .withCacheConfiguration("categories",
                                                defaultConfig.entryTtl(Duration.ofHours(24)))
                                .withCacheConfiguration("dashboard",
                                                defaultConfig.entryTtl(Duration.ofMinutes(5)))
                                .withCacheConfiguration("user",
                                                defaultConfig.entryTtl(Duration.ofMinutes(30)))
                                .withCacheConfiguration("recipe",
                                                defaultConfig.entryTtl(Duration.ofHours(2)))
                                .build();
        }
}
