package com.tom.wardrobe.annotation;

import java.lang.annotation.*;

/**
 * 幂等性注解
 * 
 * 用于标记需要保证幂等性的接口方法
 * 
 * 使用方式：
 * @Idempotent(timeout = 60)
 * public String createOrder(Order order) { ... }
 * 
 * 原理：
 * 1. 客户端请求前先获取 Token（GET /api/idempotent/token）
 * 2. 请求时在 Header 中携带 X-Idempotent-Token
 * 3. 服务端校验 Token：
 *    - Token 不存在：返回 400，提示需要获取 Token
 *    - Token 已使用：返回 409，提示重复请求
 *    - Token 有效：使用 Token，标记为已使用
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    /**
     * Token 过期时间（秒）
     * 默认 60 秒
     */
    int timeout() default 60;

    /**
     * 是否需要用户登录
     * 默认 true，只有登录用户才能获取和使用 Token
     */
    boolean requireLogin() default true;

    /**
     * 重复请求时的提示消息
     */
    String message() default "请勿重复提交！";
}
