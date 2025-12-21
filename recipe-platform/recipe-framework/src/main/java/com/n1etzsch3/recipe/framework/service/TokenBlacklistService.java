package com.n1etzsch3.recipe.framework.service;

import com.n1etzsch3.recipe.common.constant.CacheConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Token 黑名单服务
 * 用于实现 JWT Token 注销功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 默认黑名单过期时间（秒）
     * 应该与 JWT Token 的过期时间一致
     */
    private static final long DEFAULT_BLACKLIST_TTL = 86400; // 24小时

    /**
     * 将 Token 加入黑名单
     * 
     * @param jti           JWT 唯一标识
     * @param expireSeconds Token 剩余过期时间（秒）
     */
    public void addToBlacklist(String jti, long expireSeconds) {
        if (jti == null || jti.isEmpty()) {
            return;
        }
        String key = CacheConstants.KEY_TOKEN_BLACKLIST + jti;
        long ttl = expireSeconds > 0 ? expireSeconds : DEFAULT_BLACKLIST_TTL;
        redisTemplate.opsForValue().set(key, "1", ttl, TimeUnit.SECONDS);
        log.info("Token已加入黑名单: jti={}", jti);
    }

    /**
     * 将 Token 加入黑名单（使用默认过期时间）
     */
    public void addToBlacklist(String jti) {
        addToBlacklist(jti, DEFAULT_BLACKLIST_TTL);
    }

    /**
     * 检查 Token 是否在黑名单中
     * 
     * @param jti JWT 唯一标识
     * @return true 表示在黑名单中（已注销）
     */
    public boolean isBlacklisted(String jti) {
        if (jti == null || jti.isEmpty()) {
            return false;
        }
        String key = CacheConstants.KEY_TOKEN_BLACKLIST + jti;
        Boolean exists = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(exists);
    }
}
