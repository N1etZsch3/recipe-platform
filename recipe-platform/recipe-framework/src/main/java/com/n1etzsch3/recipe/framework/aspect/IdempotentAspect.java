package com.n1etzsch3.recipe.framework.aspect;

import com.n1etzsch3.recipe.common.core.domain.Result;
import com.n1etzsch3.recipe.framework.annotation.Idempotent;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 接口幂等性切面
 * 通过消费 Token 实现防重复提交
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class IdempotentAspect {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String IDEMPOTENT_KEY_PREFIX = "idempotent:";
    private static final String HEADER_IDEMPOTENT_TOKEN = "X-Idempotent-Token";

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint point, Idempotent idempotent) throws Throwable {
        HttpServletRequest request = getRequest();
        if (request == null) {
            return point.proceed();
        }

        String token = request.getHeader(HEADER_IDEMPOTENT_TOKEN);

        // Token为空则不做校验（兼容旧版本前端）
        if (!StringUtils.hasText(token)) {
            log.debug("幂等性Token为空，跳过校验");
            return point.proceed();
        }

        String key = IDEMPOTENT_KEY_PREFIX + token;

        // 尝试删除Token，删除成功表示首次请求
        Boolean deleted = redisTemplate.delete(key);

        if (Boolean.FALSE.equals(deleted)) {
            log.warn("幂等性校验失败，重复提交: token={}", token);
            return Result.fail(400, idempotent.message());
        }

        log.info("幂等性校验通过: token={}", token);
        return point.proceed();
    }

    private HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
}
