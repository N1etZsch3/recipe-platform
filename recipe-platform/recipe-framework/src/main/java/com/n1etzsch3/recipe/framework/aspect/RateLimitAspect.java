package com.n1etzsch3.recipe.framework.aspect;

import com.n1etzsch3.recipe.common.constant.CacheConstants;
import com.n1etzsch3.recipe.common.context.UserContext;
import com.n1etzsch3.recipe.common.core.domain.Result;
import com.n1etzsch3.recipe.framework.annotation.RateLimit;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * 接口限流切面
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final RedisTemplate<String, Object> redisTemplate;

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint point, RateLimit rateLimit) throws Throwable {
        String key = buildKey(point, rateLimit);
        int count = rateLimit.count();
        int time = rateLimit.time();

        // 获取当前计数
        Integer current = (Integer) redisTemplate.opsForValue().get(key);

        if (current == null) {
            // 首次访问，设置计数为1
            redisTemplate.opsForValue().set(key, 1, time, TimeUnit.SECONDS);
        } else if (current >= count) {
            // 超过限制
            log.warn("接口限流触发: key={}, current={}, limit={}", key, current, count);
            return Result.fail("请求过于频繁，请稍后再试");
        } else {
            // 计数加1
            redisTemplate.opsForValue().increment(key, 1);
        }

        return point.proceed();
    }

    /**
     * 构建限流 key
     */
    private String buildKey(ProceedingJoinPoint point, RateLimit rateLimit) {
        StringBuilder sb = new StringBuilder(CacheConstants.KEY_RATE_LIMIT);

        // 获取方法签名
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();

        // key 前缀
        String prefix = StringUtils.hasText(rateLimit.key())
                ? rateLimit.key()
                : method.getDeclaringClass().getSimpleName() + ":" + method.getName();
        sb.append(prefix).append(":");

        // 根据限流类型添加标识
        switch (rateLimit.limitType()) {
            case IP:
                sb.append(getClientIp());
                break;
            case USER:
                Long userId = UserContext.getUserId();
                sb.append(userId != null ? userId : getClientIp());
                break;
            case GLOBAL:
                sb.append("global");
                break;
        }

        return sb.toString();
    }

    /**
     * 获取客户端 IP
     */
    private String getClientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "unknown";
        }
        HttpServletRequest request = attributes.getRequest();

        String ip = request.getHeader("X-Forwarded-For");
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 处理多级代理
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip != null ? ip : "unknown";
    }
}
