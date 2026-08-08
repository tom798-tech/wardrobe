package com.tom.wardrobe.interceptor;

import com.tom.wardrobe.annotation.RateLimit;
import com.tom.wardrobe.util.RedisUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.lang.reflect.Method;
import java.util.Set;

/**
 * 限流拦截器
 * 使用 Redis ZSET 实现滑动窗口限流算法
 * 
 * 滑动窗口算法原理：
 * 1. 使用 ZSET 存储请求时间戳作为 score 和 member
 * 2. 每次请求时，移除窗口时间之外的旧请求（ZREMRANGEBYSCORE）
 * 3. 统计当前窗口内的请求数量（ZCARD）
 * 4. 如果超过阈值则拒绝请求
 * 5. 否则将当前请求时间戳加入 ZSET
 * 
 * 优点：解决固定窗口算法在边界时刻可能出现2倍流量穿透的问题
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);

    private final RedisUtils redisUtils;

    public RateLimitInterceptor(RedisUtils redisUtils) {
        this.redisUtils = redisUtils;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 只拦截方法级别
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        Method method = handlerMethod.getMethod();

        // 检查是否有限流注解
        RateLimit rateLimit = method.getAnnotation(RateLimit.class);
        if (rateLimit == null) {
            return true;
        }

        // 获取限流参数
        int windowSeconds = rateLimit.windowSeconds();
        int maxRequests = rateLimit.maxRequests();
        String message = rateLimit.message();

        // 获取客户端 IP
        String clientIp = getClientIp(request);

        // 构建 Redis Key（格式：rate_limit:{ip}:{method}）
        String key = String.format("rate_limit:%s:%s", clientIp, method.getName());

        try {
            // 当前时间戳（毫秒）
            long currentTime = System.currentTimeMillis();
            
            // 窗口开始时间（毫秒）= 当前时间 - 窗口大小
            long windowStart = currentTime - (windowSeconds * 1000L);

            // 步骤1：移除窗口时间之外的旧请求（保留窗口内的请求）
            redisUtils.zremrangebyscore(key, 0, windowStart);

            // 步骤2：统计当前窗口内的请求数量
            long count = redisUtils.zcard(key);

            // 步骤3：判断是否超过限流阈值
            if (count >= maxRequests) {
                log.warn("限流触发: ip={}, method={}, window={}s, max={}, current={}", 
                        clientIp, method.getName(), windowSeconds, maxRequests, count);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":429,\"message\":\"" + message + "\"}");
                response.setStatus(429);
                return false;
            }

            // 步骤4：将当前请求时间戳加入 ZSET
            // 使用时间戳作为 score 和 member，确保唯一性（添加随机后缀）
            redisUtils.zadd(key, currentTime, String.valueOf(currentTime) + ":" + System.nanoTime());

            // 步骤5：设置 ZSET 过期时间，避免内存泄漏（窗口时间的2倍）
            redisUtils.expire(key, windowSeconds * 2);

        } catch (Exception e) {
            log.error("限流处理失败", e);
            // Redis 异常时不阻断请求
        }

        return true;
    }

    /**
     * 获取客户端真实 IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 如果是多个代理，取第一个 IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
