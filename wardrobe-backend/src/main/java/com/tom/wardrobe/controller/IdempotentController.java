package com.tom.wardrobe.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.tom.wardrobe.util.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 幂等性 Token 控制器
 * 
 * 提供幂等性 Token 的获取接口
 * 
 * 使用流程：
 * 1. 客户端请求 GET /api/idempotent/token 获取 Token
 * 2. 在后续请求的 Header 中携带 X-Idempotent-Token: {token}
 * 3. 服务端校验 Token，确保请求只执行一次
 */
@Slf4j
@RestController
@RequestMapping("/api/idempotent")
public class IdempotentController {

    @Resource
    private RedisUtils redisUtils;

    /**
     * 幂等性 Token 前缀
     */
    private static final String IDEMPOTENT_TOKEN_PREFIX = "idempotent:";

    /**
     * 默认 Token 过期时间（秒）
     */
    private static final int DEFAULT_TIMEOUT = 60;

    /**
     * 获取幂等性 Token
     * 
     * @param timeout Token 过期时间（秒），可选，默认 60 秒
     * @return Token 信息
     */
    @GetMapping("/token")
    public ResponseEntity<Map<String, Object>> getToken(
            @RequestParam(value = "timeout", defaultValue = "60") int timeout) {
        
        // 获取当前登录用户 ID
        Integer userId = null;
        if (StpUtil.isLogin()) {
            Object loginId = StpUtil.getLoginId();
            if (loginId instanceof Integer) {
                userId = (Integer) loginId;
            } else if (loginId instanceof String) {
                userId = Integer.parseInt((String) loginId);
            }
        }

        // 生成 Token
        String token = UUID.randomUUID().toString().replace("-", "");
        String tokenKey = IDEMPOTENT_TOKEN_PREFIX + token;
        
        // 存储 Token，值为用户 ID（用于验证 Token 归属）
        String tokenValue = userId != null ? userId.toString() : "anonymous";
        redisUtils.set(tokenKey, tokenValue, timeout);

        log.debug("生成幂等性 Token，token: {}, userId: {}, timeout: {}s", token, userId, timeout);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("token", token);
        result.put("timeout", timeout);

        return ResponseEntity.ok(result);
    }

    /**
     * 验证幂等性 Token 是否有效
     * 
     * @param token 要验证的 Token
     * @return Token 状态
     */
    @GetMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyToken(@RequestParam String token) {
        String tokenKey = IDEMPOTENT_TOKEN_PREFIX + token;
        Object tokenValue = redisUtils.get(tokenKey);

        Map<String, Object> result = new HashMap<>();
        if (tokenValue != null) {
            result.put("code", 200);
            result.put("message", "Token 有效");
            result.put("valid", true);
        } else {
            result.put("code", 400);
            result.put("message", "Token 无效或已过期");
            result.put("valid", false);
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 手动删除幂等性 Token（用于取消操作）
     * 
     * @param token 要删除的 Token
     * @return 删除结果
     */
    @DeleteMapping("/token")
    public ResponseEntity<Map<String, Object>> deleteToken(@RequestParam String token) {
        String tokenKey = IDEMPOTENT_TOKEN_PREFIX + token;
        redisUtils.del(tokenKey);

        log.debug("删除幂等性 Token，token: {}", token);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "删除成功");

        return ResponseEntity.ok(result);
    }
}
