package com.n1etzsch3.recipe.framework.annotation;

import java.lang.annotation.*;

/**
 * 接口限流注解
 * 基于 Redis 实现的滑动窗口限流
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 限流 key 前缀，默认使用方法名
     */
    String key() default "";

    /**
     * 时间窗口（秒）
     */
    int time() default 60;

    /**
     * 时间窗口内允许的最大请求次数
     */
    int count() default 100;

    /**
     * 限流类型
     */
    LimitType limitType() default LimitType.IP;

    enum LimitType {
        /**
         * 根据 IP 限流
         */
        IP,
        /**
         * 根据用户 ID 限流（需要登录）
         */
        USER,
        /**
         * 全局限流
         */
        GLOBAL
    }
}
