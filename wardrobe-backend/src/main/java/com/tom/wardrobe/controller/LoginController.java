package com.tom.wardrobe.controller;

import com.tom.wardrobe.annotation.RateLimit;
import com.tom.wardrobe.entity.LoginResponse;
import com.tom.wardrobe.entity.User;
import com.tom.wardrobe.service.UserService;
import com.tom.wardrobe.util.RedisUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@RestController
public class LoginController {

    @Resource
    private UserService userService;

    @Resource
    private RedisUtils redisUtils;

    /**
     * 登录失败多少次后需要验证码
     */
    private static final int MAX_FAILED_ATTEMPTS = 3;

    /**
     * 失败次数记录过期时间（秒）
     */
    private static final int FAILED_ATTEMPTS_EXPIRE_SECONDS = 60 * 5;

    @PostMapping("/register")
    @RateLimit(windowSeconds = 60, maxRequests = 10, message = "注册过于频繁，请稍后再试")
    public Object register(@RequestBody User user) {
        Map<String, Object> result = new HashMap<>();
        String msg = userService.register(user);
        if (msg.contains("成功")) {
            result.put("success", true);
            result.put("message", msg);
        } else {
            result.put("success", false);
            result.put("message", msg);
        }
        return result;
    }

    @PostMapping("/login")
    @RateLimit(windowSeconds = 60, maxRequests = 5, message = "登录失败次数过多，请稍后再试")
    public Object login(@RequestBody Map<String, String> params, HttpServletRequest request) {
        String userInfo = params.get("userInfo");
        String password = params.get("password");
        Boolean isAdminLogin = Boolean.valueOf(params.getOrDefault("isAdminLogin", "false"));

        // 获取客户端 IP
        String clientIp = getClientIp(request);
        
        // 构建失败次数 Key
        String failKey = "login_fail:" + clientIp;
        
        // 获取当前失败次数
        Integer failCount = (Integer) redisUtils.get(failKey);
        if (failCount == null) {
            failCount = 0;
        }

        // 如果失败超过 3 次，需要验证码
        if (failCount >= MAX_FAILED_ATTEMPTS) {
            String captchaId = params.get("captchaId");
            String captchaCode = params.get("captchaCode");
            
            if (!validateCaptcha(captchaId, captchaCode)) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", "验证码错误");
                result.put("needCaptcha", true);
                return result;
            }
        }

        User loginUser = userService.login(userInfo, password);
        
        if (loginUser == null) {
            // 登录失败，增加失败次数
            failCount++;
            redisUtils.set(failKey, failCount, FAILED_ATTEMPTS_EXPIRE_SECONDS);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "用户名或密码错误！");
            
            // 如果失败超过等于 3 次，提示需要验证码
            if (failCount >= MAX_FAILED_ATTEMPTS) {
                result.put("needCaptcha", true);
            }
            
            return result;
        }

        // 登录成功，清除失败次数记录
        redisUtils.del(failKey);

        if (isAdminLogin) {
            int role = loginUser.getRole();
            if (role != 1) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", "您没有权限！");
                return result;
            }
        }

        // 返回统一格式响应，包含用户信息（不包含密码等敏感信息）
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", LoginResponse.fromUser(loginUser));
        return result;
    }

    /**
     * 验证验证码
     */
    private boolean validateCaptcha(String captchaId, String captchaCode) {
        if (captchaId == null || captchaId.isBlank() || captchaCode == null || captchaCode.isBlank()) {
            return false;
        }
        
        String storedCode = (String) redisUtils.get("captcha:" + captchaId);
        
        if (storedCode == null) {
            return false;
        }
        
        // 验证成功后删除验证码
        if (storedCode.equalsIgnoreCase(captchaCode)) {
            redisUtils.del("captcha:" + captchaId);
            return true;
        }
        
        return false;
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
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}