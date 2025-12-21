package com.n1etzsch3.recipe.framework.service;

import com.n1etzsch3.recipe.common.constant.CacheConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 登录尝试限制服务
 * 防止暴力破解密码
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 最大失败次数
     */
    private static final int MAX_ATTEMPTS = 5;

    /**
     * 锁定时间（秒）
     */
    private static final long LOCK_TIME = 900; // 15分钟

    /**
     * 失败记录过期时间（秒）
     */
    private static final long ATTEMPT_TTL = 600; // 10分钟

    /**
     * 记录登录失败
     * 
     * @param username 用户名
     */
    public void loginFailed(String username) {
        String key = CacheConstants.KEY_LOGIN_FAIL + username;
        Integer attempts = (Integer) redisTemplate.opsForValue().get(key);

        if (attempts == null) {
            attempts = 1;
        } else {
            attempts = attempts + 1;
        }

        redisTemplate.opsForValue().set(key, attempts, ATTEMPT_TTL, TimeUnit.SECONDS);
        log.warn("登录失败记录: username={}, attempts={}", username, attempts);

        // 达到最大次数时锁定账户
        if (attempts >= MAX_ATTEMPTS) {
            String lockKey = CacheConstants.KEY_LOGIN_FAIL + "lock:" + username;
            redisTemplate.opsForValue().set(lockKey, "1", LOCK_TIME, TimeUnit.SECONDS);
            log.warn("账户已锁定: username={}, lockTime={}s", username, LOCK_TIME);
        }
    }

    /**
     * 登录成功，清除失败记录
     * 
     * @param username 用户名
     */
    public void loginSuccess(String username) {
        String key = CacheConstants.KEY_LOGIN_FAIL + username;
        String lockKey = CacheConstants.KEY_LOGIN_FAIL + "lock:" + username;
        redisTemplate.delete(key);
        redisTemplate.delete(lockKey);
    }

    /**
     * 检查账户是否被锁定
     * 
     * @param username 用户名
     * @return true 表示已锁定
     */
    public boolean isLocked(String username) {
        String lockKey = CacheConstants.KEY_LOGIN_FAIL + "lock:" + username;
        Boolean exists = redisTemplate.hasKey(lockKey);
        return Boolean.TRUE.equals(exists);
    }

    /**
     * 获取剩余锁定时间（秒）
     */
    public long getRemainingLockTime(String username) {
        String lockKey = CacheConstants.KEY_LOGIN_FAIL + "lock:" + username;
        Long ttl = redisTemplate.getExpire(lockKey, TimeUnit.SECONDS);
        return ttl != null && ttl > 0 ? ttl : 0;
    }

    /**
     * 获取失败次数
     */
    public int getFailedAttempts(String username) {
        String key = CacheConstants.KEY_LOGIN_FAIL + username;
        Integer attempts = (Integer) redisTemplate.opsForValue().get(key);
        return attempts != null ? attempts : 0;
    }
}
