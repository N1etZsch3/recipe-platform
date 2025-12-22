package com.n1etzsch3.recipe.framework.service;

import com.n1etzsch3.recipe.common.constant.CacheConstants;
import com.n1etzsch3.recipe.framework.websocket.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 用户在线状态服务
 * 使用 Redis 跟踪用户在线状态
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserOnlineService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final WebSocketSessionManager sessionManager;

    private static final String KEY_ONLINE_USERS = CacheConstants.KEY_PREFIX + "online:users";
    private static final String KEY_USER_SESSION = CacheConstants.KEY_PREFIX + "session:";
    private static final long HEARTBEAT_TIMEOUT = 60; // 60秒无心跳视为离线

    /**
     * 用户上线/心跳
     */
    public void heartbeat(Long userId) {
        String key = KEY_USER_SESSION + userId;
        redisTemplate.opsForValue().set(key, System.currentTimeMillis(), HEARTBEAT_TIMEOUT, TimeUnit.SECONDS);
        redisTemplate.opsForSet().add(KEY_ONLINE_USERS, userId.toString());
    }

    /**
     * 用户下线
     */
    public void offline(Long userId) {
        String key = KEY_USER_SESSION + userId;
        redisTemplate.delete(key);
        redisTemplate.opsForSet().remove(KEY_ONLINE_USERS, userId.toString());
    }

    /**
     * 检查用户是否在线
     */
    public boolean isOnline(Long userId) {
        String key = KEY_USER_SESSION + userId;
        return redisTemplate.hasKey(key);
    }

    /**
     * 获取所有在线用户ID
     */
    public Set<Long> getOnlineUserIds() {
        Set<Object> members = redisTemplate.opsForSet().members(KEY_ONLINE_USERS);
        if (members == null || members.isEmpty()) {
            return Collections.emptySet();
        }

        Set<Long> result = new HashSet<>();
        for (Object member : members) {
            try {
                Long userId = Long.parseLong(member.toString());
                // 双重检查是否真的在线
                if (isOnline(userId)) {
                    result.add(userId);
                } else {
                    // 清理过期数据
                    redisTemplate.opsForSet().remove(KEY_ONLINE_USERS, member);
                }
            } catch (NumberFormatException e) {
                log.warn("Invalid user ID in online set: {}", member);
            }
        }
        return result;
    }

    /**
     * 批量检查用户在线状态
     */
    public Map<Long, Boolean> batchCheckOnline(List<Long> userIds) {
        Map<Long, Boolean> result = new HashMap<>();
        for (Long userId : userIds) {
            result.put(userId, isOnline(userId));
        }
        return result;
    }

    /**
     * 踢用户下线
     * 
     * @param userId 要踢下线的用户ID
     */
    public void kickUser(Long userId) {
        log.info("管理员踢用户下线: userId={}", userId);
        // 1. 先关闭 WebSocket 连接（会发送 FORCED_LOGOUT 消息）
        sessionManager.closeAllSessions(userId, "您已被管理员强制下线");
        // 2. 清除 Redis 在线状态
        offline(userId);
    }
}
