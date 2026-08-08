package com.tom.wardrobe.annotation;

import java.lang.annotation.*;

/**
 * 限流注解
 * 使用 Redis 滑动窗口算法实现限流
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 限流时间窗口（秒）
     */
    int windowSeconds() default 60;

    /**
     * 时间窗口内允许的最大请求数
     */
    int maxRequests() default 100;

    /**
     * 限流提示消息
     */
    String message() default "请求过于频繁，请稍后再试";
}