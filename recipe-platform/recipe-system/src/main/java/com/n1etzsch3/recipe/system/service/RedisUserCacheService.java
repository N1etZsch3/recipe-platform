package com.n1etzsch3.recipe.system.service;

import com.n1etzsch3.recipe.common.constant.CacheConstants;
import com.n1etzsch3.recipe.system.entity.SysUser;
import com.n1etzsch3.recipe.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 用户信息 Redis 缓存服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisUserCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final SysUserMapper userMapper;

    /**
     * 获取用户信息（优先从缓存）
     */
    public SysUser getUserById(Long userId) {
        if (userId == null) {
            return null;
        }

        String key = CacheConstants.KEY_USER_INFO + userId;
        SysUser user = (SysUser) redisTemplate.opsForValue().get(key);

        if (user == null) {
            user = userMapper.selectById(userId);
            if (user != null) {
                // 脱敏处理
                user.setPassword(null);
                redisTemplate.opsForValue().set(key, user,
                        CacheConstants.TTL_USER, TimeUnit.SECONDS);
            }
        }
        return user;
    }

    /**
     * 批量获取用户信息（优先从缓存）
     */
    public Map<Long, SysUser> batchGetUsers(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // 1. 构建 keys
        List<Long> idList = new ArrayList<>(userIds);
        List<String> keys = idList.stream()
                .map(id -> CacheConstants.KEY_USER_INFO + id)
                .collect(Collectors.toList());

        // 2. 批量从 Redis 获取
        List<Object> cachedUsers = redisTemplate.opsForValue().multiGet(keys);

        Map<Long, SysUser> result = new HashMap<>();
        List<Long> missIds = new ArrayList<>();

        // 3. 处理缓存结果
        for (int i = 0; i < idList.size(); i++) {
            Object cached = cachedUsers != null ? cachedUsers.get(i) : null;
            if (cached != null) {
                result.put(idList.get(i), (SysUser) cached);
            } else {
                missIds.add(idList.get(i));
            }
        }

        // 4. 从数据库查询未命中的
        if (!missIds.isEmpty()) {
            List<SysUser> dbUsers = userMapper.selectBatchIds(missIds);
            for (SysUser user : dbUsers) {
                user.setPassword(null);
                result.put(user.getId(), user);
                // 写入缓存
                redisTemplate.opsForValue().set(
                        CacheConstants.KEY_USER_INFO + user.getId(),
                        user,
                        CacheConstants.TTL_USER,
                        TimeUnit.SECONDS);
            }
        }

        return result;
    }

    /**
     * 清除用户缓存
     */
    public void evictUser(Long userId) {
        if (userId != null) {
            redisTemplate.delete(CacheConstants.KEY_USER_INFO + userId);
        }
    }

    /**
     * 更新用户缓存
     */
    public void updateUserCache(SysUser user) {
        if (user != null && user.getId() != null) {
            SysUser cached = new SysUser();
            cached.setId(user.getId());
            cached.setUsername(user.getUsername());
            cached.setNickname(user.getNickname());
            cached.setAvatar(user.getAvatar());
            cached.setIntro(user.getIntro());
            cached.setRole(user.getRole());
            cached.setStatus(user.getStatus());
            cached.setCreateTime(user.getCreateTime());
            // 不缓存密码
            redisTemplate.opsForValue().set(
                    CacheConstants.KEY_USER_INFO + user.getId(),
                    cached,
                    CacheConstants.TTL_USER,
                    TimeUnit.SECONDS);
        }
    }
}
