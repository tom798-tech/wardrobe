package com.tom.wardrobe.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码哈希工具类
 * 用于生成 BCrypt 哈希值
 */
public class PasswordHashUtil {

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        // 生成测试密码的哈希值
        System.out.println("admin: " + encoder.encode("Admin@2026"));
        System.out.println("123123: " + encoder.encode("ZhangSan@2026"));
        System.out.println("111111: " + encoder.encode("LiSi@2026"));
    }
}