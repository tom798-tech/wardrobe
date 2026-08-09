package com.tom.wardrobe.interceptor;

import cn.dev33.satoken.stp.StpUtil;
import com.tom.wardrobe.annotation.Idempotent;
import com.tom.wardrobe.util.RedisUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.annotation.Resource;
import java.io.IOException;

/**
 * 幂等性拦截器
 * 
 * 用于拦截带有 @Idempotent 注解的接口，校验请求的幂等性 Token
 */
@Slf4j
@Component
public class IdempotentInterceptor implements HandlerInterceptor {

    @Resource
    private RedisUtils redisUtils;

    /**
     * 幂等性 Token Header 名称
     */
    private static final String IDEMPOTENT_TOKEN_HEADER = "X-Idempotent-Token";

    /**
     * 幂等性 Token 前缀
     */
    private static final String IDEMPOTENT_TOKEN_PREFIX = "idempotent:";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 仅处理 Controller 方法
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        Idempotent idempotent = handlerMethod.getMethodAnnotation(Idempotent.class);

        if (idempotent == null) {
            return true;
        }

        // 检查是否需要登录
        if (idempotent.requireLogin() && !StpUtil.isLogin()) {
            writeResponse(response, HttpStatus.UNAUTHORIZED, "请先登录！");
            return false;
        }

        // 获取 Token
        String token = request.getHeader(IDEMPOTENT_TOKEN_HEADER);
        if (token == null || token.isEmpty()) {
            writeResponse(response, HttpStatus.BAD_REQUEST, "缺少幂等性 Token，请先获取！");
            return false;
        }

        // 校验 Token
        String tokenKey = IDEMPOTENT_TOKEN_PREFIX + token;
        Object tokenValue = redisUtils.get(tokenKey);

        if (tokenValue == null) {
            // Token 不存在或已过期
            writeResponse(response, HttpStatus.BAD_REQUEST, "Token 无效或已过期，请重新获取！");
            return false;
        }

        // 检查 Token 是否已使用（使用 Lua 脚本原子性检查并删除）
        // Token 是高熵随机值，存在即可代表未使用；删除成功后同一 Token 不能再次提交。
        String luaScript =
                "if redis.call('exists', KEYS[1]) == 1 then\n" +
                "    redis.call('del', KEYS[1])\n" +
                "    return 1\n" +
                "end\n" +
                "return 0";

        Object result = redisUtils.executeLua(luaScript,
                new String[]{tokenKey},
                new String[]{});

        if (result == null || !"1".equals(result.toString())) {
            // Token 已被使用
            writeResponse(response, HttpStatus.CONFLICT, idempotent.message());
            return false;
        }

        log.debug("幂等性校验通过，token: {}", token);
        return true;
    }

    /**
     * 写入响应
     */
    private void writeResponse(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"code\": " + status.value() + ", \"message\": \"" + message + "\"}");
    }
}
