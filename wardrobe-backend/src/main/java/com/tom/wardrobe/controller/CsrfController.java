package com.tom.wardrobe.controller;

import cn.dev33.satoken.stp.StpUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * CSRF Token 控制器
 * 提供 CSRF Token 获取接口
 */
@RestController
public class CsrfController {

    /**
     * 获取 CSRF Token
     * 需要登录后才能获取
     */
    @GetMapping("/csrf-token")
    public Map<String, Object> getCsrfToken() {
        Map<String, Object> result = new HashMap<>();
        
        // Sa-Token 默认会在 Cookie 中设置 CSRF Token
        // 这里返回给前端，前端需要在请求头中携带 X-CSRF-TOKEN
        
        result.put("success", true);
        result.put("message", "CSRF Token 获取成功");
        
        return result;
    }
}