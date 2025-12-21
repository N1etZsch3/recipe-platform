package com.n1etzsch3.recipe.framework.annotation;

import java.lang.annotation.*;

/**
 * 接口幂等性注解
 * 基于 Redis Token 机制实现防重复提交
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    /**
     * Token过期时间（秒）
     */
    int expireTime() default 300;

    /**
     * 重复提交时的提示消息
     */
    String message() default "请勿重复提交";
}
